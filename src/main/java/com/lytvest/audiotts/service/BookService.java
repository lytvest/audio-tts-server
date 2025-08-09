package com.lytvest.audiotts.service;

import com.lytvest.audiotts.dto.*;
import com.lytvest.audiotts.dto.request.BookUploadRequest;
import com.lytvest.audiotts.model.entity.*;
import com.lytvest.audiotts.model.enums.ChapterStatus;
import com.lytvest.audiotts.model.enums.SentenceStatus;
import com.lytvest.audiotts.repository.*;
import com.lytvest.audiotts.service.queue.CharacterDeterminationTask;
import com.lytvest.audiotts.service.queue.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {
    
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final CharacterRepository characterRepository;
    private final SentenceRepository sentenceRepository;
    private final Fb2ParserService fb2ParserService;
    private final QueueService queueService;
    
    @Value("${app.storage.books-path}")
    private String booksStoragePath;
    
    /**
     * Загружает и обрабатывает FB2 файл
     */
    @Transactional
    public BookDto uploadBook(BookUploadRequest request) {
        try {
            MultipartFile file = request.getFile();
            
            // Проверяем тип файла
            if (!file.getOriginalFilename().toLowerCase().endsWith(".fb2")) {
                throw new IllegalArgumentException("Only FB2 files are supported");
            }
            
            // Создаем директорию для хранения файлов если не существует
            Path storagePath = Paths.get(booksStoragePath);
            Files.createDirectories(storagePath);
            
            // Сохраняем файл
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = storagePath.resolve(filename);
            file.transferTo(filePath);
            
            // Парсим FB2 файл
            Fb2ParserService.ParsedBook parsedBook = fb2ParserService.parseFb2File(file.getBytes());
            
            // Создаем сущность книги
            Book book = new Book();
            book.setTitle(request.getTitle() != null ? request.getTitle() : parsedBook.title);
            book.setAuthor(request.getAuthor() != null ? request.getAuthor() : parsedBook.author);
            book.setOriginalFilename(file.getOriginalFilename());
            book.setFilePath(filePath.toString());
            
            book = bookRepository.save(book);
            
            // Создаем главы и предложения
            for (Fb2ParserService.ParsedChapter parsedChapter : parsedBook.chapters) {
                Chapter chapter = new Chapter();
                chapter.setBook(book);
                chapter.setTitle(parsedChapter.title);
                chapter.setChapterNumber(parsedChapter.chapterNumber);
                chapter.setStatus(ChapterStatus.WAITING);
                
                chapter = chapterRepository.save(chapter);
                
                // Создаем предложения
                int sentenceNumber = 1;
                for (String sentenceText : parsedChapter.sentences) {
                    Sentence sentence = new Sentence();
                    sentence.setChapter(chapter);
                    sentence.setSentenceNumber(sentenceNumber++);
                    sentence.setOriginalText(sentenceText);
                    sentence.setStatus(SentenceStatus.WAITING_FOR_CHARACTER);
                    
                    sentence = sentenceRepository.save(sentence);
                    
                    // Добавляем задачу определения персонажа в очередь
                    List<String> existingCharacters = characterRepository.findByBookId(book.getId())
                            .stream()
                            .map(CharacterBook::getName)
                            .collect(Collectors.toList());
                    
                    CharacterDeterminationTask task = new CharacterDeterminationTask(
                            sentence.getId(),
                            sentenceText,
                            existingCharacters,
                            book.getId()
                    );
                    queueService.addCharacterDeterminationTask(task);
                }
                
                // Устанавливаем статус главы "в работе"
                chapter.setStatus(ChapterStatus.IN_PROGRESS);
                chapterRepository.save(chapter);
            }
            
            log.info("Successfully uploaded book: {} with {} chapters", book.getTitle(), parsedBook.chapters.size());
            
            return convertToDto(book);
            
        } catch (IOException e) {
            log.error("Error uploading book", e);
            throw new RuntimeException("Failed to upload book", e);
        }
    }
    
    /**
     * Получает список всех книг
     */
    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Получает книгу по ID с главами
     */
    public Optional<BookDto> getBookById(Long bookId) {
        return bookRepository.findByIdWithChapters(bookId)
                .map(this::convertToDtoWithChapters);
    }
    
    /**
     * Получает книгу по ID с персонажами
     */
    public Optional<BookDto> getBookWithCharacters(Long bookId) {
        return bookRepository.findByIdWithCharacters(bookId)
                .map(this::convertToDtoWithCharacters);
    }
    
    /**
     * Удаляет книгу
     */
    @Transactional
    public void deleteBook(Long bookId) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();
            
            // Удаляем файл
            try {
                Path filePath = Paths.get(book.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Could not delete file: {}", book.getFilePath(), e);
            }
            
            // Удаляем из базы данных (каскадно удалятся главы, предложения и персонажи)
            bookRepository.delete(book);
            
            log.info("Deleted book: {}", book.getTitle());
        }
    }
    
    /**
     * Получает статистику обработки книги
     */
    public BookProcessingStats getBookProcessingStats(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        
        List<Chapter> chapters = chapterRepository.findByBookIdOrderByChapterNumber(bookId);
        
        int totalChapters = chapters.size();
        int readyChapters = (int) chapters.stream()
                .filter(c -> c.getStatus() == ChapterStatus.READY)
                .count();
        
        long totalSentences = 0;
        long readySentences = 0;
        
        for (Chapter chapter : chapters) {
            long chapterSentenceCount = sentenceRepository.countByChapterId(chapter.getId());
            long chapterReadySentenceCount = sentenceRepository.countByChapterIdAndStatus(
                    chapter.getId(), SentenceStatus.READY);
            
            totalSentences += chapterSentenceCount;
            readySentences += chapterReadySentenceCount;
        }
        
        return BookProcessingStats.builder()
                .bookId(bookId)
                .bookTitle(book.getTitle())
                .totalChapters(totalChapters)
                .readyChapters(readyChapters)
                .totalSentences((int) totalSentences)
                .readySentences((int) readySentences)
                .progressPercentage(totalSentences > 0 ? (int) ((readySentences * 100) / totalSentences) : 0)
                .build();
    }
    
    private BookDto convertToDto(Book book) {
        BookDto dto = new BookDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setOriginalFilename(book.getOriginalFilename());
        dto.setCreatedAt(book.getCreatedAt());
        dto.setUpdatedAt(book.getUpdatedAt());
        return dto;
    }
    
    private BookDto convertToDtoWithChapters(Book book) {
        BookDto dto = convertToDto(book);
        
        List<ChapterDto> chapterDtos = book.getChapters().stream()
                .map(this::convertChapterToDto)
                .collect(Collectors.toList());
        
        dto.setChapters(chapterDtos);
        return dto;
    }
    
    private BookDto convertToDtoWithCharacters(Book book) {
        BookDto dto = convertToDto(book);
        
        List<CharacterDto> characterDtos = book.getCharacters().stream()
                .map(this::convertCharacterToDto)
                .collect(Collectors.toList());
        
        dto.setCharacters(characterDtos);
        return dto;
    }
    
    private ChapterDto convertChapterToDto(Chapter chapter) {
        ChapterDto dto = new ChapterDto();
        dto.setId(chapter.getId());
        dto.setTitle(chapter.getTitle());
        dto.setChapterNumber(chapter.getChapterNumber());
        dto.setStatus(chapter.getStatus());
        dto.setAudioFilePath(chapter.getAudioFilePath());
        dto.setCreatedAt(chapter.getCreatedAt());
        dto.setUpdatedAt(chapter.getUpdatedAt());
        
        // Добавляем статистику предложений
        long totalSentences = sentenceRepository.countByChapterId(chapter.getId());
        long readySentences = sentenceRepository.countByChapterIdAndStatus(
                chapter.getId(), SentenceStatus.READY);
        
        dto.setTotalSentences((int) totalSentences);
        dto.setReadySentences((int) readySentences);
        
        return dto;
    }
    
    private CharacterDto convertCharacterToDto(CharacterBook character) {
        CharacterDto dto = new CharacterDto();
        dto.setId(character.getId());
        dto.setName(character.getName());
        dto.setVoiceId(character.getVoiceId());
        dto.setVoiceName(character.getVoiceName());
        dto.setDescription(character.getDescription());
        dto.setCreatedAt(character.getCreatedAt());
        dto.setUpdatedAt(character.getUpdatedAt());
        return dto;
    }
    
    public static class BookProcessingStats {
        public final Long bookId;
        public final String bookTitle;
        public final int totalChapters;
        public final int readyChapters;
        public final int totalSentences;
        public final int readySentences;
        public final int progressPercentage;
        
        private BookProcessingStats(Long bookId, String bookTitle, int totalChapters, 
                                  int readyChapters, int totalSentences, int readySentences, 
                                  int progressPercentage) {
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.totalChapters = totalChapters;
            this.readyChapters = readyChapters;
            this.totalSentences = totalSentences;
            this.readySentences = readySentences;
            this.progressPercentage = progressPercentage;
        }
        
        public static BookProcessingStatsBuilder builder() {
            return new BookProcessingStatsBuilder();
        }
        
        public static class BookProcessingStatsBuilder {
            private Long bookId;
            private String bookTitle;
            private int totalChapters;
            private int readyChapters;
            private int totalSentences;
            private int readySentences;
            private int progressPercentage;
            
            public BookProcessingStatsBuilder bookId(Long bookId) {
                this.bookId = bookId;
                return this;
            }
            
            public BookProcessingStatsBuilder bookTitle(String bookTitle) {
                this.bookTitle = bookTitle;
                return this;
            }
            
            public BookProcessingStatsBuilder totalChapters(int totalChapters) {
                this.totalChapters = totalChapters;
                return this;
            }
            
            public BookProcessingStatsBuilder readyChapters(int readyChapters) {
                this.readyChapters = readyChapters;
                return this;
            }
            
            public BookProcessingStatsBuilder totalSentences(int totalSentences) {
                this.totalSentences = totalSentences;
                return this;
            }
            
            public BookProcessingStatsBuilder readySentences(int readySentences) {
                this.readySentences = readySentences;
                return this;
            }
            
            public BookProcessingStatsBuilder progressPercentage(int progressPercentage) {
                this.progressPercentage = progressPercentage;
                return this;
            }
            
            public BookProcessingStats build() {
                return new BookProcessingStats(bookId, bookTitle, totalChapters, 
                                             readyChapters, totalSentences, readySentences, 
                                             progressPercentage);
            }
        }
    }
}

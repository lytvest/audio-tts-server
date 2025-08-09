package com.lytvest.audiotts.service;

import com.lytvest.audiotts.dto.ChapterDto;
import com.lytvest.audiotts.model.entity.Chapter;
import com.lytvest.audiotts.model.entity.Sentence;
import com.lytvest.audiotts.model.enums.SentenceStatus;
import com.lytvest.audiotts.repository.ChapterRepository;
import com.lytvest.audiotts.repository.SentenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterService {
    
    private final ChapterRepository chapterRepository;
    private final SentenceRepository sentenceRepository;
    
    @Value("${app.storage.audio-path}")
    private String audioStoragePath;
    
    /**
     * Получает главы книги
     */
    public List<ChapterDto> getBookChapters(Long bookId) {
        List<Chapter> chapters = chapterRepository.findByBookIdOrderByChapterNumber(bookId);
        return chapters.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Генерирует аудио файл главы (объединяет все предложения)
     */
    public byte[] generateChapterAudio(Long chapterId) {
        try {
            Chapter chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new RuntimeException("Chapter not found"));
            
            List<Sentence> sentences = sentenceRepository.findByChapterIdOrderBySentenceNumber(chapterId);
            
            // Проверяем, что все предложения готовы
            List<Sentence> readySentences = sentences.stream()
                    .filter(s -> s.getStatus() == SentenceStatus.READY && s.getAudioFilePath() != null)
                    .collect(Collectors.toList());
            
            if (readySentences.isEmpty()) {
                throw new RuntimeException("No ready sentences found for chapter");
            }
            
            if (readySentences.size() != sentences.size()) {
                log.warn("Chapter {} has {} sentences but only {} are ready", 
                        chapterId, sentences.size(), readySentences.size());
            }
            
            // Простая конкатенация аудио файлов (в реальном проекте лучше использовать специальную библиотеку)
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            for (Sentence sentence : readySentences) {
                Path audioPath = Paths.get(sentence.getAudioFilePath());
                if (Files.exists(audioPath)) {
                    byte[] audioData = Files.readAllBytes(audioPath);
                    outputStream.write(audioData);
                } else {
                    log.warn("Audio file not found for sentence {}: {}", sentence.getId(), sentence.getAudioFilePath());
                }
            }
            
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("Error generating chapter audio for chapter {}", chapterId, e);
            throw new RuntimeException("Failed to generate chapter audio", e);
        }
    }
    
    /**
     * Создает ZIP архив с аудио файлами всех глав книги
     */
    public byte[] generateBookZip(Long bookId) {
        try {
            List<Chapter> chapters = chapterRepository.findByBookIdOrderByChapterNumber(bookId);
            
            if (chapters.isEmpty()) {
                throw new RuntimeException("No chapters found for book");
            }
            
            ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
            try (ZipOutputStream zipStream = new ZipOutputStream(zipOutput)) {
                
                for (Chapter chapter : chapters) {
                    try {
                        byte[] chapterAudio = generateChapterAudio(chapter.getId());
                        
                        // Создаем запись в ZIP архиве
                        String fileName = String.format("Chapter_%02d_%s.mp3", 
                                chapter.getChapterNumber(), 
                                sanitizeFileName(chapter.getTitle()));
                        
                        ZipEntry entry = new ZipEntry(fileName);
                        zipStream.putNextEntry(entry);
                        zipStream.write(chapterAudio);
                        zipStream.closeEntry();
                        
                        log.debug("Added chapter {} to ZIP: {}", chapter.getChapterNumber(), fileName);
                        
                    } catch (Exception e) {
                        log.warn("Failed to add chapter {} to ZIP: {}", chapter.getId(), e.getMessage());
                        // Продолжаем с другими главами
                    }
                }
            }
            
            return zipOutput.toByteArray();
            
        } catch (IOException e) {
            log.error("Error generating book ZIP for book {}", bookId, e);
            throw new RuntimeException("Failed to generate book ZIP", e);
        }
    }
    
    /**
     * Очищает имя файла от недопустимых символов
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "Unknown";
        }
        
        return fileName
                .replaceAll("[^a-zA-Z0-9а-яА-Я\\s\\-_.]", "")
                .trim()
                .replaceAll("\\s+", "_");
    }
    
    private ChapterDto convertToDto(Chapter chapter) {
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
}

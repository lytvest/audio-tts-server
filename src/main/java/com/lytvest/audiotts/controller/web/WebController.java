package com.lytvest.audiotts.controller.web;

import com.lytvest.audiotts.dto.BookDto;
import com.lytvest.audiotts.dto.CharacterDto;
import com.lytvest.audiotts.dto.SentenceDto;
import com.lytvest.audiotts.dto.response.VoiceDto;
import com.lytvest.audiotts.model.enums.SentenceStatus;
import com.lytvest.audiotts.service.BookService;
import com.lytvest.audiotts.service.SentenceProcessingService;
import com.lytvest.audiotts.service.external.F5TtsService;
import com.lytvest.audiotts.service.queue.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web")
@RequiredArgsConstructor
@Slf4j
public class WebController {
    
    private final BookService bookService;
    private final SentenceProcessingService sentenceProcessingService;
    private final F5TtsService f5TtsService;
    private final QueueService queueService;
    
    /**
     * Главная страница - дашборд
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        try {
            List<BookDto> allBooks = bookService.getAllBooks();
            
            // Базовые статистики
            model.addAttribute("totalBooks", allBooks.size());
            model.addAttribute("completedBooks", 0L); // TODO: implement when methods are available
            model.addAttribute("processingBooks", 0L);
            model.addAttribute("waitingBooks", (long) allBooks.size());
            model.addAttribute("totalSentences", 0);
            model.addAttribute("readySentences", 0);
            model.addAttribute("progressPercentage", 0);
            
            // Статистика очередей (если доступна)
            try {
                QueueService.QueueStats queueStats = queueService.getQueueStats();
                model.addAttribute("queueStats", queueStats);
            } catch (Exception e) {
                log.warn("Could not load queue stats, using defaults", e);
                model.addAttribute("queueStats", createDefaultQueueStats());
            }
            
            // Последние книги (ограничиваем до 5)
            List<BookDto> recentBooks = allBooks.stream()
                    .limit(5)
                    .collect(Collectors.toList());
            model.addAttribute("recentBooks", recentBooks);
            
            return "dashboard";
        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            model.addAttribute("error", "Ошибка загрузки дашборда: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Страница со списком книг
     */
    @GetMapping("/books")
    public String books(@RequestParam(defaultValue = "all") String status, Model model) {
        try {
            List<BookDto> books = bookService.getAllBooks();
            
            // Создаем BookWithStats для каждой книги
            List<BookWithStats> booksWithStats = books.stream()
                    .map(book -> new BookWithStats(book, createDefaultBookStats()))
                    .collect(Collectors.toList());
            
            model.addAttribute("books", booksWithStats);
            model.addAttribute("currentStatus", status);
            
            return "books/list";
        } catch (Exception e) {
            log.error("Error loading books", e);
            model.addAttribute("error", "Ошибка загрузки книг: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Страница загрузки новой книги
     */
    @GetMapping("/books/upload")
    public String uploadBookForm(Model model) {
        return "books/upload";
    }
    
    /**
     * Детальная страница книги
     */
    @GetMapping("/books/{bookId}")
    public String bookDetails(@PathVariable Long bookId, Model model) {
        try {
            Optional<BookDto> bookOpt = bookService.getBookById(bookId);
            if (bookOpt.isEmpty()) {
                model.addAttribute("error", "Книга не найдена");
                return "error";
            }
            
            BookDto book = bookOpt.get();
            BookStats stats = createDefaultBookStats();
            
            // Пытаемся получить персонажей
            List<CharacterDto> characters = Collections.emptyList();
            try {
                characters = sentenceProcessingService.getBookCharacters(bookId);
            } catch (Exception e) {
                log.warn("Could not load characters for book {}", bookId, e);
            }
            
            model.addAttribute("book", book);
            model.addAttribute("stats", stats);
            model.addAttribute("characters", characters);
            
            return "books/details";
        } catch (Exception e) {
            log.error("Error loading book details for ID: {}", bookId, e);
            model.addAttribute("error", "Ошибка загрузки деталей книги: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Страница управления персонажами книги
     */
    @GetMapping("/books/{bookId}/characters")
    public String bookCharacters(@PathVariable Long bookId, Model model) {
        try {
            Optional<BookDto> bookOpt = bookService.getBookById(bookId);
            if (bookOpt.isEmpty()) {
                model.addAttribute("error", "Книга не найдена");
                return "error";
            }
            
            BookDto book = bookOpt.get();
            
            // Пытаемся получить персонажей
            List<CharacterDto> characters = Collections.emptyList();
            try {
                characters = sentenceProcessingService.getBookCharacters(bookId);
            } catch (Exception e) {
                log.warn("Could not load characters for book {}", bookId, e);
            }
            
            // Получаем доступные голоса
            List<VoiceDto> voices = getAvailableVoices();
            
            model.addAttribute("book", book);
            model.addAttribute("characters", characters);
            model.addAttribute("voices", voices);
            
            return "books/characters";
        } catch (Exception e) {
            log.error("Error loading book characters for ID: {}", bookId, e);
            model.addAttribute("error", "Ошибка загрузки персонажей: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Страница предложений
     */
    @GetMapping("/sentences")
    public String sentences(
            @RequestParam(defaultValue = "WAITING_FOR_CHARACTER") String statusStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        try {
            SentenceStatus status;
            try {
                status = SentenceStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                status = SentenceStatus.WAITING_FOR_CHARACTER;
            }
            
            Pageable pageable = PageRequest.of(page, size);
            
            // Пытаемся получить предложения
            Page<SentenceDto> sentences;
            try {
                sentences = sentenceProcessingService.getSentencesByStatus(status, pageable);
            } catch (Exception e) {
                log.warn("Could not load sentences, using empty page", e);
                sentences = Page.empty(pageable);
            }
            
            model.addAttribute("sentences", sentences);
            model.addAttribute("currentStatus", status);
            model.addAttribute("statuses", SentenceStatus.values());
            
            return "sentences/list";
        } catch (Exception e) {
            log.error("Error loading sentences", e);
            model.addAttribute("error", "Ошибка загрузки предложений: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Страница мониторинга очередей
     */
    @GetMapping("/queues")
    public String queues(Model model) {
        try {
            QueueService.QueueStats queueStats;
            try {
                queueStats = queueService.getQueueStats();
            } catch (Exception e) {
                log.warn("Could not load queue stats, using defaults", e);
                queueStats = createDefaultQueueStats();
            }
            
            model.addAttribute("queueStats", queueStats);
            
            return "queues/monitor";
        } catch (Exception e) {
            log.error("Error loading queue stats", e);
            model.addAttribute("error", "Ошибка загрузки статистики очередей: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Перезапуск обработки книги
     */
    @PostMapping("/books/{bookId}/restart")
    public String restartBookProcessing(@PathVariable Long bookId, RedirectAttributes redirectAttributes) {
        try {
            // Проверяем, что книга существует
            if (bookService.getBookById(bookId).isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Книга не найдена");
                return "redirect:/web/books";
            }
            
            // Пытаемся перезапустить обработку
            try {
                bookService.restartBookProcessing(bookId);
                redirectAttributes.addFlashAttribute("success", "Обработка книги перезапущена");
            } catch (Exception e) {
                log.warn("Could not restart book processing for ID: {}", bookId, e);
                redirectAttributes.addFlashAttribute("error", "Функция перезапуска временно недоступна");
            }
        } catch (Exception e) {
            log.error("Error restarting book processing", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка перезапуска обработки");
        }
        
        return "redirect:/web/books/" + bookId;
    }
    
    /**
     * Удаление книги
     */
    @PostMapping("/books/{bookId}/delete")
    public String deleteBook(@PathVariable Long bookId, RedirectAttributes redirectAttributes) {
        try {
            // Проверяем, что книга существует
            if (bookService.getBookById(bookId).isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Книга не найдена");
                return "redirect:/web/books";
            }
            
            bookService.deleteBook(bookId);
            redirectAttributes.addFlashAttribute("success", "Книга удалена");
        } catch (Exception e) {
            log.error("Error deleting book", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления книги: " + e.getMessage());
        }
        
        return "redirect:/web/books";
    }
    
    // Вспомогательные методы
    
    private List<VoiceDto> getAvailableVoices() {
        try {
            Map<String, Object> voicesResponse = f5TtsService.getAvailableVoices().block();
            if (voicesResponse != null && voicesResponse.containsKey("voices")) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> voicesMap = (Map<String, Map<String, Object>>) voicesResponse.get("voices");
                
                return voicesMap.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> voiceInfo = entry.getValue();
                        return new VoiceDto(
                            entry.getKey(),
                            (String) voiceInfo.getOrDefault("name", entry.getKey()),
                            (String) voiceInfo.getOrDefault("language", "ru"),
                            (String) voiceInfo.getOrDefault("gender", "neutral"),
                            (Boolean) voiceInfo.getOrDefault("available", true)
                        );
                    })
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Could not load voices from F5TTS service", e);
        }
        
        // Возвращаем дефолтные голоса
        return Arrays.asList(
            new VoiceDto("default", "Default Voice", "ru", "neutral", true),
            new VoiceDto("female", "Female Voice", "ru", "female", true),
            new VoiceDto("male", "Male Voice", "ru", "male", true)
        );
    }
    
    private QueueService.QueueStats createDefaultQueueStats() {
        return new QueueService.QueueStats(0, 0, 0);
    }
    
    private BookStats createDefaultBookStats() {
        return new BookStats(0, 0, 0, 0);
    }
    
    // Внутренние классы
    
    /**
     * Класс для передачи книги со статистикой в шаблон
     */
    public static class BookWithStats {
        public final BookDto book;
        public final BookStats stats;
        
        public BookWithStats(BookDto book, BookStats stats) {
            this.book = book;
            this.stats = stats;
        }
    }
    
    /**
     * Упрощенная статистика книги
     */
    public static class BookStats {
        public final int totalChapters;
        public final int totalSentences;
        public final int readySentences;
        public final int progressPercentage;
        
        public BookStats(int totalChapters, int totalSentences, int readySentences, int progressPercentage) {
            this.totalChapters = totalChapters;
            this.totalSentences = totalSentences;
            this.readySentences = readySentences;
            this.progressPercentage = progressPercentage;
        }
    }
}
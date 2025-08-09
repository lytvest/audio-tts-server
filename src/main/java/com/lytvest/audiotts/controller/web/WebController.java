package com.lytvest.audiotts.controller.web;

import com.lytvest.audiotts.dto.BookDto;
import com.lytvest.audiotts.dto.CharacterDto;
import com.lytvest.audiotts.dto.ChapterDto;
import com.lytvest.audiotts.dto.SentenceDto;
import com.lytvest.audiotts.dto.response.VoiceDto;
import com.lytvest.audiotts.model.enums.SentenceStatus;
import com.lytvest.audiotts.service.BookService;
import com.lytvest.audiotts.service.ChapterService;
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

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/web")
@RequiredArgsConstructor
@Slf4j
public class WebController {
    
    private final BookService bookService;
    private final ChapterService chapterService;
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
            List<BookService.BookProcessingStats> allStats = allBooks.stream()
                    .map(book -> bookService.getBookProcessingStats(book.getId()))
                    .toList();
            
            // Статистика по статусам
            long completedBooks = allStats.stream().filter(s -> s.progressPercentage == 100).count();
            long processingBooks = allStats.stream().filter(s -> s.progressPercentage > 0 && s.progressPercentage < 100).count();
            long waitingBooks = allStats.stream().filter(s -> s.progressPercentage == 0).count();
            
            // Общая статистика предложений
            int totalSentences = allStats.stream().mapToInt(s -> s.totalSentences).sum();
            int readySentences = allStats.stream().mapToInt(s -> s.readySentences).sum();
            
            // Статистика очередей
            QueueService.QueueStats queueStats = queueService.getQueueStats();
            
            model.addAttribute("totalBooks", allBooks.size());
            model.addAttribute("completedBooks", completedBooks);
            model.addAttribute("processingBooks", processingBooks);
            model.addAttribute("waitingBooks", waitingBooks);
            model.addAttribute("totalSentences", totalSentences);
            model.addAttribute("readySentences", readySentences);
            model.addAttribute("progressPercentage", totalSentences > 0 ? (readySentences * 100) / totalSentences : 0);
            model.addAttribute("queueStats", queueStats);
            model.addAttribute("recentBooks", allBooks.stream().limit(5).toList());
            
            return "dashboard";
        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            model.addAttribute("error", "Ошибка загрузки дашборда");
            return "error";
        }
    }
    
    /**
     * Страница со списком книг
     */
    @GetMapping("/books")
    public String books(@RequestParam(defaultValue = "all") String status, Model model) {
        try {
            List<BookDto> books;
            switch (status.toLowerCase()) {
                case "completed" -> books = bookService.getCompletedBooks();
                case "processing" -> books = bookService.getProcessingBooks();
                case "waiting" -> books = bookService.getWaitingBooks();
                default -> books = bookService.getAllBooks();
            }
            
            // Добавляем статистику для каждой книги
            List<BookWithStats> booksWithStats = books.stream()
                    .map(book -> {
                        BookService.BookProcessingStats stats = bookService.getBookProcessingStats(book.getId());
                        return new BookWithStats(book, stats);
                    })
                    .toList();
            
            model.addAttribute("books", booksWithStats);
            model.addAttribute("currentStatus", status);
            
            return "books/list";
        } catch (Exception e) {
            log.error("Error loading books", e);
            model.addAttribute("error", "Ошибка загрузки книг");
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
            BookDto book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));
            
            BookService.BookProcessingStats stats = bookService.getBookProcessingStats(bookId);
            List<CharacterDto> characters = sentenceProcessingService.getBookCharacters(bookId);
            
            model.addAttribute("book", book);
            model.addAttribute("stats", stats);
            model.addAttribute("characters", characters);
            
            return "books/details";
        } catch (Exception e) {
            log.error("Error loading book details for ID: {}", bookId, e);
            model.addAttribute("error", "Книга не найдена");
            return "error";
        }
    }
    
    /**
     * Страница управления персонажами книги
     */
    @GetMapping("/books/{bookId}/characters")
    public String bookCharacters(@PathVariable Long bookId, Model model) {
        try {
            BookDto book = bookService.getBookById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));
            
            List<CharacterDto> characters = sentenceProcessingService.getBookCharacters(bookId);
            
            // Получаем доступные голоса
            try {
                Map<String, Object> voicesResponse = f5TtsService.getAvailableVoices().block();
                @SuppressWarnings(\"unchecked\")\n                Map<String, Map<String, Object>> voicesMap = (Map<String, Map<String, Object>>) voicesResponse.get(\"voices\");\n                \n                List<VoiceDto> voices = voicesMap.entrySet().stream()\n                    .map(entry -> {\n                        Map<String, Object> voiceInfo = entry.getValue();\n                        return new VoiceDto(\n                            entry.getKey(),\n                            (String) voiceInfo.getOrDefault(\"name\", entry.getKey()),\n                            (String) voiceInfo.getOrDefault(\"language\", \"unknown\"),\n                            (String) voiceInfo.getOrDefault(\"gender\", \"unknown\"),\n                            (Boolean) voiceInfo.getOrDefault(\"available\", true)\n                        );\n                    })\n                    .toList();\n                \n                model.addAttribute(\"voices\", voices);\n            } catch (Exception e) {\n                log.warn(\"Could not load voices, using default\", e);\n                model.addAttribute(\"voices\", List.of(new VoiceDto(\"default\", \"Default Voice\", \"ru\", \"neutral\", true)));\n            }\n            \n            model.addAttribute(\"book\", book);\n            model.addAttribute(\"characters\", characters);\n            \n            return \"books/characters\";\n        } catch (Exception e) {\n            log.error(\"Error loading book characters for ID: {}\", bookId, e);\n            model.addAttribute(\"error\", \"Книга не найдена\");\n            return \"error\";\n        }\n    }\n    \n    /**\n     * Страница предложений\n     */\n    @GetMapping(\"/sentences\")\n    public String sentences(\n            @RequestParam(defaultValue = \"WAITING_FOR_CHARACTER\") SentenceStatus status,\n            @RequestParam(defaultValue = \"0\") int page,\n            @RequestParam(defaultValue = \"20\") int size,\n            Model model) {\n        \n        try {\n            Pageable pageable = PageRequest.of(page, size);\n            Page<SentenceDto> sentences = sentenceProcessingService.getSentencesByStatus(status, pageable);\n            \n            model.addAttribute(\"sentences\", sentences);\n            model.addAttribute(\"currentStatus\", status);\n            model.addAttribute(\"statuses\", SentenceStatus.values());\n            \n            return \"sentences/list\";\n        } catch (Exception e) {\n            log.error(\"Error loading sentences\", e);\n            model.addAttribute(\"error\", \"Ошибка загрузки предложений\");\n            return \"error\";\n        }\n    }\n    \n    /**\n     * Страница мониторинга очередей\n     */\n    @GetMapping(\"/queues\")\n    public String queues(Model model) {\n        try {\n            QueueService.QueueStats queueStats = queueService.getQueueStats();\n            model.addAttribute(\"queueStats\", queueStats);\n            \n            return \"queues/monitor\";\n        } catch (Exception e) {\n            log.error(\"Error loading queue stats\", e);\n            model.addAttribute(\"error\", \"Ошибка загрузки статистики очередей\");\n            return \"error\";\n        }\n    }\n    \n    /**\n     * Перезапуск обработки книги\n     */\n    @PostMapping(\"/books/{bookId}/restart\")\n    public String restartBookProcessing(@PathVariable Long bookId, RedirectAttributes redirectAttributes) {\n        try {\n            bookService.restartBookProcessing(bookId);\n            redirectAttributes.addFlashAttribute(\"success\", \"Обработка книги перезапущена\");\n        } catch (Exception e) {\n            log.error(\"Error restarting book processing\", e);\n            redirectAttributes.addFlashAttribute(\"error\", \"Ошибка перезапуска обработки\");\n        }\n        \n        return \"redirect:/web/books/\" + bookId;\n    }\n    \n    /**\n     * Удаление книги\n     */\n    @PostMapping(\"/books/{bookId}/delete\")\n    public String deleteBook(@PathVariable Long bookId, RedirectAttributes redirectAttributes) {\n        try {\n            bookService.deleteBook(bookId);\n            redirectAttributes.addFlashAttribute(\"success\", \"Книга удалена\");\n        } catch (Exception e) {\n            log.error(\"Error deleting book\", e);\n            redirectAttributes.addFlashAttribute(\"error\", \"Ошибка удаления книги\");\n        }\n        \n        return \"redirect:/web/books\";\n    }\n    \n    /**\n     * Вспомогательный класс для передачи книги со статистикой в шаблон\n     */\n    public static class BookWithStats {\n        public final BookDto book;\n        public final BookService.BookProcessingStats stats;\n        \n        public BookWithStats(BookDto book, BookService.BookProcessingStats stats) {\n            this.book = book;\n            this.stats = stats;\n        }\n    }\n}"
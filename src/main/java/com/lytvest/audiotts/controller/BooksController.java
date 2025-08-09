package com.lytvest.audiotts.controller;

import com.lytvest.audiotts.dto.BookDto;
import com.lytvest.audiotts.dto.request.BookUploadRequest;
import com.lytvest.audiotts.dto.response.ApiResponse;
import com.lytvest.audiotts.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class BooksController {
    
    private final BookService bookService;
    
    /**
     * Загружает FB2 файл
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BookDto>> uploadBook(@Valid @ModelAttribute BookUploadRequest request) {
        try {
            BookDto book = bookService.uploadBook(request);
            return ResponseEntity.ok(ApiResponse.success("Book uploaded successfully", book));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid book upload request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading book", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload book: " + e.getMessage()));
        }
    }
    
    /**
     * Получает список всех книг
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookDto>>> getAllBooks() {
        try {
            List<BookDto> books = bookService.getAllBooks();
            return ResponseEntity.ok(ApiResponse.success(books));
        } catch (Exception e) {
            log.error("Error getting all books", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get books"));
        }
    }
    
    /**
     * Получает книгу по ID с главами
     */
    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookDto>> getBook(@PathVariable Long bookId) {
        try {
            return bookService.getBookById(bookId)
                    .map(book -> ResponseEntity.ok(ApiResponse.success(book)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting book {}", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get book"));
        }
    }
    
    /**
     * Получает книгу с персонажами
     */
    @GetMapping("/{bookId}/characters")
    public ResponseEntity<ApiResponse<BookDto>> getBookWithCharacters(@PathVariable Long bookId) {
        try {
            return bookService.getBookWithCharacters(bookId)
                    .map(book -> ResponseEntity.ok(ApiResponse.success(book)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting book {} with characters", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get book with characters"));
        }
    }
    
    /**
     * Получает статистику обработки книги
     */
    @GetMapping("/{bookId}/stats")
    public ResponseEntity<ApiResponse<BookService.BookProcessingStats>> getBookStats(@PathVariable Long bookId) {
        try {
            BookService.BookProcessingStats stats = bookService.getBookProcessingStats(bookId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (RuntimeException e) {
            log.warn("Book {} not found for stats", bookId);
            return ResponseEntity.notFound()
                    .body(ApiResponse.error("Book not found"));
        } catch (Exception e) {
            log.error("Error getting stats for book {}", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get book stats"));
        }
    }
    
    /**
     * Получает прогресс обработки всех книг
     */
    @GetMapping("/stats/all")
    public ResponseEntity<ApiResponse<List<BookService.BookProcessingStats>>> getAllBooksStats() {
        try {
            List<BookDto> books = bookService.getAllBooks();
            List<BookService.BookProcessingStats> allStats = books.stream()
                    .map(book -> bookService.getBookProcessingStats(book.getId()))
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(allStats));
        } catch (Exception e) {
            log.error("Error getting stats for all books", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get books stats"));
        }
    }
    
    /**
     * Обновляет информацию о книге
     */
    @PutMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookDto>> updateBook(
            @PathVariable Long bookId,
            @RequestBody UpdateBookRequest request) {
        try {
            BookDto updatedBook = bookService.updateBook(bookId, request);
            return ResponseEntity.ok(ApiResponse.success("Book updated successfully", updatedBook));
        } catch (RuntimeException e) {
            log.warn("Book {} not found for update", bookId);
            return ResponseEntity.notFound()
                    .body(ApiResponse.error("Book not found"));
        } catch (Exception e) {
            log.error("Error updating book {}", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update book"));
        }
    }
    
    /**
     * Удаляет книгу
     */
    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long bookId) {
        try {
            bookService.deleteBook(bookId);
            return ResponseEntity.ok(ApiResponse.success("Book deleted successfully", null));
        } catch (RuntimeException e) {
            log.warn("Book {} not found for deletion", bookId);
            return ResponseEntity.notFound()
                    .body(ApiResponse.error("Book not found"));
        } catch (Exception e) {
            log.error("Error deleting book {}", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete book"));
        }
    }
    
    /**
     * Перезапускает обработку книги (добавляет все предложения обратно в очереди)
     */
    @PostMapping("/{bookId}/restart")
    public ResponseEntity<ApiResponse<Void>> restartBookProcessing(@PathVariable Long bookId) {
        try {
            bookService.restartBookProcessing(bookId);
            return ResponseEntity.ok(ApiResponse.success("Book processing restarted", null));
        } catch (RuntimeException e) {
            log.warn("Book {} not found for restart", bookId);
            return ResponseEntity.notFound()
                    .body(ApiResponse.error("Book not found"));
        } catch (Exception e) {
            log.error("Error restarting book {} processing", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to restart book processing"));
        }
    }
    
    /**
     * Получает книги по статусу обработки
     */
    @GetMapping("/by-status")
    public ResponseEntity<ApiResponse<List<BookDto>>> getBooksByProcessingStatus(
            @RequestParam(defaultValue = "all") String status) {
        try {
            List<BookDto> books;
            switch (status.toLowerCase()) {
                case "completed" -> books = bookService.getCompletedBooks();
                case "processing" -> books = bookService.getProcessingBooks();
                case "waiting" -> books = bookService.getWaitingBooks();
                default -> books = bookService.getAllBooks();
            }
            
            return ResponseEntity.ok(ApiResponse.success(books));
        } catch (Exception e) {
            log.error("Error getting books by status {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get books by status"));
        }
    }
    
    /**
     * DTO для обновления книги
     */
    public static class UpdateBookRequest {
        private String title;
        private String author;
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }
}

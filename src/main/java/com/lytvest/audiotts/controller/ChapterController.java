package com.lytvest.audiotts.controller;

import com.lytvest.audiotts.dto.ChapterDto;
import com.lytvest.audiotts.dto.response.ApiResponse;
import com.lytvest.audiotts.service.ChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
@Slf4j
public class ChapterController {
    
    private final ChapterService chapterService;
    
    /**
     * Получает главы книги
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<ApiResponse<List<ChapterDto>>> getBookChapters(@PathVariable Long bookId) {
        List<ChapterDto> chapters = chapterService.getBookChapters(bookId);
        return ResponseEntity.ok(ApiResponse.success(chapters));
    }
    
    /**
     * Скачивает аудио файл главы
     */
    @GetMapping("/{chapterId}/audio")
    public ResponseEntity<byte[]> downloadChapterAudio(@PathVariable Long chapterId) {
        try {
            byte[] audioData = chapterService.generateChapterAudio(chapterId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentDispositionFormData("attachment", "chapter_" + chapterId + ".mp3");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(audioData);
                    
        } catch (RuntimeException e) {
            log.error("Error downloading audio for chapter {}", chapterId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Скачивает всю книгу как ZIP архив
     */
    @GetMapping("/book/{bookId}/download")
    public ResponseEntity<byte[]> downloadBookAsZip(@PathVariable Long bookId) {
        try {
            byte[] zipData = chapterService.generateBookZip(bookId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDispositionFormData("attachment", "book_" + bookId + ".zip");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipData);
                    
        } catch (RuntimeException e) {
            log.error("Error downloading book {} as ZIP", bookId, e);
            return ResponseEntity.notFound().build();
        }
    }
}
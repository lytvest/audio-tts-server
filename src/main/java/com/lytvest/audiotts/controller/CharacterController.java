package com.lytvest.audiotts.controller;

import com.lytvest.audiotts.dto.CharacterDto;
import com.lytvest.audiotts.dto.request.CharacterUpdateRequest;
import com.lytvest.audiotts.dto.response.ApiResponse;
import com.lytvest.audiotts.service.SentenceProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
@Slf4j
public class CharacterController {
    
    private final SentenceProcessingService sentenceProcessingService;
    
    /**
     * Получает персонажей книги
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<ApiResponse<List<CharacterDto>>> getBookCharacters(@PathVariable Long bookId) {
        List<CharacterDto> characters = sentenceProcessingService.getBookCharacters(bookId);
        return ResponseEntity.ok(ApiResponse.success(characters));
    }
    
    /**
     * Обновляет настройки персонажа
     */
    @PutMapping("/{characterId}")
    public ResponseEntity<ApiResponse<CharacterDto>> updateCharacter(
            @PathVariable Long characterId,
            @Valid @RequestBody CharacterUpdateRequest request) {
        
        try {
            CharacterDto character = sentenceProcessingService.updateCharacter(characterId, request);
            return ResponseEntity.ok(ApiResponse.success("Character updated successfully", character));
        } catch (Exception e) {
            log.error("Error updating character {}", characterId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update character"));
        }
    }
}
package com.lytvest.audiotts.controller;

import com.lytvest.audiotts.dto.response.ApiResponse;
import com.lytvest.audiotts.dto.response.VoiceDto;
import com.lytvest.audiotts.service.external.F5TtsService;
import com.lytvest.audiotts.service.queue.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SystemController {
    
    private final F5TtsService f5TtsService;
    private final QueueService queueService;
    
    /**
     * Проверка здоровья системы
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis(),
            "queue", queueService.getQueueStats()
        );
        
        return ResponseEntity.ok(ApiResponse.success(health));
    }
    
    /**
     * Получает доступные голоса
     */
    @GetMapping("/voices")
    public ResponseEntity<ApiResponse<List<VoiceDto>>> getVoices() {
        try {
            Map<String, Object> voicesResponse = f5TtsService.getAvailableVoices().block();
            
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> voicesMap = (Map<String, Map<String, Object>>) voicesResponse.get("voices");
            
            List<VoiceDto> voices = voicesMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> voiceInfo = entry.getValue();
                    return new VoiceDto(
                        entry.getKey(),
                        (String) voiceInfo.getOrDefault("name", entry.getKey()),
                        (String) voiceInfo.getOrDefault("language", "unknown"),
                        (String) voiceInfo.getOrDefault("gender", "unknown"),
                        (Boolean) voiceInfo.getOrDefault("available", true)
                    );
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(voices));
            
        } catch (Exception e) {
            log.error("Error getting voices", e);
            // Возвращаем голос по умолчанию
            List<VoiceDto> defaultVoices = List.of(
                new VoiceDto("default", "Default Voice", "ru", "neutral", true)
            );
            return ResponseEntity.ok(ApiResponse.success(defaultVoices));
        }
    }
    
    /**
     * Получает статистику очередей
     */
    @GetMapping("/queue/stats")
    public ResponseEntity<ApiResponse<QueueService.QueueStats>> getQueueStats() {
        QueueService.QueueStats stats = queueService.getQueueStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
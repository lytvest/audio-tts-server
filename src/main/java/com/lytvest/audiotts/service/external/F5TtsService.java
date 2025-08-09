package com.lytvest.audiotts.service.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class F5TtsService {
    
    private final WebClient webClient;
    
    @Value("${app.f5tts.base-url}")
    private String f5ttsBaseUrl;
    
    /**
     * Генерирует аудио из текста
     */
    public Mono<byte[]> generateAudio(String text, String voiceId) {
        Map<String, Object> request = Map.of(
            "text", text,
            "voice_id", voiceId != null ? voiceId : "default",
            "format", "mp3"
        );
        
        return webClient.post()
                .uri(f5ttsBaseUrl + "/api/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnSuccess(audio -> log.info("Generated audio for text: {} (size: {} bytes)", 
                    text.substring(0, Math.min(50, text.length())), audio.length))
                .doOnError(error -> log.error("Error generating audio for text: {}", text, error));
    }
    
    /**
     * Получает список доступных голосов
     */
    public Mono<Map<String, Object>> getAvailableVoices() {
        return webClient.get()
                .uri(f5ttsBaseUrl + "/api/voices")
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(voices -> log.info("Retrieved available voices: {}", voices.keySet()))
                .doOnError(error -> log.error("Error retrieving available voices", error));
    }
    
    /**
     * Проверяет доступность сервиса F5-TTS
     */
    public Mono<Boolean> isServiceAvailable() {
        return webClient.get()
                .uri(f5ttsBaseUrl + "/api/health")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> "ok".equals(response.get("status")))
                .onErrorReturn(false)
                .doOnNext(available -> log.info("F5-TTS service available: {}", available));
    }
}
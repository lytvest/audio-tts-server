package com.lytvest.audiotts.service.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {
    
    private final WebClient webClient;
    
    @Value("${app.ollama.base-url}")
    private String ollamaBaseUrl;
    
    @Value("${app.ollama.model}")
    private String ollamaModel;
    
    /**
     * Определяет персонажа для предложения
     */
    public Mono<String> determineCharacter(String sentence, List<String> existingCharacters) {
        String prompt = buildCharacterPrompt(sentence, existingCharacters);
        
        Map<String, Object> request = Map.of(
            "model", ollamaModel,
            "prompt", prompt,
            "stream", false
        );
        
        return webClient.post()
                .uri(ollamaBaseUrl + "/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String responseText = (String) response.get("response");
                    return extractCharacterName(responseText);
                })
                .doOnSuccess(character -> log.info("Determined character: {} for sentence: {}", character, sentence.substring(0, Math.min(50, sentence.length()))))
                .doOnError(error -> log.error("Error determining character for sentence: {}", sentence, error));
    }
    
    /**
     * Расставляет ударения в тексте
     */
    public Mono<String> addStressMarks(String text) {
        String prompt = buildStressPrompt(text);
        
        Map<String, Object> request = Map.of(
            "model", ollamaModel,
            "prompt", prompt,
            "stream", false
        );
        
        return webClient.post()
                .uri(ollamaBaseUrl + "/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String responseText = (String) response.get("response");
                    return extractStressedText(responseText);
                })
                .doOnSuccess(stressedText -> log.info("Added stress marks to text: {}", text.substring(0, Math.min(50, text.length()))))
                .doOnError(error -> log.error("Error adding stress marks to text: {}", text, error));
    }
    
    private String buildCharacterPrompt(String sentence, List<String> existingCharacters) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Определи, какой персонаж произносит это предложение: \"").append(sentence).append("\".\n");
        
        if (!existingCharacters.isEmpty()) {
            prompt.append("Известные персонажи: ").append(String.join(", ", existingCharacters)).append(".\n");
            prompt.append("Если это один из известных персонажей, используй точное имя из списка.\n");
        }
        
        prompt.append("Если это новый персонаж, придумай подходящее имя.\n");
        prompt.append("Если это авторская речь или повествование, ответь \"Автор\".\n");
        prompt.append("Ответь только именем персонажа, без дополнительных объяснений.");
        
        return prompt.toString();
    }
    
    private String buildStressPrompt(String text) {
        return "Расставь ударения в русском тексте, используя символ + после ударной гласной. " +
               "Например: 'приве+т' для слова 'привет'. " +
               "Текст: \"" + text + "\". " +
               "Ответь только текстом с расставленными ударениями, без дополнительных объяснений.";
    }
    
    private String extractCharacterName(String response) {
        if (response == null) {
            return "Автор";
        }
        
        // Извлекаем имя персонажа из ответа
        String cleaned = response.trim();
        
        // Убираем кавычки и лишние символы
        cleaned = cleaned.replaceAll("[\"'«»]", "");
        
        // Берем первое слово, если ответ содержит несколько слов
        String[] words = cleaned.split("\\s+");
        if (words.length > 0) {
            return words[0];
        }
        
        return "Автор";
    }
    
    private String extractStressedText(String response) {
        if (response == null) {
            return "";
        }
        
        // Извлекаем текст с ударениями из ответа
        String cleaned = response.trim();
        
        // Убираем кавычки если они есть
        cleaned = cleaned.replaceAll("^[\"'«»]", "").replaceAll("[\"'«»]$", "");
        
        return cleaned;
    }
}
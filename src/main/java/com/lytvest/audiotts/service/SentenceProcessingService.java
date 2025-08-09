package com.lytvest.audiotts.service;

import com.lytvest.audiotts.dto.CharacterDto;
import com.lytvest.audiotts.dto.SentenceDto;
import com.lytvest.audiotts.dto.request.CharacterUpdateRequest;
import com.lytvest.audiotts.model.entity.Character;
import com.lytvest.audiotts.model.entity.Sentence;
import com.lytvest.audiotts.model.enums.SentenceStatus;
import com.lytvest.audiotts.repository.CharacterRepository;
import com.lytvest.audiotts.repository.SentenceRepository;
import com.lytvest.audiotts.service.external.F5TtsService;
import com.lytvest.audiotts.service.external.OllamaService;
import com.lytvest.audiotts.service.queue.QueueService;
import com.lytvest.audiotts.service.queue.StressTask;
import com.lytvest.audiotts.service.queue.TtsTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class SentenceProcessingService {
    
    private final SentenceRepository sentenceRepository;
    private final CharacterRepository characterRepository;
    private final OllamaService ollamaService;
    private final F5TtsService f5TtsService;
    private final QueueService queueService;
    
    @Value("${app.storage.audio-path}")
    private String audioStoragePath;
    
    /**
     * Получает предложения по статусу
     */
    public Page<SentenceDto> getSentencesByStatus(SentenceStatus status, Pageable pageable) {
        Page<Sentence> sentences = sentenceRepository.findByStatus(status, pageable);
        return sentences.map(this::convertToDto);
    }
    
    /**
     * Получает предложения главы
     */
    public List<SentenceDto> getChapterSentences(Long chapterId) {
        List<Sentence> sentences = sentenceRepository.findByChapterIdOrderBySentenceNumber(chapterId);
        return sentences.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Обновляет персонажа для предложения
     */
    @Transactional
    public void updateSentenceCharacter(Long sentenceId, String characterName) {
        Sentence sentence = sentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new RuntimeException("Sentence not found"));
        
        // Ищем или создаем персонажа
        Character character = characterRepository
                .findByBookIdAndName(sentence.getChapter().getBook().getId(), characterName)
                .orElseGet(() -> {
                    Character newCharacter = new Character();
                    newCharacter.setBook(sentence.getChapter().getBook());
                    newCharacter.setName(characterName);
                    return characterRepository.save(newCharacter);
                });
        
        sentence.setCharacter(character);
        sentence.setStatus(SentenceStatus.WAITING_FOR_STRESS);
        sentenceRepository.save(sentence);
        
        // Добавляем задачу расстановки ударений в очередь
        StressTask stressTask = new StressTask(sentenceId, sentence.getOriginalText());
        queueService.addStressTask(stressTask);
        
        log.info("Updated character for sentence {}: {}", sentenceId, characterName);
    }
    
    /**
     * Обновляет текст с ударениями
     */
    @Transactional
    public void updateSentenceStress(Long sentenceId, String textWithStress) {
        Sentence sentence = sentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new RuntimeException("Sentence not found"));
        
        sentence.setTextWithStress(textWithStress);
        sentence.setStatus(SentenceStatus.WAITING_FOR_TTS);
        sentenceRepository.save(sentence);
        
        // Добавляем задачу TTS в очередь
        String voiceId = sentence.getCharacter() != null ? sentence.getCharacter().getVoiceId() : "default";
        String outputPath = generateAudioPath(sentenceId);
        
        TtsTask ttsTask = new TtsTask(sentenceId, textWithStress, voiceId, outputPath);
        queueService.addTtsTask(ttsTask);
        
        log.info("Updated stress for sentence {}", sentenceId);
    }
    
    /**
     * Сохраняет аудио файл и обновляет статус предложения
     */
    @Transactional
    public void saveSentenceAudio(Long sentenceId, byte[] audioData) {
        try {
            Sentence sentence = sentenceRepository.findById(sentenceId)
                    .orElseThrow(() -> new RuntimeException("Sentence not found"));
            
            // Создаем директорию для аудио файлов
            Path audioPath = Paths.get(audioStoragePath);
            Files.createDirectories(audioPath);
            
            // Сохраняем аудио файл
            String filename = "sentence_" + sentenceId + ".mp3";
            Path filePath = audioPath.resolve(filename);
            Files.write(filePath, audioData);
            
            // Обновляем предложение
            sentence.setAudioFilePath(filePath.toString());
            sentence.setStatus(SentenceStatus.READY);
            sentenceRepository.save(sentence);
            
            log.info("Saved audio for sentence {}: {}", sentenceId, filename);
            
        } catch (IOException e) {
            log.error("Error saving audio for sentence {}", sentenceId, e);
            throw new RuntimeException("Failed to save audio", e);
        }
    }
    
    /**
     * Обновляет настройки персонажа
     */
    @Transactional
    public CharacterDto updateCharacter(Long characterId, CharacterUpdateRequest request) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new RuntimeException("Character not found"));
        
        character.setVoiceId(request.getVoiceId());
        character.setVoiceName(request.getVoiceName());
        character.setDescription(request.getDescription());
        
        character = characterRepository.save(character);
        
        log.info("Updated character {}: voice={}", character.getName(), request.getVoiceId());
        
        return convertCharacterToDto(character);
    }
    
    /**
     * Получает персонажей книги
     */
    public List<CharacterDto> getBookCharacters(Long bookId) {
        List<Character> characters = characterRepository.findByBookId(bookId);
        return characters.stream()
                .map(this::convertCharacterToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Скачивает аудио файл предложения
     */
    public byte[] downloadSentenceAudio(Long sentenceId) {
        try {
            Sentence sentence = sentenceRepository.findById(sentenceId)
                    .orElseThrow(() -> new RuntimeException("Sentence not found"));
            
            if (sentence.getAudioFilePath() == null) {
                throw new RuntimeException("Audio file not available");
            }
            
            Path filePath = Paths.get(sentence.getAudioFilePath());
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Audio file not found");
            }
            
            return Files.readAllBytes(filePath);
            
        } catch (IOException e) {
            log.error("Error reading audio file for sentence {}", sentenceId, e);
            throw new RuntimeException("Failed to read audio file", e);
        }
    }
    
    private String generateAudioPath(Long sentenceId) {
        return Paths.get(audioStoragePath, "sentence_" + sentenceId + ".mp3").toString();
    }
    
    private SentenceDto convertToDto(Sentence sentence) {
        SentenceDto dto = new SentenceDto();
        dto.setId(sentence.getId());
        dto.setSentenceNumber(sentence.getSentenceNumber());
        dto.setOriginalText(sentence.getOriginalText());
        dto.setTextWithStress(sentence.getTextWithStress());
        dto.setStatus(sentence.getStatus());
        dto.setAudioFilePath(sentence.getAudioFilePath());
        dto.setCreatedAt(sentence.getCreatedAt());
        dto.setUpdatedAt(sentence.getUpdatedAt());
        
        if (sentence.getCharacter() != null) {
            dto.setCharacter(convertCharacterToDto(sentence.getCharacter()));
        }
        
        return dto;
    }
    
    private CharacterDto convertCharacterToDto(Character character) {
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
}

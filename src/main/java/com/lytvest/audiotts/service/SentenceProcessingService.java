package com.lytvest.audiotts.service;

import com.lytvest.audiotts.dto.CharacterDto;
import com.lytvest.audiotts.dto.SentenceDto;
import com.lytvest.audiotts.dto.request.CharacterUpdateRequest;
import com.lytvest.audiotts.model.entity.CharacterBook;
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
        CharacterBook characterBook = characterRepository
                .findByBookIdAndName(sentence.getChapter().getBook().getId(), characterName)
                .orElseGet(() -> {
                    CharacterBook newCharacterBook = new CharacterBook();
                    newCharacterBook.setBook(sentence.getChapter().getBook());
                    newCharacterBook.setName(characterName);
                    return characterRepository.save(newCharacterBook);
                });
        
        sentence.setCharacter(characterBook);
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
        CharacterBook characterBook = characterRepository.findById(characterId)
                .orElseThrow(() -> new RuntimeException("Character not found"));
        
        characterBook.setVoiceId(request.getVoiceId());
        characterBook.setVoiceName(request.getVoiceName());
        characterBook.setDescription(request.getDescription());
        
        characterBook = characterRepository.save(characterBook);
        
        log.info("Updated character {}: voice={}", characterBook.getName(), request.getVoiceId());
        
        return convertCharacterToDto(characterBook);
    }
    
    /**
     * Получает персонажей книги
     */
    public List<CharacterDto> getBookCharacters(Long bookId) {
        List<CharacterBook> characterBooks = characterRepository.findByBookId(bookId);
        return characterBooks.stream()
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
    
    private CharacterDto convertCharacterToDto(CharacterBook characterBook) {
        CharacterDto dto = new CharacterDto();
        dto.setId(characterBook.getId());
        dto.setName(characterBook.getName());
        dto.setVoiceId(characterBook.getVoiceId());
        dto.setVoiceName(characterBook.getVoiceName());
        dto.setDescription(characterBook.getDescription());
        dto.setCreatedAt(characterBook.getCreatedAt());
        dto.setUpdatedAt(characterBook.getUpdatedAt());
        return dto;
    }
}

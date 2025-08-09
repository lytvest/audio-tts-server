package com.lytvest.audiotts.service.processor;

import com.lytvest.audiotts.service.SentenceProcessingService;
import com.lytvest.audiotts.service.external.F5TtsService;
import com.lytvest.audiotts.service.external.OllamaService;
import com.lytvest.audiotts.service.queue.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Асинхронные обработчики очередей
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueProcessorService {
    
    private final QueueService queueService;
    private final OllamaService ollamaService;
    private final F5TtsService f5TtsService;
    private final SentenceProcessingService sentenceProcessingService;
    
    private volatile boolean running = true;
    
    /**
     * Запускает обработчики очередей после старта приложения
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startProcessors() {
        log.info("Starting queue processors...");
        processCharacterDeterminationQueue();
        processStressQueue();
        processTtsQueue();
    }
    
    /**
     * Обработчик очереди определения персонажей
     */
    @Async
    public void processCharacterDeterminationQueue() {
        log.info("Started character determination queue processor");
        
        while (running) {
            try {
                CharacterDeterminationTask task = queueService.takeCharacterDeterminationTask();
                
                log.debug("Processing character determination for sentence: {}", task.getSentenceId());
                
                // Получаем разрешение на использование Ollama
                queueService.acquireOllamaPermit();
                
                try {
                    // Определяем персонажа через Ollama
                    String character = ollamaService.determineCharacter(
                            task.getText(), 
                            task.getExistingCharacters()
                    ).block();
                    
                    // Обновляем предложение
                    sentenceProcessingService.updateSentenceCharacter(task.getSentenceId(), character);
                    
                    log.info("Character determined for sentence {}: {}", task.getSentenceId(), character);
                    
                } finally {
                    queueService.releaseOllamaPermit();
                }
                
            } catch (InterruptedException e) {
                log.info("Character determination queue processor interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing character determination task", e);
                // Небольшая пауза перед следующей попыткой
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.info("Character determination queue processor stopped");
    }
    
    /**
     * Обработчик очереди расстановки ударений
     */
    @Async
    public void processStressQueue() {
        log.info("Started stress queue processor");
        
        while (running) {
            try {
                StressTask task = queueService.takeStressTask();
                
                log.debug("Processing stress for sentence: {}", task.getSentenceId());
                
                // Получаем разрешение на использование Ollama
                queueService.acquireOllamaPermit();
                
                try {
                    // Расставляем ударения через Ollama
                    String textWithStress = ollamaService.addStressMarks(task.getText()).block();
                    
                    // Обновляем предложение
                    sentenceProcessingService.updateSentenceStress(task.getSentenceId(), textWithStress);
                    
                    log.info("Stress added for sentence {}", task.getSentenceId());
                    
                } finally {
                    queueService.releaseOllamaPermit();
                }
                
            } catch (InterruptedException e) {
                log.info("Stress queue processor interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing stress task", e);
                // Небольшая пауза перед следующей попыткой
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.info("Stress queue processor stopped");
    }
    
    /**
     * Обработчик очереди TTS
     */
    @Async
    public void processTtsQueue() {
        log.info("Started TTS queue processor");
        
        while (running) {
            try {
                TtsTask task = queueService.takeTtsTask();
                
                log.debug("Processing TTS for sentence: {}", task.getSentenceId());
                
                // Получаем разрешение на использование F5-TTS
                queueService.acquireF5TtsPermit();
                
                try {
                    // Генерируем аудио через F5-TTS
                    byte[] audioData = f5TtsService.generateAudio(task.getText(), task.getVoiceId()).block();
                    
                    // Сохраняем аудио файл
                    sentenceProcessingService.saveSentenceAudio(task.getSentenceId(), audioData);
                    
                    log.info("TTS generated for sentence {}", task.getSentenceId());
                    
                } finally {
                    queueService.releaseF5TtsPermit();
                }
                
            } catch (InterruptedException e) {
                log.info("TTS queue processor interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing TTS task", e);
                // Небольшая пауза перед следующей попыткой
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.info("TTS queue processor stopped");
    }
    
    /**
     * Останавливает все обработчики
     */
    public void stopProcessors() {
        log.info("Stopping queue processors...");
        running = false;
    }
}

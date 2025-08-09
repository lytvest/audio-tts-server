package com.lytvest.audiotts.service.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Сервис очередей для управления последовательной обработкой запросов к внешним сервисам
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {
    
    @Value("${app.processing.sentence-queue-size}")
    private int sentenceQueueSize;
    
    @Value("${app.processing.chapter-queue-size}")
    private int chapterQueueSize;
    
    // Семафоры для ограничения одновременных запросов
    private final Semaphore ollamaSemaphore = new Semaphore(1); // Ollama может обрабатывать только 1 запрос
    private final Semaphore f5ttsSemaphore = new Semaphore(1);  // F5-TTS может обрабатывать только 1 запрос
    
    // Очереди для задач
    private final BlockingQueue<CharacterDeterminationTask> characterQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<StressTask> stressQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<TtsTask> ttsQueue = new LinkedBlockingQueue<>();
    
    /**
     * Добавляет задачу определения персонажа в очередь
     */
    public void addCharacterDeterminationTask(CharacterDeterminationTask task) {
        try {
            characterQueue.put(task);
            log.debug("Added character determination task for sentence ID: {}", task.getSentenceId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while adding character determination task", e);
        }
    }
    
    /**
     * Добавляет задачу расстановки ударений в очередь
     */
    public void addStressTask(StressTask task) {
        try {
            stressQueue.put(task);
            log.debug("Added stress task for sentence ID: {}", task.getSentenceId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while adding stress task", e);
        }
    }
    
    /**
     * Добавляет задачу TTS в очередь
     */
    public void addTtsTask(TtsTask task) {
        try {
            ttsQueue.put(task);
            log.debug("Added TTS task for sentence ID: {}", task.getSentenceId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while adding TTS task", e);
        }
    }
    
    /**
     * Получает следующую задачу определения персонажа
     */
    public CharacterDeterminationTask takeCharacterDeterminationTask() throws InterruptedException {
        return characterQueue.take();
    }
    
    /**
     * Получает следующую задачу расстановки ударений
     */
    public StressTask takeStressTask() throws InterruptedException {
        return stressQueue.take();
    }
    
    /**
     * Получает следующую задачу TTS
     */
    public TtsTask takeTtsTask() throws InterruptedException {
        return ttsQueue.take();
    }
    
    /**
     * Получает разрешение на использование Ollama
     */
    public void acquireOllamaPermit() throws InterruptedException {
        ollamaSemaphore.acquire();
    }
    
    /**
     * Освобождает разрешение на использование Ollama
     */
    public void releaseOllamaPermit() {
        ollamaSemaphore.release();
    }
    
    /**
     * Получает разрешение на использование F5-TTS
     */
    public void acquireF5TtsPermit() throws InterruptedException {
        f5ttsSemaphore.acquire();
    }
    
    /**
     * Освобождает разрешение на использование F5-TTS
     */
    public void releaseF5TtsPermit() {
        f5ttsSemaphore.release();
    }
    
    /**
     * Возвращает статистику очередей
     */
    public QueueStats getQueueStats() {
        return QueueStats.builder()
            .characterQueueSize(characterQueue.size())
            .stressQueueSize(stressQueue.size())
            .ttsQueueSize(ttsQueue.size())
            .ollamaAvailable(ollamaSemaphore.availablePermits() > 0)
            .f5ttsAvailable(f5ttsSemaphore.availablePermits() > 0)
            .build();
    }
    
    public static class QueueStats {
        public final int characterQueueSize;
        public final int stressQueueSize;
        public final int ttsQueueSize;
        public final boolean ollamaAvailable;
        public final boolean f5ttsAvailable;
        
        private QueueStats(int characterQueueSize, int stressQueueSize, int ttsQueueSize, 
                          boolean ollamaAvailable, boolean f5ttsAvailable) {
            this.characterQueueSize = characterQueueSize;
            this.stressQueueSize = stressQueueSize;
            this.ttsQueueSize = ttsQueueSize;
            this.ollamaAvailable = ollamaAvailable;
            this.f5ttsAvailable = f5ttsAvailable;
        }
        
        public static QueueStatsBuilder builder() {
            return new QueueStatsBuilder();
        }
        
        public static class QueueStatsBuilder {
            private int characterQueueSize;
            private int stressQueueSize;
            private int ttsQueueSize;
            private boolean ollamaAvailable;
            private boolean f5ttsAvailable;
            
            public QueueStatsBuilder characterQueueSize(int characterQueueSize) {
                this.characterQueueSize = characterQueueSize;
                return this;
            }
            
            public QueueStatsBuilder stressQueueSize(int stressQueueSize) {
                this.stressQueueSize = stressQueueSize;
                return this;
            }
            
            public QueueStatsBuilder ttsQueueSize(int ttsQueueSize) {
                this.ttsQueueSize = ttsQueueSize;
                return this;
            }
            
            public QueueStatsBuilder ollamaAvailable(boolean ollamaAvailable) {
                this.ollamaAvailable = ollamaAvailable;
                return this;
            }
            
            public QueueStatsBuilder f5ttsAvailable(boolean f5ttsAvailable) {
                this.f5ttsAvailable = f5ttsAvailable;
                return this;
            }
            
            public QueueStats build() {
                return new QueueStats(characterQueueSize, stressQueueSize, ttsQueueSize, 
                                    ollamaAvailable, f5ttsAvailable);
            }
        }
    }
}
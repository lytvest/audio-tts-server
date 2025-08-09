package com.lytvest.audiotts.model.entity;

import com.lytvest.audiotts.model.enums.SentenceStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "sentences")
@Data
@EqualsAndHashCode(exclude = {"chapter", "character"})
@ToString(exclude = {"chapter", "character"})
public class Sentence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private Character character;
    
    @Column(name = "sentence_number", nullable = false)
    private Integer sentenceNumber;
    
    @Lob
    @Column(name = "original_text", nullable = false)
    private String originalText;
    
    @Lob
    @Column(name = "text_with_stress")
    private String textWithStress;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SentenceStatus status = SentenceStatus.WAITING_FOR_CHARACTER;
    
    @Column(name = "audio_file_path")
    private String audioFilePath;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

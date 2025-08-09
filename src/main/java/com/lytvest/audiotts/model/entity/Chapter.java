package com.lytvest.audiotts.model.entity;

import com.lytvest.audiotts.model.enums.ChapterStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chapters")
@Data
@EqualsAndHashCode(exclude = {"book", "sentences"})
@ToString(exclude = {"book", "sentences"})
public class Chapter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    @Column(nullable = false)
    private String title;
    
    @Column(name = "chapter_number")
    private Integer chapterNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChapterStatus status = ChapterStatus.WAITING;
    
    @Column(name = "audio_file_path")
    private String audioFilePath;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Sentence> sentences = new ArrayList<>();
    
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

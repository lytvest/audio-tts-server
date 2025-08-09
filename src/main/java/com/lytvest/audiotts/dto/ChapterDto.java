package com.lytvest.audiotts.dto;

import com.lytvest.audiotts.model.enums.ChapterStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChapterDto {
    private Long id;
    private String title;
    private Integer chapterNumber;
    private ChapterStatus status;
    private String audioFilePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SentenceDto> sentences;
    private int totalSentences;
    private int readySentences;
}
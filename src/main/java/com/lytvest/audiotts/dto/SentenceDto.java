package com.lytvest.audiotts.dto;

import com.lytvest.audiotts.model.enums.SentenceStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SentenceDto {
    private Long id;
    private Integer sentenceNumber;
    private String originalText;
    private String textWithStress;
    private SentenceStatus status;
    private String audioFilePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private CharacterDto character;
}
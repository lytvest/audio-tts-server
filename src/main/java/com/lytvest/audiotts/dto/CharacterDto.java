package com.lytvest.audiotts.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CharacterDto {
    private Long id;
    private String name;
    private String voiceId;
    private String voiceName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
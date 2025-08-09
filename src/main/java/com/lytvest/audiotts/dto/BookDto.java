package com.lytvest.audiotts.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookDto {
    private Long id;
    private String title;
    private String author;
    private String originalFilename;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChapterDto> chapters;
    private List<CharacterDto> characters;
}
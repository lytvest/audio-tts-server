package com.lytvest.audiotts.service.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterDeterminationTask {
    private Long sentenceId;
    private String text;
    private List<String> existingCharacters;
    private Long bookId;
}
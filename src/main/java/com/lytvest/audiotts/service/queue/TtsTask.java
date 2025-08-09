package com.lytvest.audiotts.service.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TtsTask {
    private Long sentenceId;
    private String text;
    private String voiceId;
    private String outputPath;
}
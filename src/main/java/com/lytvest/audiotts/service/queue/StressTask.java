package com.lytvest.audiotts.service.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StressTask {
    private Long sentenceId;
    private String text;
}
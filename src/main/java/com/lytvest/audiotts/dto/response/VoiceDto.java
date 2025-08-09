package com.lytvest.audiotts.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoiceDto {
    private String id;
    private String name;
    private String language;
    private String gender;
    private boolean available;
}
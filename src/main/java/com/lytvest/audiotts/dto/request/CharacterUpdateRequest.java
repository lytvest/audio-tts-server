package com.lytvest.audiotts.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CharacterUpdateRequest {
    
    @NotBlank(message = "Voice ID is required")
    private String voiceId;
    
    @Size(max = 255, message = "Voice name must not exceed 255 characters")
    private String voiceName;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
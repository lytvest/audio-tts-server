package com.lytvest.audiotts.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class BookUploadRequest {
    
    @NotNull(message = "File is required")
    private MultipartFile file;
    
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Size(max = 255, message = "Author must not exceed 255 characters")
    private String author;
}
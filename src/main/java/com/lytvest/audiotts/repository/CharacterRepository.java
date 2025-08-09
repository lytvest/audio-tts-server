package com.lytvest.audiotts.repository;

import com.lytvest.audiotts.model.entity.CharacterBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<CharacterBook, Long> {
    
    List<CharacterBook> findByBookId(Long bookId);
    
    Optional<CharacterBook> findByBookIdAndName(Long bookId, String name);
    
    List<CharacterBook> findByBookIdAndVoiceIdIsNotNull(Long bookId);
    
    List<CharacterBook> findByVoiceId(String voiceId);
}
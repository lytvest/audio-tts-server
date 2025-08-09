package com.lytvest.audiotts.repository;

import com.lytvest.audiotts.model.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {
    
    List<Character> findByBookId(Long bookId);
    
    Optional<Character> findByBookIdAndName(Long bookId, String name);
    
    List<Character> findByBookIdAndVoiceIdIsNotNull(Long bookId);
    
    List<Character> findByVoiceId(String voiceId);
}
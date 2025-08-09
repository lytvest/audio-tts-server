package com.lytvest.audiotts.repository;

import com.lytvest.audiotts.model.entity.Chapter;
import com.lytvest.audiotts.model.enums.ChapterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    
    List<Chapter> findByBookIdOrderByChapterNumber(Long bookId);
    
    List<Chapter> findByStatus(ChapterStatus status);
    
    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.sentences WHERE c.id = :id")
    Optional<Chapter> findByIdWithSentences(Long id);
    
    @Query("SELECT c FROM Chapter c WHERE c.book.id = :bookId AND c.chapterNumber = :chapterNumber")
    Optional<Chapter> findByBookIdAndChapterNumber(Long bookId, Integer chapterNumber);
}
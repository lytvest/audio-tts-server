package com.lytvest.audiotts.repository;

import com.lytvest.audiotts.model.entity.Sentence;
import com.lytvest.audiotts.model.enums.SentenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentenceRepository extends JpaRepository<Sentence, Long> {
    
    List<Sentence> findByChapterIdOrderBySentenceNumber(Long chapterId);
    
    List<Sentence> findByStatus(SentenceStatus status);
    
    Page<Sentence> findByStatus(SentenceStatus status, Pageable pageable);
    
    List<Sentence> findByChapterIdAndStatus(Long chapterId, SentenceStatus status);
    
    @Query("SELECT COUNT(s) FROM Sentence s WHERE s.chapter.id = :chapterId AND s.status = :status")
    long countByChapterIdAndStatus(Long chapterId, SentenceStatus status);
    
    @Query("SELECT COUNT(s) FROM Sentence s WHERE s.chapter.id = :chapterId")
    long countByChapterId(Long chapterId);
    
    @Query("SELECT s FROM Sentence s WHERE s.status = :status ORDER BY s.id LIMIT 1")
    Sentence findFirstByStatusOrderById(SentenceStatus status);
}
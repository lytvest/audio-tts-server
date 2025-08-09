package com.lytvest.audiotts.repository;

import com.lytvest.audiotts.model.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    Optional<Book> findByTitle(String title);
    
    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.chapters WHERE b.id = :id")
    Optional<Book> findByIdWithChapters(Long id);
    
    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.characters WHERE b.id = :id")
    Optional<Book> findByIdWithCharacters(Long id);
    
    List<Book> findByAuthorContainingIgnoreCase(String author);
}
package com.lytvest.audiotts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.text.BreakIterator;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class Fb2ParserService {
    
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]+\\s*");
    
    public static class ParsedBook {
        public String title;
        public String author;
        public List<ParsedChapter> chapters = new ArrayList<>();
    }
    
    public static class ParsedChapter {
        public String title;
        public int chapterNumber;
        public List<String> sentences = new ArrayList<>();
    }
    
    /**
     * Парсит FB2 файл и извлекает структуру книги
     */
    public ParsedBook parseFb2File(byte[] fileContent) {
        try {
            String content = new String(fileContent, "UTF-8");
            Document doc = Jsoup.parse(content, "", org.jsoup.parser.Parser.xmlParser());
            
            ParsedBook book = new ParsedBook();
            
            // Извлекаем метаданные
            extractMetadata(doc, book);
            
            // Извлекаем главы
            extractChapters(doc, book);
            
            log.info("Parsed FB2 book: '{}' by {} with {} chapters", 
                book.title, book.author, book.chapters.size());
            
            return book;
            
        } catch (Exception e) {
            log.error("Error parsing FB2 file", e);
            throw new RuntimeException("Failed to parse FB2 file", e);
        }
    }
    
    private void extractMetadata(Document doc, ParsedBook book) {
        // Извлекаем название книги
        Element titleInfo = doc.selectFirst("title-info");
        if (titleInfo != null) {
            Element bookTitle = titleInfo.selectFirst("book-title");
            if (bookTitle != null) {
                book.title = bookTitle.text().trim();
            }
            
            // Извлекаем автора
            Element author = titleInfo.selectFirst("author");
            if (author != null) {
                StringBuilder authorName = new StringBuilder();
                Element firstName = author.selectFirst("first-name");
                Element lastName = author.selectFirst("last-name");
                Element middleName = author.selectFirst("middle-name");
                
                if (firstName != null) {
                    authorName.append(firstName.text());
                }
                if (middleName != null) {
                    if (authorName.length() > 0) authorName.append(" ");
                    authorName.append(middleName.text());
                }
                if (lastName != null) {
                    if (authorName.length() > 0) authorName.append(" ");
                    authorName.append(lastName.text());
                }
                
                book.author = authorName.toString().trim();
            }
        }
        
        // Значения по умолчанию если не найдены
        if (book.title == null || book.title.isEmpty()) {
            book.title = "Неизвестная книга";
        }
        if (book.author == null || book.author.isEmpty()) {
            book.author = "Неизвестный автор";
        }
    }
    
    private void extractChapters(Document doc, ParsedBook book) {
        // Ищем body элемент
        Element body = doc.selectFirst("body");
        if (body == null) {
            log.warn("No body element found in FB2 file");
            return;
        }
        
        // Ищем секции (главы)
        Elements sections = body.select("section");
        
        if (sections.isEmpty()) {
            // Если нет секций, создаем одну главу из всего содержимого body
            ParsedChapter chapter = new ParsedChapter();
            chapter.title = "Глава 1";
            chapter.chapterNumber = 1;
            extractSentencesFromElement(body, chapter);
            
            if (!chapter.sentences.isEmpty()) {
                book.chapters.add(chapter);
            }
        } else {
            // Обрабатываем каждую секцию как главу
            int chapterNumber = 1;
            for (Element section : sections) {
                ParsedChapter chapter = new ParsedChapter();
                chapter.chapterNumber = chapterNumber++;
                
                // Извлекаем заголовок главы
                Element title = section.selectFirst("title");
                if (title != null) {
                    chapter.title = title.text().trim();
                } else {
                    chapter.title = "Глава " + chapter.chapterNumber;
                }
                
                // Извлекаем предложения
                extractSentencesFromElement(section, chapter);
                
                if (!chapter.sentences.isEmpty()) {
                    book.chapters.add(chapter);
                }
            }
        }
    }
    
    private void extractSentencesFromElement(Element element, ParsedChapter chapter) {
        // Извлекаем все параграфы
        Elements paragraphs = element.select("p");
        
        for (Element paragraph : paragraphs) {
            String text = paragraph.text().trim();
            if (!text.isEmpty()) {
                List<String> sentences = splitIntoSentences(text);
                chapter.sentences.addAll(sentences);
            }
        }
        
        // Если нет параграфов, берем весь текст
        if (paragraphs.isEmpty()) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                List<String> sentences = splitIntoSentences(text);
                chapter.sentences.addAll(sentences);
            }
        }
    }
    
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        
        // Используем BreakIterator для более точного разбиения на предложения
        BreakIterator iterator = BreakIterator.getSentenceInstance(new Locale("ru"));
        iterator.setText(text);
        
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start, end).trim();
            
            // Фильтруем слишком короткие "предложения"
            if (sentence.length() > 3 && !sentence.matches("^[\\s\\p{Punct}]*$")) {
                sentences.add(sentence);
            }
        }
        
        // Если BreakIterator не сработал, используем простое разбиение
        if (sentences.isEmpty()) {
            String[] parts = SENTENCE_PATTERN.split(text);
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.length() > 3) {
                    sentences.add(trimmed);
                }
            }
        }
        
        return sentences;
    }
}

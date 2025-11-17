package com.npu.lms.service;

import com.npu.lms.dto.CategoryStatsDTO;
import com.npu.lms.dto.StagnantBookDTO;
import com.npu.lms.entity.Book;
import com.npu.lms.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    // --- (基础 CRUD 方法) ---

    // 用于借阅记录 DTO 映射
    public Book findBookById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    // 用于图书管理页面
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // 用于图书入库
    @Transactional
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    // 用于图书出库
    @Transactional
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    // 用于图书编辑
    @Transactional
    public Book updateBook(Long id, Book bookDetails) {
        Book book = findBookById(id);
        if (book == null) {
            throw new RuntimeException("未找到 ID 为 " + id + " 的图书");
        }

        // 更新所有 V2 字段
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setIsbn(bookDetails.getIsbn());
        book.setStock(bookDetails.getStock());
        book.setAvailable(bookDetails.getAvailable());
        book.setCategory(bookDetails.getCategory());
        book.setCover(bookDetails.getCover());
        book.setPublisher(bookDetails.getPublisher());
        book.setPublicationDate(bookDetails.getPublicationDate());
        book.setStatus(bookDetails.getStatus());
        book.setPrice(bookDetails.getPrice());
        book.setSupplier(bookDetails.getSupplier());
        book.setIntroduction(bookDetails.getIntroduction());

        return bookRepository.save(book);
    }

    // --- 【数据分析方法 (2 个)】 ---

    // 1. 图书分类占比
    public List<CategoryStatsDTO> getBookCategoryStats() {
        return bookRepository.getBookCategoryStats();
    }

    // 2. 滞销图书
    public List<StagnantBookDTO> getStagnantBooks() {
        // 定义“滞销”为 6 个月内未被借阅
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        // 返回 Top 10 滞销图书
        return bookRepository.findStagnantBooks(sixMonthsAgo, PageRequest.of(0, 10));
    }
}
package com.npu.lms.service;

import com.npu.lms.dto.CategoryStatsDTO; // 引入 DTO
import com.npu.lms.entity.Book;
import com.npu.lms.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List; // 引入 List

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    // --- (您原有的 findBookById, getAllBooks 等方法) ---

    // 假设您有这个方法 (被 RecordService 使用)
    public Book findBookById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    // 假设您有这个方法 (被 Controller 使用)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // (其他保存、删除图书的方法...)
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }


    // --- 【新增：数据分析方法】 ---
    public List<CategoryStatsDTO> getBookCategoryStats() {
        return bookRepository.getBookCategoryStats();
    }
}
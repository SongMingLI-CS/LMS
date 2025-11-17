package com.npu.lms.controller;

import com.npu.lms.dto.CategoryStatsDTO; // 引入 DTO
import com.npu.lms.entity.Book;
import com.npu.lms.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List; // 引入 List

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    // --- (您原有的图书 CRUD 接口) ---

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @PostMapping
    public Book createBook(@RequestBody Book book) {
        return bookService.saveBook(book);
    }

    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        Book book = bookService.findBookById(id);
        if (book != null) {
            // (更新逻辑)
            book.setTitle(bookDetails.getTitle());
            book.setAuthor(bookDetails.getAuthor());
            book.setIsbn(bookDetails.getIsbn());
            book.setStock(bookDetails.getStock());
            book.setAvailable(bookDetails.getAvailable());
            book.setCategory(bookDetails.getCategory()); // 确保分类被更新
            book.setCover(bookDetails.getCover());
            return bookService.saveBook(book);
        }
        return null; // or throw exception
    }

    @DeleteMapping("/{id}")
    public void deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
    }

    // --- 【新增：数据分析接口】 ---
    @GetMapping("/analysis/categories")
    public ResponseEntity<List<CategoryStatsDTO>> getCategoryStats() {
        return ResponseEntity.ok(bookService.getBookCategoryStats());
    }
}
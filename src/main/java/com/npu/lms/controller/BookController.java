package com.npu.lms.controller;

import com.npu.lms.dto.CategoryStatsDTO;
import com.npu.lms.dto.StagnantBookDTO;
import com.npu.lms.entity.Book;
import com.npu.lms.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    // --- (基础 CRUD 接口) ---

    // 获取所有图书 (供图书管理页面使用)
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    // 图书入库 (新增)
    @PostMapping
    public Book createBook(@RequestBody Book book) {
        // 在 V2 中，我们应该使用 DTO，但目前保持实体以匹配前端
        return bookService.saveBook(book);
    }

    // 图书编辑 (更新)
    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        // 使用 Service 层的更新逻辑
        return bookService.updateBook(id, bookDetails);
    }

    // 图书出库 (删除)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok().build();
    }

    // --- 【数据分析接口 (2 个)】 ---

    // 图书分类占比
    @GetMapping("/analysis/categories")
    public ResponseEntity<List<CategoryStatsDTO>> getCategoryStats() {
        return ResponseEntity.ok(bookService.getBookCategoryStats());
    }

    // 【新增】滞销图书
    @GetMapping("/analysis/stagnant-books")
    public ResponseEntity<List<StagnantBookDTO>> getStagnantBooks() {
        return ResponseEntity.ok(bookService.getStagnantBooks());
    }
}
package com.npu.lms.controller;

import com.npu.lms.entity.Book;
import com.npu.lms.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books") // 统一 API 路径
public class BookController {

    @Autowired
    private BookService bookService;

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.findAllBooks();
    }

    @PostMapping
    public Book createBook(@RequestBody Book book) {
        // 新增时 ID 应为 null
        book.setId(null);
        return bookService.saveBook(book);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        Book book = bookService.findBookById(id);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        // 更新字段
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setIsbn(bookDetails.getIsbn());
        book.setStock(bookDetails.getStock());
        book.setAvailable(bookDetails.getAvailable());
        book.setCover(bookDetails.getCover());

        return ResponseEntity.ok(bookService.saveBook(book));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        // 确保图书存在
        if (bookService.findBookById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
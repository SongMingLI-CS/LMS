package com.npu.lms.service;

import com.npu.lms.entity.Book;
import com.npu.lms.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    public List<Book> findAllBooks() {
        return bookRepository.findAll();
    }

    public Book saveBook(Book book) {
        // (可以添加 ISBN 唯一性检查等)
        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        // (可以增加检查：如果书在借，是否允许删除？)
        bookRepository.deleteById(id);
    }

    public Book findBookById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }
}
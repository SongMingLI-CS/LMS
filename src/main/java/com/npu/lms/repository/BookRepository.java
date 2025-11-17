package com.npu.lms.repository;

import com.npu.lms.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
    // JpaRepository 提供了所有基础 CRUD (增删改查)
}
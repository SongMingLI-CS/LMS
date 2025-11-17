package com.npu.lms.repository;

import com.npu.lms.dto.CategoryStatsDTO; // 引入 DTO
import com.npu.lms.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // 引入 Query
import org.springframework.stereotype.Repository;
import java.util.List; // 引入 List

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // 【新增】图书分类统计
    @Query("SELECT NEW com.npu.lms.dto.CategoryStatsDTO(b.category, COUNT(b.id)) " +
            "FROM Book b " +
            "WHERE b.category IS NOT NULL " +
            "GROUP BY b.category ORDER BY COUNT(b.id) DESC")
    List<CategoryStatsDTO> getBookCategoryStats();
}
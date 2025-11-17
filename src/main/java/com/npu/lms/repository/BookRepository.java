package com.npu.lms.repository;

import com.npu.lms.dto.CategoryStatsDTO;
import com.npu.lms.dto.StagnantBookDTO; // 1. 引入新 DTO
import com.npu.lms.entity.Book;
import org.springframework.data.domain.Pageable; // 引入 Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // 引入 Param
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime; // 引入 LocalDateTime

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // (图书分类统计查询保持不变)
    @Query("SELECT NEW com.npu.lms.dto.CategoryStatsDTO(b.category, COUNT(b.id)) " +
            "FROM Book b WHERE b.category IS NOT NULL " +
            "GROUP BY b.category ORDER BY COUNT(b.id) DESC")
    List<CategoryStatsDTO> getBookCategoryStats();

    // --- 【新增：滞销图书统计】 ---
    // (查询所有图书，按最后借阅时间排序)
    // (LEFT JOIN 确保从未被借阅过的书也会被包含，其 MAX(r.borrowTime) 为 NULL)
    @Query("SELECT NEW com.npu.lms.dto.StagnantBookDTO(b.title, b.category, MAX(r.borrowTime)) " +
            "FROM Book b LEFT JOIN BorrowRecord r ON b.id = r.book.id " +
            "GROUP BY b.id, b.title, b.category " +
            "HAVING MAX(r.borrowTime) < :sixMonthsAgo OR MAX(r.borrowTime) IS NULL " +
            "ORDER BY MAX(r.borrowTime) ASC NULLS FIRST") // NULLS FIRST 确保从未借过的书排在最前面
    List<StagnantBookDTO> findStagnantBooks(@Param("sixMonthsAgo") LocalDateTime sixMonthsAgo, Pageable pageable);
}
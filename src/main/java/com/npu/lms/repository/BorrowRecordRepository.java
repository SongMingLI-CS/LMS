package com.npu.lms.repository;

// 引入新的 DTO
import com.npu.lms.dto.PopularBookDTO;
import com.npu.lms.dto.ActiveUserDTO;
import com.npu.lms.entity.BorrowRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    // JPQL 查询 1: 热门图书 (使用 PopularBookDTO)
    @Query("SELECT NEW com.npu.lms.dto.PopularBookDTO(b.title, COUNT(r.book.id)) " +
            "FROM BorrowRecord r JOIN r.book b " + // JOIN r.book 隐式关联
            "WHERE r.status = 'borrowed' " +
            "GROUP BY b.title ORDER BY COUNT(r.book.id) DESC")
    List<PopularBookDTO> findTop5PopularBooks(Pageable pageable); // <--- 修改返回类型


    // JPQL 查询 2: 活跃读者 (使用 ActiveUserDTO)
    @Query("SELECT NEW com.npu.lms.dto.ActiveUserDTO(u.name, COUNT(r.user.id)) " +
            "FROM BorrowRecord r JOIN r.user u " + // JOIN r.user 隐式关联
            "WHERE r.status = 'borrowed' " +
            "GROUP BY u.name ORDER BY COUNT(r.user.id) DESC")
    List<ActiveUserDTO> findTop5ActiveUsers(Pageable pageable); // <--- 修改返回类型

    // --- 以下是您原有的方法 (保持不变) ---
    List<BorrowRecord> findByUserId(Long userId);
    List<BorrowRecord> findByUserIdAndStatus(Long userId, String status);
    List<BorrowRecord> findByBookIdAndStatus(Long bookId, String status);
    Optional<BorrowRecord> findByBookIdAndUserIdAndStatus(Long bookId, Long userId, String status);
}
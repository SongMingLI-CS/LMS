package com.npu.lms.repository;

import com.npu.lms.dto.PopularBookDTO;
import com.npu.lms.dto.ActiveUserDTO;
import com.npu.lms.dto.PeakTimeDTO; // 引入 DTO
import com.npu.lms.entity.BorrowRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    // --- (Top 5 查询保持不变) ---
    @Query("SELECT NEW com.npu.lms.dto.PopularBookDTO(b.title, COUNT(r.book.id)) " +
            "FROM BorrowRecord r JOIN r.book b " +
            "WHERE r.status = 'borrowed' " +
            "GROUP BY b.title ORDER BY COUNT(r.book.id) DESC")
    List<PopularBookDTO> findTop5PopularBooks(Pageable pageable);

    @Query("SELECT NEW com.npu.lms.dto.ActiveUserDTO(u.name, COUNT(r.user.id)) " +
            "FROM BorrowRecord r JOIN r.user u " +
            "WHERE r.status = 'borrowed' " +
            "GROUP BY u.name ORDER BY COUNT(r.user.id) DESC")
    List<ActiveUserDTO> findTop5ActiveUsers(Pageable pageable);

    // --- (原有业务查询保持不变) ---
    List<BorrowRecord> findByUserId(Long userId);
    List<BorrowRecord> findByUserIdAndStatus(Long userId, String status);
    List<BorrowRecord> findByBookIdAndStatus(Long bookId, String status);
    Optional<BorrowRecord> findByBookIdAndUserIdAndStatus(Long bookId, Long userId, String status);


    // --- 【新增：高峰时段统计 (原生 SQL)】 ---
    // (假设您的 borrowTime 字段在数据库中是 borrow_time (DATETIME 类型))
    @Query(value = "SELECT " +
            "    CASE " +
            "        WHEN HOUR(r.borrow_time) BETWEEN 9 AND 10 THEN '9-11' " +
            "        WHEN HOUR(r.borrow_time) BETWEEN 11 AND 12 THEN '11-13' " +
            "        WHEN HOUR(r.borrow_time) BETWEEN 13 AND 14 THEN '13-15' " +
            "        WHEN HOUR(r.borrow_time) BETWEEN 15 AND 16 THEN '15-17' " +
            "        WHEN HOUR(r.borrow_time) BETWEEN 17 AND 18 THEN '17-19' " +
            "        ELSE 'Other' " +
            "    END AS hourSlot, " +
            "    COUNT(r.id) AS count " +
            "FROM borrow_records r " +
            "WHERE HOUR(r.borrow_time) BETWEEN 9 AND 18 " +
            "GROUP BY hourSlot " +
            "ORDER BY hourSlot",
            nativeQuery = true)
    List<Object[]> getPeakBorrowingTimesNative(); // 返回 Object 数组
}
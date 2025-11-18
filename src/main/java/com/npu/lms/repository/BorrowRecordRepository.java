package com.npu.lms.repository;

// 1. 引入所有必需的 DTO
import com.npu.lms.dto.PopularBookDTO;
import com.npu.lms.dto.ActiveUserDTO;
import com.npu.lms.dto.PeakTimeDTO;
import com.npu.lms.dto.OverdueUserDTO; // 引入逾期 DTO

import com.npu.lms.entity.BorrowRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    // --- (数据分析 JPQL 查询) ---

    // 1. 热门图书 Top 5 (【修复后】)
    @Query("SELECT NEW com.npu.lms.dto.PopularBookDTO(b.title, COUNT(r.book.id)) " +
            "FROM BorrowRecord r JOIN r.book b " +
            // 我们要统计所有借阅记录 (包括 'borrowed' 和 'returned')，所以删除 WHERE r.status
            "GROUP BY b.title ORDER BY COUNT(r.book.id) DESC")
    List<PopularBookDTO> findTop5PopularBooks(Pageable pageable);

    // 2. 活跃读者 Top 5
    @Query("SELECT NEW com.npu.lms.dto.ActiveUserDTO(u.name, COUNT(r.user.id)) " +
            "FROM BorrowRecord r JOIN r.user u " +
            "WHERE r.status = 'borrowed' " +
            "GROUP BY u.name ORDER BY COUNT(r.user.id) DESC")
    List<ActiveUserDTO> findTop5ActiveUsers(Pageable pageable);

    // 3. 借阅高峰时段 (原生 SQL)
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

    // 4. 【新增】逾期读者统计
    // (查询当前状态为 'borrowed' 且应还日期已过的记录)
    @Query("SELECT NEW com.npu.lms.dto.OverdueUserDTO(u.name, COUNT(r.id)) " +
            "FROM BorrowRecord r JOIN r.user u " +
            "WHERE r.status = 'borrowed' AND r.dueDate < CURRENT_DATE " +
            "GROUP BY u.name ORDER BY COUNT(r.id) DESC")
    List<OverdueUserDTO> findOverdueUsers();


    List<BorrowRecord> findByUserId(Long userId);
    List<BorrowRecord> findByUserIdAndStatus(Long userId, String status);
    List<BorrowRecord> findByBookIdAndStatus(Long bookId, String status);
    Optional<BorrowRecord> findByBookIdAndUserIdAndStatus(Long bookId, Long userId, String status);
    List<BorrowRecord> findByBookIdAndStatusOrderByBorrowTimeAsc(Long bookId, String status);
    // 【新增】查找所有状态为 'awaiting_pickup' 且预约到期日早于“今天”的记录
    List<BorrowRecord> findByStatusAndReservationExpiryDateBefore(String status, LocalDate today);
}
package com.npu.lms.repository;

import com.npu.lms.dto.PopularBookDTO;
import com.npu.lms.dto.ActiveUserDTO;
import com.npu.lms.dto.PeakTimeDTO;
import com.npu.lms.dto.OverdueUserDTO; // 1. 引入新 DTO
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

    // --- (高峰时段查询保持不变) ---
    @Query(value = "SELECT CASE ... (代码不变) ... END AS hourSlot, COUNT(r.id) AS count " +
            "FROM borrow_records r WHERE HOUR(r.borrow_time) BETWEEN 9 AND 18 " +
            "GROUP BY hourSlot ORDER BY hourSlot",
            nativeQuery = true)
    List<Object[]> getPeakBorrowingTimesNative();

    // --- 【新增：逾期读者统计】 ---
    // (查询当前状态为 'borrowed' 且应还日期已过的记录)
    @Query("SELECT NEW com.npu.lms.dto.OverdueUserDTO(u.name, COUNT(r.id)) " +
            "FROM BorrowRecord r JOIN r.user u " +
            "WHERE r.status = 'borrowed' AND r.dueDate < CURRENT_DATE " +
            "GROUP BY u.name ORDER BY COUNT(r.id) DESC")
    List<OverdueUserDTO> findOverdueUsers();


    // --- (原有业务查询保持不变) ---
    List<BorrowRecord> findByUserId(Long userId);
    List<BorrowRecord> findByUserIdAndStatus(Long userId, String status);
    List<BorrowRecord> findByBookIdAndStatus(Long bookId, String status);
    Optional<BorrowRecord> findByBookIdAndUserIdAndStatus(Long bookId, Long userId, String status);
}
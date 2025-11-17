package com.npu.lms.repository;

import com.npu.lms.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    // 查找某个用户的所有记录
    List<BorrowRecord> findByUserId(Long userId);

    // 查找某个用户处于特定状态的记录 (例如：统计在借数量)
    List<BorrowRecord> findByUserIdAndStatus(Long userId, String status);

    // 查找某本书的特定状态记录 (例如：查找预约)
    List<BorrowRecord> findByBookIdAndStatus(Long bookId, String status);

    // 查找某个用户对某本书的特定记录 (用于还书或检查是否已借)
    Optional<BorrowRecord> findByBookIdAndUserIdAndStatus(Long bookId, Long userId, String status);
}
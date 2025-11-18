package com.npu.lms.repository;

import com.npu.lms.entity.BookBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookBatchRepository extends JpaRepository<BookBatch, Long> {
    // 暂时不需要自定义方法
}
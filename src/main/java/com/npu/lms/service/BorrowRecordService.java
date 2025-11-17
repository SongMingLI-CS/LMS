package com.npu.lms.service;

import com.npu.lms.entity.BorrowRecord;
import com.npu.lms.repository.BorrowRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BorrowRecordService {

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    public List<BorrowRecord> findAllRecords() {
        return borrowRecordRepository.findAll();
    }

    // TODO: 在这里添加借阅、归还、预约的业务逻辑
    // (TODO: Add business logic for borrow, return, reserve here)
}
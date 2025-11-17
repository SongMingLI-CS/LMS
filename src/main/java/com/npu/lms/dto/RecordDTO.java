package com.npu.lms.dto;

import java.time.LocalDate;

/**
 * 借阅记录数据传输对象 (用于前端显示)
 */
public class RecordDTO {
    private Long id;

    // 关键：直接包含名称，而不是让前端去查
    private String bookTitle;
    private String userName;

    private Long bookId;
    private Long userId;

    private LocalDate borrowDate;
    private LocalDate dueDate;
    private String status;

    // --- Getters and Setters (请确保完整复制) ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
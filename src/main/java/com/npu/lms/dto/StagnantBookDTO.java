package com.npu.lms.dto;

import java.time.LocalDateTime;

/**
 * DTO (数据传输对象) - 滞销图书统计
 */
public class StagnantBookDTO {
    private String title;
    private String category;
    private LocalDateTime lastBorrowTime; // 最后借阅时间 (可能为 null)

    /**
     * 供 JPQL 使用的构造函数
     * @param title (b.title)
     * @param category (b.category)
     * @param lastBorrowTime (MAX(r.borrowTime))
     */
    public StagnantBookDTO(String title, String category, LocalDateTime lastBorrowTime) {
        this.title = title;
        this.category = category;
        this.lastBorrowTime = lastBorrowTime;
    }

    // --- Getters and Setters ---
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getLastBorrowTime() { return lastBorrowTime; }
    public void setLastBorrowTime(LocalDateTime lastBorrowTime) { this.lastBorrowTime = lastBorrowTime; }
}
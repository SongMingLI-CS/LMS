package com.npu.lms.dto;

/**
 * DTO (数据传输对象) - 热门图书分析结果
 * 字段名 (title, borrowCount) 严格匹配前端 Vue 模板的需求
 */
public class PopularBookDTO {
    private String title;       // 匹配前端的 book.title
    private Long borrowCount; // 匹配前端的 book.borrowCount

    /**
     * 供 JPQL 使用的构造函数，参数顺序和类型必须匹配
     * @param title (b.title)
     * @param borrowCount (COUNT(r.book.id))
     */
    public PopularBookDTO(String title, Long borrowCount) {
        this.title = title;
        this.borrowCount = borrowCount;
    }

    // --- Getters and Setters ---

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getBorrowCount() {
        return borrowCount;
    }

    public void setBorrowCount(Long borrowCount) {
        this.borrowCount = borrowCount;
    }
}
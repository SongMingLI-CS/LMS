package com.npu.lms.dto;

/**
 * DTO (数据传输对象) - 活跃读者分析结果
 * 字段名 (name, borrowCount) 严格匹配前端 Vue 模板的需求
 */
public class ActiveUserDTO {
    private String name;        // 匹配前端的 user.name
    private Long borrowCount; // 匹配前端的 user.borrowCount

    /**
     * 供 JPQL 使用的构造函数，参数顺序和类型必须匹配
     * @param name (u.name)
     * @param borrowCount (COUNT(r.user.id))
     */
    public ActiveUserDTO(String name, Long borrowCount) {
        this.name = name;
        this.borrowCount = borrowCount;
    }

    // --- Getters and Setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBorrowCount() {
        return borrowCount;
    }

    public void setBorrowCount(Long borrowCount) {
        this.borrowCount = borrowCount;
    }
}
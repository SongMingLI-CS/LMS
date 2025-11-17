package com.npu.lms.dto;

/**
 * DTO (数据传输对象) - 图书分类统计
 * 字段名 (category, count) 严格匹配前端 Vue 模板的需求
 */
public class CategoryStatsDTO {
    private String category;
    private Long count;

    /**
     * 供 JPQL 使用的构造函数，参数顺序和类型必须匹配
     * @param category (b.category)
     * @param count (COUNT(b.id))
     */
    public CategoryStatsDTO(String category, Long count) {
        this.category = category;
        this.count = count;
    }

    // --- Getters and Setters ---
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public Long getCount() {
        return count;
    }
    public void setCount(Long count) {
        this.count = count;
    }
}
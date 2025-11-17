package com.npu.lms.dto;

/**
 * DTO (数据传输对象) - 逾期读者统计
 */
public class OverdueUserDTO {
    private String name;
    private Long overdueCount; // 逾期次数

    /**
     * 供 JPQL 使用的构造函数
     * @param name (u.name)
     * @param overdueCount (COUNT(r.id))
     */
    public OverdueUserDTO(String name, Long overdueCount) {
        this.name = name;
        this.overdueCount = overdueCount;
    }

    // --- Getters and Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(Long overdueCount) { this.overdueCount = overdueCount; }
}
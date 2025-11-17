package com.npu.lms.dto;

/**
 * DTO (数据传输对象) - 借阅高峰时段统计
 */
public class PeakTimeDTO {
    private String hourSlot; // 例如 "9-11"
    private Long count;

    /**
     * 供 Service 层手动映射使用
     * @param hourSlot (e.g., "9-11")
     * @param count (e.g., 40)
     */
    public PeakTimeDTO(String hourSlot, Long count) {
        this.hourSlot = hourSlot;
        this.count = count;
    }

    // --- Getters and Setters ---
    public String getHourSlot() {
        return hourSlot;
    }
    public void setHourSlot(String hourSlot) {
        this.hourSlot = hourSlot;
    }
    public Long getCount() {
        return count;
    }
    public void setCount(Long count) {
        this.count = count;
    }
}
package com.npu.lms.dto;

// (你可能需要添加 @Data (lombok) 或者手写 Getters/Setters)
public class ProfileUpdateRequest {

    private String name;
    // (将来可以扩展，例如添加电话号码等)

    // --- Getters and Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
package com.npu.lms.service;

import org.springframework.core.io.InputStreamSource;

/**
 * 【新增 V3】邮件附件的 DTO
 */
public class EmailAttachment {

    private String filename;
    private InputStreamSource inputStreamSource;
    private String contentType;

    public EmailAttachment(String filename, InputStreamSource inputStreamSource, String contentType) {
        this.filename = filename;
        this.inputStreamSource = inputStreamSource;
        this.contentType = contentType;
    }

    // --- Getters ---
    public String getFilename() { return filename; }
    public InputStreamSource getInputStreamSource() { return inputStreamSource; }
    public String getContentType() { return contentType; }
}
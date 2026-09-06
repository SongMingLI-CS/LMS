package com.npu.lms.dto;

import java.math.BigDecimal;

/**
 * P2 采购：订单明细请求。
 */
public class PurchaseOrderItemRequest {

    private String isbn;
    private String title;
    private int quantity;
    private BigDecimal unitPrice;

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}

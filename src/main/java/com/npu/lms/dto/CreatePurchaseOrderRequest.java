package com.npu.lms.dto;

import java.util.List;

/**
 * P2 采购：创建采购订单请求。
 */
public class CreatePurchaseOrderRequest {

    private Long supplierId;
    private String remark;
    private List<PurchaseOrderItemRequest> items;

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public List<PurchaseOrderItemRequest> getItems() { return items; }
    public void setItems(List<PurchaseOrderItemRequest> items) { this.items = items; }
}

package com.npu.lms.controller;

import com.npu.lms.dto.CreatePurchaseOrderRequest;
import com.npu.lms.entity.PurchaseOrder;
import com.npu.lms.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * P2 采购与资产：采购订单（创建/验收/取消），仅限管理员/超级管理员。
 */
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @GetMapping
    public List<PurchaseOrder> listOrders() {
        return purchaseOrderService.listOrders();
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreatePurchaseOrderRequest request) {
        try {
            return ResponseEntity.ok(purchaseOrderService.createOrder(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<?> receiveOrder(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(purchaseOrderService.receiveOrder(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(purchaseOrderService.cancelOrder(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

package com.npu.lms.controller;

import com.npu.lms.entity.Supplier;
import com.npu.lms.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * P2 采购与资产：供应商管理（仅限管理员/超级管理员）。
 */
@RestController
@RequestMapping("/api/suppliers")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
public class SupplierController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @GetMapping
    public List<Supplier> listSuppliers() {
        return purchaseOrderService.listSuppliers();
    }

    @PostMapping
    public ResponseEntity<?> createSupplier(@RequestBody Supplier supplier) {
        try {
            return ResponseEntity.ok(purchaseOrderService.createSupplier(supplier));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

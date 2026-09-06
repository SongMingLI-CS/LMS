package com.npu.lms.service;

import com.npu.lms.dto.CreatePurchaseOrderRequest;
import com.npu.lms.dto.PurchaseOrderItemRequest;
import com.npu.lms.entity.Book;
import com.npu.lms.entity.BookBatch;
import com.npu.lms.entity.PurchaseOrder;
import com.npu.lms.entity.PurchaseOrderItem;
import com.npu.lms.entity.Supplier;
import com.npu.lms.repository.BookBatchRepository;
import com.npu.lms.repository.BookRepository;
import com.npu.lms.repository.PurchaseOrderRepository;
import com.npu.lms.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * P2 采购与资产：供应商 + 采购订单 + 验收入库。
 *
 * <p>验收入库时会自动：查/建书目 -> 增加库存 -> 记录批次 -> 生成馆藏单册，
 * 与馆藏建模闭环。</p>
 */
@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository orderRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookBatchRepository bookBatchRepository;

    @Autowired
    private BookItemService bookItemService;

    // --- 供应商 ---

    @Transactional
    public Supplier createSupplier(Supplier supplier) {
        if (supplierRepository.findByName(supplier.getName()).isPresent()) {
            throw new RuntimeException("供应商已存在: " + supplier.getName());
        }
        return supplierRepository.save(supplier);
    }

    public List<Supplier> listSuppliers() {
        return supplierRepository.findAll();
    }

    // --- 采购订单 ---

    @Transactional
    public PurchaseOrder createOrder(CreatePurchaseOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("订单明细不能为空");
        }
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("未找到供应商"));

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(generateOrderNo());
        order.setSupplier(supplier);
        order.setOrderDate(LocalDate.now());
        order.setStatus(PurchaseOrder.STATUS_DRAFT);
        order.setRemark(request.getRemark());

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getIsbn() == null || itemReq.getIsbn().isBlank() || itemReq.getQuantity() <= 0) {
                throw new RuntimeException("订单明细的 ISBN 与数量必须有效");
            }
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(order);
            item.setIsbn(itemReq.getIsbn().trim());
            item.setTitle(itemReq.getTitle() == null ? "" : itemReq.getTitle());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            order.getItems().add(item);
            if (item.getUnitPrice() != null) {
                total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    /**
     * 验收入库：将草稿订单的每项转化为实际馆藏。
     */
    @Transactional
    public PurchaseOrder receiveOrder(Long orderId) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("未找到订单"));
        if (!PurchaseOrder.STATUS_DRAFT.equals(order.getStatus())) {
            throw new RuntimeException("只有草稿状态订单可以验收");
        }
        for (PurchaseOrderItem item : order.getItems()) {
            receiveItem(item, order.getSupplier());
        }
        order.setStatus(PurchaseOrder.STATUS_RECEIVED);
        return orderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder cancelOrder(Long orderId) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("未找到订单"));
        if (!PurchaseOrder.STATUS_DRAFT.equals(order.getStatus())) {
            throw new RuntimeException("只有草稿状态订单可以取消");
        }
        order.setStatus(PurchaseOrder.STATUS_CANCELLED);
        return orderRepository.save(order);
    }

    public List<PurchaseOrder> listOrders() {
        return orderRepository.findAll();
    }

    private void receiveItem(PurchaseOrderItem item, Supplier supplier) {
        Book book = bookRepository.findByIsbn(item.getIsbn()).orElse(null);
        if (book == null) {
            // 新书目：先落库获得 ID，便于后续生成单册
            book = new Book();
            book.setTitle(item.getTitle());
            book.setAuthor("未知");
            book.setIsbn(item.getIsbn());
            book.setStock(0);
            book.setAvailable(0);
            book.setStatus("可借");
            book = bookRepository.save(book);
        }
        book.setStock(book.getStock() + item.getQuantity());
        book.setAvailable(book.getAvailable() + item.getQuantity());
        bookRepository.save(book);

        // 记录入库批次
        BookBatch batch = new BookBatch();
        batch.setBook(book);
        batch.setQuantity(item.getQuantity());
        batch.setSupplier(supplier.getName());
        batch.setPrice(item.getUnitPrice());
        bookBatchRepository.save(batch);

        // 生成馆藏单册
        bookItemService.addItemsToBook(book, item.getQuantity());
    }

    private String generateOrderNo() {
        return "PO" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}

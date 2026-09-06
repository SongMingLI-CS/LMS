package com.npu.lms.service;

import com.npu.lms.dto.CreatePurchaseOrderRequest;
import com.npu.lms.dto.PurchaseOrderItemRequest;
import com.npu.lms.entity.Book;
import com.npu.lms.entity.BookBatch;
import com.npu.lms.entity.Branch;
import com.npu.lms.entity.PurchaseOrder;
import com.npu.lms.entity.PurchaseOrderItem;
import com.npu.lms.entity.Supplier;
import com.npu.lms.repository.BookBatchRepository;
import com.npu.lms.repository.BookItemRepository;
import com.npu.lms.repository.BookRepository;
import com.npu.lms.repository.BranchRepository;
import com.npu.lms.repository.PurchaseOrderRepository;
import com.npu.lms.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseOrderServiceTest {

    private PurchaseOrderRepository orderRepository;
    private SupplierRepository supplierRepository;
    private BookRepository bookRepository;
    private BookBatchRepository bookBatchRepository;
    private BookItemRepository bookItemRepository;
    private PurchaseOrderService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(PurchaseOrderRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        bookRepository = mock(BookRepository.class);
        bookBatchRepository = mock(BookBatchRepository.class);
        bookItemRepository = mock(BookItemRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);

        // BookItemService 是具体类，Java 25 + Mockito inline mock 无法直接 mock，
        // 因此注入真实实例 + mock 其依赖。
        BookItemService realBookItemService = new BookItemService();
        Branch branch = new Branch();
        branch.setId(1L);
        branch.setCode("MAIN");
        branch.setName("主馆");
        when(branchRepository.findByCode("MAIN")).thenReturn(Optional.of(branch));
        when(bookItemRepository.countByBookId(anyLong())).thenReturn(0L);
        when(bookItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        ReflectionTestUtils.setField(realBookItemService, "bookItemRepository", bookItemRepository);
        ReflectionTestUtils.setField(realBookItemService, "branchRepository", branchRepository);
        ReflectionTestUtils.setField(realBookItemService, "bookRepository", bookRepository);

        service = new PurchaseOrderService();
        ReflectionTestUtils.setField(service, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(service, "supplierRepository", supplierRepository);
        ReflectionTestUtils.setField(service, "bookRepository", bookRepository);
        ReflectionTestUtils.setField(service, "bookBatchRepository", bookBatchRepository);
        ReflectionTestUtils.setField(service, "bookItemService", realBookItemService);
    }

    @Test
    void createOrderComputesTotalAmount() {
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("书商A");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderItemRequest item = new PurchaseOrderItemRequest();
        item.setIsbn("978-111");
        item.setTitle("测试书");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("30.00"));

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
        request.setSupplierId(1L);
        request.setItems(List.of(item));

        PurchaseOrder order = service.createOrder(request);

        assertEquals("draft", order.getStatus());
        assertEquals(new BigDecimal("60.00"), order.getTotalAmount());
        assertEquals(1, order.getItems().size());
        assertEquals(supplier, order.getSupplier());
    }

    @Test
    void receiveOrderCreatesNewBookBatchAndItems() {
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("书商A");

        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setSupplier(supplier);
        order.setStatus(PurchaseOrder.STATUS_DRAFT);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(order);
        item.setIsbn("978-222");
        item.setTitle("新书");
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("10"));
        order.getItems().add(item);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(bookRepository.findByIsbn("978-222")).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book book = inv.getArgument(0);
            if (book.getId() == null) {
                book.setId(100L);
            }
            return book;
        });
        when(bookBatchRepository.save(any(BookBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        PurchaseOrder received = service.receiveOrder(1L);

        assertEquals(PurchaseOrder.STATUS_RECEIVED, received.getStatus());
        // 书目被创建并加了库存
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository, atLeastOnce()).save(bookCaptor.capture());
        Book saved = bookCaptor.getValue();
        assertEquals(3, saved.getStock());
        assertEquals(3, saved.getAvailable());
        // 批次已记录
        verify(bookBatchRepository).save(any(BookBatch.class));

        // 馆藏单册生成了 3 条
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(bookItemRepository).saveAll(itemCaptor.capture());
        assertEquals(3, itemCaptor.getValue().size());
    }

    @Test
    void receiveOrderRejectsNonDraft() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setStatus(PurchaseOrder.STATUS_RECEIVED);
        order.setItems(List.of());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(RuntimeException.class, () -> service.receiveOrder(1L));
    }

    @Test
    void createOrderRejectsEmptyItems() {
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest();
        request.setSupplierId(1L);
        request.setItems(List.of());
        assertThrows(RuntimeException.class, () -> service.createOrder(request));
        verify(orderRepository, never()).save(any());
    }
}

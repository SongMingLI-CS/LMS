package com.npu.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * P1 馆藏建模：馆藏单册（一本实体书）。
 *
 * <p>与书目 {@link Book}（作品/版本）分离，每条单册拥有唯一条码、馆藏位置（分馆 + 书架）与状态，
 * 用于支撑盘点、调拨、遗失、维修等操作。</p>
 */
@Entity
@Table(name = "book_items")
public class BookItem {

    public static final String STATUS_AVAILABLE = "available";
    public static final String STATUS_BORROWED = "borrowed";
    public static final String STATUS_RESERVED = "reserved";
    public static final String STATUS_LOST = "lost";
    public static final String STATUS_DAMAGED = "damaged";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String barcode; // 馆藏条码

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "shelf_no")
    private String shelfNo; // 书架号

    @Column(nullable = false)
    private String status; // available / borrowed / reserved / lost / damaged

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt; // 入库时间

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public String getShelfNo() { return shelfNo; }
    public void setShelfNo(String shelfNo) { this.shelfNo = shelfNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAcquiredAt() { return acquiredAt; }
    public void setAcquiredAt(LocalDateTime acquiredAt) { this.acquiredAt = acquiredAt; }
}

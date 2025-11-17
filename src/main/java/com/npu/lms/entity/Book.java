package com.npu.lms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal; // 引入 BigDecimal 用于价格
import java.time.LocalDate;  // 引入 LocalDate 用于出版日期

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(nullable = false)
    private int stock; // 总库存

    @Column(nullable = false)
    private int available; // 可借阅数量

    private String cover; // 封面图片URL

    @Column(name = "category")
    private String category; // 图书分类 (我们之前已添加)

    // --- 【新增字段 (V2 需求)】 ---

    @Column(name = "publisher")
    private String publisher; // 出版社

    @Column(name = "publication_date")
    private LocalDate publicationDate; // 出版日期

    @Column(name = "status", length = 50)
    private String status; // 图书状态 (例如: "可借", "维修中", "下架")

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price; // 价格

    @Column(name = "supplier")
    private String supplier; // 供应商

    @Column(name = "introduction", length = 2048) // 简介 (设置为 2048 字符)
    private String introduction;


    // --- Getters and Setters (包含所有新字段) ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getAvailable() { return available; }
    public void setAvailable(int available) { this.available = available; }
    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // 新字段的 Getters/Setters
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }
}
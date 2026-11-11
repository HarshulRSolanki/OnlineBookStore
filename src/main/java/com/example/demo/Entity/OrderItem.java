package com.example.demo.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private double unitPrice;

    @Column(nullable = false)
    private double subtotal;

    @Column(name = "book_title")
    private String bookTitle;

    @Column(name = "book_author")
    private String bookAuthor;

    @Column(name = "category_name")
    private String categoryName;

    public OrderItem() {
    }

    public OrderItem(Book book, int quantity, double unitPrice) {
        this.book = book;
        this.quantity = quantity > 0 ? quantity : 1;
        this.unitPrice = unitPrice;
        this.subtotal = this.unitPrice * this.quantity;
        if (book != null) {
            this.bookTitle = book.getTitle();
            this.bookAuthor = book.getAuthor();
            this.categoryName = book.getCategory() != null ? book.getCategory().getName() : "General";
        }
    }

    @PrePersist
    public void prePersist() {
        if (quantity <= 0) {
            quantity = 1;
        }
        if (unitPrice <= 0 && book != null) {
            unitPrice = book.getPrice();
        }
        if (subtotal <= 0) {
            subtotal = unitPrice * quantity;
        }
        if (book != null) {
            if (bookTitle == null || bookTitle.isBlank()) {
                bookTitle = book.getTitle();
            }
            if (bookAuthor == null || bookAuthor.isBlank()) {
                bookAuthor = book.getAuthor();
            }
            if ((categoryName == null || categoryName.isBlank()) && book.getCategory() != null) {
                categoryName = book.getCategory().getName();
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
        if (book != null) {
            if (this.bookTitle == null) this.bookTitle = book.getTitle();
            if (this.bookAuthor == null) this.bookAuthor = book.getAuthor();
            if (this.categoryName == null && book.getCategory() != null) {
                this.categoryName = book.getCategory().getName();
            }
        }
    }

    public int getQuantity() {
        return quantity <= 0 ? 1 : quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity > 0 ? quantity : 1;
    }

    public double getUnitPrice() {
        if (unitPrice <= 0 && book != null) {
            return book.getPrice();
        }
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getSubtotal() {
        if (subtotal <= 0) {
            return getUnitPrice() * getQuantity();
        }
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getEffectiveTitle() {
        if (book != null && book.getTitle() != null && !book.getTitle().isBlank()) {
            return book.getTitle();
        }
        return bookTitle != null ? bookTitle : "Acquired Literary Edition";
    }

    public String getEffectiveAuthor() {
        if (book != null && book.getAuthor() != null && !book.getAuthor().isBlank()) {
            return book.getAuthor();
        }
        return bookAuthor != null ? bookAuthor : "Curated Collection";
    }

    public String getEffectiveCategory() {
        if (book != null && book.getCategory() != null && book.getCategory().getName() != null) {
            return book.getCategory().getName();
        }
        return categoryName != null ? categoryName : "Literature";
    }

    public Long getEffectiveBookId() {
        return book != null ? book.getId() : null;
    }
}


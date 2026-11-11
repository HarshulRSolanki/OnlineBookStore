package com.example.demo.Entity;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

@Entity
public class Book 
{
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    
    private String author;
    
    @ManyToOne
    @JoinColumn(name = "category_id")  // Foreign key column in Book table
    private Category category;
    
    private String isbn;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private int price;
    
    @Lob 
    private byte[] coverImage;

    private LocalDate publishDate; // Store the publishing date
    
    private String language;
    
    @Basic(fetch = FetchType.LAZY)
	@Lob // This annotation ensures the field is stored as a BLOB in the database
	@Column(columnDefinition = "LONGBLOB") 
	private byte[] fileData;
    
    @Column(nullable = false)
    private boolean isDeleted = false; 

    @Column(name = "stock")
    private Integer stock = 100;
    
    @Transient
    private Long downloadCount;
    
    @Transient
    private Long cartCount;
    
    
    
    
    public Long getDownloadCount() {
		return downloadCount;
	}
	public void setDownloadCount(Long downloadCount) {
		this.downloadCount = downloadCount;
	}
	public Long getCartCount() {
		return cartCount;
	}
	public void setCartCount(Long cartCount) {
		this.cartCount = cartCount;
	}
	public List<Cart> getCarts() {
		return carts;
	}
	public void setCarts(List<Cart> carts) {
		this.carts = carts;
	}
	public List<Order> getOrders() {
		return orders;
	}
	public void setOrders(List<Order> orders) {
		this.orders = orders;
	}
	public boolean isDeleted() {
		return isDeleted;
	}
	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}


	private String fileType; // MIME type (e.g., "application/pdf")
    
    private String publishername;
    
    private double averageRating;
    
    private int ratingCount;
    
    public double getAverageRating() {
		return averageRating;
	}
	public void setAverageRating(double averageRating) {
		this.averageRating = averageRating;
	}
	public int getRatingCount() {
		return ratingCount;
	}
	public void setRatingCount(int ratingCount) {
		this.ratingCount = ratingCount;
	}
	public String getPublishername() {
		return publishername;
	}
	public void setPublishername(String publishername) {
		this.publishername = publishername;
	}
	
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}

 	public byte[] getCoverImage() {
 
		return coverImage;
	}
	public void setCoverImage(byte[] coverImage) {
		this.coverImage = coverImage;
	} 
	public LocalDate getPublishDate() {
		return publishDate;
	}
	public void setPublishDate(LocalDate publishDate) {
		this.publishDate = publishDate;
	}


	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public Category getCategory() {
		return category;
	}
	public void setCategory(Category category) {
		this.category = category;
	}
	public String getIsbn() {
		return isbn;
	}
	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	

	@OneToMany(mappedBy = "book", cascade = CascadeType.REMOVE)
    private List<Cart> carts;

    @OneToMany(mappedBy = "book")
    private List<Order> orders;

    @OneToMany(mappedBy = "book")
    private List<OrderItem> orderItems;

	public Integer getStock() {
		return stock != null ? stock : 100;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

	public List<OrderItem> getOrderItems() {
		return orderItems;
	}

	public void setOrderItems(List<OrderItem> orderItems) {
		this.orderItems = orderItems;
	}
	

	public byte[] getFileData() {
		return fileData;
	}
	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}
	
}

package com.example.demo.Entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Size;

@Entity
public class User
{
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userid;
	
	@Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
	@Column(unique = true)
	private String username;
	
	@Column(unique = true)
	private String email;
	
	//@Size(max = 16 ,min = 8)
	//@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$", message = "Password must be at least 8 characters long, contain an uppercase letter, a lowercase letter, and a number")
	private String password;
	
	private String mobileno;
	
	private boolean isregistered;

	
	public boolean isIsregistered() {
		return isregistered;
	}

	public void setIsregistered(boolean isregistered) {
		this.isregistered = isregistered;
	}

	public Long getUserid() {
		return userid;
	}

	public void setUserid(Long userid) {
		this.userid = userid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getMobileno() {
		return mobileno;
	}

	public void setMobileno(String mobileno) {
		this.mobileno = mobileno;
	}

	public void setOtp(String otp) {	}
	

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private List<Cart> cartItems = new ArrayList<>();


	public List<Cart> getCartItems() {
		return cartItems;
	}

	public void setCartItems(List<Cart> cartItems) {
		this.cartItems = cartItems;
	}
	
	
	 @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	    private List<Order> orders;


	public List<Order> getOrders() {
		return orders;
	}

	public void setOrders(List<Order> orders) {
		this.orders = orders;
	}

	@OneToMany(mappedBy = "user")
	private List<Download> downloadedBooks;


	public List<Download> getDownloadedBooks() {
		return downloadedBooks;
	}

	public void setDownloadedBooks(List<Download> downloadedBooks) {
		this.downloadedBooks = downloadedBooks;
	}

}

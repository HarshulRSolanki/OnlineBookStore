package com.example.demo.Entity;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Download
{
	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne
	    @JoinColumn(name = "user_id")
	    private User user; // The user who downloaded the book

	    @ManyToOne
	    @JoinColumn(name = "book_id")
	    private Book book; // The downloaded book

	    private LocalDateTime downloadDate; // Timestamp of the download

	    // Constructors
	    public Download() {
	        this.downloadDate = LocalDateTime.now(); // Set current time when downloaded
	    }

		public Download(User user2, Book book2) {
			this();
			this.user = user2;
			this.book = book2;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}

		public Book getBook() {
			return book;
		}

		public void setBook(Book book) {
			this.book = book;
		}

		public LocalDateTime getDownloadDate() {
			return downloadDate;
		}

		public void setDownloadDate(LocalDateTime downloadDate) {
			this.downloadDate = downloadDate;
		}

	}



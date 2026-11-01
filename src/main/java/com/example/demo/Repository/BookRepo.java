package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Category;

@Repository
public interface BookRepo extends JpaRepository<Book, Long>
{
	List<Book> findByIsDeletedFalse();
	
	 List<Book> findByCategoryAndIsDeletedFalse(Category category);
	
	List<Book> findByTitleContainingIgnoreCaseAndIsDeletedFalse(String title);

	List<Book> findByAuthorContainingIgnoreCaseAndIsDeletedFalse(String author);

	List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrCategory_NameContainingIgnoreCase(
			String query, String query2, String query3);
	
	
}

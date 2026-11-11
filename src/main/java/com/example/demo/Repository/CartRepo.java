package com.example.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Cart;
import com.example.demo.Entity.User;

@Repository
public interface CartRepo extends JpaRepository<Cart,Long> 
{
	
/*
 	List<Cart> findByUser(User user);
 
	boolean existsByUserAndBook(User user, Book book);	

	boolean findByUserAndBook(User user, Book book);
	
	Optional<Cart> findByUserAndBookId(User user, Long bookId);
*/
	List<Cart> findByUser(User user);

	boolean existsByUserAndBook(User user, Book book);

	Optional<Cart> findByUserAndBook(User user, Book book);

	Optional<Cart> findByUserAndBookId(User user, Long bookId);

	int countByBookId(Long bookId);

	@Transactional
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.book.id = :bookId")
	int deleteByBookId(@Param("bookId") Long bookId);

	@Query("SELECT COUNT(c) FROM Cart c WHERE c.book = :book")
	    Long countCartsByBook(@Param("book") Book book);

	@Transactional
	@Modifying
	@Query("DELETE FROM Cart c WHERE c.user.userid = :userId")
	int deleteAllByUserId(@Param("userId") Long userId);

	@Transactional
	@Modifying
	@Query("UPDATE Cart c SET c.order = NULL WHERE c.user = :user")
	int clearOrderReferenceForUser(@Param("user") User user);
	
}

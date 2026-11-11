package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Download;
import com.example.demo.Entity.User;

import jakarta.transaction.Transactional;

@Repository
public interface DownloadRepo extends JpaRepository<Download,Long>
{
	List<Download> findByUser(User user);

	@Modifying
	@Transactional
	@Query("DELETE FROM Download d WHERE d.book.id = :bookId")
	void deleteDownloadsByBookId(@Param("bookId") Long bookId);
	
	boolean existsByUserAndBook(User user, Book book);
	 	
	@Query("SELECT COUNT(d) FROM Download d WHERE d.book = :book") 
	Long countDownloadsByBook(@Param("book") Book book);

	boolean existsByBookId(Long bookId);
	
	@Query("SELECT d FROM Download d WHERE FUNCTION('MONTH', d.downloadDate) = :value")
	List<Download> findDownloadsByMonth(@Param("value") int month);

	@Query("SELECT d FROM Download d WHERE FUNCTION('YEAR', d.downloadDate) = :value")
	List<Download> findDownloadsByYear(@Param("value") int year);
	
}

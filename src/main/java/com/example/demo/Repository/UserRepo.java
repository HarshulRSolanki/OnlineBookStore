package com.example.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.User;

@Repository
public interface UserRepo extends JpaRepository<User,Long> {

	 User findByUsername(String username);

	boolean existsByEmail(String email);

	public User getUserByEmail(String name);

	User findByEmail(String email);

	Optional<User> findById(Long userId);


	
	@Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.orders LEFT JOIN FETCH u.downloadedBooks LEFT JOIN FETCH u.cartItems")
	List<User> fetchUserReportData();

	
	//List<User> getAllUsers();

	

	

}

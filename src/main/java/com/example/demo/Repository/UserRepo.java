package com.example.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.User;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {

    User findByUsername(String username);

    User findByUsernameIgnoreCase(String username);

    boolean existsByEmail(String email);

    User getUserByEmail(String email);

    User findByEmail(String email);

    User findByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE LOWER(TRIM(u.username)) = LOWER(TRIM(:identifier)) OR LOWER(TRIM(u.email)) = LOWER(TRIM(:identifier))")
    List<User> findByIdentifierFlexible(@org.springframework.data.repository.query.Param("identifier") String identifier);

    Optional<User> findById(Long userId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.orders LEFT JOIN FETCH u.downloadedBooks LEFT JOIN FETCH u.cartItems")
    List<User> fetchUserReportData();
}

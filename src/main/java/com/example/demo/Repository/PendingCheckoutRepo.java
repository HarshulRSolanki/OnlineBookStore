package com.example.demo.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entity.PendingCheckout;
import com.example.demo.Entity.User;

@Repository
public interface PendingCheckoutRepo extends JpaRepository<PendingCheckout, Long> {

    Optional<PendingCheckout> findByCheckoutToken(String checkoutToken);

    @Transactional
    @Modifying
    @Query("DELETE FROM PendingCheckout p WHERE p.user = :user")
    int deleteAllByUser(@Param("user") User user);
}


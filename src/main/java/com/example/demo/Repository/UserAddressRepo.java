package com.example.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entity.User;
import com.example.demo.Entity.UserAddress;

@Repository
public interface UserAddressRepo extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserOrderByDefaultAddressDescUpdatedAtDesc(User user);

    Optional<UserAddress> findByIdAndUser(Long id, User user);

    Optional<UserAddress> findFirstByUserAndDefaultAddressTrue(User user);

    long countByUser(User user);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserAddress a SET a.defaultAddress = false WHERE a.user = :user")
    int clearDefaultForUser(@Param("user") User user);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserAddress a WHERE a.user = :user")
    int deleteAllByUser(@Param("user") User user);
}


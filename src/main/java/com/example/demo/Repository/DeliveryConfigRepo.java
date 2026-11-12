package com.example.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.DeliveryConfig;

@Repository
public interface DeliveryConfigRepo extends JpaRepository<DeliveryConfig, Long> {
}


package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.StoreLocation;

@Repository
public interface StoreLocationRepo extends JpaRepository<StoreLocation, Long> {

    List<StoreLocation> findAllByOrderByActiveDescStateAscCityAscStoreNameAsc();

    List<StoreLocation> findByActiveTrueOrderByStateAscCityAscStoreNameAsc();

    long countByActiveTrue();
}


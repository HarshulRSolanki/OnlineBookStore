package com.example.demo.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.Entity.Inquiry;

@Repository
public interface InquiryRepo extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findAllByOrderBySubmittedAtDesc();
}


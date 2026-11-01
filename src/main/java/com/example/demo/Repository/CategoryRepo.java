package com.example.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.Category;

@Repository
public interface CategoryRepo extends JpaRepository<Category,Long>
{
	@Query("SELECT COUNT(d) FROM Download d WHERE d.book.category = :category")
    Long countByCategory(@Param("category") Category category);
}

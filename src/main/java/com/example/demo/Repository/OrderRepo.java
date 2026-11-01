package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.Entity.Order;
import com.example.demo.Entity.User;

public interface OrderRepo  extends JpaRepository<Order,Long>
{
	@Query("SELECT o FROM Order o WHERE FUNCTION('MONTH', o.orderDate) = :value")
	List<Order> findOrdersByMonth(@Param("value") int month);

	@Query("SELECT o FROM Order o WHERE FUNCTION('YEAR', o.orderDate) = :value")
	List<Order> findOrdersByYear(@Param("value") int year);
	
	List<Order> getOrdersByUser(User user);

	List<Order> findByUser(User user);

	
	

}

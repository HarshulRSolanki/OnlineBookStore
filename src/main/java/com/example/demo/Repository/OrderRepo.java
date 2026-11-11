package com.example.demo.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.Entity.Order;
import com.example.demo.Entity.User;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE FUNCTION('MONTH', o.orderDate) = :value")
    List<Order> findOrdersByMonth(@Param("value") int month);

    @Query("SELECT o FROM Order o WHERE FUNCTION('YEAR', o.orderDate) = :value")
    List<Order> findOrdersByYear(@Param("value") int year);

    List<Order> getOrdersByUser(User user);

    List<Order> findByUser(User user);

    List<Order> findByUserOrderByOrderDateDesc(User user);

    List<Order> findAllByOrderByOrderDateDesc();

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByReceiptNumber(String receiptNumber);

    List<Order> findAllByStripeSessionId(String stripeSessionId);

    Optional<Order> findFirstByStripeSessionIdOrderByIdAsc(String stripeSessionId);

    boolean existsByStripeSessionId(String stripeSessionId);

    boolean existsByBookId(Long bookId);

    long countByUser(User user);

    boolean existsByStripeSessionIdAndPaymentStatus(String stripeSessionId, String paymentStatus);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    List<Order> findByPaymentStatus(String paymentStatus);

    long countByPaymentStatus(String paymentStatus);

    @Query("SELECT COALESCE(SUM(o.amount), 0.0) FROM Order o WHERE o.paymentStatus = 'SUCCESS' OR o.paymentStatus = 'PAID'")
    Double sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.quantity), 0) FROM Order o WHERE o.paymentStatus = 'SUCCESS' OR o.paymentStatus = 'PAID'")
    Long sumTotalBooksSold();

    @Query("SELECT o.book.title, COUNT(o), COALESCE(SUM(o.amount), 0.0) FROM Order o WHERE (o.paymentStatus = 'SUCCESS' OR o.paymentStatus = 'PAID') AND o.book IS NOT NULL GROUP BY o.book.title ORDER BY COUNT(o) DESC")
    List<Object[]> findTopSellingBooks();

    @Query("SELECT o.book.category.name, COALESCE(SUM(o.amount), 0.0) FROM Order o WHERE (o.paymentStatus = 'SUCCESS' OR o.paymentStatus = 'PAID') AND o.book IS NOT NULL AND o.book.category IS NOT NULL GROUP BY o.book.category.name ORDER BY SUM(o.amount) DESC")
    List<Object[]> findRevenueByCategory();
}

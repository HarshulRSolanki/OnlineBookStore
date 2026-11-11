package com.example.demo.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Cart;
import com.example.demo.Entity.User;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.UserRepo;

@Service
public class CartService {

    @Autowired
    private CartRepo cartRepository;

    @Autowired
    private UserRepo userRepository;

    public List<Cart> getCartItems(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return cartRepository.findByUser(user);
    }

    public List<Cart> getActiveCartItems(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        return cartRepository.findByUser(user).stream()
                .filter(c -> c.getBook() != null && !c.getBook().isDeleted())
                .toList();
    }

    public Optional<Cart> getCartItemByUserAndBook(User user, Long bookId) {
        return cartRepository.findByUserAndBookId(user, bookId);
    }

    public Optional<Cart> getCartItemById(Long cartId) {
        return cartRepository.findById(cartId);
    }

    @Transactional
    public void removeBookFromCarts(Long cartId) {
        if (cartId != null && cartRepository.existsById(cartId)) {
            cartRepository.deleteById(cartId);
        }
    }

    @Transactional
    public Cart updateQuantity(Long cartId, User owner, int requestedQuantity) {
        if (cartId == null || owner == null) {
            return null;
        }
        Cart cart = cartRepository.findById(cartId).orElse(null);
        if (cart == null || cart.getUser() == null || !cart.getUser().getUserid().equals(owner.getUserid())) {
            return null;
        }
        if (requestedQuantity <= 0) {
            cartRepository.delete(cart);
            return null;
        }
        Book book = cart.getBook();
        int finalQty = requestedQuantity;
        if (book != null && book.getStock() != null && book.getStock() > 0 && finalQty > book.getStock()) {
            finalQty = book.getStock();
        }
        cart.setQuantity(finalQty);
        return cartRepository.save(cart);
    }

    public double calculateCartTotal(User user) {
        double total = 0.0;
        for (Cart item : getActiveCartItems(user)) {
            int qty = item.getQuantity() > 0 ? item.getQuantity() : 1;
            total += item.getBook().getPrice() * qty;
        }
        return total;
    }

    @Transactional
    public void clearCart(Long userId) {
        if (userId != null) {
            cartRepository.deleteAllByUserId(userId);
            cartRepository.flush();
        }
    }
}

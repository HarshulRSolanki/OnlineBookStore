package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Entity.Cart;
import com.example.demo.Entity.User;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.UserRepo;

@Service
public class CartService 
{
 
 	@Autowired
    private CartRepo cartRepository;

    @Autowired
    private UserRepo userRepository;
    /*
   @Autowired
    private BookRepo bookRepository;

    
     public void addToCart(Long userId, Long bookId) {
   
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new RuntimeException("Book not found"));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setBook(book);
        cart.setQuantity(1);

        cartRepository.save(cart);
    }
   
    */	
    
 
    public List<Cart> getCartItems(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return cartRepository.findByUser(user);
    }

    public Optional<Cart> getCartItemByUserAndBook(User user, Long bookId) {
        return cartRepository.findByUserAndBookId(user, bookId);
    }

 
   
    public Optional<Cart> getCartItemById(Long cartId) {
        return cartRepository.findById(cartId);
    }

    public void removeBookFromCarts(Long cartId) {
        if (cartRepository.existsById(cartId)) {
            cartRepository.deleteById(cartId);
        }
    }

	public void clearCart(Long userid) {
		// TODO Auto-generated method stub
		
	}

}
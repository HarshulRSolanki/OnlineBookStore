package com.example.demo.Controller;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Cart;
import com.example.demo.Entity.Category;
import com.example.demo.Entity.User;
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.CategoryRepo;
import com.example.demo.Repository.UserRepo;
import com.example.demo.service.CartService;
import com.example.demo.service.EmailService;
import com.example.demo.service.OTPservice;

import jakarta.servlet.http.HttpSession;

@Controller
public class Logincontroller 
{		
	@Autowired
	PasswordEncoder passwordEncoder;
		
    @Autowired
    private UserRepo userRepo;	

    @Autowired
    private BookRepo bookRepo;
    
    @Autowired
    private CartRepo cartRepo;
    
    @Autowired
    private CategoryRepo categoryRepo;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private OTPservice otpservice;
    
    @Autowired
    private EmailService emailService;
    
    @GetMapping("/")
    public String index(HttpSession session,Model model) 
    {
    	
    	Object user = session.getAttribute("loggedInUser");
        boolean isLoggedIn = user != null;
        
        model.addAttribute("loggedInUser", isLoggedIn);
        model.addAttribute("logoutUser", !isLoggedIn);
    	
    	
    	List<Book> books = bookRepo.findByIsDeletedFalse();
        List<Category> categories = categoryRepo.findAll();

        // Encode each book's image in Base64 and store it in a map
        Map<Long, String> bookImages = new HashMap<>();
        for (Book book : books) {
            if (book.getCoverImage() != null) {
                String base64Image = Base64.getEncoder().encodeToString(book.getCoverImage());
                bookImages.put(book.getId(), "data:image/jpeg;base64," + base64Image);
            }
        }
        model.addAttribute("categories", categories);
        model.addAttribute("books", books);
        model.addAttribute("bookImages", bookImages); // Store encoded images for each book
        return "index";
    }

    
    @GetMapping("/books")
    public String getBooks(@RequestParam(value = "sort", required = false, defaultValue = "title_asc") String sort,
                           Model model) {

        Sort sortObj;

        switch (sort) {
            case "title_desc":
                sortObj = Sort.by(Sort.Direction.DESC, "title");
                break;
            case "price_asc":
                sortObj = Sort.by(Sort.Direction.ASC, "price");
                break;
            case "price_desc":
                sortObj = Sort.by(Sort.Direction.DESC, "price");
                break;
            case "date_asc":
                sortObj = Sort.by(Sort.Direction.ASC, "createdAt");
                break;
            case "date_desc":
                sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
            default:
                sortObj = Sort.by(Sort.Direction.ASC, "title");
        }

        List<Book> books = bookRepo.findAll(sortObj);
        model.addAttribute("books", books);
        model.addAttribute("sort", sort); // so selected option can stay selected
        return "index"; // name of your Thymeleaf template
    }

    
    
    @GetMapping("/login")
    public String login() 
    {
        return "login";
    }

    @PostMapping("/login")
    public String loginauth(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "targetBookId", required = false) Long targetBookId,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            Model model) 
    	{
        // 🔹 Admin Login
        if ("admin123".equals(username) && "admin".equals(password)) {
            session.setAttribute("loggedInUser", "Admin");
            model.addAttribute("categories", categoryRepo.findAll());
            model.addAttribute("books", bookRepo.findAll());
            model.addAttribute("users", userRepo.findAll());
            return "Admin/admindash";
        }

        // 🔹 User Login
        User user = userRepo.findByUsername(username);
        if (user == null) {
            user = userRepo.findByEmail(username);
        }
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "Invalid username or password. Please try again.");
            return "login";
        }

        // ✅ Successful login
        session.setAttribute("loggedInUser", user);

        // ✅ If a book was selected before login, add it to the cart
        if (targetBookId != null) {
            Optional<Book> bookOpt = bookRepo.findById(targetBookId);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                
                // Check if book is already in cart
                boolean exists = cartRepo.existsByUserAndBook(user, book);
                if (!exists) {
                    Cart cartItem = new Cart();
                    cartItem.setUser(user);
                    cartItem.setBook(book);
                    cartItem.setQuantity(1);
                    cartRepo.save(cartItem);
                    redirectAttributes.addFlashAttribute("success", "Book added to cart!");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Book is already in your cart.");
                }
            }
        }

        // ✅ Load user's cart items
        List<Cart> cartItems = cartService.getCartItems(user.getUserid());
        model.addAttribute("cartItems", cartItems);

        // ✅ Calculate total cart price
        double totalCartPrice = cartItems.stream()
                .mapToDouble(cart -> cart.getBook().getPrice() * cart.getQuantity()) // Sum price * quantity
                .sum();

        // ✅ Store total price in session & model
        model.addAttribute("categories", categoryRepo.findAll());
        session.setAttribute("cartTotal", totalCartPrice);
        model.addAttribute("cartTotal", totalCartPrice);
        return "redirect:/";
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session)
    {
        session.invalidate(); 
        return "redirect:/login";
    }
    
    @PostMapping("/logout")
    public String logouts(HttpSession session)
    {
        session.invalidate(); 
        return "redirect:/login";
    }

    @GetMapping("/errorpage")
    public String handleError(Model model)
    {
        model.addAttribute("errorMessage", "Something went wrong. Please try again later.");
        return "errorpage";
    }
    
    @GetMapping("/forgetpwd")
    public String forgetpass()
    {
    	return "forgetpwd";
    }
  
    @PostMapping("/forgetpwd")
    public String forgetPassword(@RequestParam("email") String email, Model model) {
        // Check if the email exists in the database
        User userOptional = userRepo.findByEmail(email);
        if (userOptional == null)
        {
            model.addAttribute("error", "Email not found. Please check and try again.");
            return "forgetpwd"; // Redirects back to forget password page
        }

        try {
            // Generate OTP and send to user
            String otp = otpservice.generateOTP(email);
            otpservice.sendOTP(email, otp);

            // Redirect to OTP verification page with email pre-filled
            model.addAttribute("email", email);
            model.addAttribute("message", "An OTP has been sent to your email for verification.");
            return "verifyotp"; // Go to OTP verification page
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to send OTP. Please try again.");
            return "errorpage"; // Stay on forget password page if there's an error
        }
    }
    
    @GetMapping("/newpass")
    public String newpass(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "User/newpwd";
    }
    
    @PostMapping("/newpass")
    public String setNewPassword(@RequestParam("email") String email,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes redirectAttributes) {

        User user = userRepo.findByEmail(email);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/forgetpwd";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/newpass?email=" + email;
        }

        user.setPassword(passwordEncoder.encode(newPassword)); // Encrypt password before storing
        userRepo.save(user);

        redirectAttributes.addFlashAttribute("success", "Password updated successfully!");
        return "redirect:/login";  // Redirect to login page
    }
    
    @GetMapping("/search")
    public String searchBooks(@RequestParam(value = "title", required = false) String title,
                              @RequestParam(value = "author", required = false) String author,
                              @RequestParam(value = "categoryId", required = false) Long categoryId, 
                              Model model) {

        List<Book> books = new ArrayList<>();

        if (title != null && !title.isEmpty())
        {
            books = bookRepo.findByTitleContainingIgnoreCaseAndIsDeletedFalse(title);
        } 
        else if (author != null && !author.isEmpty())
        {
            books = bookRepo.findByAuthorContainingIgnoreCaseAndIsDeletedFalse(author);
        } 
        else if (categoryId != null) 
        {
            Optional<Category> categoryOpt = categoryRepo.findById(categoryId);
            if (categoryOpt.isPresent()) {
                Category category = categoryOpt.get();
                books = bookRepo.findByCategoryAndIsDeletedFalse(category);
            } else {
                // Optional: Handle case where category ID is invalid
                model.addAttribute("error", "Category not found.");
            }
        }
        else
        {
            // Fetch all non-deleted books when no search criteria are provided
            books = bookRepo.findByIsDeletedFalse();
        }

        // Process book images
        Map<Long, String> bookImages = new HashMap<>();
        for (Book book : books) {
            if (book.getCoverImage() != null) {
                String base64Image = Base64.getEncoder().encodeToString(book.getCoverImage());
                bookImages.put(book.getId(), "data:image/jpeg;base64," + base64Image);
            }
        }

        // Always add categories to the model
        List<Category > categories = categoryRepo.findAll();
        model.addAttribute("categories", categories);
        model.addAttribute("books", books);
        model.addAttribute("bookImages", bookImages);

        if (books.isEmpty()) {
            model.addAttribute("error", "No books found for your search.");
        }

        return "index"; // Return to the index page with search results
    }
        
    @GetMapping("/contact")
    public String contact() 
    {       
        return "contact";
    }
    
    @PostMapping("/contact")
    public String handleContactForm(@RequestParam("name") String name,
                                    @RequestParam("email") String email,
                                    @RequestParam("subject") String subject,
                                    @RequestParam("message") String message,
                                    Model model) {
        
        // Send emails to admin & user
        emailService.sendContactEmail(name, email, subject, message);

        // Show success message
        model.addAttribute("success", "Your message has been sent successfully!");
        return "contact"; // Redirect back to the contact page
    }
    
    
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "About Us - Book Haven");
        return "about";
    }
}

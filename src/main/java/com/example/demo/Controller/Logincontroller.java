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

import com.example.demo.AuthHelper;
import com.example.demo.Entity.Book;
import com.example.demo.Entity.Cart;
import com.example.demo.Entity.Category;
import com.example.demo.Entity.Inquiry;
import com.example.demo.Entity.User;
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.CategoryRepo;
import com.example.demo.Repository.InquiryRepo;
import com.example.demo.Repository.UserRepo;
import com.example.demo.service.CartService;
import com.example.demo.service.EmailService;
import com.example.demo.service.OTPservice;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class Logincontroller {

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @Autowired
    private InquiryRepo inquiryRepo;

    @Autowired
    private AuthHelper authHelper;

    private Map<Long, String> buildBookImagesMap(List<Book> books) {
        Map<Long, String> bookImages = new HashMap<>();
        if (books != null) {
            for (Book book : books) {
                if (book != null && book.getCoverImage() != null && book.getCoverImage().length > 0) {
                    String base64Image = Base64.getEncoder().encodeToString(book.getCoverImage());
                    bookImages.put(book.getId(), "data:image/jpeg;base64," + base64Image);
                }
            }
        }
        return bookImages;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        boolean isLoggedIn = authHelper.isAuthenticated(session);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("logoutUser", !isLoggedIn);

        List<Book> books = bookRepo.findByIsDeletedFalse();
        List<Category> categories = categoryRepo.findAll();

        model.addAttribute("categories", categories);
        model.addAttribute("books", books);
        model.addAttribute("bookImages", buildBookImagesMap(books));
        return "index";
    }

    @GetMapping("/books")
    public String getBooks(
            @RequestParam(value = "sort", required = false, defaultValue = "title_asc") String sort,
            HttpSession session,
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
                sortObj = Sort.by(Sort.Direction.ASC, "publishDate");
                break;
            case "date_desc":
                sortObj = Sort.by(Sort.Direction.DESC, "publishDate");
                break;
            default:
                sortObj = Sort.by(Sort.Direction.ASC, "title");
        }

        List<Book> books = bookRepo.findAll(sortObj).stream()
                .filter(b -> !b.isDeleted())
                .toList();

        model.addAttribute("isLoggedIn", authHelper.isAuthenticated(session));
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("books", books);
        model.addAttribute("bookImages", buildBookImagesMap(books));
        model.addAttribute("sort", sort);
        return "index";
    }

    @GetMapping("/categories")
    public String categoriesDirectory(HttpSession session, Model model) {
        List<Book> books = bookRepo.findByIsDeletedFalse();
        List<Category> categories = categoryRepo.findAll();

        model.addAttribute("isLoggedIn", authHelper.isAuthenticated(session));
        model.addAttribute("categories", categories);
        model.addAttribute("books", books);
        model.addAttribute("bookImages", buildBookImagesMap(books));
        return "index";
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "targetBookId", required = false) Long targetBookId,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "message", required = false) String message,
            Model model,
            HttpSession session) {
        if (authHelper.isAuthenticated(session)) {
            if (authHelper.isAdmin(session)) {
                return "redirect:/admin/dashboard";
            }
            return "redirect:/";
        }
        if (targetBookId != null) {
            model.addAttribute("targetBookId", targetBookId);
        }
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginauth(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "targetBookId", required = false) Long targetBookId,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            Model model) {
        String trimmedUser = username != null ? username.trim() : "";
        String trimmedPass = password != null ? password.trim() : "";

        if (trimmedUser.isEmpty() || trimmedPass.isEmpty()) {
            model.addAttribute("error", "Please enter both username/email and password.");
            if (targetBookId != null) {
                model.addAttribute("targetBookId", targetBookId);
            }
            return "login";
        }

        // Database-backed lookup by username or email
        User user = null;
        try {
            List<User> flexibleMatches = userRepo.findByIdentifierFlexible(trimmedUser);
            if (flexibleMatches != null && !flexibleMatches.isEmpty()) {
                user = flexibleMatches.get(0);
            }
        } catch (Exception e) {
            System.err.println("Notice: Flexible user lookup fallback: " + e.getMessage());
        }

        if (user == null) {
            user = userRepo.findByUsernameIgnoreCase(trimmedUser);
        }
        if (user == null) {
            user = userRepo.findByEmailIgnoreCase(trimmedUser);
        }
        if (user == null) {
            user = userRepo.findByUsername(trimmedUser);
        }
        if (user == null) {
            user = userRepo.findByEmail(trimmedUser);
        }

        // Password verification
        boolean passwordMatches = false;
        if (user != null && user.getPassword() != null && password != null) {
            try {
                passwordMatches = passwordEncoder.matches(password, user.getPassword());
            } catch (Exception ignored) {
            }

            if (!passwordMatches && !password.equals(trimmedPass)) {
                try {
                    passwordMatches = passwordEncoder.matches(trimmedPass, user.getPassword());
                } catch (Exception ignored) {
                }
            }

            if (!passwordMatches && !user.getPassword().startsWith("$2")) {
                String storedPass = user.getPassword();
                passwordMatches = password.equals(storedPass) || trimmedPass.equals(storedPass.trim());
            }

            if (passwordMatches) {
                try {
                    if (passwordEncoder.upgradeEncoding(user.getPassword()) || !user.getPassword().startsWith("$2")) {
                        user.setPassword(passwordEncoder.encode(trimmedPass));
                        user = userRepo.save(user);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (user == null || !passwordMatches) {
            model.addAttribute("error", "Invalid username or password. Please try again.");
            if (targetBookId != null) {
                model.addAttribute("targetBookId", targetBookId);
            }
            return "login";
        }

        if (!user.isEnabled()) {
            model.addAttribute("error", "This account has been deactivated. Please contact support.");
            return "login";
        }

        // Sync HTTP session & Spring Security Context with authenticated user + role
        authHelper.syncSessionAuth(session, user);

        // Administrators always go to dedicated Admin Dashboard
        if (user.isAdmin()) {
            return "redirect:/admin/dashboard";
        }

        // If targetBookId was provided, add book to customer's cart
        if (targetBookId != null) {
            try {
                Optional<Book> bookOpt = bookRepo.findById(targetBookId);
                if (bookOpt.isPresent() && !bookOpt.get().isDeleted()) {
                    Book book = bookOpt.get();
                    Optional<Cart> existingCartOpt = cartRepo.findByUserAndBook(user, book);
                    if (existingCartOpt.isEmpty()) {
                        Cart cartItem = new Cart();
                        cartItem.setUser(user);
                        cartItem.setBook(book);
                        cartItem.setQuantity(1);
                        cartRepo.save(cartItem);
                        redirectAttributes.addFlashAttribute("message", "Book added to cart!");
                    } else {
                        redirectAttributes.addFlashAttribute("message", "This book is already in your cart.");
                    }
                }
            } catch (Exception e) {
                System.err.println("Notice: Could not add target book to cart: " + e.getMessage());
            }
            session.setAttribute("cartTotal", cartService.calculateCartTotal(user));
            return "redirect:/cart";
        }

        session.setAttribute("cartTotal", cartService.calculateCartTotal(user));
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        authHelper.clearAuth(session);
        redirectAttributes.addFlashAttribute("message", "You have been signed out cleanly.");
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logouts(HttpSession session, RedirectAttributes redirectAttributes) {
        authHelper.clearAuth(session);
        redirectAttributes.addFlashAttribute("message", "You have been signed out cleanly.");
        return "redirect:/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied(HttpServletResponse response, Model model) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("errorMessage", "You do not have administrator permissions to access this resource.");
        return "error/403";
    }

    @GetMapping("/errorpage")
    public String handleError(Model model) {
        model.addAttribute("errorMessage", "Something went wrong. Please try again later.");
        return "errorpage";
    }

    @GetMapping("/forgetpwd")
    public String forgetpass() {
        return "forgetpwd";
    }

    @PostMapping("/forgetpwd")
    public String forgetPassword(@RequestParam("email") String email, Model model) {
        String normalizedEmail = email != null ? email.trim() : "";
        User userOptional = userRepo.findByEmailIgnoreCase(normalizedEmail);
        if (userOptional == null) {
            userOptional = userRepo.findByEmail(normalizedEmail);
        }
        if (userOptional == null) {
            model.addAttribute("error", "Email not found. Please check and try again.");
            return "forgetpwd";
        }

        try {
            String targetEmail = userOptional.getEmail();
            String otp = otpservice.generateOTP(targetEmail);
            otpservice.sendOTP(targetEmail, otp);

            model.addAttribute("email", targetEmail);
            model.addAttribute("message", "An OTP has been sent to your email for verification.");
            return "verifyotp";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to send OTP. Please try again.");
            return "errorpage";
        }
    }

    @GetMapping("/newpass")
    public String newpass(@RequestParam("email") String email, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String verifiedEmail = (String) session.getAttribute("verifiedResetEmail");
        if (verifiedEmail == null || email == null || !verifiedEmail.equalsIgnoreCase(email.trim())) {
            redirectAttributes.addFlashAttribute("error", "Please verify your OTP before resetting your password.");
            return "redirect:/forgetpwd";
        }
        model.addAttribute("email", email);
        return "User/newpwd";
    }

    @PostMapping("/newpass")
    public String setNewPassword(
            @RequestParam("email") String email,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String verifiedEmail = (String) session.getAttribute("verifiedResetEmail");
        if (verifiedEmail == null || email == null || !verifiedEmail.equalsIgnoreCase(email.trim())) {
            redirectAttributes.addFlashAttribute("error", "OTP verification session expired or invalid. Please request a new OTP.");
            return "redirect:/forgetpwd";
        }

        User user = userRepo.findByEmailIgnoreCase(email.trim());
        if (user == null) {
            user = userRepo.findByEmail(email.trim());
        }
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/forgetpwd";
        }

        if (newPassword == null || newPassword.isBlank() || !newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/newpass?email=" + user.getEmail();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        session.removeAttribute("verifiedResetEmail");

        redirectAttributes.addFlashAttribute("message", "Password updated successfully! Please sign in.");
        return "redirect:/login";
    }

    @GetMapping("/search")
    public String searchBooks(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            HttpSession session,
            Model model) {

        List<Book> books = new ArrayList<>();

        if (title != null && !title.trim().isEmpty()) {
            books = bookRepo.findByTitleContainingIgnoreCaseAndIsDeletedFalse(title.trim());
        } else if (author != null && !author.trim().isEmpty()) {
            books = bookRepo.findByAuthorContainingIgnoreCaseAndIsDeletedFalse(author.trim());
        } else if (categoryId != null) {
            Optional<Category> categoryOpt = categoryRepo.findById(categoryId);
            if (categoryOpt.isPresent()) {
                books = bookRepo.findByCategoryAndIsDeletedFalse(categoryOpt.get());
            } else {
                model.addAttribute("error", "Category not found.");
            }
        } else {
            books = bookRepo.findByIsDeletedFalse();
        }

        List<Category> categories = categoryRepo.findAll();
        model.addAttribute("isLoggedIn", authHelper.isAuthenticated(session));
        model.addAttribute("categories", categories);
        model.addAttribute("books", books);
        model.addAttribute("bookImages", buildBookImagesMap(books));

        if (books.isEmpty()) {
            model.addAttribute("error", "No books found for your search.");
        }

        return "index";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @PostMapping("/contact")
    public String handleContactForm(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            Model model) {
        try {
            Inquiry inquiry = new Inquiry(name, email, subject, message);
            inquiryRepo.save(inquiry);
        } catch (Exception e) {
            System.err.println("Notice: Could not record inquiry in database: " + e.getMessage());
        }

        try {
            emailService.sendContactEmail(name, email, subject, message);
        } catch (Exception e) {
            System.err.println("Notice: Could not send contact email: " + e.getMessage());
        }

        model.addAttribute("success",
                "Your message has been received! Our concierge team will review your inquiry shortly.");
        return "contact";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "About Us - Quickread Bookstore");
        return "about";
    }
}

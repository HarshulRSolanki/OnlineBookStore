package com.example.demo.Controller;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;

import com.example.demo.AuthHelper;
import com.example.demo.Entity.Book;
import com.example.demo.Entity.Cart;
import com.example.demo.Entity.Download;
import com.example.demo.Entity.Order;
import com.example.demo.Entity.OrderItem;
import com.example.demo.Entity.User;
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.DownloadRepo;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.UserRepo;
import com.example.demo.service.BookService;
import com.example.demo.service.CartService;
import com.example.demo.service.OTPservice;
import com.example.demo.service.OrderReceiptService;
import com.example.demo.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

import jakarta.servlet.http.HttpSession;

@Controller
public class Usercontroller {

    public static final String SESSION_PENDING_PROFILE_UPDATE = "pendingProfileUpdate";

    public static class PendingProfileUpdate implements Serializable {
        private static final long serialVersionUID = 1L;
        private String token;
        private Long userId;
        private String newUsername;
        private String newEmail;
        private String newMobile;
        private String encodedNewPassword;
        private String otpKey;
        private String targetEmail;
        private long createdAt;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getNewUsername() { return newUsername; }
        public void setNewUsername(String newUsername) { this.newUsername = newUsername; }
        public String getNewEmail() { return newEmail; }
        public void setNewEmail(String newEmail) { this.newEmail = newEmail; }
        public String getNewMobile() { return newMobile; }
        public void setNewMobile(String newMobile) { this.newMobile = newMobile; }
        public String getEncodedNewPassword() { return encodedNewPassword; }
        public void setEncodedNewPassword(String encodedNewPassword) { this.encodedNewPassword = encodedNewPassword; }
        public String getOtpKey() { return otpKey; }
        public void setOtpKey(String otpKey) { this.otpKey = otpKey; }
        public String getTargetEmail() { return targetEmail; }
        public void setTargetEmail(String targetEmail) { this.targetEmail = targetEmail; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DownloadRepo downloadRepo;

    @Autowired
    private OTPservice otpservice;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BookRepo bookRepo;

    @Autowired
    private BookService bookService;

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private OrderReceiptService orderReceiptService;

    @Autowired
    private AuthHelper authHelper;

    private String resolveCurrentBaseUrl() {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        } catch (Exception e) {
            return null;
        }
    }

    private User resolveStripeOrderUser(Session stripeSession, HttpSession httpSession) {
        User user = authHelper.getLoggedInUser(httpSession);
        if (user != null) {
            return user;
        }
        if (stripeSession.getMetadata() != null && stripeSession.getMetadata().containsKey("userId")) {
            try {
                Long uid = Long.parseLong(stripeSession.getMetadata().get("userId"));
                user = userRepo.findById(uid).orElse(null);
            } catch (Exception ignored) {}
        }
        if (user == null && stripeSession.getMetadata() != null && stripeSession.getMetadata().containsKey("userEmail")) {
            String metaEmail = stripeSession.getMetadata().get("userEmail");
            user = userRepo.findByEmailIgnoreCase(metaEmail);
        }
        if (user == null && stripeSession.getCustomerDetails() != null && stripeSession.getCustomerDetails().getEmail() != null) {
            String customerEmail = stripeSession.getCustomerDetails().getEmail();
            user = userRepo.findByEmailIgnoreCase(customerEmail);
            if (user == null) {
                user = userRepo.getUserByEmail(customerEmail);
            }
        }
        return user;
    }

    private boolean hasUserPurchasedBook(User user, Book book) {
        if (user == null || book == null) {
            return false;
        }
        if (downloadRepo.existsByUserAndBook(user, book)) {
            return true;
        }
        List<Order> userOrders = orderRepo.findByUser(user);
        for (Order o : userOrders) {
            if (!o.isPaid()) {
                continue;
            }
            for (OrderItem item : o.getEffectiveItems()) {
                if (item.getBook() != null && item.getBook().getId().equals(book.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @GetMapping("/userdash")
    public String user(HttpSession session, Model model) {
        if (authHelper.isAdmin(session)) {
            return "redirect:/admin/dashboard";
        }
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        List<Cart> cartItems = cartRepo.findByUser(user);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("user", user);
        return "User/userdash";
    }

    @PostMapping("/userdash")
    public String userdash(HttpSession session) {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        return "redirect:/userdash";
    }

    @GetMapping("/userreg")
    public String userreg() {
        return "User/userreg";
    }

    @PostMapping("/userreg")
    public String userreg(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirm_password") String confirmPassword,
            @RequestParam("mobilenumber") String mobilenumber,
            Model model,
            HttpSession session) {

        String trimmedName = name != null ? name.trim() : "";
        String trimmedEmail = email != null ? email.trim() : "";
        String trimmedMobile = mobilenumber != null ? mobilenumber.trim() : "";

        if (trimmedName.length() < 3 || trimmedName.length() > 50) {
            model.addAttribute("error", "Username must be between 3 and 50 characters.");
            return "User/userreg";
        }
        if (password == null || !password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "User/userreg";
        }
        if (userRepo.existsByEmail(trimmedEmail) || userRepo.findByEmailIgnoreCase(trimmedEmail) != null) {
            model.addAttribute("error", "Email already registered.");
            return "User/userreg";
        }
        if (userRepo.findByUsernameIgnoreCase(trimmedName) != null) {
            model.addAttribute("error", "Username is already taken. Please choose another.");
            return "User/userreg";
        }

        User pendingUser = new User();
        pendingUser.setUsername(trimmedName);
        pendingUser.setEmail(trimmedEmail);
        pendingUser.setPassword(passwordEncoder.encode(password));
        pendingUser.setMobileno(trimmedMobile);
        pendingUser.setRole("USER");
        pendingUser.setEnabled(true);
        pendingUser.setIsregistered(false);

        session.setAttribute("tempUser", pendingUser);

        String otp = otpservice.generateOTP(trimmedEmail);
        otpservice.sendOTP(trimmedEmail, otp);

        model.addAttribute("email", trimmedEmail);
        model.addAttribute("message", "A 6-digit verification code has been sent to your email.");
        return "verifyotp";
    }

    @GetMapping("/verify-otp")
    public String verifyforforget() {
        return "login";
    }

    @PostMapping("/verify-otp")
    public String verifyOTP(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String trimmedEmail = email != null ? email.trim() : "";
        model.addAttribute("email", trimmedEmail);

        if (trimmedEmail.isEmpty() || otp == null || otp.trim().isEmpty()) {
            model.addAttribute("error", "Please enter a valid email and OTP code.");
            return "verifyotp";
        }

        OTPservice.OtpStatus status = otpservice.verifyOTPWithStatus(trimmedEmail, otp);
        if (status != OTPservice.OtpStatus.VALID) {
            switch (status) {
                case EXPIRED:
                    model.addAttribute("error", "Your OTP has expired. Please request a new verification code.");
                    break;
                case MAX_ATTEMPTS:
                    model.addAttribute("error", "Too many incorrect OTP attempts. Please request a new verification code.");
                    break;
                case NOT_FOUND:
                    model.addAttribute("error", "No active verification code found. Please resend OTP.");
                    break;
                default:
                    model.addAttribute("error", "Invalid OTP code. Please try again.");
                    break;
            }
            return "verifyotp";
        }

        try {
            User pendingUser = (User) session.getAttribute("tempUser");
            if (pendingUser == null) {
                pendingUser = userRepo.findByEmailIgnoreCase(trimmedEmail);
                if (pendingUser == null) {
                    pendingUser = userRepo.findByEmail(trimmedEmail);
                }
            }

            if (pendingUser == null) {
                model.addAttribute("error", "User not found. Please register again.");
                return "User/userreg";
            }

            if (pendingUser.isIsregistered()) {
                session.setAttribute("verifiedResetEmail", pendingUser.getEmail().toLowerCase());
                model.addAttribute("email", pendingUser.getEmail());
                return "User/newpwd";
            }

            pendingUser.setRole("USER");
            pendingUser.setEnabled(true);
            pendingUser.setIsregistered(true);
            userRepo.save(pendingUser);
            session.removeAttribute("tempUser");

            redirectAttributes.addFlashAttribute("message", "Registration successful! Please log in.");
            return "redirect:/login";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
            return "errorpage";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam("email") String email, Model model) {
        String trimmedEmail = email != null ? email.trim() : "";
        model.addAttribute("email", trimmedEmail);
        if (trimmedEmail.isEmpty()) {
            model.addAttribute("error", "Email is required to resend verification code.");
            return "verifyotp";
        }
        if (!otpservice.canResend(trimmedEmail)) {
            long remaining = otpservice.getCooldownRemainingSeconds(trimmedEmail);
            model.addAttribute("error", "Please wait " + remaining + " seconds before requesting a new code.");
            return "verifyotp";
        }
        String newOtp = otpservice.generateOTP(trimmedEmail);
        otpservice.sendOTP(trimmedEmail, newOtp);
        model.addAttribute("message", "A new verification code has been dispatched to your email.");
        return "verifyotp";
    }

    // =========================================================================
    // PROFILE UPDATE WITH OTP VERIFICATION
    // =========================================================================

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        User currentUser = authHelper.getLoggedInUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.isAdmin()) {
            return "redirect:/admin/profile";
        }
        populateProfileModel(currentUser, session, model);
        return "User/profile";
    }

    @GetMapping("/editProfile")
    public String editProfile(
            @RequestParam(value = "userId", required = false) Long userId,
            HttpSession session,
            Model model) {
        User currentUser = authHelper.getLoggedInUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.isAdmin()) {
            return "redirect:/admin/profile";
        }
        if (userId != null && !userId.equals(currentUser.getUserid())) {
            return "redirect:/access-denied";
        }
        populateProfileModel(currentUser, session, model);
        return "User/profile";
    }

    private void populateProfileModel(User currentUser, HttpSession session, Model model) {
        model.addAttribute("user", currentUser);
        Object pendingObj = session.getAttribute(SESSION_PENDING_PROFILE_UPDATE);
        if (pendingObj instanceof PendingProfileUpdate pending
                && pending.getUserId() != null
                && pending.getUserId().equals(currentUser.getUserid())) {
            model.addAttribute("pendingUpdate", pending);
            model.addAttribute("otpActive", otpservice.hasActiveOTP(pending.getOtpKey()));
            model.addAttribute("cooldownSeconds", otpservice.getCooldownRemainingSeconds(pending.getOtpKey()));
            model.addAttribute("remainingAttempts", otpservice.getRemainingAttempts(pending.getOtpKey()));
        }
    }

    @PostMapping("/profile/request-otp")
    public String requestProfileUpdateOtp(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String mobile,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        return handleProfileUpdateRequest(null, username, email, password, mobile, session, redirectAttributes);
    }

    @PostMapping("/updateProfile")
    public String updateProfilePost(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String mobile,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        return handleProfileUpdateRequest(null, username, email, password, mobile, session, redirectAttributes);
    }

    @PostMapping("/updateProfile/{userId}")
    public String updateUserProfile(
            @PathVariable Long userId,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String mobile,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        return handleProfileUpdateRequest(userId, username, email, password, mobile, session, redirectAttributes);
    }

    private String handleProfileUpdateRequest(
            Long pathUserId,
            String username,
            String email,
            String password,
            String mobile,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = authHelper.getLoggedInUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.isAdmin()) {
            return "redirect:/admin/profile";
        }
        if (pathUserId != null && !pathUserId.equals(currentUser.getUserid())) {
            return "redirect:/access-denied";
        }

        String newUsername = username != null ? username.trim() : "";
        String newEmail = email != null ? email.trim() : "";
        String newMobile = mobile != null ? mobile.trim() : "";
        String rawPassword = password != null ? password.trim() : "";

        if (newUsername.length() < 3 || newUsername.length() > 50) {
            redirectAttributes.addFlashAttribute("error", "Username must be between 3 and 50 characters.");
            return "redirect:/profile";
        }
        if (newEmail.isEmpty() || !newEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid email address.");
            return "redirect:/profile";
        }
        if (!newMobile.isEmpty() && !newMobile.matches("^[0-9]{10}$")) {
            redirectAttributes.addFlashAttribute("error", "Contact number must be exactly 10 digits.");
            return "redirect:/profile";
        }

        // Uniqueness checks against other user records
        User byUsername = userRepo.findByUsernameIgnoreCase(newUsername);
        if (byUsername != null && !byUsername.getUserid().equals(currentUser.getUserid())) {
            redirectAttributes.addFlashAttribute("error", "Username '" + newUsername + "' is already taken.");
            return "redirect:/profile";
        }
        User byEmail = userRepo.findByEmailIgnoreCase(newEmail);
        if (byEmail != null && !byEmail.getUserid().equals(currentUser.getUserid())) {
            redirectAttributes.addFlashAttribute("error", "Email '" + newEmail + "' is already registered to another account.");
            return "redirect:/profile";
        }

        String currentMobile = currentUser.getMobileno() != null ? currentUser.getMobileno().trim() : "";
        boolean usernameChanged = !newUsername.equals(currentUser.getUsername());
        boolean emailChanged = !newEmail.equalsIgnoreCase(currentUser.getEmail());
        boolean mobileChanged = !newMobile.equals(currentMobile);
        boolean passwordChanged = !rawPassword.isEmpty();

        if (!usernameChanged && !emailChanged && !mobileChanged && !passwordChanged) {
            redirectAttributes.addFlashAttribute("error", "No profile changes detected.");
            return "redirect:/profile";
        }

        // Send verification OTP to new email if email changed, else current registered email
        String targetEmail = emailChanged ? newEmail : currentUser.getEmail();
        String otpKey = OTPservice.profileUpdateKey(currentUser.getUserid(), targetEmail);

        Object existingObj = session.getAttribute(SESSION_PENDING_PROFILE_UPDATE);
        if (existingObj instanceof PendingProfileUpdate existing
                && !existing.getOtpKey().equals(otpKey)) {
            otpservice.invalidateOTP(existing.getOtpKey());
        }

        // Duplicate OTP request prevention during active cooldown
        if (!otpservice.canResend(otpKey)) {
            long remaining = otpservice.getCooldownRemainingSeconds(otpKey);
            if (existingObj instanceof PendingProfileUpdate existing && existing.getOtpKey().equals(otpKey)) {
                existing.setNewUsername(newUsername);
                existing.setNewEmail(newEmail);
                existing.setNewMobile(newMobile.isEmpty() ? currentUser.getMobileno() : newMobile);
                if (passwordChanged) {
                    existing.setEncodedNewPassword(passwordEncoder.encode(rawPassword));
                }
                session.setAttribute(SESSION_PENDING_PROFILE_UPDATE, existing);
            }
            redirectAttributes.addFlashAttribute("message",
                    "An active OTP was already sent to " + targetEmail + ". Enter that code below or wait " + remaining + "s before requesting a new one.");
            return "redirect:/profile";
        }

        PendingProfileUpdate pending = new PendingProfileUpdate();
        pending.setToken(UUID.randomUUID().toString());
        pending.setUserId(currentUser.getUserid());
        pending.setNewUsername(newUsername);
        pending.setNewEmail(newEmail);
        pending.setNewMobile(newMobile.isEmpty() ? currentUser.getMobileno() : newMobile);
        pending.setEncodedNewPassword(passwordChanged ? passwordEncoder.encode(rawPassword) : null);
        pending.setOtpKey(otpKey);
        pending.setTargetEmail(targetEmail);
        pending.setCreatedAt(System.currentTimeMillis());

        String otp = otpservice.generateOTP(otpKey);
        otpservice.sendProfileUpdateOTP(targetEmail, otp);

        session.setAttribute(SESSION_PENDING_PROFILE_UPDATE, pending);
        redirectAttributes.addFlashAttribute("message",
                "Verification OTP sent to " + targetEmail + ". Please verify the code below to save your profile updates.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/verify-otp")
    public String verifyProfileUpdateOtp(
            @RequestParam("otp") String otp,
            @RequestParam(value = "token", required = false) String token,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = authHelper.getLoggedInUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Object pendingObj = session.getAttribute(SESSION_PENDING_PROFILE_UPDATE);
        if (!(pendingObj instanceof PendingProfileUpdate pending)
                || !currentUser.getUserid().equals(pending.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "No pending profile verification request found, or it has already been completed.");
            return "redirect:/profile";
        }

        if (token != null && !token.isBlank() && !token.equals(pending.getToken())) {
            redirectAttributes.addFlashAttribute("error", "Duplicate or outdated verification request ignored.");
            return "redirect:/profile";
        }

        OTPservice.OtpStatus status = otpservice.verifyOTPWithStatus(pending.getOtpKey(), otp);
        if (status != OTPservice.OtpStatus.VALID) {
            switch (status) {
                case EXPIRED:
                    redirectAttributes.addFlashAttribute("error", "Your verification OTP has expired. Please click Resend OTP to receive a new code.");
                    break;
                case MAX_ATTEMPTS:
                    redirectAttributes.addFlashAttribute("error", "Maximum incorrect attempts reached. The OTP has been invalidated; please request a new code.");
                    break;
                case NOT_FOUND:
                    redirectAttributes.addFlashAttribute("error", "No active OTP was found or it has expired. Please request a new code.");
                    break;
                default:
                    int rem = otpservice.getRemainingAttempts(pending.getOtpKey());
                    redirectAttributes.addFlashAttribute("error",
                            "Invalid verification code. Profile changes were not applied. (" + rem + " attempt(s) remaining).");
                    break;
            }
            return "redirect:/profile";
        }

        // OTP verified! Persist changes in MySQL and keep user session authenticated
        User freshUser = userRepo.findById(currentUser.getUserid()).orElse(null);
        if (freshUser == null) {
            session.removeAttribute(SESSION_PENDING_PROFILE_UPDATE);
            redirectAttributes.addFlashAttribute("error", "User account no longer exists.");
            return "redirect:/login";
        }

        User conflictUser = userRepo.findByUsernameIgnoreCase(pending.getNewUsername());
        if (conflictUser != null && !conflictUser.getUserid().equals(freshUser.getUserid())) {
            session.removeAttribute(SESSION_PENDING_PROFILE_UPDATE);
            redirectAttributes.addFlashAttribute("error", "That username was claimed before verification completed.");
            return "redirect:/profile";
        }

        User conflictEmail = userRepo.findByEmailIgnoreCase(pending.getNewEmail());
        if (conflictEmail != null && !conflictEmail.getUserid().equals(freshUser.getUserid())) {
            session.removeAttribute(SESSION_PENDING_PROFILE_UPDATE);
            redirectAttributes.addFlashAttribute("error", "That email was claimed before verification completed.");
            return "redirect:/profile";
        }

        freshUser.setUsername(pending.getNewUsername());
        freshUser.setEmail(pending.getNewEmail());
        if (pending.getNewMobile() != null) {
            freshUser.setMobileno(pending.getNewMobile());
        }
        if (pending.getEncodedNewPassword() != null && !pending.getEncodedNewPassword().isBlank()) {
            freshUser.setPassword(pending.getEncodedNewPassword());
        }

        User saved = userRepo.save(freshUser);
        session.removeAttribute(SESSION_PENDING_PROFILE_UPDATE);
        authHelper.syncSessionAuth(session, saved);

        redirectAttributes.addFlashAttribute("message", "OTP verified! Your profile changes have been saved successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/resend-otp")
    public String resendProfileUpdateOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = authHelper.getLoggedInUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Object pendingObj = session.getAttribute(SESSION_PENDING_PROFILE_UPDATE);
        if (!(pendingObj instanceof PendingProfileUpdate pending)
                || !currentUser.getUserid().equals(pending.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "No pending profile update found. Submit the form again to request an OTP.");
            return "redirect:/profile";
        }

        if (!otpservice.canResend(pending.getOtpKey())) {
            long remaining = otpservice.getCooldownRemainingSeconds(pending.getOtpKey());
            redirectAttributes.addFlashAttribute("error", "Please wait " + remaining + " seconds before requesting another OTP.");
            return "redirect:/profile";
        }

        pending.setToken(UUID.randomUUID().toString());
        pending.setCreatedAt(System.currentTimeMillis());
        String newOtp = otpservice.generateOTP(pending.getOtpKey());
        otpservice.sendProfileUpdateOTP(pending.getTargetEmail(), newOtp);
        session.setAttribute(SESSION_PENDING_PROFILE_UPDATE, pending);

        redirectAttributes.addFlashAttribute("message", "A new verification OTP has been sent to " + pending.getTargetEmail() + ".");
        return "redirect:/profile";
    }

    @PostMapping("/profile/cancel-otp")
    public String cancelProfileUpdateOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        Object pendingObj = session.getAttribute(SESSION_PENDING_PROFILE_UPDATE);
        if (pendingObj instanceof PendingProfileUpdate pending) {
            otpservice.invalidateOTP(pending.getOtpKey());
            session.removeAttribute(SESSION_PENDING_PROFILE_UPDATE);
        }
        redirectAttributes.addFlashAttribute("message", "Pending profile update was cancelled. No changes were made.");
        return "redirect:/profile";
    }

    // =========================================================================
    // CART MANAGEMENT & CHECKOUT
    // =========================================================================

    @PostMapping("/add/{bookId}")
    public String addToCart(
            @PathVariable Long bookId,
            @RequestParam(value = "quantity", required = false, defaultValue = "1") int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to add items to your cart.");
            return "redirect:/login?targetBookId=" + bookId;
        }

        Optional<Book> bookOpt = bookRepo.findById(bookId);
        if (bookOpt.isEmpty() || bookOpt.get().isDeleted()) {
            redirectAttributes.addFlashAttribute("error", "Requested book is unavailable.");
            return "redirect:/cart";
        }

        Book book = bookOpt.get();
        int qtyToAdd = Math.max(1, quantity);
        if (book.getStock() != null && book.getStock() <= 0) {
            redirectAttributes.addFlashAttribute("error", "'" + book.getTitle() + "' is currently out of stock.");
            return "redirect:/cart";
        }

        Optional<Cart> existingOpt = cartRepo.findByUserAndBook(user, book);
        if (existingOpt.isPresent()) {
            Cart existing = existingOpt.get();
            int newQty = Math.max(1, existing.getQuantity()) + qtyToAdd;
            if (book.getStock() != null && book.getStock() > 0 && newQty > book.getStock()) {
                newQty = book.getStock();
            }
            existing.setQuantity(newQty);
            cartRepo.save(existing);
            redirectAttributes.addFlashAttribute("message", "Updated quantity for '" + book.getTitle() + "' in your cart.");
        } else {
            int cappedQty = (book.getStock() != null && book.getStock() > 0) ? Math.min(qtyToAdd, book.getStock()) : qtyToAdd;
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setBook(book);
            cart.setQuantity(cappedQty);
            cartRepo.save(cart);
            redirectAttributes.addFlashAttribute("message", "'" + book.getTitle() + "' added to your cart!");
        }

        session.setAttribute("cartTotal", cartService.calculateCartTotal(user));
        return "redirect:/cart";
    }

    @PostMapping("/cart/update/{cartId}")
    public String updateCartQuantity(
            @PathVariable Long cartId,
            @RequestParam("quantity") int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        cartService.updateQuantity(cartId, user, quantity);
        session.setAttribute("cartTotal", cartService.calculateCartTotal(user));
        redirectAttributes.addFlashAttribute("message", "Cart updated.");
        return "redirect:/cart";
    }

    @PostMapping("/delete/{cartId}")
    public String removeFromCart(
            @PathVariable Long cartId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        Optional<Cart> cartOpt = cartRepo.findById(cartId);
        if (cartOpt.isPresent() && cartOpt.get().getUser() != null
                && cartOpt.get().getUser().getUserid().equals(user.getUserid())) {
            cartRepo.deleteById(cartId);
            redirectAttributes.addFlashAttribute("message", "Book removed from cart.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Cart item not found.");
        }
        session.setAttribute("cartTotal", cartService.calculateCartTotal(user));
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String cart(
            @RequestParam(value = "paymentCancelled", required = false) Boolean paymentCancelled,
            @RequestParam(value = "error", required = false) String errorParam,
            HttpSession session,
            Model model) {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        if (Boolean.TRUE.equals(paymentCancelled)) {
            model.addAttribute("error", "Payment was cancelled. Your cart items are still saved and no receipt was generated.");
        } else if (errorParam != null && !errorParam.isBlank()) {
            model.addAttribute("error", errorParam);
        }

        List<Cart> cartItems = cartRepo.findByUser(user);
        Map<Long, String> bookImages = new HashMap<>();

        double cartTotal = 0.0;
        for (Cart cartItem : cartItems) {
            Book book = cartItem.getBook();
            if (book != null) {
                if (!book.isDeleted()) {
                    int qty = Math.max(1, cartItem.getQuantity());
                    cartTotal += book.getPrice() * qty;
                }
                if (book.getCoverImage() != null && book.getCoverImage().length > 0) {
                    String base64Image = Base64.getEncoder().encodeToString(book.getCoverImage());
                    bookImages.put(book.getId(), "data:image/jpeg;base64," + base64Image);
                }
            }
        }

        session.setAttribute("cartTotal", cartTotal);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("bookImages", bookImages);
        return "User/cart";
    }

    @PostMapping("/download/{cartId}")
    public ResponseEntity<ByteArrayResource> downloadBook(
            @PathVariable Long cartId,
            HttpSession session) {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Cart> cartItemOpt = cartService.getCartItemById(cartId);
        if (cartItemOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found.");
        }

        Cart cartItem = cartItemOpt.get();
        if (cartItem.getUser() == null || !cartItem.getUser().getUserid().equals(user.getUserid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Book book = cartItem.getBook();
        if (book == null || book.getFileData() == null || book.getFileData().length == 0) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Book file is empty.");
        }
        if (book.getPrice() > 0 && !user.isAdmin() && !hasUserPurchasedBook(user, book)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!downloadRepo.existsByUserAndBook(user, book)) {
            downloadRepo.save(new Download(user, book));
        }

        String safeTitle = book.getTitle() != null ? book.getTitle().replaceAll("[^a-zA-Z0-9._-]", "_") : "book";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeTitle + ".pdf\"")
                .body(new ByteArrayResource(book.getFileData()));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<ByteArrayResource> downloadBookById(
            @PathVariable Long id,
            HttpSession session) {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Book> bookOpt = bookService.getBookById(id);
        if (bookOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Book book = bookOpt.get();
        if (book.getPrice() > 0 && !user.isAdmin() && !hasUserPurchasedBook(user, book)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (book.getFileData() == null || book.getFileData().length == 0) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        if (!downloadRepo.existsByUserAndBook(user, book)) {
            downloadRepo.save(new Download(user, book));
        }

        String safeTitle = book.getTitle() != null ? book.getTitle().replaceAll("[^a-zA-Z0-9._-]", "_") : "book";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeTitle + ".pdf\"")
                .body(new ByteArrayResource(book.getFileData()));
    }

    @GetMapping("/viewpdf/{id}")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) throws Exception {
        return bookService.getPdfPreview(id, 10);
    }

    @GetMapping("/checkout/{bookId}")
    public RedirectView purchase(
            @PathVariable Long bookId,
            HttpSession session,
            RedirectAttributes redirectAttributes) throws StripeException {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to purchase books.");
            return new RedirectView("/login?targetBookId=" + bookId);
        }

        Optional<Book> bookOptional = bookService.getBookById(bookId);
        if (bookOptional.isEmpty() || bookOptional.get().isDeleted()) {
            redirectAttributes.addFlashAttribute("error", "Book not found or unavailable.");
            return new RedirectView("/cart");
        }

        Book book = bookOptional.get();
        if (book.getStock() != null && book.getStock() <= 0) {
            redirectAttributes.addFlashAttribute("error", "'" + book.getTitle() + "' is currently out of stock.");
            return new RedirectView("/books/view/" + bookId);
        }
        if (book.getPrice() <= 0) {
            return new RedirectView("/download/" + bookId);
        }

        String stripeCheckoutUrl = stripeService.createCheckoutSession(
                bookId,
                book.getTitle(),
                book.getPrice(),
                user.getUserid(),
                user.getEmail(),
                resolveCurrentBaseUrl());

        return new RedirectView(stripeCheckoutUrl);
    }

    @PostMapping("/checkout-all")
    public RedirectView checkoutAll(
            HttpSession session,
            RedirectAttributes redirectAttributes) throws StripeException {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            return new RedirectView("/login");
        }

        List<Cart> cartItems = cartService.getActiveCartItems(user);
        if (cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Your cart is empty.");
            return new RedirectView("/cart");
        }

        for (Cart item : cartItems) {
            Book b = item.getBook();
            int qty = Math.max(1, item.getQuantity());
            if (b.getStock() != null && b.getStock() < qty) {
                redirectAttributes.addFlashAttribute("error",
                        "Insufficient stock for '" + b.getTitle() + "' (available: " + b.getStock() + ").");
                return new RedirectView("/cart");
            }
        }

        double totalPrice = cartService.calculateCartTotal(user);
        if (totalPrice <= 0) {
            redirectAttributes.addFlashAttribute("error", "Free books can be downloaded directly without paid checkout.");
            return new RedirectView("/cart");
        }

        String stripeCheckoutUrl = stripeService.createCheckoutSessionForMultipleBooks(
                cartItems,
                totalPrice,
                user.getUserid(),
                user.getEmail(),
                resolveCurrentBaseUrl());

        return new RedirectView(stripeCheckoutUrl);
    }

    @GetMapping("/success")
    public String paymentSuccess(
            @RequestParam("session_id") String sessionId,
            @RequestParam(value = "bookId", required = false) Long bookId,
            HttpSession sessionHttp,
            RedirectAttributes redirectAttributes) {

        if (sessionId == null || sessionId.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Missing payment session reference.");
            return "redirect:/cart";
        }

        Session stripeSession;
        try {
            stripeSession = stripeService.retrieveCheckoutSession(sessionId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to verify Stripe payment: " + e.getMessage());
            return "redirect:/cart";
        }

        if (stripeSession == null || !OrderReceiptService.isStripePaid(stripeSession.getPaymentStatus())) {
            redirectAttributes.addFlashAttribute("error", "Payment failed or was not completed. No paid receipt was issued.");
            return "redirect:/cart";
        }

        User user = resolveStripeOrderUser(stripeSession, sessionHttp);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User profile not found for this transaction. Please sign in.");
            return "redirect:/login";
        }
        authHelper.syncSessionAuth(sessionHttp, user);

        Long resolvedBookId = bookId;
        if (resolvedBookId == null && stripeSession.getMetadata() != null && stripeSession.getMetadata().containsKey("bookId")) {
            try {
                resolvedBookId = Long.parseLong(stripeSession.getMetadata().get("bookId"));
            } catch (NumberFormatException ignored) {}
        }

        Optional<Book> bookOpt = resolvedBookId != null ? bookService.getBookById(resolvedBookId) : Optional.empty();
        if (bookOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Book not found for this transaction.");
            return "redirect:/cart";
        }

        Order order = orderReceiptService.finalizeSingleBookCheckout(
                user,
                bookOpt.get(),
                1,
                sessionId,
                stripeSession.getPaymentIntent(),
                stripeSession.getPaymentStatus());

        redirectAttributes.addFlashAttribute("message",
                "Payment verified! Order " + order.getOrderNumber() + " and receipt " + order.getReceiptNumber() + " have been issued.");
        return "redirect:/orders/" + order.getId() + "/receipt";
    }

    @GetMapping("/payment-success")
    public String paymentSuccessMulti(
            @RequestParam("session_id") String sessionId,
            HttpSession sessionHttp,
            RedirectAttributes redirectAttributes) {

        if (sessionId == null || sessionId.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Missing payment session reference.");
            return "redirect:/cart";
        }

        Session stripeSession;
        try {
            stripeSession = stripeService.retrieveCheckoutSession(sessionId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to verify Stripe payment: " + e.getMessage());
            return "redirect:/cart";
        }

        if (stripeSession == null || !OrderReceiptService.isStripePaid(stripeSession.getPaymentStatus())) {
            redirectAttributes.addFlashAttribute("error", "Payment was not completed or failed. No paid receipt was generated.");
            return "redirect:/cart";
        }

        User user = resolveStripeOrderUser(stripeSession, sessionHttp);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User profile not identified. Please sign in.");
            return "redirect:/login";
        }
        authHelper.syncSessionAuth(sessionHttp, user);

        List<Cart> cartItems = cartService.getActiveCartItems(user);
        String metadataCartItems = stripeSession.getMetadata() != null ? stripeSession.getMetadata().get("cartItems") : null;

        try {
            Order order = orderReceiptService.finalizeMultiBookCheckout(
                    user,
                    cartItems,
                    metadataCartItems,
                    sessionId,
                    stripeSession.getPaymentIntent(),
                    stripeSession.getPaymentStatus());

            sessionHttp.setAttribute("cartTotal", 0.0);
            redirectAttributes.addFlashAttribute("message",
                    "Payment successful! Combined Order " + order.getOrderNumber()
                            + " (" + order.getDistinctBookCount() + " book(s)) and Receipt "
                            + order.getReceiptNumber() + " have been issued.");
            return "redirect:/orders/" + order.getId() + "/receipt";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not finalize order: " + e.getMessage());
            return "redirect:/order";
        }
    }

    @GetMapping("/order")
    public String viewUserOrders(Model model, HttpSession session) {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        List<Order> orders = orderRepo.findByUserOrderByOrderDateDesc(user);
        model.addAttribute("orders", orders);
        return "User/order";
    }

    @GetMapping("/cancel")
    public String paymentCancel(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Payment cancelled. No charge was made and no receipt was generated.");
        return "redirect:/cart";
    }

    @Transactional
    @PostMapping("/delete-account")
    public String deleteAccount(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = authHelper.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        if (user.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Administrator accounts cannot be self-deleted.");
            return "redirect:/admin/profile";
        }
        try {
            cartRepo.clearOrderReferenceForUser(user);
            cartRepo.deleteAll(cartRepo.findByUser(user));
            downloadRepo.deleteAll(downloadRepo.findByUser(user));
            orderRepo.deleteAll(orderRepo.findByUser(user));
            userRepo.delete(user);
            authHelper.clearAuth(session);
            redirectAttributes.addFlashAttribute("message", "Your reader account and personal library have been permanently removed.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error removing account: " + e.getMessage());
            return "redirect:/profile";
        }
    }
}

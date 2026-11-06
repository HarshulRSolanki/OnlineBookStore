package com.example.demo.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Cart;
import com.example.demo.Entity.Download;
import com.example.demo.Entity.Order;
import com.example.demo.Entity.User;
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.DownloadRepo;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.UserRepo;
import com.example.demo.service.BookService;
import com.example.demo.service.CartService;
import com.example.demo.service.EmailService;
import com.example.demo.service.OTPservice;
import com.example.demo.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

import jakarta.servlet.http.HttpSession;

@Controller
public class Usercontroller {

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	private DownloadRepo downloadRepo;

	@Autowired
	private OTPservice otpservice;

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private BookRepo bookRepo;

	@Autowired
	private BookService bookService;

	private User user;

	@Autowired
	private CartService cartService;

	@Autowired
	private CartRepo cartRepo;

	@Autowired
	private OrderRepo orderRepo;

	@Autowired
	private StripeService stripeService;

	@Autowired
	private com.example.demo.service.OrderReceiptService orderReceiptService;

	@GetMapping("/userdash")
	public String user(HttpSession session, Model model) {
		Object loggedIn = session.getAttribute("loggedInUser");
		if (!(loggedIn instanceof User)) {
			return "redirect:/login";
		}
		User user = (User) loggedIn;
		List<Cart> cartItems = cartRepo.findByUser(user);
		model.addAttribute("cartItems", cartItems);
		return "User/userdash";
	}

	@PostMapping("/userdash")
	public String userdash(HttpSession session, Model model) {
		Object loggedIn = session.getAttribute("loggedInUser");
		if (!(loggedIn instanceof User)) {
			return "redirect:/login";
		}
		User user = (User) loggedIn;
		List<Cart> cartItems = cartRepo.findByUser(user);
		model.addAttribute("cartItems", cartItems);
		return "redirect:/userdash";
	}

	@GetMapping("/userreg")
	public String userreg() {
		return "User/userreg";
	}

	@PostMapping("/userreg")
	public String userreg(@RequestParam("name") String name,
			@RequestParam("email") String email,
			@RequestParam("password") String password,
			@RequestParam("confirm_password") String confirmPassword,
			@RequestParam("mobilenumber") String mobilenumber,
			Model model, HttpSession session) {

		String trimmedName = name != null ? name.trim() : "";
		String trimmedEmail = email != null ? email.trim() : "";
		String trimmedMobile = mobilenumber != null ? mobilenumber.trim() : "";

		if (!password.equals(confirmPassword)) {
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

		user = new User();
		user.setUsername(trimmedName);
		user.setEmail(trimmedEmail);
		user.setPassword(passwordEncoder.encode(password));
		user.setMobileno(trimmedMobile);
		user.setIsregistered(false);

		session.setAttribute("tempUser", user);

		String otp = otpservice.generateOTP(trimmedEmail);
		otpservice.sendOTP(trimmedEmail, otp);
		user.setOtp(otp); // Store OTP in user entity

		// Redirect to OTP verification page
		model.addAttribute("email", trimmedEmail);
		return "verifyotp";
	}

	@GetMapping("/verify-otp")
	public String verifyforforget() {
		return "login";
	}

	@PostMapping("/verify-otp")
	public String verifyOTP(@RequestParam("email") String email,
			@RequestParam("otp") String otp,
			Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {

		// Validate email and OTP
		if (email == null || otp == null || email.isEmpty() || otp.isEmpty()) {
			model.addAttribute("error", "Invalid request. Please enter a valid email and OTP.");
			return "verifyotp";
		}

		// Verify OTP
		if (!otpservice.verifyOTP(email, otp)) {
			model.addAttribute("error", "Invalid OTP. Please try again.");
			return "verifyotp";
		}

		try {
			// Retrieve user from session (for first-time registration)
			User user = (User) session.getAttribute("tempUser");

			// If user is null, check if they exist in the database
			if (user == null) {
				user = userRepo.findByEmail(email);
			}

			// If user is still null, registration failed
			if (user == null) {
				model.addAttribute("error", "User not found. Please register again.");
				return "User/userreg";
			}

			if (user.isIsregistered()) {
				model.addAttribute("email", email);
				return "User/newpwd";
			}
			// Mark user as registered
			user.setIsregistered(true);
			userRepo.save(user);

			// Remove user from session after successful registration
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
		if (email != null && !otpservice.canResend(email)) {
			long remaining = otpservice.getCooldownRemainingSeconds(email);
			model.addAttribute("email", email);
			model.addAttribute("error", "Security Cooldown: Please wait " + remaining + " seconds before requesting a new code.");
			return "verifyotp";
		}
		// Generate a new OTP and send it again
		String newOtp = otpservice.generateOTP(email);
		otpservice.sendOTP(email, newOtp);
		model.addAttribute("email", email);
		model.addAttribute("message", "A new secure verification code has been dispatched to your email.");
		return "verifyotp";
	}

	@PostMapping("/add/{bookId}")
	public String addToCart(@PathVariable Long bookId,
			HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			// ✅ Ensure user is logged in
			User user = (User) session.getAttribute("loggedInUser");
			if (user == null) {
				redirectAttributes.addFlashAttribute("error", "Please log in to add items to your cart.");
				redirectAttributes.addFlashAttribute("targetBookId", bookId);
				return "redirect:/login";
			}

			// ✅ Find the book

			Optional<Book> bookOpt = bookRepo.findById(bookId);

			if (!bookOpt.isPresent()) {
				redirectAttributes.addFlashAttribute("error", "Book not found.");
				return "redirect:/cart";
			}

			Book book = bookOpt.get();

			// ✅ Prevent duplicate book entries for the same user
			if (cartRepo.existsByUserAndBook(user, book)) {
				redirectAttributes.addFlashAttribute("error", "This book is already in your cart.");
				return "redirect:/cart";
			}

			// ✅ Add the book to the user's cart
			Cart cart = new Cart();
			cart.setUser(user);
			cart.setBook(book);
			cart.setQuantity(1);
			cartRepo.save(cart);

			redirectAttributes.addFlashAttribute("success", "Book successfully added to your cart!");
		} catch (Exception e) {
			model.addAttribute("errorMessage", "An error occurred " + e.getMessage());
			return "errorpage";
		}

		return "redirect:/cart";
	}

	@PostMapping("/download/{cartId}")
	public ResponseEntity<ByteArrayResource> downloadBook(@PathVariable Long cartId) {
		// ✅ Fetch cart item from the database
		Optional<Cart> cartItemOpt = cartService.getCartItemById(cartId);
		if (cartItemOpt.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found.");
		}

		Cart cartItem = cartItemOpt.get();
		Book book = cartItem.getBook();

		// ✅ Check if the book file exists
		if (book.getFileData() == null || book.getFileData().length == 0) {
			throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Book file is empty.");
		}

		// ✅ Store download record if not already stored
		if (!downloadRepo.existsByUserAndBook(cartItem.getUser(), book)) {
			Download download = new Download();
			download.setUser(cartItem.getUser());
			download.setBook(book);
			download.setDownloadDate(LocalDateTime.now());
			downloadRepo.save(download);
		}

		// ✅ Return the book as a downloadable response
		ByteArrayResource resource = new ByteArrayResource(book.getFileData());
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + book.getTitle() + ".pdf\"")
				.body(resource);
	}

	@GetMapping("/download/{id}")
	public ResponseEntity<ByteArrayResource> downloadBook(@PathVariable Long id, HttpSession session) {
		// ✅ Check if user is logged in
		User user = (User) session.getAttribute("loggedInUser");
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// ✅ Get book from the database
		Optional<Book> bookOpt = bookService.getBookById(id);
		if (bookOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		Book book = bookOpt.get();

		// ✅ Store download record in database if not already stored
		boolean alreadyDownloaded = downloadRepo.existsByUserAndBook(user, book);
		if (!alreadyDownloaded) {
			Download download = new Download();
			download.setUser(user);
			download.setBook(book);
			download.setDownloadDate(LocalDateTime.now());
			downloadRepo.save(download);
		}

		// ✅ Return the book as a downloadable response
		ByteArrayResource resource = new ByteArrayResource(book.getFileData());
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + book.getTitle() + ".pdf\"")
				.body(resource);
	}

	@PostMapping("/delete/{cartId}")
	public String removeFromCart(@PathVariable Long cartId, Model model) {
		cartRepo.deleteById(cartId);
		model.addAttribute("message", "Book removed from cart.");
		return "redirect:/cart"; // Adjust the redirect as needed
	}

	@GetMapping("/viewpdf/{id}")
	public ResponseEntity<byte[]> getPdf(@PathVariable Long id) throws Exception {
		return bookService.getPdfPreview(id, 10); // Show first 5 pages
	}

	@GetMapping("/checkout/{bookId}")
	public RedirectView purchase(@PathVariable Long bookId, HttpSession session, RedirectAttributes redirectAttributes) throws StripeException {
		User user = (User) session.getAttribute("loggedInUser");
		if (user == null) {
			redirectAttributes.addFlashAttribute("error", "Please log in to purchase books.");
			redirectAttributes.addFlashAttribute("targetBookId", bookId);
			return new RedirectView("/login");
		}

		Optional<Book> bookOptional = bookService.getBookById(bookId);
		if (bookOptional.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "Book not found.");
			return new RedirectView("/cart");
		}

		Book book = bookOptional.get();

		// Create a Stripe payment session with user ID and email
		String stripeCheckoutUrl = stripeService.createCheckoutSession(bookId, book.getTitle(), book.getPrice(), user.getUserid(), user.getEmail());

		// Redirect to Stripe payment page
		return new RedirectView(stripeCheckoutUrl);
	}

	@GetMapping("/success")
	public String paymentSuccess(@RequestParam("session_id") String sessionId,
			@RequestParam("bookId") Long bookId,
			HttpSession sessionHttp,
			RedirectAttributes redirectAttributes) throws Exception {

		// ✅ Retrieve Stripe session details
		Session session = Session.retrieve(sessionId);
		if (session == null || !"paid".equals(session.getPaymentStatus())) {
			redirectAttributes.addFlashAttribute("error", "Payment failed or incomplete. Please try again.");
			return "redirect:/cart";
		}

		// ✅ Check idempotency to prevent duplicate orders and emails
		if (orderRepo.findByStripeSessionId(sessionId).isPresent()) {
			redirectAttributes.addFlashAttribute("message", "Payment verified! Your edition is ready in your library.");
			return "redirect:/order";
		}

		// ✅ Fetch user details from session, metadata, or customer email
		User user = (User) sessionHttp.getAttribute("loggedInUser");
		if (user == null && session.getMetadata() != null && session.getMetadata().containsKey("userId")) {
			try {
				Long uid = Long.parseLong(session.getMetadata().get("userId"));
				user = userRepo.findById(uid).orElse(null);
			} catch (Exception ignored) {}
		}
		if (user == null && session.getCustomerDetails() != null && session.getCustomerDetails().getEmail() != null) {
			String customerEmail = session.getCustomerDetails().getEmail();
			user = userRepo.findByEmail(customerEmail);
			if (user == null) {
				user = userRepo.getUserByEmail(customerEmail);
			}
		}

		if (user == null) {
			redirectAttributes.addFlashAttribute("error", "User profile not found for this transaction. Please log in.");
			return "redirect:/login";
		}

		sessionHttp.setAttribute("loggedInUser", user);

		// ✅ Fetch book details
		Optional<Book> bookOptional = bookService.getBookById(bookId);
		if (bookOptional.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "Book not found.");
			return "redirect:/cart";
		}
		Book book = bookOptional.get();

		Order order = new Order();
		order.setUser(user);
		order.setBook(book);
		order.setQuantity(1);
		double price = (double) book.getPrice();
		order.setUnitPrice(price);
		order.setAmount(price);
		order.setPaymentStatus("PAID");
		order.setCheckedout(true);
		order.setOrderDate(LocalDateTime.now());
		order.setStripeSessionId(sessionId);
		if (session.getPaymentIntent() != null) {
			order.setStripePaymentIntentId(session.getPaymentIntent());
		}

		// Automated receipt processing & email dispatch with PDF attachment
		orderReceiptService.processAndSendReceipt(order, sessionId, session.getPaymentIntent());

		// Store download record if not already stored
		if (!downloadRepo.existsByUserAndBook(user, book)) {
			Download download = new Download();
			download.setUser(user);
			download.setBook(book);
			download.setDownloadDate(LocalDateTime.now());
			downloadRepo.save(download);
		}

		redirectAttributes.addFlashAttribute("message", "Payment successful! The edition and tax receipt have been issued.");
		return "redirect:/order";
	}

	@GetMapping("/payment-success")
	public String paymentSuccessMulti(@RequestParam("session_id") String sessionId,
			HttpSession sessionHttp,
			RedirectAttributes redirectAttributes) throws StripeException {
		// Retrieve session from Stripe
		Session session = Session.retrieve(sessionId);
		if (session == null || !"paid".equals(session.getPaymentStatus())) {
			redirectAttributes.addFlashAttribute("error", "Payment could not be verified.");
			return "redirect:/cart";
		}

		// Check idempotency
		if (orderRepo.findByStripeSessionId(sessionId).isPresent()) {
			redirectAttributes.addFlashAttribute("message", "Payment verified! Your editions are ready in your library.");
			return "redirect:/order";
		}

		User user = (User) sessionHttp.getAttribute("loggedInUser");
		if (user == null && session.getMetadata() != null && session.getMetadata().containsKey("userId")) {
			try {
				Long uid = Long.parseLong(session.getMetadata().get("userId"));
				user = userRepo.findById(uid).orElse(null);
			} catch (Exception ignored) {}
		}
		if (user == null && session.getCustomerDetails() != null && session.getCustomerDetails().getEmail() != null) {
			String customerEmail = session.getCustomerDetails().getEmail();
			user = userRepo.findByEmail(customerEmail);
			if (user == null) {
				user = userRepo.getUserByEmail(customerEmail);
			}
		}

		if (user == null) {
			redirectAttributes.addFlashAttribute("error", "User profile not identified. Please sign in.");
			return "redirect:/login";
		}

		sessionHttp.setAttribute("loggedInUser", user);

		// Get user's cart items
		List<Cart> cartItems = cartService.getCartItems(user.getUserid());

		// Save each book in the cart as an order
		for (Cart cart : cartItems) {
			Order order = new Order();
			order.setUser(user);
			order.setBook(cart.getBook());
			int qty = cart.getQuantity() > 0 ? cart.getQuantity() : 1;
			order.setQuantity(qty);
			double price = (double) cart.getBook().getPrice();
			order.setUnitPrice(price);
			order.setAmount(price * qty);
			order.setPaymentStatus("PAID");
			order.setCheckedout(true);
			order.setOrderDate(LocalDateTime.now());
			order.setStripeSessionId(sessionId);
			if (session.getPaymentIntent() != null) {
				order.setStripePaymentIntentId(session.getPaymentIntent());
			}

			// Automated receipt processing & email dispatch with PDF attachment
			orderReceiptService.processAndSendReceipt(order, sessionId, session.getPaymentIntent());

			// Store as downloaded book
			if (!downloadRepo.existsByUserAndBook(user, cart.getBook())) {
				Download download = new Download();
				download.setUser(user);
				download.setBook(cart.getBook());
				download.setDownloadDate(LocalDateTime.now());
				downloadRepo.save(download);
			}
		}

		// Clear cart after order is saved
		cartService.clearCart(user.getUserid());
		sessionHttp.setAttribute("cartTotal", 0.0);

		redirectAttributes.addFlashAttribute("message", "Payment successful! Your orders and official receipts have been generated.");
		return "redirect:/order";
	}

	@GetMapping("/order")
	public String viewUserOrders(Model model, HttpSession session) {
		User user = (User) session.getAttribute("loggedInUser");
		if (user == null) {
			return "redirect:/login";
		}
		List<Order> orders = orderRepo.findByUser(user); // Fetch only this user's orders
		model.addAttribute("orders", orders);
		return "User/order";
	}

	@GetMapping("/cancel")
	public String paymentCancel(Model model) {
		model.addAttribute("message", "Payment Canceled! Try again.");
		return "User/cart";
	}

	@GetMapping("/editProfile")
	public String editProfile(@RequestParam("userId") Long userId, Model model) {

		Optional<User> userOptional = userRepo.findById(userId);

		if (userOptional.isPresent()) {
			model.addAttribute("user", userOptional.get());
			return "User/profile";
		} else {
			model.addAttribute("error", "User not found!");
			return "error";
		}
	}

	@PostMapping("/updateProfile/{userId}")
	public String updateUserProfile(@PathVariable Long userId,
			@RequestParam String username,
			@RequestParam String email,
			@RequestParam(required = false) String password,
			@RequestParam(required = false) String mobile,
			Model model) {
		try {
			Optional<User> optionalUser = userRepo.findById(userId);

			if (optionalUser.isPresent()) {
				User user = optionalUser.get();

				// Update username and email (they are required)
				user.setUsername(username);
				user.setEmail(email);

				// Only update password if provided
				if (password != null && !password.trim().isEmpty()) {

					user.setPassword(passwordEncoder.encode(password)); // Make sure to encrypt this if necessary
				}

				// Only update mobile number if provided
				if (mobile != null && !mobile.trim().isEmpty()) {
					user.setMobileno(mobile);
				}

				userRepo.save(user);
				return "redirect:/userdash?success=Profile updated successfully";
			} else {
				model.addAttribute("errorMessage", "User not found!");
				return "errorpage";
			}
		} catch (Exception e) {
			model.addAttribute("errorMessage", "Error updating profile: " + e.getMessage());
			return "errorpage";
		}
	}

	@PostMapping("/checkout-all")
	public RedirectView checkoutAll(HttpSession session) throws StripeException {
		// Get logged-in user
		User user = (User) session.getAttribute("loggedInUser");
		if (user == null) {
			return new RedirectView("/login"); // Redirect if not logged in
		}

		// Get user's cart items
		List<Cart> cartItems = cartService.getCartItems(user.getUserid());
		if (cartItems.isEmpty()) {
			return new RedirectView("/cart?error=CartEmpty"); // Redirect if cart is empty
		}

		// Calculate total price
		double totalPrice = cartItems.stream()
				.filter(cart -> !cart.getBook().isDeleted()) // Ignore deleted books
				.mapToDouble(cart -> cart.getBook().getPrice())
				.sum();

		if (totalPrice == 0) {
			return new RedirectView("/cart?error=FreeBooksOnly"); // Handle free books separately
		}

		// Create Stripe Payment Session for all books with user details
		String stripeCheckoutUrl = stripeService.createCheckoutSessionForMultipleBooks(cartItems, totalPrice, user.getUserid(), user.getEmail());

		// Redirect to Stripe payment page
		return new RedirectView(stripeCheckoutUrl);
	}

	@PostMapping("/delete-account")
	public String deleteAccount(HttpSession session, RedirectAttributes redirectAttributes) {
		User user = (User) session.getAttribute("loggedInUser");
		if (user == null) {
			return "redirect:/login";
		}
		try {
			cartRepo.deleteAll(cartRepo.findByUser(user));
			downloadRepo.deleteAll(downloadRepo.findByUser(user));
			orderRepo.deleteAll(orderRepo.findByUser(user));
			userRepo.delete(user);
			session.invalidate();
			redirectAttributes.addFlashAttribute("message", "Your reader account and personal library have been permanently removed.");
			return "redirect:/login";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error removing account: " + e.getMessage());
			return "redirect:/editProfile?userId=" + user.getUserid();
		}
	}

	@GetMapping("/cart")
	public String cart(HttpSession session, Model model) {
		User user = (User) session.getAttribute("loggedInUser");

		List<Cart> cartItems = cartRepo.findByUser(user);
		Map<Long, String> bookImages = new HashMap<>();

		double cartTotal = cartItems.stream()
				.filter(c -> !c.getBook().isDeleted())
				.mapToDouble(c -> c.getBook().getPrice() * c.getQuantity())
				.sum();

		// Process images for books in the cart
		for (Cart cartItem : cartItems) {
			Book book = cartItem.getBook(); // Assuming Cart has a Book relationship
			if (book != null && book.getCoverImage() != null) {
				String base64Image = Base64.getEncoder().encodeToString(book.getCoverImage());
				bookImages.put(book.getId(), "data:image/jpeg;base64," + base64Image);
			}
		}
		model.addAttribute("cartTotal", cartTotal);
		model.addAttribute("cartItems", cartItems);
		model.addAttribute("bookImages", bookImages);
		return "User/cart";
	}

}

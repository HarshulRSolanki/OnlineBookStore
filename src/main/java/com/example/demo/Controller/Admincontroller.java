package com.example.demo.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.AuthHelper;
import com.example.demo.Entity.Book;
import com.example.demo.Entity.Category;
import com.example.demo.Entity.Order;
import com.example.demo.Entity.User;
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.CategoryRepo;
import com.example.demo.Repository.DownloadRepo;
import com.example.demo.Repository.InquiryRepo;
import com.example.demo.Repository.OrderItemRepo;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.PendingCheckoutRepo;
import com.example.demo.Repository.UserAddressRepo;
import com.example.demo.Repository.UserRepo;
import com.example.demo.service.SalesReportService;
import com.example.demo.service.StoreLocationService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class Admincontroller {

    private static final Set<String> ALLOWED_ORDER_STATUSES = Set.of("CONFIRMED", "PROCESSING", "COMPLETED", "CANCELLED");

    @Autowired
    private BookRepo bookRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private DownloadRepo downloadRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private OrderItemRepo orderItemRepo;

    @Autowired
    private InquiryRepo inquiryRepo;

    @Autowired
    private UserAddressRepo userAddressRepo;

    @Autowired
    private PendingCheckoutRepo pendingCheckoutRepo;

    @Autowired
    private StoreLocationService storeLocationService;

    @Autowired
    private SalesReportService salesReportService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthHelper authHelper;

    @org.springframework.web.bind.annotation.InitBinder
    public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
        binder.registerCustomEditor(LocalDate.class, new java.beans.PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                    return;
                }
                try {
                    setValue(LocalDate.parse(text.trim(), java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                } catch (Exception e1) {
                    try {
                        setValue(LocalDate.parse(text.trim(), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
                    } catch (Exception e2) {
                        throw new IllegalArgumentException("Invalid date format: " + text);
                    }
                }
            }
        });
    }

    // =========================================================================
    // ADMIN DASHBOARD
    // =========================================================================

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        List<Book> allBooks = bookRepo.findAll();
        List<Book> activeBooks = allBooks.stream().filter(b -> !b.isDeleted()).toList();
        List<User> customerUsers = userRepo.findByRoleIgnoreCase("USER");
        List<Order> allOrders = orderRepo.findAllByOrderByOrderDateDesc();
        List<Order> recentOrders = allOrders.size() > 8 ? allOrders.subList(0, 8) : allOrders;

        Map<String, Object> salesStats = salesReportService.getDashboardStats();
        int totalStockUnits = activeBooks.stream()
                .mapToInt(b -> b.getStock() != null ? b.getStock() : 0)
                .sum();

        model.addAttribute("totalUsers", customerUsers.size());
        model.addAttribute("totalBooks", activeBooks.size());
        model.addAttribute("totalOrders", allOrders.size());
        model.addAttribute("totalSales", salesStats.getOrDefault("totalRevenue", 0.0));
        model.addAttribute("totalBooksSold", salesStats.getOrDefault("totalBooksSold", 0L));
        model.addAttribute("totalStockUnits", totalStockUnits);

        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("inventoryBooks", activeBooks);
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("books", allBooks);
        model.addAttribute("users", customerUsers);
        model.addAttribute("inquiries", inquiryRepo.findAllByOrderBySubmittedAtDesc());
        model.addAttribute("activeStoreCount", storeLocationService.countActiveStores());
        model.addAttribute("totalStoreCount", storeLocationService.getAllStoresForAdmin().size());
        return "Admin/admindash";
    }

    @GetMapping("/admindash")
    public String legacyAdminDashRedirect() {
        return "redirect:/admin/dashboard";
    }

    // =========================================================================
    // ADMIN BOOK MANAGEMENT
    // =========================================================================

    @GetMapping({"/admin/books", "/manage-books", "/displayBooks"})
    public String manageBooks(Model model) {
        List<Book> books = bookRepo.findAll();
        books.forEach(book -> {
            Long cartCount = cartRepo.countCartsByBook(book);
            Long downloadCount = downloadRepo.countDownloadsByBook(book);
            book.setCartCount(cartCount != null ? cartCount : 0L);
            book.setDownloadCount(downloadCount != null ? downloadCount : 0L);
        });
        model.addAttribute("books", books);
        model.addAttribute("categories", categoryRepo.findAll());
        return "Admin/books";
    }

    @GetMapping({"/admin/books/add", "/addBooks"})
    public String showAddBookForm(Model model) {
        Book book = new Book();
        book.setStock(100);
        model.addAttribute("book", book);
        model.addAttribute("categories", categoryRepo.findAll());
        return "Admin/add-book";
    }

    @PostMapping({"/admin/books/add", "/addBooks"})
    public String addBook(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam Long categoryId,
            @RequestParam String isbn,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) MultipartFile coverImage,
            @RequestParam Integer price,
            @RequestParam(required = false, defaultValue = "100") Integer stock,
            @RequestParam(required = false) LocalDate publishDate,
            @RequestParam String language,
            @RequestParam String publishername,
            RedirectAttributes redirectAttributes) {

        Optional<Category> categoryOpt = categoryRepo.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid category selected.");
            return "redirect:/admin/books/add";
        }

        if (price == null || price < 0) {
            redirectAttributes.addFlashAttribute("error", "Price cannot be negative.");
            return "redirect:/admin/books/add";
        }

        try {
            Book book = new Book();
            book.setTitle(title.trim());
            book.setAuthor(author.trim());
            book.setCategory(categoryOpt.get());
            book.setIsbn(isbn.trim());
            book.setDescription(description != null ? description.trim() : "");
            book.setPrice(price);
            book.setStock(stock != null && stock >= 0 ? stock : 100);
            book.setPublishDate(publishDate);
            book.setLanguage(language);
            book.setPublishername(publishername);
            if (file != null && !file.isEmpty()) {
                book.setFileType(file.getContentType());
                book.setFileData(file.getBytes());
            }
            if (coverImage != null && !coverImage.isEmpty()) {
                book.setCoverImage(coverImage.getBytes());
            }
            book.setDeleted(false);
            bookRepo.save(book);
            redirectAttributes.addFlashAttribute("message", "Book '" + book.getTitle() + "' added to the catalog.");
            return "redirect:/admin/books";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error saving book files: " + e.getMessage());
            return "redirect:/admin/books/add";
        }
    }

    @GetMapping({"/admin/books/edit/{bookId}", "/updateBooks/{bookId}"})
    public String showEditBookForm(@PathVariable Long bookId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Book> optionalBook = bookRepo.findById(bookId);
        if (optionalBook.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Book not found.");
            return "redirect:/admin/books";
        }
        model.addAttribute("book", optionalBook.get());
        model.addAttribute("categories", categoryRepo.findAll());
        return "Admin/editBook";
    }

    @PostMapping("/updateBooks/{bookId}")
    public String editBookPostCompat(@PathVariable Long bookId, Model model, RedirectAttributes redirectAttributes) {
        return showEditBookForm(bookId, model, redirectAttributes);
    }

    @PostMapping({"/admin/books/edit/{bookId}", "/update/{bookId}"})
    public String updateBook(
            @PathVariable Long bookId,
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam Long category,
            @RequestParam String isbn,
            @RequestParam String description,
            @RequestParam Integer price,
            @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) MultipartFile coverImage,
            @RequestParam(required = false) LocalDate publishDate,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String publishername,
            RedirectAttributes redirectAttributes) {

        Optional<Book> optionalBook = bookRepo.findById(bookId);
        if (optionalBook.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Book not found.");
            return "redirect:/admin/books";
        }

        Optional<Category> categoryOpt = categoryRepo.findById(category);
        if (categoryOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid category selected.");
            return "redirect:/admin/books/edit/" + bookId;
        }

        try {
            Book book = optionalBook.get();
            book.setTitle(title.trim());
            book.setAuthor(author.trim());
            book.setCategory(categoryOpt.get());
            book.setIsbn(isbn.trim());
            book.setDescription(description != null ? description.trim() : "");
            book.setPrice(price != null && price >= 0 ? price : 0);
            if (stock != null && stock >= 0) {
                book.setStock(stock);
            }
            if (publishDate != null) {
                book.setPublishDate(publishDate);
            }
            if (language != null) {
                book.setLanguage(language);
            }
            if (publishername != null) {
                book.setPublishername(publishername);
            }
            if (coverImage != null && !coverImage.isEmpty()) {
                book.setCoverImage(coverImage.getBytes());
            }
            if (file != null && !file.isEmpty()) {
                book.setFileData(file.getBytes());
                book.setFileType(file.getContentType());
            }
            book.setDeleted(false);
            bookRepo.save(book);
            redirectAttributes.addFlashAttribute("message", "Book '" + book.getTitle() + "' updated successfully.");
            return "redirect:/admin/books";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error updating book: " + e.getMessage());
            return "redirect:/admin/books/edit/" + bookId;
        }
    }

    @PostMapping("/admin/books/{bookId}/stock")
    public String updateBookStock(
            @PathVariable Long bookId,
            @RequestParam Integer stock,
            RedirectAttributes redirectAttributes) {
        Optional<Book> optionalBook = bookRepo.findById(bookId);
        if (optionalBook.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Book not found.");
            return "redirect:/admin/books";
        }
        Book book = optionalBook.get();
        book.setStock(stock != null && stock >= 0 ? stock : 0);
        bookRepo.save(book);
        redirectAttributes.addFlashAttribute("message",
                "Stock updated to " + book.getStock() + " for '" + book.getTitle() + "'.");
        return "redirect:/admin/books";
    }

    @Transactional
    @PostMapping({"/admin/books/delete", "/deleteBooks"})
    public String deleteBook(@RequestParam Long bookId, RedirectAttributes redirectAttributes) {
        Optional<Book> bookOpt = bookRepo.findById(bookId);
        if (bookOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Book not found.");
            return "redirect:/admin/books";
        }

        Book existingBook = bookOpt.get();
        boolean referenced = downloadRepo.existsByBookId(bookId)
                || orderRepo.existsByBookId(bookId)
                || orderItemRepo.existsByBookId(bookId);

        if (referenced) {
            existingBook.setDeleted(true);
            bookRepo.save(existingBook);
            redirectAttributes.addFlashAttribute("message",
                    "Book '" + existingBook.getTitle() + "' was archived (soft-deleted) to preserve existing order receipts and library records.");
        } else {
            cartRepo.deleteByBookId(bookId);
            bookRepo.deleteById(bookId);
            redirectAttributes.addFlashAttribute("message",
                    "Book '" + existingBook.getTitle() + "' removed permanently.");
        }
        return "redirect:/admin/books";
    }

    @GetMapping("/books/report/pdf")
    public void generateBooksPdfReport(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=booksreport.pdf");

        List<Book> books = bookRepo.findAll();
        books.forEach(book -> {
            book.setCartCount(cartRepo.countCartsByBook(book));
            book.setDownloadCount(downloadRepo.countDownloadsByBook(book));
        });

        try (PdfWriter writer = new PdfWriter(response.getOutputStream());
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc)) {

            document.add(new Paragraph("Quickread Books Inventory Report")
                    .setFontSize(20).setBold().setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Generated on: " + LocalDate.now())
                    .setFontSize(11).setTextAlignment(TextAlignment.CENTER).setMarginBottom(16));

            float[] columnWidths = {3, 2, 2, 2, 1.5f, 1.5f, 1.5f, 1.5f};
            Table table = new Table(columnWidths);
            table.setWidth(UnitValue.createPercentValue(100));

            String[] headers = {"Title", "Author", "Category", "ISBN", "Price", "Stock", "In Carts", "Downloads"};
            for (String headerText : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(headerText).setBold().setFontSize(9)));
            }

            for (Book book : books) {
                table.addCell(createCell(book.getTitle()));
                table.addCell(createCell(book.getAuthor()));
                table.addCell(createCell(book.getCategory() != null ? book.getCategory().getName() : "N/A"));
                table.addCell(createCell(book.getIsbn()));
                table.addCell(createCell("INR " + book.getPrice()));
                table.addCell(createCell(String.valueOf(book.getStock())));
                table.addCell(createCell(String.valueOf(book.getCartCount())));
                table.addCell(createCell(String.valueOf(book.getDownloadCount())));
            }

            Cell totalCell = new Cell(1, 8)
                    .add(new Paragraph("Total Books: " + books.size()).setBold())
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER);
            table.addCell(totalCell);
            document.add(table);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error generating PDF: " + e.getMessage());
        }
    }

    private Cell createCell(String content) {
        return new Cell().add(new Paragraph(content != null ? content : "-").setFontSize(9));
    }

    // =========================================================================
    // ADMIN CATEGORY MANAGEMENT
    // =========================================================================

    @GetMapping("/admin/categories")
    public String manageCategories(Model model) {
        List<Category> categories = categoryRepo.findAll();
        List<Book> allBooks = bookRepo.findAll();
        Map<Long, Long> categoryBookCounts = new LinkedHashMap<>();
        for (Category cat : categories) {
            long count = allBooks.stream()
                    .filter(b -> b.getCategory() != null && b.getCategory().getId().equals(cat.getId()))
                    .count();
            categoryBookCounts.put(cat.getId(), count);
        }
        model.addAttribute("categories", categories);
        model.addAttribute("categoryBookCounts", categoryBookCounts);
        return "Admin/categories";
    }

    @PostMapping({"/admin/categories/add", "/add-category"})
    public String addCategory(@RequestParam String categoryName, RedirectAttributes redirectAttributes) {
        String trimmed = categoryName != null ? categoryName.trim() : "";
        if (trimmed.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Category name cannot be empty.");
            return "redirect:/admin/categories";
        }
        if (categoryRepo.findByNameIgnoreCase(trimmed).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Category '" + trimmed + "' already exists.");
            return "redirect:/admin/categories";
        }
        Category category = new Category();
        category.setName(trimmed);
        categoryRepo.save(category);
        redirectAttributes.addFlashAttribute("message", "Category '" + trimmed + "' created successfully.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/edit")
    public String editCategory(
            @RequestParam Long categoryId,
            @RequestParam String categoryName,
            RedirectAttributes redirectAttributes) {
        String trimmed = categoryName != null ? categoryName.trim() : "";
        if (trimmed.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Category name cannot be empty.");
            return "redirect:/admin/categories";
        }
        Optional<Category> catOpt = categoryRepo.findById(categoryId);
        if (catOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Category not found.");
            return "redirect:/admin/categories";
        }
        Optional<Category> existingByName = categoryRepo.findByNameIgnoreCase(trimmed);
        if (existingByName.isPresent() && !existingByName.get().getId().equals(categoryId)) {
            redirectAttributes.addFlashAttribute("error", "Another category named '" + trimmed + "' already exists.");
            return "redirect:/admin/categories";
        }
        Category cat = catOpt.get();
        cat.setName(trimmed);
        categoryRepo.save(cat);
        redirectAttributes.addFlashAttribute("message", "Category renamed to '" + trimmed + "'.");
        return "redirect:/admin/categories";
    }

    @PostMapping({"/admin/categories/delete", "/delete-category"})
    public String deleteCategory(@RequestParam Long categoryId, RedirectAttributes redirectAttributes) {
        Optional<Category> categoryOpt = categoryRepo.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Category not found.");
            return "redirect:/admin/categories";
        }
        Category category = categoryOpt.get();
        if (bookRepo.existsByCategoryId(categoryId)) {
            redirectAttributes.addFlashAttribute("error",
                    "Cannot delete category '" + category.getName() + "' because it is assigned to existing books.");
            return "redirect:/admin/categories";
        }
        categoryRepo.delete(category);
        redirectAttributes.addFlashAttribute("message", "Category '" + category.getName() + "' deleted successfully.");
        return "redirect:/admin/categories";
    }

    // =========================================================================
    // ADMIN USER MANAGEMENT (ADMIN ACCOUNTS PROTECTED)
    // =========================================================================

    @GetMapping("/admin/users")
    public String manageUsers(Model model) {
        List<User> customerUsers = userRepo.findByRoleIgnoreCase("USER");
        Map<Long, Long> orderCounts = new LinkedHashMap<>();
        Map<Long, Integer> cartCounts = new LinkedHashMap<>();
        Map<Long, Integer> downloadCounts = new LinkedHashMap<>();

        for (User u : customerUsers) {
            orderCounts.put(u.getUserid(), orderRepo.countByUser(u));
            cartCounts.put(u.getUserid(), cartRepo.findByUser(u).size());
            downloadCounts.put(u.getUserid(), downloadRepo.findByUser(u).size());
        }

        model.addAttribute("users", customerUsers);
        model.addAttribute("orderCounts", orderCounts);
        model.addAttribute("cartCounts", cartCounts);
        model.addAttribute("downloadCounts", downloadCounts);
        return "Admin/users";
    }

    @GetMapping("/admin/users/{userId}")
    public String viewUserDetail(@PathVariable Long userId, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        User targetUser = userOpt.get();
        if (targetUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Administrator accounts are protected and cannot be modified from User Management.");
            return "redirect:/admin/users";
        }

        List<Order> userOrders = orderRepo.findByUserOrderByOrderDateDesc(targetUser);
        model.addAttribute("targetUser", targetUser);
        model.addAttribute("userOrders", userOrders);
        model.addAttribute("cartItems", cartRepo.findByUser(targetUser));
        model.addAttribute("downloads", downloadRepo.findByUser(targetUser));
        return "Admin/user-detail";
    }

    @PostMapping("/admin/users/{userId}/toggle-status")
    public String toggleUserStatus(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        User targetUser = userOpt.get();
        if (targetUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Protected: Administrator accounts cannot be disabled.");
            return "redirect:/admin/users";
        }
        boolean nextState = !targetUser.isEnabled();
        targetUser.setEnabled(nextState);
        userRepo.save(targetUser);
        redirectAttributes.addFlashAttribute("message",
                "User '" + targetUser.getUsername() + "' has been " + (nextState ? "enabled" : "disabled") + ".");
        return "redirect:/admin/users";
    }

    @Transactional
    @PostMapping({"/admin/users/delete", "/delete-user"})
    public String deleteUser(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        User user = userOpt.get();
        if (user.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Protected: Administrator accounts cannot be deleted.");
            return "redirect:/admin/users";
        }
        try {
            cartRepo.clearOrderReferenceForUser(user);
            cartRepo.deleteAll(cartRepo.findByUser(user));
            downloadRepo.deleteAll(downloadRepo.findByUser(user));
            pendingCheckoutRepo.deleteAllByUser(user);
            userAddressRepo.deleteAllByUser(user);
            orderRepo.deleteAll(orderRepo.findByUser(user));
            userRepo.delete(user);
            redirectAttributes.addFlashAttribute("message",
                    "User account '" + user.getUsername() + "' removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // =========================================================================
    // ADMIN ORDER MANAGEMENT
    // =========================================================================

    @GetMapping("/admin/orders")
    public String manageOrders(
            @RequestParam(required = false, defaultValue = "ALL") String paymentStatus,
            Model model) {
        List<Order> allOrders = orderRepo.findAllByOrderByOrderDateDesc();
        List<Order> filtered = allOrders;
        if (paymentStatus != null && !"ALL".equalsIgnoreCase(paymentStatus.trim())) {
            String target = paymentStatus.trim();
            filtered = allOrders.stream()
                    .filter(o -> o.getPaymentStatus() != null && o.getPaymentStatus().equalsIgnoreCase(target))
                    .toList();
        }
        model.addAttribute("orders", filtered);
        model.addAttribute("filterPaymentStatus", paymentStatus);
        return "Admin/orders";
    }

    @GetMapping("/admin/orders/{orderId}")
    public String viewOrderDetail(@PathVariable Long orderId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Order> orderOpt = orderRepo.findById(orderId);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found.");
            return "redirect:/admin/orders";
        }
        Order order = orderOpt.get();
        List<String> allowed = Order.getAllowedStatusesForMethod(order.getDeliveryMethod());
        Map<String, String> statusLabels = new LinkedHashMap<>();
        for (String st : allowed) {
            statusLabels.put(st, Order.formatStatusLabel(st));
        }
        model.addAttribute("order", order);
        model.addAttribute("items", order.getEffectiveItems());
        model.addAttribute("allowedStatuses", allowed);
        model.addAttribute("statusLabels", statusLabels);
        return "Admin/order-detail";
    }

    @PostMapping("/admin/orders/{orderId}/status")
    public String updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String orderStatus,
            RedirectAttributes redirectAttributes) {
        Optional<Order> orderOpt = orderRepo.findById(orderId);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found.");
            return "redirect:/admin/orders";
        }
        Order order = orderOpt.get();
        String normalized = orderStatus != null ? orderStatus.trim().toUpperCase().replace(' ', '_') : "";
        List<String> allowedForOrder = Order.getAllowedStatusesForMethod(order.getDeliveryMethod());
        boolean allowed = allowedForOrder.contains(normalized)
                || (order.getDeliveryMethod() == null && ALLOWED_ORDER_STATUSES.contains(normalized));
        if (!allowed) {
            redirectAttributes.addFlashAttribute("error",
                    "Unsupported status '" + orderStatus + "' for " + order.getDeliveryMethodDisplayName() + ".");
            return "redirect:/admin/orders/" + orderId;
        }
        order.setOrderStatus(normalized);
        orderRepo.save(order);
        redirectAttributes.addFlashAttribute("message",
                "Order " + order.getOrderNumber() + " fulfillment status updated to "
                        + Order.formatStatusLabel(normalized) + ".");
        return "redirect:/admin/orders/" + orderId;
    }

    // =========================================================================
    // ADMIN INQUIRIES & PROFILE
    // =========================================================================

    @GetMapping("/admin/inquiries")
    public String viewInquiries(Model model) {
        model.addAttribute("inquiries", inquiryRepo.findAllByOrderBySubmittedAtDesc());
        return "Admin/inquiries";
    }

    @PostMapping({"/admin/inquiries/delete", "/delete-inquiry"})
    public String deleteInquiry(@RequestParam Long inquiryId, RedirectAttributes redirectAttributes) {
        try {
            inquiryRepo.deleteById(inquiryId);
            redirectAttributes.addFlashAttribute("message", "Inquiry removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not remove inquiry: " + e.getMessage());
        }
        return "redirect:/admin/inquiries";
    }

    @GetMapping("/admin/profile")
    public String viewAdminProfile(HttpSession session, Model model) {
        User adminUser = authHelper.getLoggedInUser(session);
        if (adminUser == null || !adminUser.isAdmin()) {
            return "redirect:/login";
        }
        model.addAttribute("adminUser", adminUser);
        return "Admin/profile";
    }

    @PostMapping("/admin/profile")
    public String updateAdminProfile(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User adminUser = authHelper.getLoggedInUser(session);
        if (adminUser == null || !adminUser.isAdmin()) {
            return "redirect:/login";
        }

        String trimmedUsername = username != null ? username.trim() : "";
        String trimmedEmail = email != null ? email.trim() : "";
        String trimmedMobile = mobile != null ? mobile.trim() : "";

        if (trimmedUsername.length() < 3 || trimmedUsername.length() > 50) {
            redirectAttributes.addFlashAttribute("error", "Username must be between 3 and 50 characters.");
            return "redirect:/admin/profile";
        }
        if (trimmedEmail.isEmpty() || !trimmedEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid email address.");
            return "redirect:/admin/profile";
        }

        User conflictUser = userRepo.findByUsernameIgnoreCase(trimmedUsername);
        if (conflictUser != null && !conflictUser.getUserid().equals(adminUser.getUserid())) {
            redirectAttributes.addFlashAttribute("error", "Username '" + trimmedUsername + "' is already in use.");
            return "redirect:/admin/profile";
        }
        User conflictEmail = userRepo.findByEmailIgnoreCase(trimmedEmail);
        if (conflictEmail != null && !conflictEmail.getUserid().equals(adminUser.getUserid())) {
            redirectAttributes.addFlashAttribute("error", "Email '" + trimmedEmail + "' is already in use.");
            return "redirect:/admin/profile";
        }

        User freshAdmin = userRepo.findById(adminUser.getUserid()).orElse(adminUser);
        freshAdmin.setUsername(trimmedUsername);
        freshAdmin.setEmail(trimmedEmail);
        if (!trimmedMobile.isEmpty()) {
            freshAdmin.setMobileno(trimmedMobile);
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (currentPassword == null || currentPassword.isEmpty()
                    || !passwordEncoder.matches(currentPassword, freshAdmin.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Current password verification failed.");
                return "redirect:/admin/profile";
            }
            freshAdmin.setPassword(passwordEncoder.encode(newPassword.trim()));
        }

        User saved = userRepo.save(freshAdmin);
        authHelper.syncSessionAuth(session, saved);
        redirectAttributes.addFlashAttribute("message", "Administrator profile updated successfully.");
        return "redirect:/admin/profile";
    }
}

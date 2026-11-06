package com.example.demo.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Category;
import com.example.demo.Entity.User;
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.CategoryRepo;
import com.example.demo.Repository.DownloadRepo;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.UserRepo;
import com.example.demo.Repository.InquiryRepo;
import com.example.demo.Entity.Inquiry;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

@Controller
public class Admincontroller {
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
    private InquiryRepo inquiryRepo;

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

    @GetMapping("/manage-books")
    public String manageBooks(Model model) {
        List<Book> books = bookRepo.findAll();

        books.forEach(book -> {
            Long cartCount = cartRepo.countCartsByBook(book);
            Long downloadCount = downloadRepo.countDownloadsByBook(book);
            book.setCartCount(cartCount != null ? cartCount : 0L);
            book.setDownloadCount(downloadCount != null ? downloadCount : 0L);
        });

        model.addAttribute("books", books);
        return "Admin/books";
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

            // Add title
            Paragraph header = new Paragraph("Books Report")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(header);

            // Add subtitle
            Paragraph subheader = new Paragraph("Generated on: " + LocalDate.now())
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(subheader);

            // Create table with adjusted column widths (removed ID column)
            float[] columnWidths = { 3, 3, 2, 3, 2, 2, 2 }; // Removed first column width
            Table table = new Table(columnWidths);
            table.setWidth(UnitValue.createPercentValue(100));

            // Updated table headers (removed ID)
            String[] headers = {
                    "Title", "Author", "Category", "ISBN", "Price",
                    "In Carts", "Downloads"
            };
            for (String headerText : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(headerText).setBold()));
            }

            // Add table rows (removed ID cell)
            for (Book book : books) {
                table.addCell(createCell(book.getTitle()));
                table.addCell(createCell(book.getAuthor()));
                table.addCell(createCell(book.getCategory().getName()));
                table.addCell(createCell(book.getIsbn()));
                table.addCell(createCell("₹" + book.getPrice()));
                table.addCell(createCell(String.valueOf(book.getCartCount())));
                table.addCell(createCell(String.valueOf(book.getDownloadCount())));
            }

            // Adjusted total count cell span (6 columns instead of 7)
            Cell totalCell = new Cell(1, 6)
                    .add(new Paragraph("Total Books: " + books.size()).setBold())
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER);
            table.addCell(totalCell.add(new Paragraph("")));

            document.add(table);

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error generating PDF: " + e.getMessage());
        }
    }

    private Cell createCell(String content) {
        return new Cell().add(new Paragraph(content));
    }

    @GetMapping("/admindash")
    public String adminDashboard(Model model) {
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("books", bookRepo.findAll());
        model.addAttribute("users", userRepo.findAll());
        model.addAttribute("inquiries", inquiryRepo.findAllByOrderBySubmittedAtDesc());
        return "Admin/admindash";
    }

    @GetMapping("/addBooks")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryRepo.findAll());
        return "addbook"; // Ensure this template exists
    }

    @PostMapping("/addBooks")
    public String addBook(@RequestParam String title,
            @RequestParam String author,
            @RequestParam Long categoryId,
            @RequestParam String isbn,
            @RequestParam String description,
            @RequestParam MultipartFile file,
            @RequestParam MultipartFile coverImage,
            @RequestParam Integer price,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate publishDate,
            @RequestParam String language,
            @RequestParam String publishername,
            Model model) {

        Optional<Category> categoryOpt = categoryRepo.findById(categoryId);
        if (!categoryOpt.isPresent()) {
            model.addAttribute("error", "Invalid category.");
            return "Admin/admindash";
        }

        try {
            // ✅ Convert files into byte arrays
            byte[] fileBytes = file.getBytes();
            byte[] coverImageBytes = coverImage.getBytes();

            Book book = new Book();
            book.setTitle(title);
            book.setAuthor(author);
            book.setCategory(categoryOpt.get());
            book.setIsbn(isbn);
            book.setDescription(description);
            book.setPrice(price);
            book.setPublishDate(publishDate);
            book.setLanguage(language);
            book.setPublishername(publishername);
            book.setFileType(file.getContentType());
            book.setFileData(fileBytes);
            book.setCoverImage(coverImageBytes);
            bookRepo.save(book);
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Error saving book: " + e.getMessage());
            return "errorpage";
        }
        return "redirect:/admindash";
    }

    @GetMapping("/displayBooks")
    public String displayBooks(Model model) {
        List<Book> books = bookRepo.findAll();
        model.addAttribute("books", books);
        return "redirect:/admindash#displaybook"; // Ensure this template exists
    }

    @PostMapping("/add-category")
    public String addCategory(@RequestParam String categoryName, Model model) {
        try {
            if (categoryName.trim().isEmpty()) {
                model.addAttribute("error", "Category name cannot be empty.");
                return "redirect:/admindash#addCategory";
            }
            Category category = new Category();
            category.setName(categoryName);
            categoryRepo.save(category);
            model.addAttribute("books", bookRepo.findAll());
            return "redirect:/admindash#addCategory";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred while adding the category Already Exist ");
            return "redirect:/errorpage";
        }
    }

    @PostMapping("/deleteBooks")
    public String deleteBook(@RequestParam Long bookId, Model model) {
        Optional<Book> book = bookRepo.findById(bookId);

        if (book.isPresent()) {
            boolean hasDownloads = downloadRepo.existsByBookId(bookId);
            if (hasDownloads) {
                Book existingBook = book.get();
                existingBook.setDeleted(true); // Soft delete
                bookRepo.save(existingBook);
                model.addAttribute("message", "Book removed from store.");
            } else {
                bookRepo.deleteById(bookId);
            }
            model.addAttribute("message", "Book removed from store but remains in cart and downloads.");
        } else {
            model.addAttribute("error", "Book not found.");
        }

        return "redirect:/admindash";
    }

    @PostMapping("/updateBooks/{bookId}")
    public String editBook(@PathVariable Long bookId, Model model) {
        Optional<Book> optionalBook = bookRepo.findById(bookId);
        if (optionalBook.isPresent()) {
            Book book = optionalBook.get();
            model.addAttribute("book", book);

            // Fetch categories to populate the dropdown
            List<Category> categories = categoryRepo.findAll();
            model.addAttribute("categories", categories);

            return "Admin/editBook"; // Return the editBook template
        } else {
            model.addAttribute("error", "Book ID not found.");
            return "Admin/admindash"; // Redirect to admin dashboard if not found
        }
    }

    @PostMapping("/update/{bookId}")
    public String updateBook(@PathVariable Long bookId,
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam Long category,
            @RequestParam String isbn,
            @RequestParam String description,
            @RequestParam Integer price,
            @RequestParam(required = false) MultipartFile file, // Optional PDF
            @RequestParam(required = false) MultipartFile coverImage, // Optional cover
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate publishDate,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String publishername,
            Model model) {

        Optional<Book> optionalBook = bookRepo.findById(bookId);

        if (optionalBook.isPresent()) {
            try {
                Book book = optionalBook.get();
                book.setTitle(title);
                book.setAuthor(author);

                // ✅ Update category if provided
                Optional<Category> categoryOpt = categoryRepo.findById(category);
                if (categoryOpt.isPresent()) {
                    book.setCategory(categoryOpt.get());
                } else {
                    model.addAttribute("error", "Invalid category ID.");
                    return "Admin/admindash";
                }
                book.setIsbn(isbn);
                book.setDescription(description);
                book.setPrice(price);
                book.setPublishDate(publishDate);
                book.setLanguage(language);
                book.setPublishername(publishername);
                // ✅ Update cover image only if a new one is uploaded
                if (coverImage != null && !coverImage.isEmpty()) {
                    book.setCoverImage(coverImage.getBytes());
                }

                // ✅ Update PDF file only if a new one is uploaded
                if (file != null && !file.isEmpty()) {
                    book.setFileData(file.getBytes());
                    book.setFileType(file.getContentType());
                }
                book.setDeleted(false);
                bookRepo.save(book);
                return "redirect:/admindash?success=Book updated successfully";

            } catch (IOException e) {
                model.addAttribute("error", "Error updating book: " + e.getMessage());
                return "errorpage";
            }
        } else {
            model.addAttribute("error", "Book ID not found.");
            return "Admin/admindash";
        }
    }

    @PostMapping("/delete-category")
    public String deleteCategory(@RequestParam Long categoryId, Model model) {
        Optional<Category> category = categoryRepo.findById(categoryId);
        if (category.isPresent()) {
            categoryRepo.delete(category.get());
            model.addAttribute("message", "Category deleted successfully.");
        } else {
            model.addAttribute("error", "Category not found.");
        }
        if (!category.get().getBooks().isEmpty()) {
            model.addAttribute("error", "Cannot delete category with books.");
            return "redirect:admindash";
        }
        return "redirect:/admindash#manageCategories";
    }

    @PostMapping("/delete-user")
    public String deleteUser(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            try {
                // Delete related records to respect foreign key constraints
                cartRepo.deleteAll(cartRepo.findByUser(user));
                downloadRepo.deleteAll(downloadRepo.findByUser(user));
                orderRepo.deleteAll(orderRepo.findByUser(user));
                userRepo.delete(user);
                redirectAttributes.addFlashAttribute("message",
                        "User dossier '" + user.getUsername() + "' removed successfully.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Could not delete user: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        }
        return "redirect:/admindash#users";
    }

    @PostMapping("/delete-inquiry")
    public String deleteInquiry(@RequestParam Long inquiryId, RedirectAttributes redirectAttributes) {
        try {
            inquiryRepo.deleteById(inquiryId);
            redirectAttributes.addFlashAttribute("message", "Inquiry removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not remove inquiry: " + e.getMessage());
        }
        return "redirect:/admindash#inquiries";
    }
}

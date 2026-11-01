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
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.CategoryRepo;
import com.example.demo.Repository.DownloadRepo;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.UserRepo;
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
public class Admincontroller 
{
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
    
    /*
     @GetMapping("/admin/report/pdf")
     
    public void exportUserReportToPDF(HttpServletResponse response) throws IOException {
    	try {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=user_report.pdf");

            List<User> users = userRepo.findAll(); // Make sure orders, cart, and downloads are fetched

            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // ✅ Add logo
            try {
                InputStream logoStream = getClass().getResourceAsStream("/static/images/logo.png");
                if (logoStream != null) {
                    Image logo = Image.getInstance(logoStream.readAllBytes());
                    logo.scaleAbsolute(100, 50);
                    logo.setAlignment(Image.ALIGN_LEFT);
                    document.add(logo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // ✅ Title
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("User Management Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // ✅ Table setup
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 10);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 4, 3, 2, 5, 2, 2, 2});

            String[] headers = {"Name", "Email", "Mobile", "Book Ordered", "Book Titles", "Books in Cart", "Downloaded Books", "Total Price"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(headerCell);
            }

            for (User user : users) {
                table.addCell(new Phrase(user.getUsername(), bodyFont));
                table.addCell(new Phrase(user.getEmail(), bodyFont));
                table.addCell(new Phrase(user.getMobileno(), bodyFont));
                table.addCell(new Phrase(String.valueOf(user.getOrders().size()), bodyFont));

                String bookTitles = user.getOrders().stream()
                        .map(order -> order.getBook().getTitle())
                        .collect(Collectors.joining(", "));
                table.addCell(new Phrase(bookTitles, bodyFont));

                table.addCell(new Phrase(String.valueOf(user.getCartItems().size()), bodyFont));
                table.addCell(new Phrase(String.valueOf(user.getDownloadedBooks().size()), bodyFont));

                double totalAmount = user.getOrders().stream()
                        .mapToDouble(order -> order.getAmount())
                        .sum();
                table.addCell(new Phrase(String.format("%.2f", totalAmount), bodyFont));
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export PDF", e);
        }
    }
    */

    

   
     /*
         @GetMapping("/admin/report/pdf")
      
        public void exportUserReportToPDF(HttpServletResponse response) throws IOException {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=users_report.pdf");

            List<User> users = userRepo.getAllUsers(); // Fetch your data

            Document document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try {
                PdfWriter.getInstance(document, out);
                document.open();

                // Title
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
                Paragraph title = new Paragraph("User Report", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);
                document.add(Chunk.NEWLINE);

                // Table setup
                PdfPTable table = new PdfPTable(8);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2, 3, 3, 2, 4, 2, 2, 2});

                // Headers
                Stream.of("Name", "Email", "Mobile", "Books Ordered", "Book Titles", "Books in Cart", "Downloaded", "Total Price")
                        .forEach(header -> {
                            PdfPCell cell = new PdfPCell(new Phrase(header));
                            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                            table.addCell(cell);
                        });

                // Data rows
                for (User user : users) {
                    table.addCell(user.getUsername());
                    table.addCell(user.getEmail());
                    table.addCell(user.getMobileno());
                    table.addCell(String.valueOf(user.getOrders().size()));

                    String bookTitles = user.getOrders().stream()
                            .map(order -> order.getBook().getTitle())
                            .collect(Collectors.joining(", "));
                    table.addCell(bookTitles);

                    table.addCell(String.valueOf(user.getCartItems().size()));
                    table.addCell(String.valueOf(user.getDownloadedBooks().size()));

                    double total = user.getOrders().stream().mapToDouble(Order::getAmount).sum();
                    table.addCell(String.format("%.2f", total));
                }

                document.add(table);
                document.close();

                ByteArrayInputStream pdfInput = new ByteArrayInputStream(out.toByteArray());
                IOUtils.copy(pdfInput, response.getOutputStream());
            } catch (DocumentException e) {
                throw new IOException("Error generating PDF", e);
            }
        }


    */
    
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
        return "admin/books";
    }
    
    
    
    
    
    
    
    
    @GetMapping("/books/report/pdf")
    public void generateBooksPdfReport(HttpServletResponse response) throws IOException {
        // Set response headers
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=booksreport.pdf");

        // Get all books with statistics
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
            float[] columnWidths = {3, 3, 2, 3, 2, 2, 2}; // Removed first column width
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
    public String adminDashboard(Model model) 
    { 
    	
    	model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("books", bookRepo.findAll());
        model.addAttribute("users",userRepo.findAll());
        return "Admin/admindash";
    }
  
    @GetMapping("/addBooks")
    public String showAddBookForm(Model model)
    {
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

            // ✅ Store book details in the database
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
        } 
        catch (IOException e) {
            model.addAttribute("errorMessage", "Error saving book: " + e.getMessage());
            return "errorpage";
        }
        return "redirect:/admindash";
    }

    @GetMapping("/displayBooks")
    public String displayBooks(Model model)
    {
        List<Book> books = bookRepo.findAll();
        model.addAttribute("books", books);
        return "redirect:/admindash#displaybook"; // Ensure this template exists
    }    
    @PostMapping("/add-category")
    public String addCategory(@RequestParam String categoryName, Model model)
    {
        try 
        {
            if (categoryName.trim().isEmpty()) 
            {
                model.addAttribute("error", "Category name cannot be empty.");
                return "redirect:/admindash#addCategory";
            }
            Category category = new Category();
            category.setName(categoryName);
            categoryRepo.save(category);
            model.addAttribute("books", bookRepo.findAll());
            return "redirect:/admindash#addCategory";
        }
        catch (Exception e) 
        {
        	model.addAttribute("errorMessage", "An error occurred while adding the category Already Exist "  );
            return "redirect:/errorpage";
        }
    }
    @PostMapping("/deleteBooks")
    public String deleteBook(@RequestParam Long bookId, Model model) {
        Optional<Book> book = bookRepo.findById(bookId);
        
        if (book.isPresent())
        {
        	boolean hasDownloads = downloadRepo.existsByBookId(bookId);
        	if (hasDownloads) 
        	{
        		 Book existingBook = book.get();
                 existingBook.setDeleted(true); // Soft delete
                 bookRepo.save(existingBook);
                 model.addAttribute("message", "Book removed from store.");
            } 
        	else
        	{
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
                             @RequestParam (required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate publishDate,
                             @RequestParam (required = false) String language,
                             @RequestParam (required = false) String publishername,
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
}

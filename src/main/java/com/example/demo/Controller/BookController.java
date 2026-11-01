package com.example.demo.Controller;

import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.example.demo.Entity.Book;
import com.example.demo.service.BookService;

@Controller
@RequestMapping("/books")
public class BookController 
{ 
    
    @Autowired
    private BookService bookService;
    
    
     
    @GetMapping("/view/{id}")
    public String getBookDetails(@PathVariable Long id, Model model) {
        Optional<Book> optionalBook = bookService.findById(id);

        if (optionalBook.isEmpty()) {
            model.addAttribute("errorMessage", "Book not found.");
            return "errorpage"; // Ensure "errorpage.html" exists
        }

        Book book = optionalBook.get();
        
        // ✅ Convert cover image from @Lob field to Base64
        String base64Image = "";
        if (book.getCoverImage() != null) {
            base64Image = Base64.getEncoder().encodeToString(book.getCoverImage());
        }

        model.addAttribute("book", book);
        model.addAttribute("bookImage", "data:image/jpeg;base64," + base64Image);

        return "viewbook"; // Ensure "viewbook.html" exists in the templates folder
    }
}

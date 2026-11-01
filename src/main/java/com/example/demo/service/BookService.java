package com.example.demo.service;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.demo.Entity.Book;
import com.example.demo.Repository.BookRepo;
import com.itextpdf.io.exceptions.IOException;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.pdf.EncryptionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;


@Service
public class BookService 
{
	@Autowired
	private BookRepo bookRepo;
	
	 public List<Book> getAllBooks() {
	        return bookRepo.findAll();
	    }

	 public Optional<Book> getBookById(Long id) {
	        return bookRepo.findById(id);
	    }
	    
	    public void deleteBook(Long id) {
	        bookRepo.deleteById(id);
	    }
	
		public Optional<Book> findById(Long id) 
		{
			return bookRepo.findById(id);
		}
		
		
	
		public ResponseEntity<byte[]> getPdfPreview(Long id, int maxPages) throws Exception {
	        Optional<Book> bookOptional = bookRepo.findById(id);

	        if (bookOptional.isPresent()) {
	            Book book = bookOptional.get();
	           
	            byte[] fullPdfData = book.getFileData();

	            try {
	                byte[] previewPdf = extractFirstPages(fullPdfData, maxPages); // Extract first 5 pages
	                return ResponseEntity.ok()
	                        .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
	                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
	                        .body(previewPdf);
	            } catch (IOException e) {
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
	            }
	        } else {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
	        }
	    }

	    private byte[] extractFirstPages(byte[] fullPdfData, int maxPages) throws Exception {
	        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(fullPdfData));
	             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

	            PDDocument newDocument = new PDDocument();
	            int totalPages = document.getNumberOfPages();

	            int pagesToExtract = Math.min(totalPages, maxPages);

	            for (int i = 0; i < pagesToExtract; i++) {
	                PDPage page = document.getPage(i);
	                newDocument.addPage(page);
	            }

	            newDocument.save(outputStream);
	            newDocument.close();

	            return outputStream.toByteArray();
	        }
	    }
}
	


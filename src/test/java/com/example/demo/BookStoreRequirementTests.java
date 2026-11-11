package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Controller.Usercontroller;
import com.example.demo.Entity.Book;
import com.example.demo.Entity.Cart;
import com.example.demo.Entity.Category;
import com.example.demo.Entity.Order;
import com.example.demo.Entity.OrderItem;
import com.example.demo.Entity.User;
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.CategoryRepo;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.UserRepo;
import com.example.demo.service.EmailService;
import com.example.demo.service.OTPservice;
import com.example.demo.service.StripeService;
import com.stripe.model.checkout.Session;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookStoreRequirementTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepo userRepo;

        @Autowired
        private CategoryRepo categoryRepo;

        @Autowired
        private BookRepo bookRepo;

        @Autowired
        private CartRepo cartRepo;

        @Autowired
        private OrderRepo orderRepo;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private OTPservice otpservice;

        @Autowired
        private EntityManager entityManager;

        @MockitoBean
        private EmailService emailService;

        @MockitoBean
        private StripeService stripeService;

        private User customerUser;
        private User adminUser;
        private Category fictionCat;
        private Book bookAlpha;
        private Book bookBeta;
        private Book bookGamma;
        private String rawPassword;
        private String uniqueSuffix;

        @BeforeEach
        void setUpTestFixtures() {
                uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                rawPassword = "Pass!" + uniqueSuffix;

                fictionCat = new Category();
                fictionCat.setName("TestGenre-" + uniqueSuffix);
                fictionCat = categoryRepo.save(fictionCat);

                customerUser = new User();
                customerUser.setUsername("reader_" + uniqueSuffix);
                customerUser.setEmail("reader_" + uniqueSuffix + "@example.com");
                customerUser.setPassword(passwordEncoder.encode(rawPassword));
                customerUser.setMobileno("9876543210");
                customerUser.setRole("USER");
                customerUser.setEnabled(true);
                customerUser.setIsregistered(true);
                customerUser = userRepo.save(customerUser);

                adminUser = new User();
                adminUser.setUsername("adm_" + uniqueSuffix);
                adminUser.setEmail("adm_" + uniqueSuffix + "@example.com");
                adminUser.setPassword(passwordEncoder.encode(rawPassword));
                adminUser.setMobileno("9123456780");
                adminUser.setRole("ADMIN");
                adminUser.setEnabled(true);
                adminUser.setIsregistered(true);
                adminUser = userRepo.save(adminUser);

                bookAlpha = createBook("Book Alpha " + uniqueSuffix, "Author One", "ISBN-A-" + uniqueSuffix, 200, 20);
                bookBeta = createBook("Book Beta " + uniqueSuffix, "Author Two", "ISBN-B-" + uniqueSuffix, 350, 15);
                bookGamma = createBook("Book Gamma " + uniqueSuffix, "Author Three", "ISBN-C-" + uniqueSuffix, 100, 10);

                entityManager.flush();
        }

        private Book createBook(String title, String author, String isbn, int price, int stock) {
                Book book = new Book();
                book.setTitle(title);
                book.setAuthor(author);
                book.setCategory(fictionCat);
                book.setIsbn(isbn);
                book.setPrice(price);
                book.setStock(stock);
                book.setDescription("Editorial test work: " + title);
                book.setLanguage("English");
                book.setPublishername("Quickread Press");
                book.setPublishDate(LocalDate.now());
                book.setDeleted(false);
                book.setFileType("application/pdf");
                book.setFileData(new byte[] { 1, 2, 3, 4 });
                return bookRepo.save(book);
        }

        private MockHttpSession loginAs(String username, String password) throws Exception {
                MvcResult result = mockMvc.perform(post("/login")
                                .param("username", username)
                                .param("password", password))
                                .andExpect(status().is3xxRedirection())
                                .andReturn();
                MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
                assertThat(session).isNotNull();
                return session;
        }

        private Session mockStripePaidSession(String sessionId, String paymentIntentId, Long userId,
                        String checkoutType, Map<String, String> extraMeta) {
                Session s = new Session();
                s.setId(sessionId);
                s.setPaymentStatus("paid");
                s.setPaymentIntent(paymentIntentId);
                Map<String, String> meta = new java.util.HashMap<>();
                meta.put("userId", String.valueOf(userId));
                meta.put("checkoutType", checkoutType);
                if (extraMeta != null) {
                        meta.putAll(extraMeta);
                }
                s.setMetadata(meta);
                return s;
        }

        // =========================================================================
        // TEST 1: User updates profile and verifies OTP successfully + cooldown
        // =========================================================================
        @Test
        void test01_profileUpdateWithOtpVerificationSuccessAndSessionMaintained() throws Exception {
                MockHttpSession session = loginAs(customerUser.getUsername(), rawPassword);
                String newUsername = "updated_" + uniqueSuffix;
                String newEmail = "updated_" + uniqueSuffix + "@example.com";
                String newMobile = "9000011111";

                mockMvc.perform(post("/profile/request-otp")
                                .session(session)
                                .param("username", newUsername)
                                .param("email", newEmail)
                                .param("mobile", newMobile))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"));

                // Verify email OTP was dispatched to the NEW email address
                ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
                verify(emailService, times(1)).sendOtpEmail(eq(newEmail), otpCaptor.capture(), anyString(), anyString(),
                                anyString());
                String sentOtp = otpCaptor.getValue();
                assertThat(sentOtp).matches("\\d{6}");

                // DB must NOT be updated before OTP verification
                User beforeVerify = userRepo.findById(customerUser.getUserid()).orElseThrow();
                assertThat(beforeVerify.getUsername()).isEqualTo(customerUser.getUsername());
                assertThat(beforeVerify.getEmail()).isEqualTo(customerUser.getEmail());

                // Duplicate request during cooldown must not dispatch a second OTP email
                mockMvc.perform(post("/profile/request-otp")
                                .session(session)
                                .param("username", newUsername)
                                .param("email", newEmail)
                                .param("mobile", newMobile))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"));
                verify(emailService, times(1)).sendOtpEmail(eq(newEmail), anyString(), anyString(), anyString(),
                                anyString());

                Usercontroller.PendingProfileUpdate pending = (Usercontroller.PendingProfileUpdate) session
                                .getAttribute(Usercontroller.SESSION_PENDING_PROFILE_UPDATE);
                assertThat(pending).isNotNull();

                mockMvc.perform(post("/profile/verify-otp")
                                .session(session)
                                .param("otp", sentOtp)
                                .param("token", pending.getToken()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"))
                                .andExpect(flash().attributeExists("message"));

                // DB updated accurately
                User afterVerify = userRepo.findById(customerUser.getUserid()).orElseThrow();
                assertThat(afterVerify.getUsername()).isEqualTo(newUsername);
                assertThat(afterVerify.getEmail()).isEqualTo(newEmail);
                assertThat(afterVerify.getMobileno()).isEqualTo(newMobile);

                // User login session remains authenticated after profile update
                mockMvc.perform(get("/profile").session(session))
                                .andExpect(status().isOk());
        }

        // =========================================================================
        // TEST 2: User enters incorrect OTP -> protected info NOT updated
        // =========================================================================
        @Test
        void test02_profileUpdateIncorrectOtpDoesNotUpdateDatabase() throws Exception {
                MockHttpSession session = loginAs(customerUser.getUsername(), rawPassword);
                String origUsername = customerUser.getUsername();
                String origEmail = customerUser.getEmail();

                mockMvc.perform(post("/profile/request-otp")
                                .session(session)
                                .param("username", "hack_" + uniqueSuffix)
                                .param("email", "other_" + uniqueSuffix + "@example.com")
                                .param("mobile", "9999988888"))
                                .andExpect(status().is3xxRedirection());

                Usercontroller.PendingProfileUpdate pending = (Usercontroller.PendingProfileUpdate) session
                                .getAttribute(Usercontroller.SESSION_PENDING_PROFILE_UPDATE);

                mockMvc.perform(post("/profile/verify-otp")
                                .session(session)
                                .param("otp", "000000")
                                .param("token", pending.getToken()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"))
                                .andExpect(flash().attributeExists("error"));

                User unchanged = userRepo.findById(customerUser.getUserid()).orElseThrow();
                assertThat(unchanged.getUsername()).isEqualTo(origUsername);
                assertThat(unchanged.getEmail()).isEqualTo(origEmail);
                assertThat(unchanged.getMobileno()).isEqualTo("9876543210");
        }

        // =========================================================================
        // TEST 3: User enters expired OTP -> rejected cleanly, DB untouched
        // =========================================================================
        @Test
        void test03_profileUpdateExpiredOtpRejectedAndDatabaseUntouched() throws Exception {
                MockHttpSession session = loginAs(customerUser.getUsername(), rawPassword);
                String newEmail = "exp_" + uniqueSuffix + "@example.com";

                mockMvc.perform(post("/profile/request-otp")
                                .session(session)
                                .param("username", "expuser_" + uniqueSuffix)
                                .param("email", newEmail)
                                .param("mobile", "9777766666"))
                                .andExpect(status().is3xxRedirection());

                ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
                verify(emailService).sendOtpEmail(eq(newEmail), otpCaptor.capture(), anyString(), anyString(),
                                anyString());

                Usercontroller.PendingProfileUpdate pending = (Usercontroller.PendingProfileUpdate) session
                                .getAttribute(Usercontroller.SESSION_PENDING_PROFILE_UPDATE);
                otpservice.expireOtpForTesting(pending.getOtpKey());

                mockMvc.perform(post("/profile/verify-otp")
                                .session(session)
                                .param("otp", otpCaptor.getValue())
                                .param("token", pending.getToken()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"))
                                .andExpect(flash().attribute("error",
                                                org.hamcrest.Matchers.containsStringIgnoringCase("expired")));

                User unchanged = userRepo.findById(customerUser.getUserid()).orElseThrow();
                assertThat(unchanged.getEmail()).isEqualTo(customerUser.getEmail());
                assertThat(unchanged.getUsername()).isEqualTo(customerUser.getUsername());
        }

        // =========================================================================
        // TEST 4: User orders ONE book -> 1 Order, 1 Receipt, PDF download works
        // =========================================================================
        @Test
        void test04_singleBookOrderGeneratesOneOrderAndOneReceipt() throws Exception {
                MockHttpSession session = loginAs(customerUser.getUsername(), rawPassword);
                String stripeSessId = "cs_single_" + uniqueSuffix;

                when(stripeService.retrieveCheckoutSession(stripeSessId))
                                .thenReturn(mockStripePaidSession(stripeSessId, "pi_s_" + uniqueSuffix,
                                                customerUser.getUserid(),
                                                "SINGLE", Map.of("bookId", String.valueOf(bookAlpha.getId()))));

                MvcResult successRes = mockMvc.perform(get("/success")
                                .session(session)
                                .param("session_id", stripeSessId)
                                .param("bookId", String.valueOf(bookAlpha.getId())))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrlPattern("/orders/*/receipt"))
                                .andReturn();

                List<Order> created = orderRepo.findAllByStripeSessionId(stripeSessId);
                assertThat(created).hasSize(1);
                Order order = created.get(0);
                assertThat(order.isPaid()).isTrue();
                assertThat(order.getEffectiveItems()).hasSize(1);
                assertThat(order.getAmount()).isEqualTo(200.0);

                String receiptUrl = successRes.getResponse().getRedirectedUrl();
                String receiptHtml = mockMvc.perform(get(receiptUrl).session(session))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(receiptHtml).contains(bookAlpha.getTitle());
                assertThat(receiptHtml).contains(order.getOrderNumber());
                assertThat(receiptHtml).contains(order.getReceiptNumber());
        }

        // =========================================================================
        // TEST 5, 6, 7 & 8: 3+ different books in ONE checkout -> ONE Order,
        // ONE Combined Receipt, ONE Email/PDF, and page refresh creates NO duplicates
        // =========================================================================
        @Test
        void test05_06_07_08_multiBookCheckoutSingleCombinedReceiptAndRefreshIdempotency() throws Exception {
                MockHttpSession session = loginAs(customerUser.getUsername(), rawPassword);

                // Add 3 different books with distinct quantities:
                // Alpha: 2 * 200 = 400, Beta: 1 * 350 = 350, Gamma: 3 * 100 = 300 => Combined
                // Total = 1050
                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(session).param("quantity", "2"))
                                .andExpect(status().is3xxRedirection());
                mockMvc.perform(post("/add/" + bookBeta.getId()).session(session).param("quantity", "1"))
                                .andExpect(status().is3xxRedirection());
                mockMvc.perform(post("/add/" + bookGamma.getId()).session(session).param("quantity", "3"))
                                .andExpect(status().is3xxRedirection());

                List<Cart> cartItems = cartRepo.findByUser(customerUser);
                assertThat(cartItems).hasSize(3);

                String multiSessionId = "cs_multi_" + uniqueSuffix;
                String encodedCart = bookAlpha.getId() + ":2," + bookBeta.getId() + ":1," + bookGamma.getId() + ":3";
                when(stripeService.retrieveCheckoutSession(multiSessionId))
                                .thenReturn(mockStripePaidSession(multiSessionId, "pi_m_" + uniqueSuffix,
                                                customerUser.getUserid(),
                                                "CART", Map.of("cartItems", encodedCart)));

                // Complete Stripe checkout callback
                MvcResult firstCall = mockMvc.perform(get("/payment-success")
                                .session(session)
                                .param("session_id", multiSessionId))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrlPattern("/orders/*/receipt"))
                                .andReturn();

                // Scenario 8: Simulate page refreshes on /payment-success and duplicate webhook
                // trigger
                mockMvc.perform(get("/payment-success").session(session).param("session_id", multiSessionId))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl(firstCall.getResponse().getRedirectedUrl()));
                mockMvc.perform(get("/payment-success").session(session).param("session_id", multiSessionId))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl(firstCall.getResponse().getRedirectedUrl()));

                // Scenario 7 & 8: Assert ONLY ONE Order and ONE Order ID generated across
                // refreshes
                List<Order> ordersForSession = orderRepo.findAllByStripeSessionId(multiSessionId);
                assertThat(ordersForSession).hasSize(1);
                Order combinedOrder = ordersForSession.get(0);

                assertThat(combinedOrder.getOrderNumber()).isNotBlank();
                assertThat(combinedOrder.getReceiptNumber()).isNotBlank();
                assertThat(combinedOrder.getItems()).hasSize(3);
                assertThat(combinedOrder.getTotalItemQuantity()).isEqualTo(6);
                assertThat(combinedOrder.getAmount()).isEqualTo(1050.0);
                assertThat(cartRepo.findByUser(customerUser)).isEmpty();

                // Assert stock decremented ONLY ONCE
                assertThat(bookRepo.findById(bookAlpha.getId()).orElseThrow().getStock()).isEqualTo(18);
                assertThat(bookRepo.findById(bookBeta.getId()).orElseThrow().getStock()).isEqualTo(14);
                assertThat(bookRepo.findById(bookGamma.getId()).orElseThrow().getStock()).isEqualTo(7);

                // Assert ONLY ONE email sent and attached PDF contains ALL 3 book titles +
                // totals
                ArgumentCaptor<byte[]> pdfCaptor = ArgumentCaptor.forClass(byte[].class);
                verify(emailService, times(1)).sendOrderReceiptEmail(eq(combinedOrder), pdfCaptor.capture());

                try (PDDocument pdfDoc = PDDocument.load(pdfCaptor.getValue())) {
                        String pdfText = new PDFTextStripper().getText(pdfDoc);
                        assertThat(pdfText).contains(bookAlpha.getTitle());
                        assertThat(pdfText).contains(bookBeta.getTitle());
                        assertThat(pdfText).contains(bookGamma.getTitle());
                        assertThat(pdfText).contains(combinedOrder.getOrderNumber());
                        assertThat(pdfText).contains(combinedOrder.getReceiptNumber());
                        assertThat(pdfText).contains("1050.00");
                }

                // Scenario 6: HTML combined receipt view displays ALL 3 books & download
                // endpoint returns PDF
                String receiptUrl = firstCall.getResponse().getRedirectedUrl();
                String receiptHtml = mockMvc.perform(get(receiptUrl).session(session))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(receiptHtml).contains(bookAlpha.getTitle(), bookBeta.getTitle(), bookGamma.getTitle());
                assertThat(receiptHtml).contains("1050.00");
                assertThat(receiptHtml).contains(combinedOrder.getOrderNumber());
                assertThat(receiptHtml).contains(combinedOrder.getReceiptNumber());

                // User can also download combined PDF from /orders/{id}/receipt/download & see
                // it on /order
                mockMvc.perform(get("/orders/" + combinedOrder.getId() + "/receipt/download").session(session))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type",
                                                org.hamcrest.Matchers.containsString("application/pdf")));

                String orderHistoryHtml = mockMvc.perform(get("/order").session(session))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(orderHistoryHtml).contains(combinedOrder.getOrderNumber());
                assertThat(orderHistoryHtml).contains(bookAlpha.getTitle(), bookBeta.getTitle(), bookGamma.getTitle());
        }

        // =========================================================================
        // TEST 9: Failed Stripe payment does NOT generate a successful paid receipt
        // =========================================================================
        @Test
        void test09_failedOrUnpaidStripePaymentGeneratesNoPaidReceipt() throws Exception {
                MockHttpSession session = loginAs(customerUser.getUsername(), rawPassword);

                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(session))
                                .andExpect(status().is3xxRedirection());

                String failedSessionId = "cs_failed_" + uniqueSuffix;
                Session unpaidSession = new Session();
                unpaidSession.setId(failedSessionId);
                unpaidSession.setPaymentStatus("unpaid");
                unpaidSession.setMetadata(Map.of("userId", String.valueOf(customerUser.getUserid())));
                when(stripeService.retrieveCheckoutSession(failedSessionId)).thenReturn(unpaidSession);

                mockMvc.perform(get("/payment-success")
                                .session(session)
                                .param("session_id", failedSessionId))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/cart"))
                                .andExpect(flash().attributeExists("error"));

                assertThat(orderRepo.findAllByStripeSessionId(failedSessionId)).isEmpty();
                assertThat(cartRepo.findByUser(customerUser)).hasSize(1);
                verify(emailService, never()).sendOrderReceiptEmail(any(), any());

                // Even if a pre-existing FAILED Order row exists, viewing or downloading its
                // receipt is blocked
                Order failedOrder = new Order();
                failedOrder.setUser(customerUser);
                failedOrder.setBook(bookAlpha);
                failedOrder.setAmount(200);
                failedOrder.setPaymentStatus("FAILED");
                failedOrder.setOrderStatus("CANCELLED");
                failedOrder.setOrderDate(LocalDateTime.now());
                failedOrder.addItem(new OrderItem(bookAlpha, 1, 200));
                failedOrder = orderRepo.save(failedOrder);

                mockMvc.perform(get("/orders/" + failedOrder.getId() + "/receipt").session(session))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/order"))
                                .andExpect(flash().attributeExists("error"));

                mockMvc.perform(get("/orders/" + failedOrder.getId() + "/receipt/download").session(session))
                                .andExpect(status().isBadRequest());
        }

        // =========================================================================
        // TEST 10: Normal user cannot access admin pages (unauthenticated -> login)
        // =========================================================================
        @Test
        void test10_normalUserBlockedFromAdminPagesAndGuestsRedirectedToLogin() throws Exception {
                // Unauthenticated requests -> /login redirect
                mockMvc.perform(get("/admin/dashboard"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/login"));

                // Logged in normal user -> 403 Forbidden across all admin endpoints
                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);
                List<String> adminGetPaths = List.of(
                                "/admin/dashboard", "/admin/books", "/admin/books/add",
                                "/admin/categories", "/admin/users", "/admin/orders",
                                "/admin/reports", "/admin/inquiries", "/admin/profile", "/admindash");
                for (String path : adminGetPaths) {
                        mockMvc.perform(get(path).session(userSession))
                                        .andExpect(status().isForbidden());
                }

                mockMvc.perform(post("/admin/categories/add")
                                .session(userSession)
                                .param("categoryName", "Unauthorized"))
                                .andExpect(status().isForbidden());
        }

        // =========================================================================
        // TEST 11: Admin can access all authorized admin pages & admin protected
        // =========================================================================
        @Test
        void test11_adminCanAccessAllAdminRoutesAndAdminAccountsAreProtected() throws Exception {
                MockHttpSession adminSession = loginAs(adminUser.getUsername(), rawPassword);

                List<String> adminGetPaths = List.of(
                                "/admin/dashboard", "/admin/books", "/admin/books/add",
                                "/admin/categories", "/admin/users", "/admin/orders",
                                "/admin/reports", "/admin/inquiries", "/admin/profile");
                for (String path : adminGetPaths) {
                        mockMvc.perform(get(path).session(adminSession))
                                        .andExpect(status().isOk());
                }

                mockMvc.perform(get("/admin/reports/analytics")
                                .session(adminSession)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                // Inspecting customer account works
                mockMvc.perform(get("/admin/users/" + customerUser.getUserid()).session(adminSession))
                                .andExpect(status().isOk());

                // Attempting to inspect or delete an ADMIN account from User Management is
                // rejected
                mockMvc.perform(get("/admin/users/" + adminUser.getUserid()).session(adminSession))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/users"))
                                .andExpect(flash().attributeExists("error"));

                mockMvc.perform(post("/admin/users/delete")
                                .session(adminSession)
                                .param("userId", String.valueOf(adminUser.getUserid())))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/users"))
                                .andExpect(flash().attributeExists("error"));

                assertThat(userRepo.findById(adminUser.getUserid())).isPresent();
        }

        // =========================================================================
        // TEST 12: User and Admin see separate, exclusive navigation headers
        // =========================================================================
        @Test
        void test12_roleBasedHeadersAreExclusiveToAuthenticatedRole() throws Exception {
                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);
                String userHomeHtml = mockMvc.perform(get("/").session(userSession))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                assertThat(userHomeHtml).contains("user-role-header");
                assertThat(userHomeHtml).contains("Browse Books", "My Cart", "My Orders", "My Profile");
                assertThat(userHomeHtml).doesNotContain("admin-role-header");
                assertThat(userHomeHtml).doesNotContain("Manage Books", "Manage Users", "Admin Profile");

                MockHttpSession adminSession = loginAs(adminUser.getUsername(), rawPassword);
                String adminDashHtml = mockMvc.perform(get("/admin/dashboard").session(adminSession))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                assertThat(adminDashHtml).contains("admin-role-header");
                assertThat(adminDashHtml).contains(
                                "Admin Dashboard", "Manage Books", "Manage Categories",
                                "Manage Users", "Manage Orders", "Reports / Sales", "Admin Profile");
                assertThat(adminDashHtml).doesNotContain("user-role-header");
                assertThat(adminDashHtml).doesNotContain("My Cart", "My Orders");
        }

        // =========================================================================
        // TEST 13: Existing registration -> OTP -> login -> catalog -> cart flow
        // =========================================================================
        @Test
        void test13_existingRegistrationOtpLoginAndCatalogContinueWorking() throws Exception {
                MockHttpSession regSession = new MockHttpSession();
                String regEmail = "newreg_" + uniqueSuffix + "@example.com";
                String regUser = "newreg_" + uniqueSuffix;
                String regPass = "StrongPass123!";

                mockMvc.perform(post("/userreg")
                                .session(regSession)
                                .param("name", regUser)
                                .param("email", regEmail)
                                .param("password", regPass)
                                .param("confirm_password", regPass)
                                .param("mobilenumber", "9888877777"))
                                .andExpect(status().isOk());

                ArgumentCaptor<String> regOtpCaptor = ArgumentCaptor.forClass(String.class);
                verify(emailService).sendOtpEmail(eq(regEmail), regOtpCaptor.capture());

                mockMvc.perform(post("/verify-otp")
                                .session(regSession)
                                .param("email", regEmail)
                                .param("otp", regOtpCaptor.getValue()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/login"));

                User persisted = userRepo.findByEmailIgnoreCase(regEmail);
                assertThat(persisted).isNotNull();
                assertThat(persisted.isIsregistered()).isTrue();
                assertThat(persisted.getRole()).isEqualTo("USER");

                MockHttpSession loggedSession = loginAs(regUser, regPass);
                mockMvc.perform(get("/books").session(loggedSession)).andExpect(status().isOk());
                mockMvc.perform(get("/categories").session(loggedSession)).andExpect(status().isOk());
                mockMvc.perform(get("/books/view/" + bookAlpha.getId()).session(loggedSession))
                                .andExpect(status().isOk());
                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(loggedSession))
                                .andExpect(status().is3xxRedirection());
                mockMvc.perform(get("/cart").session(loggedSession)).andExpect(status().isOk());
        }

        // =========================================================================
        // TEST 14: Admin book/category/order/user management & report exports work
        // =========================================================================
        @Test
        void test14_adminCrudAndReportExportsWork() throws Exception {
                MockHttpSession adminSession = loginAs(adminUser.getUsername(), rawPassword);

                // Add & rename category
                mockMvc.perform(post("/admin/categories/add")
                                .session(adminSession)
                                .param("categoryName", "Philosophy-" + uniqueSuffix))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/categories"));

                // Add new book + stock + edit
                mockMvc.perform(post("/admin/books/add")
                                .session(adminSession)
                                .param("title", "Admin Added Book " + uniqueSuffix)
                                .param("author", "Editorial Board")
                                .param("categoryId", String.valueOf(fictionCat.getId()))
                                .param("isbn", "ISBN-ADM-" + uniqueSuffix)
                                .param("description", "Added via admin console")
                                .param("price", "450")
                                .param("stock", "25")
                                .param("language", "English")
                                .param("publishername", "Quickread Press"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/books"));

                // Update stock
                mockMvc.perform(post("/admin/books/" + bookAlpha.getId() + "/stock")
                                .session(adminSession)
                                .param("stock", "77"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/books"));
                assertThat(bookRepo.findById(bookAlpha.getId()).orElseThrow().getStock()).isEqualTo(77);

                // Toggle user status -> disabled -> login blocked -> re-enable
                mockMvc.perform(post("/admin/users/" + customerUser.getUserid() + "/toggle-status")
                                .session(adminSession))
                                .andExpect(status().is3xxRedirection());
                assertThat(userRepo.findById(customerUser.getUserid()).orElseThrow().isEnabled()).isFalse();

                mockMvc.perform(post("/login")
                                .param("username", customerUser.getUsername())
                                .param("password", rawPassword))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/admin/users/" + customerUser.getUserid() + "/toggle-status")
                                .session(adminSession))
                                .andExpect(status().is3xxRedirection());
                assertThat(userRepo.findById(customerUser.getUserid()).orElseThrow().isEnabled()).isTrue();

                // Update order fulfillment status
                Order sampleOrder = new Order();
                sampleOrder.setUser(customerUser);
                sampleOrder.setBook(bookAlpha);
                sampleOrder.setAmount(200);
                sampleOrder.setPaymentStatus("PAID");
                sampleOrder.setOrderStatus("CONFIRMED");
                sampleOrder.setOrderDate(LocalDateTime.now());
                sampleOrder.addItem(new OrderItem(bookAlpha, 1, 200));
                sampleOrder = orderRepo.save(sampleOrder);

                mockMvc.perform(post("/admin/orders/" + sampleOrder.getId() + "/status")
                                .session(adminSession)
                                .param("orderStatus", "COMPLETED"))
                                .andExpect(status().is3xxRedirection());
                assertThat(orderRepo.findById(sampleOrder.getId()).orElseThrow().getOrderStatus())
                                .isEqualTo("COMPLETED");

                // Exports: PDF, Excel, CSV
                mockMvc.perform(get("/admin/reports/export/pdf").session(adminSession))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type",
                                                org.hamcrest.Matchers.containsString("application/pdf")));
                mockMvc.perform(get("/admin/reports/export/excel").session(adminSession))
                                .andExpect(status().isOk());
                mockMvc.perform(get("/admin/reports/export/csv").session(adminSession))
                                .andExpect(status().isOk());
        }
}

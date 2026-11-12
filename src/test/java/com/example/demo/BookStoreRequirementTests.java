package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.example.demo.Entity.DeliveryConfig;
import com.example.demo.Entity.DeliveryMethod;
import com.example.demo.Entity.Order;
import com.example.demo.Entity.OrderItem;
import com.example.demo.Entity.PendingCheckout;
import com.example.demo.Entity.StoreLocation;
import com.example.demo.Entity.User;
import com.example.demo.Entity.UserAddress;
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.CartRepo;
import com.example.demo.Repository.CategoryRepo;
import com.example.demo.Repository.DeliveryConfigRepo;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.PendingCheckoutRepo;
import com.example.demo.Repository.StoreLocationRepo;
import com.example.demo.Repository.UserAddressRepo;
import com.example.demo.Repository.UserRepo;
import com.example.demo.service.EmailService;
import com.example.demo.service.OTPservice;
import com.example.demo.service.StoreLocationService;
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
        private UserAddressRepo userAddressRepo;

        @Autowired
        private StoreLocationRepo storeLocationRepo;

        @Autowired
        private DeliveryConfigRepo deliveryConfigRepo;

        @Autowired
        private PendingCheckoutRepo pendingCheckoutRepo;

        @Autowired
        private StoreLocationService storeLocationService;

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

        // =========================================================================
        // DELIVERY & PHYSICAL STORE PICKUP HELPERS
        // =========================================================================

        private UserAddress createSavedAddress(User owner, String fullName, String mobile,
                        String state, String city, String pinCode, String type, boolean isDefault) {
                UserAddress addr = new UserAddress();
                addr.setUser(owner);
                addr.setFullName(fullName);
                addr.setMobileNumber(mobile);
                addr.setHouseNo("A-101");
                addr.setBuildingName("Patel Chambers");
                addr.setStreetArea("C.G. Road, Navrangpura");
                addr.setLandmark("Near Municipal Market");
                addr.setCountry("India");
                addr.setState(state);
                addr.setCity(city);
                addr.setPinCode(pinCode);
                addr.setAddressType(type);
                addr.setDefaultAddress(isDefault);
                return userAddressRepo.save(addr);
        }

        private StoreLocation createStore(String name, String state, String city, String pinCode, boolean active) {
                StoreLocation store = new StoreLocation();
                store.setStoreName(name);
                store.setContactNumber("9898012345");
                store.setCountry("India");
                store.setState(state);
                store.setCity(city);
                store.setAddress("Shop 12, Heritage Book Arcade");
                store.setPinCode(pinCode);
                store.setOpeningHours("10:00 AM - 8:30 PM (Mon-Sun)");
                store.setActive(active);
                return storeLocationRepo.save(store);
        }

        // =========================================================================
        // TEST 15: Home Delivery order with saved address, fee config & immutable snapshot
        // =========================================================================
        @Test
        void test15_homeDeliveryOrderSavedAddressChargesAndImmutableSnapshot() throws Exception {
                MockHttpSession adminSession = loginAs(adminUser.getUsername(), rawPassword);
                mockMvc.perform(post("/admin/stores/delivery-config")
                                .session(adminSession)
                                .param("homeDeliveryCharge", "60")
                                .param("storePickupCharge", "0")
                                .param("pickupInstructions", "Collect at counter A with Order Number."))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/stores"));

                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);

                // Save an Indian address to user profile
                mockMvc.perform(post("/addresses/save")
                                .session(userSession)
                                .param("fullName", "Aarav Patel")
                                .param("mobileNumber", "9876543210")
                                .param("houseNo", "B-204")
                                .param("buildingName", "Suryansh Residency")
                                .param("streetArea", "Satellite Road")
                                .param("landmark", "Near Iscon Mega Mall")
                                .param("country", "India")
                                .param("state", "Gujarat")
                                .param("city", "Ahmedabad")
                                .param("pinCode", "380015")
                                .param("addressType", "HOME")
                                .param("defaultAddress", "true")
                                .param("returnTo", "/profile"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile#addresses-section"));

                List<UserAddress> savedList = userAddressRepo.findByUserOrderByDefaultAddressDescUpdatedAtDesc(customerUser);
                assertThat(savedList).hasSize(1);
                UserAddress savedAddr = savedList.get(0);

                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(userSession))
                                .andExpect(status().is3xxRedirection());

                String cartHtml = mockMvc.perform(get("/cart").session(userSession))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(cartHtml).contains("Home Delivery", "Physical Store Pickup", "Aarav Patel", "Ahmedabad", "380015");

                ArgumentCaptor<PendingCheckout> pendingCaptor = ArgumentCaptor.forClass(PendingCheckout.class);
                when(stripeService.createCheckoutSessionForMultipleBooks(
                                anyList(), anyDouble(), eq(customerUser.getUserid()), anyString(), anyString(),
                                pendingCaptor.capture()))
                                .thenReturn("https://checkout.stripe.test/home-delivery-" + uniqueSuffix);

                mockMvc.perform(post("/checkout-all")
                                .session(userSession)
                                .param("deliveryMethod", "HOME_DELIVERY")
                                .param("addressSelectionMode", "SAVED")
                                .param("selectedAddressId", String.valueOf(savedAddr.getId())))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("https://checkout.stripe.test/home-delivery-" + uniqueSuffix));

                PendingCheckout pending = pendingCaptor.getValue();
                assertThat(pending.getDeliveryMethod()).isEqualTo("HOME_DELIVERY");
                assertThat(pending.getDeliveryCharge()).isEqualTo(60.0);
                assertThat(pending.getShippingCity()).isEqualTo("Ahmedabad");

                // Paid callback creates Order with subtotal (200) + fee (60) = 260
                String stripeSessId = "cs_hd_" + uniqueSuffix;
                when(stripeService.retrieveCheckoutSession(stripeSessId))
                                .thenReturn(mockStripePaidSession(stripeSessId, "pi_hd_" + uniqueSuffix,
                                                customerUser.getUserid(), "CART",
                                                Map.of("cartItems", bookAlpha.getId() + ":1",
                                                       "checkoutToken", pending.getCheckoutToken())));

                MvcResult res = mockMvc.perform(get("/payment-success")
                                .session(userSession)
                                .param("session_id", stripeSessId))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrlPattern("/orders/*/receipt"))
                                .andReturn();

                Order order = orderRepo.findAllByStripeSessionId(stripeSessId).get(0);
                assertThat(order.getDeliveryMethod()).isEqualTo(DeliveryMethod.HOME_DELIVERY.name());
                assertThat(order.getDeliveryCharge()).isEqualTo(60.0);
                assertThat(order.getSubtotal()).isEqualTo(200.0);
                assertThat(order.getAmount()).isEqualTo(260.0);
                assertThat(order.getOrderStatus()).isEqualTo("ORDER_PLACED");
                assertThat(order.getShippingFullName()).isEqualTo("Aarav Patel");
                assertThat(order.getShippingPinCode()).isEqualTo("380015");

                // Immutability check: later deleting original UserAddress must NOT affect order snapshot
                userAddressRepo.delete(savedAddr);
                entityManager.flush();

                String receiptHtml = mockMvc.perform(get(res.getResponse().getRedirectedUrl()).session(userSession))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(receiptHtml).contains("Home Delivery", "Aarav Patel", "Suryansh Residency", "Ahmedabad", "380015", "260.00");
        }

        // =========================================================================
        // TEST 16: Physical Store Pickup order (no home address, free pickup, instructions)
        // =========================================================================
        @Test
        void test16_physicalStorePickupNoAddressRequiredFreeAndShowsInstructions() throws Exception {
                storeLocationService.updateDeliveryConfig(50.0, 0.0,
                                "Show your Order Number and Government ID at Counter #2.");
                StoreLocation vadodaraStore = createStore(
                                "Quickread Vadodara Branch " + uniqueSuffix,
                                "Gujarat", "Vadodara", "390007", true);

                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);
                assertThat(userAddressRepo.findByUserOrderByDefaultAddressDescUpdatedAtDesc(customerUser)).isEmpty();

                mockMvc.perform(post("/add/" + bookBeta.getId()).session(userSession))
                                .andExpect(status().is3xxRedirection());

                ArgumentCaptor<PendingCheckout> pendingCaptor = ArgumentCaptor.forClass(PendingCheckout.class);
                when(stripeService.createCheckoutSessionForMultipleBooks(
                                anyList(), eq(350.0), eq(customerUser.getUserid()), anyString(), anyString(),
                                pendingCaptor.capture()))
                                .thenReturn("https://checkout.stripe.test/pickup-" + uniqueSuffix);

                // Submit STORE_PICKUP without any home address fields
                mockMvc.perform(post("/checkout-all")
                                .session(userSession)
                                .param("deliveryMethod", "STORE_PICKUP")
                                .param("selectedStoreId", String.valueOf(vadodaraStore.getId())))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("https://checkout.stripe.test/pickup-" + uniqueSuffix));

                PendingCheckout pending = pendingCaptor.getValue();
                assertThat(pending.getDeliveryMethod()).isEqualTo("STORE_PICKUP");
                assertThat(pending.getDeliveryCharge()).isEqualTo(0.0);
                assertThat(pending.getPickupStoreName()).isEqualTo(vadodaraStore.getStoreName());

                String stripeSessId = "cs_pu_" + uniqueSuffix;
                when(stripeService.retrieveCheckoutSession(stripeSessId))
                                .thenReturn(mockStripePaidSession(stripeSessId, "pi_pu_" + uniqueSuffix,
                                                customerUser.getUserid(), "CART",
                                                Map.of("cartItems", bookBeta.getId() + ":1",
                                                       "checkoutToken", pending.getCheckoutToken())));

                MvcResult res = mockMvc.perform(get("/payment-success")
                                .session(userSession)
                                .param("session_id", stripeSessId))
                                .andExpect(status().is3xxRedirection())
                                .andReturn();

                Order order = orderRepo.findAllByStripeSessionId(stripeSessId).get(0);
                assertThat(order.isStorePickup()).isTrue();
                assertThat(order.getDeliveryCharge()).isEqualTo(0.0);
                assertThat(order.getAmount()).isEqualTo(350.0);
                assertThat(order.getOrderStatus()).isEqualTo("ORDER_PLACED");
                assertThat(order.getPickupStoreName()).isEqualTo(vadodaraStore.getStoreName());
                assertThat(order.getPickupStoreCity()).isEqualTo("Vadodara");

                // Receipt HTML & PDF both contain pickup store details & instructions
                String receiptHtml = mockMvc.perform(get(res.getResponse().getRedirectedUrl()).session(userSession))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(receiptHtml).contains("Physical Store Pickup", vadodaraStore.getStoreName(),
                                "Vadodara", "390007", "Show your Order Number and Government ID at Counter #2.");

                ArgumentCaptor<byte[]> pdfCaptor = ArgumentCaptor.forClass(byte[].class);
                verify(emailService, times(1)).sendOrderReceiptEmail(eq(order), pdfCaptor.capture());
                try (PDDocument pdfDoc = PDDocument.load(pdfCaptor.getValue())) {
                        String pdfText = new PDFTextStripper().getText(pdfDoc);
                        assertThat(pdfText).contains("Physical Store Pickup");
                        assertThat(pdfText).contains(vadodaraStore.getStoreName());
                        assertThat(pdfText).contains("Show your Order Number and Government ID at Counter #2.");
                }
        }

        // =========================================================================
        // TEST 17: Existing saved address default toggle & cross-user ownership isolation
        // =========================================================================
        @Test
        void test17_savedAddressDefaultManagementAndCrossUserOwnershipIsolation() throws Exception {
                UserAddress addr1 = createSavedAddress(customerUser, "Reader Home", "9876500001",
                                "Gujarat", "Surat", "395007", "HOME", true);
                UserAddress addr2 = createSavedAddress(customerUser, "Reader Office", "9876500002",
                                "Gujarat", "Gandhinagar", "382010", "OFFICE", false);

                MockHttpSession ownerSession = loginAs(customerUser.getUsername(), rawPassword);

                mockMvc.perform(post("/addresses/" + addr2.getId() + "/default")
                                .session(ownerSession)
                                .param("returnTo", "/profile"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile#addresses-section"));

                assertThat(userAddressRepo.findById(addr1.getId()).orElseThrow().isDefaultAddress()).isFalse();
                assertThat(userAddressRepo.findById(addr2.getId()).orElseThrow().isDefaultAddress()).isTrue();

                // Second user cannot access, edit, set-default, delete, or checkout with customerUser's address
                User intruder = new User();
                intruder.setUsername("intruder_" + uniqueSuffix);
                intruder.setEmail("intruder_" + uniqueSuffix + "@example.com");
                intruder.setPassword(passwordEncoder.encode(rawPassword));
                intruder.setMobileno("9111122222");
                intruder.setRole("USER");
                intruder.setEnabled(true);
                intruder.setIsregistered(true);
                intruder = userRepo.save(intruder);

                MockHttpSession intruderSession = loginAs(intruder.getUsername(), rawPassword);

                mockMvc.perform(post("/addresses/" + addr1.getId() + "/delete").session(intruderSession))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/access-denied"));
                assertThat(userAddressRepo.findById(addr1.getId())).isPresent();

                mockMvc.perform(post("/addresses/save")
                                .session(intruderSession)
                                .param("addressId", String.valueOf(addr1.getId()))
                                .param("fullName", "Hijacked")
                                .param("mobileNumber", "9999988888")
                                .param("houseNo", "1")
                                .param("buildingName", "X")
                                .param("streetArea", "Y")
                                .param("country", "India")
                                .param("state", "Delhi")
                                .param("city", "New Delhi")
                                .param("pinCode", "110001")
                                .param("addressType", "HOME"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/access-denied"));

                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(intruderSession))
                                .andExpect(status().is3xxRedirection());
                mockMvc.perform(post("/checkout-all")
                                .session(intruderSession)
                                .param("deliveryMethod", "HOME_DELIVERY")
                                .param("addressSelectionMode", "SAVED")
                                .param("selectedAddressId", String.valueOf(addr1.getId())))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/cart"))
                                .andExpect(flash().attributeExists("error"));
                verify(stripeService, never()).createCheckoutSessionForMultipleBooks(any(), anyDouble(), any(), any(), any(), any());
        }

        // =========================================================================
        // TEST 18: Add new address inline during checkout saves to user profile
        // =========================================================================
        @Test
        void test18_addNewAddressInlineDuringCheckoutSavesToProfile() throws Exception {
                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);
                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(userSession))
                                .andExpect(status().is3xxRedirection());

                ArgumentCaptor<PendingCheckout> pendingCaptor = ArgumentCaptor.forClass(PendingCheckout.class);
                when(stripeService.createCheckoutSessionForMultipleBooks(
                                anyList(), anyDouble(), eq(customerUser.getUserid()), anyString(), anyString(),
                                pendingCaptor.capture()))
                                .thenReturn("https://checkout.stripe.test/inline-addr-" + uniqueSuffix);

                mockMvc.perform(post("/checkout-all")
                                .session(userSession)
                                .param("deliveryMethod", "HOME_DELIVERY")
                                .param("addressSelectionMode", "NEW")
                                .param("selectedAddressId", "NEW")
                                .param("fullName", "Meera Deshmukh")
                                .param("mobileNumber", "9822011223")
                                .param("houseNo", "Flat 14")
                                .param("buildingName", "Koregaon Heights")
                                .param("streetArea", "Lane 7, Koregaon Park")
                                .param("landmark", "Near Osho Garden")
                                .param("country", "India")
                                .param("state", "Maharashtra")
                                .param("city", "Pune")
                                .param("pinCode", "411001")
                                .param("addressType", "OFFICE")
                                .param("saveAsDefault", "true"))
                                .andExpect(status().is3xxRedirection());

                List<UserAddress> profileAddresses = userAddressRepo.findByUserOrderByDefaultAddressDescUpdatedAtDesc(customerUser);
                assertThat(profileAddresses).hasSize(1);
                UserAddress saved = profileAddresses.get(0);
                assertThat(saved.getFullName()).isEqualTo("Meera Deshmukh");
                assertThat(saved.getState()).isEqualTo("Maharashtra");
                assertThat(saved.getCity()).isEqualTo("Pune");
                assertThat(saved.getPinCode()).isEqualTo("411001");
                assertThat(saved.getAddressType()).isEqualTo("OFFICE");
                assertThat(saved.isDefaultAddress()).isTrue();

                PendingCheckout pc = pendingCaptor.getValue();
                assertThat(pc.getDeliveryAddressId()).isEqualTo(saved.getId());
                assertThat(pc.getShippingCity()).isEqualTo("Pune");
        }

        // =========================================================================
        // TEST 19: Switch delivery method & Single-Book Express Checkout flow
        // =========================================================================
        @Test
        void test19_singleBookCheckoutDeliveryMethodSwitchingAndFinalization() throws Exception {
                storeLocationService.updateDeliveryConfig(75.0, 0.0, "Bring Order ID.");
                StoreLocation pickupStore = createStore("Quickread Anand Desk " + uniqueSuffix,
                                "Gujarat", "Anand", "388001", true);
                UserAddress addr = createSavedAddress(customerUser, "Rohan Mehta", "9876512345",
                                "Gujarat", "Rajkot", "360001", "HOME", true);

                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);

                String singleCheckoutHtml = mockMvc.perform(get("/checkout/" + bookAlpha.getId())
                                .session(userSession)
                                .param("quantity", "2"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(singleCheckoutHtml).contains("Home Delivery", "Physical Store Pickup", pickupStore.getStoreName(), "Rohan Mehta");

                ArgumentCaptor<PendingCheckout> pendingCaptor = ArgumentCaptor.forClass(PendingCheckout.class);
                when(stripeService.createCheckoutSession(
                                eq(bookAlpha.getId()), anyString(), anyDouble(), eq(2),
                                eq(customerUser.getUserid()), anyString(), anyString(), pendingCaptor.capture()))
                                .thenReturn("https://checkout.stripe.test/single-delivery-" + uniqueSuffix);

                // First submit with HOME_DELIVERY (qty 2 * 200 = 400 + 75 delivery fee)
                mockMvc.perform(post("/checkout/" + bookAlpha.getId())
                                .session(userSession)
                                .param("quantity", "2")
                                .param("deliveryMethod", "HOME_DELIVERY")
                                .param("addressSelectionMode", "SAVED")
                                .param("selectedAddressId", String.valueOf(addr.getId())))
                                .andExpect(status().is3xxRedirection());

                PendingCheckout homePc = pendingCaptor.getValue();
                assertThat(homePc.getDeliveryMethod()).isEqualTo("HOME_DELIVERY");
                assertThat(homePc.getDeliveryCharge()).isEqualTo(75.0);
                assertThat(homePc.getQuantity()).isEqualTo(2);

                // User switches to STORE_PICKUP (free pickup -> 0 fee)
                mockMvc.perform(post("/checkout/" + bookAlpha.getId())
                                .session(userSession)
                                .param("quantity", "2")
                                .param("deliveryMethod", "STORE_PICKUP")
                                .param("selectedStoreId", String.valueOf(pickupStore.getId())))
                                .andExpect(status().is3xxRedirection());

                List<PendingCheckout> allCaptures = pendingCaptor.getAllValues();
                PendingCheckout pickupPc = allCaptures.get(allCaptures.size() - 1);
                assertThat(pickupPc.getDeliveryMethod()).isEqualTo("STORE_PICKUP");
                assertThat(pickupPc.getDeliveryCharge()).isEqualTo(0.0);

                String stripeSessId = "cs_single_pu_" + uniqueSuffix;
                when(stripeService.retrieveCheckoutSession(stripeSessId))
                                .thenReturn(mockStripePaidSession(stripeSessId, "pi_spu_" + uniqueSuffix,
                                                customerUser.getUserid(), "SINGLE",
                                                Map.of("bookId", String.valueOf(bookAlpha.getId()),
                                                       "quantity", "2",
                                                       "checkoutToken", pickupPc.getCheckoutToken())));

                mockMvc.perform(get("/success")
                                .session(userSession)
                                .param("session_id", stripeSessId)
                                .param("bookId", String.valueOf(bookAlpha.getId())))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrlPattern("/orders/*/receipt"));

                Order order = orderRepo.findAllByStripeSessionId(stripeSessId).get(0);
                assertThat(order.getQuantity()).isEqualTo(2);
                assertThat(order.getSubtotal()).isEqualTo(400.0);
                assertThat(order.getDeliveryCharge()).isEqualTo(0.0);
                assertThat(order.getAmount()).isEqualTo(400.0);
                assertThat(order.getPickupStoreName()).isEqualTo(pickupStore.getStoreName());
        }

        // =========================================================================
        // TEST 20: Select different active pickup stores + India Location API check
        // =========================================================================
        @Test
        void test20_selectSecondActivePickupStoreAndIndiaLocationEndpointsWork() throws Exception {
                String statesJson = mockMvc.perform(get("/api/locations/states"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(statesJson).contains("Gujarat", "Maharashtra", "Rajasthan", "Madhya Pradesh", "Delhi", "Uttar Pradesh");

                String gujaratCitiesJson = mockMvc.perform(get("/api/locations/cities").param("state", "Gujarat"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(gujaratCitiesJson).contains("Ahmedabad", "Vadodara", "Surat", "Rajkot",
                                "Gandhinagar", "Anand", "Bharuch", "Jamnagar");

                StoreLocation storeOne = createStore("Store One Surat " + uniqueSuffix,
                                "Gujarat", "Surat", "395007", true);
                StoreLocation storeTwo = createStore("Store Two Jaipur " + uniqueSuffix,
                                "Rajasthan", "Jaipur", "302001", true);

                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);
                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(userSession))
                                .andExpect(status().is3xxRedirection());

                ArgumentCaptor<PendingCheckout> pendingCaptor = ArgumentCaptor.forClass(PendingCheckout.class);
                when(stripeService.createCheckoutSessionForMultipleBooks(
                                anyList(), anyDouble(), anyLong(), anyString(), anyString(), pendingCaptor.capture()))
                                .thenReturn("https://checkout.stripe.test/store2");

                mockMvc.perform(post("/checkout-all")
                                .session(userSession)
                                .param("deliveryMethod", "STORE_PICKUP")
                                .param("selectedStoreId", String.valueOf(storeTwo.getId())))
                                .andExpect(status().is3xxRedirection());

                PendingCheckout pc = pendingCaptor.getValue();
                assertThat(pc.getPickupStoreId()).isEqualTo(storeTwo.getId());
                assertThat(pc.getPickupStoreName()).isEqualTo(storeTwo.getStoreName());
                assertThat(pc.getPickupStoreCity()).isEqualTo("Jaipur");
                assertThat(pc.getPickupStoreName()).isNotEqualTo(storeOne.getStoreName());
        }

        // =========================================================================
        // TEST 21: Invalid Indian mobile / PIN / state / missing address blocked
        // =========================================================================
        @Test
        void test21_invalidIndianMobilePinAndIncompleteAddressAreRejected() throws Exception {
                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);

                // Invalid 10-digit Indian mobile starting with 1
                mockMvc.perform(post("/addresses/save")
                                .session(userSession)
                                .param("fullName", "Invalid Mobile")
                                .param("mobileNumber", "1234567890")
                                .param("houseNo", "10")
                                .param("buildingName", "Tower A")
                                .param("streetArea", "MG Road")
                                .param("country", "India")
                                .param("state", "Gujarat")
                                .param("city", "Ahmedabad")
                                .param("pinCode", "380009")
                                .param("addressType", "HOME"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("error"));

                // Invalid Indian PIN starting with 0
                mockMvc.perform(post("/addresses/save")
                                .session(userSession)
                                .param("fullName", "Invalid Pin")
                                .param("mobileNumber", "9876543210")
                                .param("houseNo", "10")
                                .param("buildingName", "Tower A")
                                .param("streetArea", "MG Road")
                                .param("country", "India")
                                .param("state", "Gujarat")
                                .param("city", "Ahmedabad")
                                .param("pinCode", "080009")
                                .param("addressType", "HOME"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("error"));

                assertThat(userAddressRepo.findByUserOrderByDefaultAddressDescUpdatedAtDesc(customerUser)).isEmpty();

                // Checkout Home Delivery with no address / invalid state rejected without calling Stripe
                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(userSession))
                                .andExpect(status().is3xxRedirection());

                mockMvc.perform(post("/checkout-all")
                                .session(userSession)
                                .param("deliveryMethod", "HOME_DELIVERY")
                                .param("addressSelectionMode", "NEW")
                                .param("fullName", "Reader")
                                .param("mobileNumber", "9876543210")
                                .param("houseNo", "12")
                                .param("buildingName", "Plaza")
                                .param("streetArea", "Ring Road")
                                .param("country", "India")
                                .param("state", "NonExistentState")
                                .param("city", "Bharuch")
                                .param("pinCode", "392001"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/cart"))
                                .andExpect(flash().attributeExists("error"));

                verify(stripeService, never()).createCheckoutSessionForMultipleBooks(any(), anyDouble(), any(), any(), any(), any());
        }

        // =========================================================================
        // TEST 22: No active pickup store & inactive store hidden and rejected
        // =========================================================================
        @Test
        void test22_noActivePickupStoreAndInactiveStoreHiddenAndRejected() throws Exception {
                // Deactivate all pre-existing active stores in test TX so active store list is empty
                storeLocationRepo.findByActiveTrueOrderByStateAscCityAscStoreNameAsc().forEach(s -> {
                        s.setActive(false);
                        storeLocationRepo.save(s);
                });
                StoreLocation inactiveStore = createStore("Closed Maintenance Store " + uniqueSuffix,
                                "Gujarat", "Jamnagar", "361001", false);

                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);
                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(userSession))
                                .andExpect(status().is3xxRedirection());

                String cartHtml = mockMvc.perform(get("/cart").session(userSession))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(cartHtml).contains("No Active Pickup Stores Currently Available");
                assertThat(cartHtml).doesNotContain(inactiveStore.getStoreName());

                mockMvc.perform(post("/checkout-all")
                                .session(userSession)
                                .param("deliveryMethod", "STORE_PICKUP")
                                .param("selectedStoreId", String.valueOf(inactiveStore.getId())))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/cart"))
                                .andExpect(flash().attributeExists("error"));

                // Even when another active store exists, submitting the inactive store ID must be rejected
                createStore("Open Flagship Store " + uniqueSuffix, "Gujarat", "Bharuch", "392001", true);
                mockMvc.perform(post("/checkout-all")
                                .session(userSession)
                                .param("deliveryMethod", "STORE_PICKUP")
                                .param("selectedStoreId", String.valueOf(inactiveStore.getId())))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/cart"))
                                .andExpect(flash().attributeExists("error"));

                verify(stripeService, never()).createCheckoutSessionForMultipleBooks(any(), anyDouble(), any(), any(), any(), any());
        }

        // =========================================================================
        // TEST 23: Cancelled / failed Stripe payment does NOT issue paid order or clear cart
        // =========================================================================
        @Test
        void test23_cancelledOrFailedStripePaymentWithDeliveryCreatesNoOrderOrReceipt() throws Exception {
                StoreLocation activeStore = createStore("Quickread Delhi Store " + uniqueSuffix,
                                "Delhi", "New Delhi", "110001", true);

                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);
                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(userSession))
                                .andExpect(status().is3xxRedirection());

                ArgumentCaptor<PendingCheckout> pendingCaptor = ArgumentCaptor.forClass(PendingCheckout.class);
                when(stripeService.createCheckoutSessionForMultipleBooks(
                                anyList(), anyDouble(), anyLong(), anyString(), anyString(), pendingCaptor.capture()))
                                .thenReturn("https://checkout.stripe.test/cancel-case");

                mockMvc.perform(post("/checkout-all")
                                .session(userSession)
                                .param("deliveryMethod", "STORE_PICKUP")
                                .param("selectedStoreId", String.valueOf(activeStore.getId())))
                                .andExpect(status().is3xxRedirection());

                PendingCheckout pending = pendingCaptor.getValue();
                long ordersBefore = orderRepo.count();

                // User hits /cancel
                mockMvc.perform(get("/cancel").session(userSession))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/cart"))
                                .andExpect(flash().attributeExists("error"));

                // Unpaid Stripe callback returns status=unpaid
                String unpaidSessionId = "cs_unpaid_deliv_" + uniqueSuffix;
                Session unpaid = new Session();
                unpaid.setId(unpaidSessionId);
                unpaid.setPaymentStatus("unpaid");
                unpaid.setMetadata(Map.of(
                                "userId", String.valueOf(customerUser.getUserid()),
                                "checkoutToken", pending.getCheckoutToken()));
                when(stripeService.retrieveCheckoutSession(unpaidSessionId)).thenReturn(unpaid);

                mockMvc.perform(get("/payment-success")
                                .session(userSession)
                                .param("session_id", unpaidSessionId))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/cart"))
                                .andExpect(flash().attributeExists("error"));

                assertThat(orderRepo.count()).isEqualTo(ordersBefore);
                assertThat(cartRepo.findByUser(customerUser)).hasSize(1);
                assertThat(pendingCheckoutRepo.findByCheckoutToken(pending.getCheckoutToken()).orElseThrow().isCompleted())
                                .isFalse();
                verify(emailService, never()).sendOrderReceiptEmail(any(), any());
        }

        // =========================================================================
        // TEST 24: Admin Store Locations CRUD, security enforcement & delivery-specific order status
        // =========================================================================
        @Test
        void test24_adminStoreCrudSecurityAndOrderDeliveryStatusPipelines() throws Exception {
                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);
                // Customer cannot access Admin Store Locations endpoints
                mockMvc.perform(get("/admin/stores").session(userSession))
                                .andExpect(status().isForbidden());
                mockMvc.perform(post("/admin/stores/save").session(userSession))
                                .andExpect(status().isForbidden());

                MockHttpSession adminSession = loginAs(adminUser.getUsername(), rawPassword);
                String adminDashHtml = mockMvc.perform(get("/admin/dashboard").session(adminSession))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(adminDashHtml).contains("Store Locations");

                // Add store via AdminStoreController
                String branchName = "Quickread Heritage Branch " + uniqueSuffix;
                mockMvc.perform(post("/admin/stores/save")
                                .session(adminSession)
                                .param("storeName", branchName)
                                .param("contactNumber", "9898765432")
                                .param("country", "India")
                                .param("state", "Uttar Pradesh")
                                .param("city", "Lucknow")
                                .param("address", "Hazratganj Plaza, Floor 1")
                                .param("pinCode", "226001")
                                .param("openingHours", "10:00 AM - 9:00 PM")
                                .param("active", "true"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/stores"));

                StoreLocation createdStore = storeLocationRepo.findAll().stream()
                                .filter(s -> branchName.equals(s.getStoreName()))
                                .findFirst().orElseThrow();

                String storesHtml = mockMvc.perform(get("/admin/stores").param("editId", String.valueOf(createdStore.getId()))
                                .session(adminSession))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(storesHtml).contains(branchName, "Lucknow", "226001");

                // Toggle availability active -> inactive -> active
                mockMvc.perform(post("/admin/stores/" + createdStore.getId() + "/toggle").session(adminSession))
                                .andExpect(status().is3xxRedirection());
                assertThat(storeLocationRepo.findById(createdStore.getId()).orElseThrow().isActive()).isFalse();

                // Verify delivery-method-specific order status transitions
                Order homeOrder = new Order();
                homeOrder.setUser(customerUser);
                homeOrder.setBook(bookAlpha);
                homeOrder.setAmount(260);
                homeOrder.setDeliveryMethod(DeliveryMethod.HOME_DELIVERY.name());
                homeOrder.setDeliveryCharge(60.0);
                homeOrder.setShippingFullName("Aarav Patel");
                homeOrder.setShippingCity("Ahmedabad");
                homeOrder.setShippingState("Gujarat");
                homeOrder.setShippingPinCode("380015");
                homeOrder.setShippingAddressSnapshot("B-204, Ahmedabad, Gujarat - 380015, India");
                homeOrder.setPaymentStatus("PAID");
                homeOrder.setOrderStatus("ORDER_PLACED");
                homeOrder.setOrderDate(LocalDateTime.now());
                homeOrder.addItem(new OrderItem(bookAlpha, 1, 200));
                homeOrder = orderRepo.save(homeOrder);

                // Valid HOME_DELIVERY progression
                mockMvc.perform(post("/admin/orders/" + homeOrder.getId() + "/status")
                                .session(adminSession)
                                .param("orderStatus", "OUT_FOR_DELIVERY"))
                                .andExpect(status().is3xxRedirection());
                assertThat(orderRepo.findById(homeOrder.getId()).orElseThrow().getOrderStatus())
                                .isEqualTo("OUT_FOR_DELIVERY");

                // Invalid STORE_PICKUP status rejected on HOME_DELIVERY order
                mockMvc.perform(post("/admin/orders/" + homeOrder.getId() + "/status")
                                .session(adminSession)
                                .param("orderStatus", "READY_FOR_PICKUP"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("error"));
                assertThat(orderRepo.findById(homeOrder.getId()).orElseThrow().getOrderStatus())
                                .isEqualTo("OUT_FOR_DELIVERY");

                Order pickupOrder = new Order();
                pickupOrder.setUser(customerUser);
                pickupOrder.setBook(bookBeta);
                pickupOrder.setAmount(350);
                pickupOrder.setDeliveryMethod(DeliveryMethod.STORE_PICKUP.name());
                pickupOrder.setPickupStoreName(branchName);
                pickupOrder.setPickupStoreCity("Lucknow");
                pickupOrder.setPickupStoreState("Uttar Pradesh");
                pickupOrder.setPickupStoreAddress("Hazratganj Plaza");
                pickupOrder.setPickupStorePinCode("226001");
                pickupOrder.setPaymentStatus("PAID");
                pickupOrder.setOrderStatus("ORDER_PLACED");
                pickupOrder.setOrderDate(LocalDateTime.now());
                pickupOrder.addItem(new OrderItem(bookBeta, 1, 350));
                pickupOrder = orderRepo.save(pickupOrder);

                mockMvc.perform(post("/admin/orders/" + pickupOrder.getId() + "/status")
                                .session(adminSession)
                                .param("orderStatus", "READY_FOR_PICKUP"))
                                .andExpect(status().is3xxRedirection());
                assertThat(orderRepo.findById(pickupOrder.getId()).orElseThrow().getOrderStatus())
                                .isEqualTo("READY_FOR_PICKUP");

                // Invalid HOME_DELIVERY status rejected on STORE_PICKUP order
                mockMvc.perform(post("/admin/orders/" + pickupOrder.getId() + "/status")
                                .session(adminSession)
                                .param("orderStatus", "SHIPPED"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("error"));

                // Admin order detail page renders store pickup & status label
                String detailHtml = mockMvc.perform(get("/admin/orders/" + pickupOrder.getId()).session(adminSession))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(detailHtml).contains(branchName, "Ready for Pickup");

                // Delete store works cleanly
                mockMvc.perform(post("/admin/stores/" + createdStore.getId() + "/delete").session(adminSession))
                                .andExpect(status().is3xxRedirection());
                assertThat(storeLocationRepo.findById(createdStore.getId())).isEmpty();
        }

        // =========================================================================
        // TEST 25: Multiple books in ONE order with Delivery produces ONE combined receipt & PDF
        // =========================================================================
        @Test
        void test25_multiBookCombinedOrderWithDeliveryProducesSingleOrderReceiptAndPdf() throws Exception {
                storeLocationService.updateDeliveryConfig(50.0, 0.0, "Pickup instructions.");
                UserAddress savedAddr = createSavedAddress(customerUser, "Kavya Nair", "9845012345",
                                "Madhya Pradesh", "Indore", "452001", "HOME", true);

                MockHttpSession userSession = loginAs(customerUser.getUsername(), rawPassword);
                mockMvc.perform(post("/add/" + bookAlpha.getId()).session(userSession).param("quantity", "2"))
                                .andExpect(status().is3xxRedirection());
                mockMvc.perform(post("/add/" + bookBeta.getId()).session(userSession).param("quantity", "1"))
                                .andExpect(status().is3xxRedirection());
                mockMvc.perform(post("/add/" + bookGamma.getId()).session(userSession).param("quantity", "1"))
                                .andExpect(status().is3xxRedirection());

                // Subtotal = 2*200 + 350 + 100 = 850; Delivery = 50; Total = 900
                ArgumentCaptor<PendingCheckout> pendingCaptor = ArgumentCaptor.forClass(PendingCheckout.class);
                when(stripeService.createCheckoutSessionForMultipleBooks(
                                anyList(), eq(900.0), eq(customerUser.getUserid()), anyString(), anyString(),
                                pendingCaptor.capture()))
                                .thenReturn("https://checkout.stripe.test/multi-deliv");

                mockMvc.perform(post("/checkout-all")
                                .session(userSession)
                                .param("deliveryMethod", "HOME_DELIVERY")
                                .param("addressSelectionMode", "SAVED")
                                .param("selectedAddressId", String.valueOf(savedAddr.getId())))
                                .andExpect(status().is3xxRedirection());

                PendingCheckout pc = pendingCaptor.getValue();
                String sessId = "cs_multi_deliv_" + uniqueSuffix;
                String encoded = bookAlpha.getId() + ":2," + bookBeta.getId() + ":1," + bookGamma.getId() + ":1";
                when(stripeService.retrieveCheckoutSession(sessId))
                                .thenReturn(mockStripePaidSession(sessId, "pi_md_" + uniqueSuffix,
                                                customerUser.getUserid(), "CART",
                                                Map.of("cartItems", encoded,
                                                       "checkoutToken", pc.getCheckoutToken())));

                MvcResult res = mockMvc.perform(get("/payment-success").session(userSession).param("session_id", sessId))
                                .andExpect(status().is3xxRedirection())
                                .andReturn();
                // Simulate accidental page refresh
                mockMvc.perform(get("/payment-success").session(userSession).param("session_id", sessId))
                                .andExpect(status().is3xxRedirection());

                List<Order> created = orderRepo.findAllByStripeSessionId(sessId);
                assertThat(created).hasSize(1);
                Order combined = created.get(0);
                assertThat(combined.getItems()).hasSize(3);
                assertThat(combined.getSubtotal()).isEqualTo(850.0);
                assertThat(combined.getDeliveryCharge()).isEqualTo(50.0);
                assertThat(combined.getAmount()).isEqualTo(900.0);
                assertThat(combined.getShippingCity()).isEqualTo("Indore");

                ArgumentCaptor<byte[]> pdfCaptor = ArgumentCaptor.forClass(byte[].class);
                verify(emailService, times(1)).sendOrderReceiptEmail(eq(combined), pdfCaptor.capture());
                try (PDDocument pdfDoc = PDDocument.load(pdfCaptor.getValue())) {
                        String pdfText = new PDFTextStripper().getText(pdfDoc);
                        assertThat(pdfText).contains(bookAlpha.getTitle(), bookBeta.getTitle(), bookGamma.getTitle());
                        assertThat(pdfText).contains("Home Delivery", "Kavya Nair", "Indore", "452001", "900.00");
                }

                String receiptHtml = mockMvc.perform(get(res.getResponse().getRedirectedUrl()).session(userSession))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                assertThat(receiptHtml).contains(bookAlpha.getTitle(), bookBeta.getTitle(), bookGamma.getTitle(),
                                "Home Delivery", "Kavya Nair", "Indore", "900.00");
        }
}

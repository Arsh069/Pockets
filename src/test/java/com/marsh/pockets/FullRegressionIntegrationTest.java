package com.marsh.pockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsh.pockets.auth.dto.AuthResponse;
import com.marsh.pockets.auth.dto.LoginRequest;
import com.marsh.pockets.auth.dto.RefreshRequest;
import com.marsh.pockets.auth.dto.RegisterRequest;
import com.marsh.pockets.auth.repository.RefreshTokenRepository;
import com.marsh.pockets.auth.repository.UserRepository;
import com.marsh.pockets.pocket.dto.CreatePocketRequest;
import com.marsh.pockets.pocket.dto.OverrideBalanceRequest;
import com.marsh.pockets.pocket.dto.PocketResponse;
import com.marsh.pockets.pocket.dto.UpdatePocketRequest;
import com.marsh.pockets.pocket.repository.PocketRepository;
import com.marsh.pockets.pocket.service.PocketService;
import com.marsh.pockets.transaction.dto.CreateTransactionRequest;
import com.marsh.pockets.transaction.dto.TransactionResponse;
import com.marsh.pockets.transaction.entity.TransactionStatus;
import com.marsh.pockets.transaction.repository.TransactionRepository;
import com.marsh.pockets.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class FullRegressionIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PocketRepository pocketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PocketService pocketService;

    @Autowired
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        transactionRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        pocketRepository.deleteAll();
        userRepository.deleteAll();
    }

    private AuthResponse registerUser(String phone, String password, String name, int payDay) throws Exception {
        RegisterRequest request = new RegisterRequest(phone, password, name, payDay);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    @Test
    @DisplayName("Auth flow: Register, Login, Refresh token rotation, and compromise detection")
    void testAuthFlow() throws Exception {
        AuthResponse registered = registerUser("9876543210", "Password@123", "User A", 15);
        assertNotNull(registered.accessToken());
        assertNotNull(registered.refreshToken());

        LoginRequest loginReq = new LoginRequest("9876543210", "Password@123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse loggedIn = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(loggedIn.accessToken());

        // Refresh token rotation
        RefreshRequest refreshReq = new RefreshRequest(loggedIn.refreshToken());
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse rotated = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), AuthResponse.class);
        assertNotNull(rotated.accessToken());
        assertNotNull(rotated.refreshToken());

        // Reuse of revoked refresh token must be rejected with 401
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Pocket CRUD, duplicate name prevention (case-insensitive), and cross-user isolation")
    void testPocketCrudAndUniqueness() throws Exception {
        AuthResponse userA = registerUser("9876543210", "Password@123", "User A", 15);
        AuthResponse userB = registerUser("9876543211", "Password@123", "User B", 20);

        String tokenA = "Bearer " + userA.accessToken();
        String tokenB = "Bearer " + userB.accessToken();

        // 1. User A creates pocket "Shopping"
        CreatePocketRequest createShopping = new CreatePocketRequest("Shopping", new BigDecimal("5000.00"));
        MvcResult shoppingResult = mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createShopping)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Shopping"))
                .andReturn();
        PocketResponse shoppingPocket = objectMapper.readValue(shoppingResult.getResponse().getContentAsString(), PocketResponse.class);

        // 2. User A creates pocket "Travel"
        CreatePocketRequest createTravel = new CreatePocketRequest("Travel", new BigDecimal("3000.00"));
        MvcResult travelResult = mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTravel)))
                .andExpect(status().isCreated())
                .andReturn();
        PocketResponse travelPocket = objectMapper.readValue(travelResult.getResponse().getContentAsString(), PocketResponse.class);

        // 3. User A creating exact duplicate "Shopping" is rejected with 409
        mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createShopping)))
                .andExpect(status().isConflict());

        // 4. User A creating case-insensitive duplicate "shopping" is rejected with 409
        CreatePocketRequest lowercaseShopping = new CreatePocketRequest("shopping", new BigDecimal("4000.00"));
        mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowercaseShopping)))
                .andExpect(status().isConflict());

        // 5. User B CAN create "Shopping" successfully
        mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createShopping)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Shopping"));

        // 6. User A updating "Travel" to "shopping" (collision with User A's "Shopping") is rejected with 409
        UpdatePocketRequest renameToShopping = new UpdatePocketRequest("shopping", new BigDecimal("3000.00"));
        mockMvc.perform(put("/api/pockets/" + travelPocket.id())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renameToShopping)))
                .andExpect(status().isConflict());

        // 7. User A updating "Travel" to "Vacation" succeeds
        UpdatePocketRequest renameToVacation = new UpdatePocketRequest("Vacation", new BigDecimal("3500.00"));
        mockMvc.perform(put("/api/pockets/" + travelPocket.id())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renameToVacation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vacation"));

        // 8. User B cannot get User A's pocket (returns 404 for security / anti-enumeration)
        mockMvc.perform(get("/api/pockets/" + shoppingPocket.id())
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // 9. User A deletes "Vacation" pocket
        mockMvc.perform(delete("/api/pockets/" + travelPocket.id())
                        .header("Authorization", tokenA))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Transaction lifecycle: Create, idempotency, balance checks, confirm, cancel, and FK integrity")
    void testTransactionLifecycle() throws Exception {
        AuthResponse userA = registerUser("9876543210", "Password@123", "User A", 15);
        String tokenA = "Bearer " + userA.accessToken();

        // Create pocket
        CreatePocketRequest createPocket = new CreatePocketRequest("Dining", new BigDecimal("5000.00"));
        MvcResult pocketResult = mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPocket)))
                .andExpect(status().isCreated())
                .andReturn();
        PocketResponse pocket = objectMapper.readValue(pocketResult.getResponse().getContentAsString(), PocketResponse.class);

        // Override balance to 2000 so we have funds to test with
        OverrideBalanceRequest overrideReq = new OverrideBalanceRequest(new BigDecimal("2000.00"));
        mockMvc.perform(patch("/api/pockets/" + pocket.id() + "/balance")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overrideReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(2000.0));

        // Create transaction of 500
        String idempotencyKey1 = UUID.randomUUID().toString();
        CreateTransactionRequest txReq1 = new CreateTransactionRequest(
                pocket.id(),
                new BigDecimal("500.00"),
                "payee@upi",
                "Dinner",
                idempotencyKey1
        );

        MvcResult txResult1 = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txReq1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        TransactionResponse tx1 = objectMapper.readValue(txResult1.getResponse().getContentAsString(), TransactionResponse.class);

        // Idempotency: reposting same key returns existing transaction
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txReq1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(tx1.id()));

        // Confirm transaction tx1
        mockMvc.perform(post("/api/transactions/" + tx1.id() + "/confirm")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Verify balance was reduced to 1500
        mockMvc.perform(get("/api/pockets/" + pocket.id())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(1500.0));

        // Create transaction tx2 and cancel it
        String idempotencyKey2 = UUID.randomUUID().toString();
        CreateTransactionRequest txReq2 = new CreateTransactionRequest(
                pocket.id(),
                new BigDecimal("300.00"),
                "merchant@upi",
                "Snacks",
                idempotencyKey2
        );
        MvcResult txResult2 = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txReq2)))
                .andExpect(status().isCreated())
                .andReturn();
        TransactionResponse tx2 = objectMapper.readValue(txResult2.getResponse().getContentAsString(), TransactionResponse.class);

        mockMvc.perform(post("/api/transactions/" + tx2.id() + "/cancel")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Monthly reset: Manual reset with override protection and expiration")
    void testMonthlyResetBehavior() throws Exception {
        AuthResponse userA = registerUser("9876543210", "Password@123", "User A", 15);
        String tokenA = "Bearer " + userA.accessToken();

        // Create pocket with limit 5000 (starts at balance 0)
        CreatePocketRequest createPocket = new CreatePocketRequest("Bills", new BigDecimal("5000.00"));
        MvcResult pocketResult = mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPocket)))
                .andExpect(status().isCreated())
                .andReturn();
        PocketResponse pocket = objectMapper.readValue(pocketResult.getResponse().getContentAsString(), PocketResponse.class);

        // Manual override balance to 1234.00
        OverrideBalanceRequest overrideReq = new OverrideBalanceRequest(new BigDecimal("1234.00"));
        mockMvc.perform(patch("/api/pockets/" + pocket.id() + "/balance")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overrideReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(1234.0));

        // Manual reset should respect override protection -> balance remains 1234.00, lastResetAt updated
        MvcResult resetResult1 = mockMvc.perform(post("/api/pockets/" + pocket.id() + "/reset")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(1234.0))
                .andReturn();
        PocketResponse resetPocket1 = objectMapper.readValue(resetResult1.getResponse().getContentAsString(), PocketResponse.class);
        assertNotNull(resetPocket1.lastResetAt());
    }

    @Test
    @DisplayName("V4 Migration: Deleting a User cascades and deletes all their pockets and transactions")
    void testCascadeDeleteOnUserRemoval() throws Exception {
        AuthResponse userA = registerUser("9876543210", "Password@123", "User A", 15);
        String tokenA = "Bearer " + userA.accessToken();

        // Create pocket
        CreatePocketRequest createPocket = new CreatePocketRequest("Rent", new BigDecimal("10000.00"));
        MvcResult pocketResult = mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPocket)))
                .andExpect(status().isCreated())
                .andReturn();
        PocketResponse pocket = objectMapper.readValue(pocketResult.getResponse().getContentAsString(), PocketResponse.class);

        // Fund pocket
        OverrideBalanceRequest overrideReq = new OverrideBalanceRequest(new BigDecimal("5000.00"));
        mockMvc.perform(patch("/api/pockets/" + pocket.id() + "/balance")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overrideReq)))
                .andExpect(status().isOk());

        // Create transaction
        CreateTransactionRequest txReq = new CreateTransactionRequest(
                pocket.id(),
                new BigDecimal("1000.00"),
                "landlord@upi",
                "Rent advance",
                UUID.randomUUID().toString()
        );
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txReq)))
                .andExpect(status().isCreated());

        assertEquals(1, pocketRepository.findByUserId(userA.userId()).size());
        assertEquals(1, transactionRepository.findAllFiltered(null, userA.userId()).size());

        // Delete user directly from repository (simulating user removal)
        userRepository.deleteById(userA.userId());

        // Pockets and transactions must be cascade-deleted at DB level
        assertEquals(0, pocketRepository.findByUserId(userA.userId()).size());
        assertEquals(0, transactionRepository.findAllFiltered(null, userA.userId()).size());
    }

    @Test
    @DisplayName("Part A: Manual Purchase Logging with dual-balance columns, negative balance, and displayBalance")
    void testManualPurchaseLoggingDualBalance() throws Exception {
        AuthResponse userA = registerUser("9876543210", "Password@123", "User A", 15);
        AuthResponse userB = registerUser("9876543211", "Password@123", "User B", 15);
        String tokenA = "Bearer " + userA.accessToken();
        String tokenB = "Bearer " + userB.accessToken();

        // 1. Create pocket with limit 5000
        CreatePocketRequest createPocket = new CreatePocketRequest("Snacks", new BigDecimal("5000.00"));
        MvcResult pocketResult = mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPocket)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentBalance").value(0.0))
                .andExpect(jsonPath("$.manualCurrentBalance").value(0.0))
                .andExpect(jsonPath("$.displayBalance").value(0.0))
                .andReturn();
        PocketResponse pocket = objectMapper.readValue(pocketResult.getResponse().getContentAsString(), PocketResponse.class);

        // 2. Override balance to 1000.00 -> both columns update in sync
        OverrideBalanceRequest overrideReq = new OverrideBalanceRequest(new BigDecimal("1000.00"));
        mockMvc.perform(patch("/api/pockets/" + pocket.id() + "/balance")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overrideReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(1000.0))
                .andExpect(jsonPath("$.manualCurrentBalance").value(1000.0))
                .andExpect(jsonPath("$.displayBalance").value(1000.0));

        // 3. User B cannot log purchase on User A's pocket -> 404
        com.marsh.pockets.transaction.dto.LogPurchaseRequest logReqUserB =
                new com.marsh.pockets.transaction.dto.LogPurchaseRequest(pocket.id(), new BigDecimal("100.00"), "Unauthorized");
        mockMvc.perform(post("/api/transactions/log")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logReqUserB)))
                .andExpect(status().isNotFound());

        // 4. Log manual purchase of 400.00 -> currentBalance = 600, manualCurrentBalance = 600, displayBalance = 600
        com.marsh.pockets.transaction.dto.LogPurchaseRequest logReq1 =
                new com.marsh.pockets.transaction.dto.LogPurchaseRequest(pocket.id(), new BigDecimal("400.00"), "Coffee & snacks");
        mockMvc.perform(post("/api/transactions/log")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logReq1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("MANUAL_LOG"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.amount").value(400.0));

        mockMvc.perform(get("/api/pockets/" + pocket.id())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(600.0))
                .andExpect(jsonPath("$.manualCurrentBalance").value(600.0))
                .andExpect(jsonPath("$.displayBalance").value(600.0));

        // 5. Log manual purchase of 800.00 (exceeding 600) -> currentBalance clamped at 0, manualCurrentBalance becomes -200, displayBalance becomes -200
        com.marsh.pockets.transaction.dto.LogPurchaseRequest logReq2 =
                new com.marsh.pockets.transaction.dto.LogPurchaseRequest(pocket.id(), new BigDecimal("800.00"), "Big dinner");
        mockMvc.perform(post("/api/transactions/log")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logReq2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("MANUAL_LOG"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(get("/api/pockets/" + pocket.id())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(0.0))
                .andExpect(jsonPath("$.manualCurrentBalance").value(-200.0))
                .andExpect(jsonPath("$.displayBalance").value(-200.0));
    }

    @Test
    @DisplayName("Part B: UPI Deep-Link Generation with encoding, validation, and error states")
    void testUpiDeepLinkGeneration() throws Exception {
        AuthResponse userA = registerUser("9876543210", "Password@123", "User A", 15);
        AuthResponse userB = registerUser("9876543211", "Password@123", "User B", 15);
        String tokenA = "Bearer " + userA.accessToken();
        String tokenB = "Bearer " + userB.accessToken();

        // Create and fund pocket
        CreatePocketRequest createPocket = new CreatePocketRequest("Groceries", new BigDecimal("5000.00"));
        MvcResult pocketResult = mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPocket)))
                .andExpect(status().isCreated())
                .andReturn();
        PocketResponse pocket = objectMapper.readValue(pocketResult.getResponse().getContentAsString(), PocketResponse.class);

        OverrideBalanceRequest overrideReq = new OverrideBalanceRequest(new BigDecimal("2000.00"));
        mockMvc.perform(patch("/api/pockets/" + pocket.id() + "/balance")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overrideReq)))
                .andExpect(status().isOk());

        // Create PENDING IN_APP transaction with payee UPI and note
        CreateTransactionRequest txReq = new CreateTransactionRequest(
                pocket.id(),
                new BigDecimal("450.50"),
                "store@okaxis",
                "Weekly fruit & vegetables",
                UUID.randomUUID().toString()
        );
        MvcResult txResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txReq)))
                .andExpect(status().isCreated())
                .andReturn();
        TransactionResponse tx = objectMapper.readValue(txResult.getResponse().getContentAsString(), TransactionResponse.class);

        // 1. Successful UPI deep link generation
        com.marsh.pockets.payment.dto.GenerateLinkRequest linkReq =
                new com.marsh.pockets.payment.dto.GenerateLinkRequest(tx.id());
        MvcResult linkResult = mockMvc.perform(post("/api/payments/generate-link")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkReq)))
                .andExpect(status().isOk())
                .andReturn();
        com.marsh.pockets.payment.dto.GenerateLinkResponse linkResponse =
                objectMapper.readValue(linkResult.getResponse().getContentAsString(), com.marsh.pockets.payment.dto.GenerateLinkResponse.class);

        assertNotNull(linkResponse.upiDeepLink());
        assertTrue(linkResponse.upiDeepLink().startsWith("upi://pay?pa=store%40okaxis&am=450.50"));
        assertTrue(linkResponse.upiDeepLink().contains("cu=INR"));
        assertTrue(linkResponse.upiDeepLink().contains("tr=" + tx.id()));

        // 2. User B cannot generate link for User A's transaction -> 404
        mockMvc.perform(post("/api/payments/generate-link")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkReq)))
                .andExpect(status().isNotFound());

        // 3. Confirm transaction tx -> Cannot generate link for CONFIRMED transaction -> 409
        mockMvc.perform(post("/api/transactions/" + tx.id() + "/confirm")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments/generate-link")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkReq)))
                .andExpect(status().isConflict());

        // 4. Manual purchase transaction cannot generate UPI link -> 409
        com.marsh.pockets.transaction.dto.LogPurchaseRequest manualReq =
                new com.marsh.pockets.transaction.dto.LogPurchaseRequest(pocket.id(), new BigDecimal("100.00"), "Manual cash");
        MvcResult manualResult = mockMvc.perform(post("/api/transactions/log")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualReq)))
                .andExpect(status().isCreated())
                .andReturn();
        TransactionResponse manualTx = objectMapper.readValue(manualResult.getResponse().getContentAsString(), TransactionResponse.class);

        com.marsh.pockets.payment.dto.GenerateLinkRequest manualLinkReq =
                new com.marsh.pockets.payment.dto.GenerateLinkRequest(manualTx.id());
        mockMvc.perform(post("/api/payments/generate-link")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualLinkReq)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("JPA Optimistic Locking: Concurrent modifications trigger 409 Conflict with clear message")
    void testOptimisticLockingFailureOnConcurrentUpdate() throws Exception {
        AuthResponse userA = registerUser("9876543210", "Password@123", "User A", 15);
        String tokenA = "Bearer " + userA.accessToken();

        // Create pocket
        CreatePocketRequest createPocket = new CreatePocketRequest("Entertainment", new BigDecimal("5000.00"));
        MvcResult pocketResult = mockMvc.perform(post("/api/pockets")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPocket)))
                .andExpect(status().isCreated())
                .andReturn();
        PocketResponse pocket = objectMapper.readValue(pocketResult.getResponse().getContentAsString(), PocketResponse.class);

        // Fetch stale entity at version 0
        com.marsh.pockets.pocket.entity.Pocket stalePocket = pocketRepository.findById(pocket.id()).orElseThrow();
        assertEquals(0L, stalePocket.getVersion());

        // Update pocket via API (increments version in DB to 1)
        OverrideBalanceRequest overrideReq = new OverrideBalanceRequest(new BigDecimal("3000.00"));
        mockMvc.perform(patch("/api/pockets/" + pocket.id() + "/balance")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overrideReq)))
                .andExpect(status().isOk());

        com.marsh.pockets.pocket.entity.Pocket updatedPocket = pocketRepository.findById(pocket.id()).orElseThrow();
        assertEquals(1L, updatedPocket.getVersion());

        // Attempting to save the stale entity (version 0) fails with OptimisticLockingFailureException
        stalePocket.setCurrentBalance(new BigDecimal("1000.00"));
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.OptimisticLockingFailureException.class,
                () -> pocketRepository.saveAndFlush(stalePocket)
        );
    }
}

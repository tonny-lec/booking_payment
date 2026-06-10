package com.booking.payment;

import com.booking.iam.application.port.PasswordEncoder;
import com.booking.iam.application.port.UserRepository;
import com.booking.iam.domain.model.Email;
import com.booking.iam.domain.model.HashedPassword;
import com.booking.iam.domain.model.User;
import com.booking.test.PostgresTestContainerConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test for the payment slice:
 * login -> create booking -> create payment (201) -> idempotent replay (200)
 * -> idempotency conflict (409) -> get payment (200).
 *
 * <p>Runs against a Testcontainers PostgreSQL (Flyway V1..V5 applied) with
 * the real security filter chain and OpenAPI request validation enabled,
 * using the stub payment gateway wired in bootstrap.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfig.class)
@DisplayName("Payment flow E2E")
class PaymentFlowE2ETest {

    private static final KeyPair TEST_KEY_PAIR = generateKeyPair();
    private static final String TEST_PRIVATE_KEY_PEM = toPrivateKeyPem(TEST_KEY_PAIR.getPrivate());
    private static final String TEST_PUBLIC_KEY_PEM = toPublicKeyPem(TEST_KEY_PAIR.getPublic());

    private static final String EMAIL = "payment-e2e@example.com";
    private static final String PASSWORD = "P@ssw0rd123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerJwtKeys(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.private-key-pem", () -> TEST_PRIVATE_KEY_PEM);
        registry.add("app.security.jwt.public-key-pem", () -> TEST_PUBLIC_KEY_PEM);
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM bookings");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");

        userRepository.save(User.create(
                Email.of(EMAIL),
                HashedPassword.of(passwordEncoder.encode(PASSWORD))
        ));
    }

    @Test
    @DisplayName("should create payment idempotently for own booking")
    void shouldCreatePaymentIdempotentlyForOwnBooking() throws Exception {
        String accessToken = login();
        String bookingId = createBooking(accessToken);
        String idempotencyKey = UUID.randomUUID().toString();

        // 1. First request creates the payment (201, AUTHORIZED by stub gateway).
        String createResponse = mockMvc.perform(paymentRequest(accessToken, idempotencyKey, bookingId, 10000))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Idempotency-Key", idempotencyKey))
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.amount").value(10000))
                .andExpect(jsonPath("$.currency").value("JPY"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.gatewayTransactionId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String paymentId = JsonPath.read(createResponse, "$.id");

        // 2. Same key + same content replays the stored result (200, same id).
        mockMvc.perform(paymentRequest(accessToken, idempotencyKey, bookingId, 10000))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(paymentId));

        // 3. Same key + different content is rejected (409).
        mockMvc.perform(paymentRequest(accessToken, idempotencyKey, bookingId, 99999))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("payment_idempotency_key_conflict"));

        // 4. The created payment is retrievable by its owner.
        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        // 5. A second payment for the same booking with a new key is rejected (422).
        mockMvc.perform(paymentRequest(accessToken, UUID.randomUUID().toString(), bookingId, 10000))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("payment_booking_already_paid"));
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(response, "$.accessToken");
        assertThat(accessToken).isNotBlank();
        return accessToken;
    }

    private String createBooking(String accessToken) throws Exception {
        String response = mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": "%s",
                                  "startAt": "2099-01-01T10:00:00Z",
                                  "endAt": "2099-01-01T11:00:00Z",
                                  "note": "payment e2e"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String bookingId = JsonPath.read(response, "$.id");
        assertThat(bookingId).isNotBlank();
        return bookingId;
    }

    private MockHttpServletRequestBuilder paymentRequest(
            String accessToken,
            String idempotencyKey,
            String bookingId,
            int amount
    ) {
        return post("/api/v1/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "bookingId": "%s",
                          "amount": %d,
                          "currency": "JPY",
                          "description": "payment e2e"
                        }
                        """.formatted(bookingId, amount));
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA algorithm is not available", ex);
        }
    }

    private static String toPrivateKeyPem(PrivateKey privateKey) {
        return toPem("PRIVATE KEY", privateKey.getEncoded());
    }

    private static String toPublicKeyPem(PublicKey publicKey) {
        return toPem("PUBLIC KEY", publicKey.getEncoded());
    }

    private static String toPem(String type, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n"
                + base64
                + "\n-----END " + type + "-----";
    }
}

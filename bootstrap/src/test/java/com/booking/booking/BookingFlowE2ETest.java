package com.booking.booking;

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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test for the booking lifecycle:
 * login -> create booking -> update booking -> cancel booking.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfig.class)
@DisplayName("Booking flow E2E")
class BookingFlowE2ETest {

    private static final KeyPair TEST_KEY_PAIR = generateKeyPair();
    private static final String TEST_PRIVATE_KEY_PEM = toPrivateKeyPem(TEST_KEY_PAIR.getPrivate());
    private static final String TEST_PUBLIC_KEY_PEM = toPublicKeyPem(TEST_KEY_PAIR.getPublic());

    private static final String EMAIL = "booking-e2e@example.com";
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
    @DisplayName("should create update and cancel own booking")
    void shouldCreateUpdateAndCancelOwnBooking() throws Exception {
        String accessToken = login();
        String resourceId = UUID.randomUUID().toString();

        String createResponse = mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": "%s",
                                  "startAt": "2099-01-01T10:00:00Z",
                                  "endAt": "2099-01-01T11:00:00Z",
                                  "note": "booking e2e"
                                }
                                """.formatted(resourceId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.resourceId").value(resourceId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn().getResponse().getContentAsString();
        String bookingId = JsonPath.read(createResponse, "$.id");
        assertThat(bookingId).isNotBlank();

        String updateResponse = mockMvc.perform(put("/api/v1/bookings/" + bookingId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2099-01-01T11:00:00Z",
                                  "endAt": "2099-01-01T12:00:00Z",
                                  "note": "booking e2e updated",
                                  "version": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.startAt").value("2099-01-01T11:00:00Z"))
                .andExpect(jsonPath("$.endAt").value("2099-01-01T12:00:00Z"))
                .andExpect(jsonPath("$.note").value("booking e2e updated"))
                .andExpect(jsonPath("$.version").value(3))
                .andReturn().getResponse().getContentAsString();
        Integer updatedVersion = JsonPath.read(updateResponse, "$.version");
        assertThat(updatedVersion).isEqualTo(3);

        mockMvc.perform(delete("/api/v1/bookings/" + bookingId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.version").value(4));
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

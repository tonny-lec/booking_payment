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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfig.class)
@TestPropertySource(properties = {
        "app.openapi.validation.ignored-path-patterns[0]=/api/v1/auth/**",
        "app.openapi.validation.ignored-path-patterns[1]=/actuator/**",
        "app.openapi.validation.ignored-path-patterns[2]=/swagger-ui/**",
        "app.openapi.validation.ignored-path-patterns[3]=/v3/api-docs/**",
        "app.openapi.validation.ignored-path-patterns[4]=/openapi/**",
        "app.openapi.validation.ignored-path-patterns[5]=/error",
        "app.openapi.validation.ignored-path-patterns[6]=/api/v1/bookings/**"
})
@DisplayName("Booking lifecycle E2E")
class BookingLifecycleE2ETest {

    private static final KeyPair TEST_KEY_PAIR = generateKeyPair();
    private static final String TEST_PRIVATE_KEY_PEM = toPrivateKeyPem(TEST_KEY_PAIR.getPrivate());
    private static final String TEST_PUBLIC_KEY_PEM = toPublicKeyPem(TEST_KEY_PAIR.getPublic());

    private static final String EMAIL = "booking-e2e-user@example.com";
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
        jdbcTemplate.update("DELETE FROM bookings");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");

        userRepository.save(User.create(
                Email.of(EMAIL),
                HashedPassword.of(passwordEncoder.encode(PASSWORD))
        ));
    }

    @Test
    @DisplayName("should create, update and cancel booking")
    void shouldCreateUpdateAndCancelBooking() throws Exception {
        String loginResponse = postJson(
                "/api/v1/auth/login",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}",
                null,
                status().isOk()
        );
        String accessToken = JsonPath.read(loginResponse, "$.accessToken");
        assertThat(accessToken).isNotBlank();

        String resourceId = UUID.randomUUID().toString();
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        String createResponse = postJson(
                "/api/v1/bookings",
                "{"
                        + "\"resourceId\":\"" + resourceId + "\","
                        + "\"startAt\":\"" + start + "\","
                        + "\"endAt\":\"" + end + "\","
                        + "\"note\":\"initial\""
                        + "}",
                "Bearer " + accessToken,
                status().isCreated()
        );

        String bookingId = JsonPath.read(createResponse, "$.id");
        Integer version = JsonPath.read(createResponse, "$.version");
        assertThat(bookingId).isNotBlank();
        assertThat(version).isNotNull();

        Instant updatedStart = start.plusSeconds(1800);
        Instant updatedEnd = end.plusSeconds(1800);
        String updateResponse = putJson(
                "/api/v1/bookings/" + bookingId,
                "{"
                        + "\"startAt\":\"" + updatedStart + "\","
                        + "\"endAt\":\"" + updatedEnd + "\","
                        + "\"note\":\"updated\","
                        + "\"version\":" + version
                        + "}",
                "Bearer " + accessToken,
                status().isOk()
        );
        Integer updatedVersion = JsonPath.read(updateResponse, "$.version");
        assertThat(updatedVersion).isGreaterThan(version);

        String cancelResponse = deleteJson(
                "/api/v1/bookings/" + bookingId,
                "Bearer " + accessToken,
                status().isOk()
        );
        String statusCode = JsonPath.read(cancelResponse, "$.status");
        assertThat(statusCode).isEqualTo("CANCELLED");
    }

    private String postJson(
            String path,
            String body,
            String authorizationHeader,
            org.springframework.test.web.servlet.ResultMatcher statusMatcher
    ) throws Exception {
        var requestBuilder = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (authorizationHeader != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        var result = mockMvc.perform(requestBuilder).andExpect(statusMatcher).andReturn();
        String responseBody = result.getResponse().getContentAsString();
        return responseBody == null ? "" : responseBody;
    }

    private String putJson(
            String path,
            String body,
            String authorizationHeader,
            org.springframework.test.web.servlet.ResultMatcher statusMatcher
    ) throws Exception {
        var requestBuilder = put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (authorizationHeader != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        var result = mockMvc.perform(requestBuilder).andExpect(statusMatcher).andReturn();
        String responseBody = result.getResponse().getContentAsString();
        return responseBody == null ? "" : responseBody;
    }

    private String deleteJson(
            String path,
            String authorizationHeader,
            org.springframework.test.web.servlet.ResultMatcher statusMatcher
    ) throws Exception {
        var requestBuilder = delete(path);
        if (authorizationHeader != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        var result = mockMvc.perform(requestBuilder).andExpect(statusMatcher).andReturn();
        String responseBody = result.getResponse().getContentAsString();
        return responseBody == null ? "" : responseBody;
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

package com.booking.iam;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfig.class)
@DisplayName("IAM auth flow E2E")
class IamAuthFlowE2ETest {

    private static final KeyPair TEST_KEY_PAIR = generateKeyPair();
    private static final String TEST_PRIVATE_KEY_PEM = toPrivateKeyPem(TEST_KEY_PAIR.getPrivate());
    private static final String TEST_PUBLIC_KEY_PEM = toPublicKeyPem(TEST_KEY_PAIR.getPublic());

    private static final String EMAIL = "e2e-user@example.com";
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
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");

        userRepository.save(User.create(
                Email.of(EMAIL),
                HashedPassword.of(passwordEncoder.encode(PASSWORD))
        ));
    }

    @Test
    @DisplayName("should reject refresh token after login refresh logout sequence")
    void shouldRejectRefreshTokenAfterLogout() throws Exception {
        String loginResponse = postJson(
                "/api/v1/auth/login",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}",
                null,
                status().isOk()
        );

        String initialRefreshToken = JsonPath.read(loginResponse, "$.refreshToken");
        assertThat(initialRefreshToken).isNotBlank();
        assertThat((String) JsonPath.read(loginResponse, "$.tokenType")).isEqualTo("Bearer");

        String refreshResponse = postJson(
                "/api/v1/auth/refresh",
                "{\"refreshToken\":\"" + initialRefreshToken + "\"}",
                null,
                status().isOk()
        );

        String rotatedAccessToken = JsonPath.read(refreshResponse, "$.accessToken");
        String rotatedRefreshToken = JsonPath.read(refreshResponse, "$.refreshToken");
        assertThat(rotatedAccessToken).isNotBlank();
        assertThat(rotatedRefreshToken).isNotBlank();
        assertThat(rotatedRefreshToken).isNotEqualTo(initialRefreshToken);

        postJson(
                "/api/v1/auth/logout",
                "{\"refreshToken\":\"" + rotatedRefreshToken + "\"}",
                "Bearer " + rotatedAccessToken,
                status().isNoContent()
        );

        postJson(
                "/api/v1/auth/refresh",
                "{\"refreshToken\":\"" + rotatedRefreshToken + "\"}",
                null,
                status().isUnauthorized(),
                jsonPath("$.errorCode").value("invalid_credentials")
        );
    }

    private String postJson(
            String path,
            String body,
            String authorizationHeader,
            org.springframework.test.web.servlet.ResultMatcher statusMatcher,
            org.springframework.test.web.servlet.ResultMatcher... additionalMatchers
    ) throws Exception {
        var requestBuilder = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);

        if (authorizationHeader != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        var resultActions = mockMvc.perform(requestBuilder).andExpect(statusMatcher);
        for (var matcher : additionalMatchers) {
            resultActions.andExpect(matcher);
        }

        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
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

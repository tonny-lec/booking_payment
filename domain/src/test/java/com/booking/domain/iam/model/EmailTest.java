package com.booking.domain.iam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Email} value object.
 */
@DisplayName("Email")
class EmailTest {

    @Nested
    @DisplayName("Valid email creation")
    class ValidEmailCreation {

        @Test
        @DisplayName("should create Email from valid address")
        void shouldCreateEmailFromValidAddress() {
            // When
            Email email = Email.of("user@example.com");

            // Then
            assertThat(email).isNotNull();
            assertThat(email.value()).isEqualTo("user@example.com");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "simple@example.com",
                "very.common@example.com",
                "disposable.style.email.with+symbol@example.com",
                "other.email-with-hyphen@example.com",
                "fully-qualified-domain@example.com",
                "user.name+tag+sorting@example.com",
                "x@example.com",
                "example-indeed@strange-example.com",
                "test@subdomain.example.com",
                "user123@example123.com",
                "USER@EXAMPLE.CO.JP"
        })
        @DisplayName("should accept valid email formats")
        void shouldAcceptValidEmailFormats(String validEmail) {
            // When
            Email email = Email.of(validEmail);

            // Then
            assertThat(email).isNotNull();
            assertThat(email.value()).isEqualTo(validEmail.toLowerCase());
        }

        @Test
        @DisplayName("should normalize email to lowercase")
        void shouldNormalizeToLowercase() {
            // When
            Email email = Email.of("User@Example.COM");

            // Then
            assertThat(email.value()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("should trim whitespace")
        void shouldTrimWhitespace() {
            // When
            Email email = Email.of("  user@example.com  ");

            // Then
            assertThat(email.value()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("Invalid email rejection")
    class InvalidEmailRejection {

        @Test
        @DisplayName("should reject null email")
        void shouldRejectNullEmail() {
            assertThatThrownBy(() -> Email.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("should reject empty email")
        void shouldRejectEmptyEmail() {
            assertThatThrownBy(() -> Email.of(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be empty");
        }

        @Test
        @DisplayName("should reject whitespace-only email")
        void shouldRejectWhitespaceOnlyEmail() {
            assertThatThrownBy(() -> Email.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be empty");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "plainaddress",           // Missing @ and domain
                "@no-local-part.com",     // Missing local part
                "no-at-sign.com",         // Missing @
                "no-domain@",             // Missing domain
                "spaces in@local.com",    // Spaces in local part
                "user@",                  // Missing domain after @
                "@.com",                  // Missing local part and invalid domain
                "user@.com",              // Domain starts with dot
                "user@com",               // Domain without dot
                "a\"b(c)d,e:f;g<h>i[j\\k]l@example.com"  // Special characters
        })
        @DisplayName("should reject invalid email formats")
        void shouldRejectInvalidEmailFormats(String invalidEmail) {
            assertThatThrownBy(() -> Email.of(invalidEmail))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("format is invalid");
        }

        @Test
        @DisplayName("should reject email exceeding max length")
        void shouldRejectEmailExceedingMaxLength() {
            // Given - Create email with 256 characters (exceeds 255 limit)
            // @example.com = 12 characters, so local part needs 244 chars for total of 256
            String longLocalPart = "a".repeat(244);
            String longEmail = longLocalPart + "@example.com";
            assertThat(longEmail).hasSize(256); // Verify our calculation

            // Then
            assertThatThrownBy(() -> Email.of(longEmail))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not exceed " + Email.MAX_LENGTH);
        }

        @Test
        @DisplayName("should accept email at max length boundary")
        void shouldAcceptEmailAtMaxLength() {
            // Given - Create email with exactly 255 characters
            // @example.com = 12 characters, so local part needs 243 chars for total of 255
            String longLocalPart = "a".repeat(243);
            String maxLengthEmail = longLocalPart + "@example.com";
            assertThat(maxLengthEmail).hasSize(255); // Verify our calculation

            // When
            Email email = Email.of(maxLengthEmail);

            // Then
            assertThat(email.value()).hasSize(255);
        }
    }

    @Nested
    @DisplayName("Email parts extraction")
    class EmailPartsExtraction {

        @Test
        @DisplayName("should extract local part")
        void shouldExtractLocalPart() {
            // Given
            Email email = Email.of("user.name@example.com");

            // Then
            assertThat(email.localPart()).isEqualTo("user.name");
        }

        @Test
        @DisplayName("should extract domain")
        void shouldExtractDomain() {
            // Given
            Email email = Email.of("user@subdomain.example.com");

            // Then
            assertThat(email.domain()).isEqualTo("subdomain.example.com");
        }
    }

    @Nested
    @DisplayName("PII Masking")
    class PiiMasking {

        @Test
        @DisplayName("should mask email for logging")
        void shouldMaskEmailForLogging() {
            // Given
            Email email = Email.of("user@example.com");

            // Then
            assertThat(email.masked()).isEqualTo("u***@example.com");
        }

        @Test
        @DisplayName("should mask single character local part")
        void shouldMaskSingleCharacterLocalPart() {
            // Given
            Email email = Email.of("a@example.com");

            // Then
            assertThat(email.masked()).isEqualTo("a***@example.com");
        }

        @Test
        @DisplayName("toString should use masked email")
        void toStringShouldUseMaskedEmail() {
            // Given
            Email email = Email.of("user@example.com");

            // Then
            assertThat(email.toString())
                    .contains("Email")
                    .contains("u***@example.com")
                    .doesNotContain("user@example.com");
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        @DisplayName("should be equal for same email (case-insensitive)")
        void shouldBeEqualForSameEmailCaseInsensitive() {
            // Given
            Email email1 = Email.of("user@example.com");
            Email email2 = Email.of("USER@EXAMPLE.COM");

            // Then
            assertThat(email1).isEqualTo(email2);
            assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different emails")
        void shouldNotBeEqualForDifferentEmails() {
            // Given
            Email email1 = Email.of("user1@example.com");
            Email email2 = Email.of("user2@example.com");

            // Then
            assertThat(email1).isNotEqualTo(email2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            Email email = Email.of("user@example.com");

            // Then
            assertThat(email.equals(null)).isFalse();
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            // Given
            Email email = Email.of("user@example.com");

            // Then
            assertThat(email.equals("user@example.com")).isFalse();
        }

        @Test
        @DisplayName("hashCode should be consistent")
        void hashCodeShouldBeConsistent() {
            // Given
            Email email = Email.of("user@example.com");

            // Then
            int hash1 = email.hashCode();
            int hash2 = email.hashCode();
            assertThat(hash1).isEqualTo(hash2);
        }
    }
}

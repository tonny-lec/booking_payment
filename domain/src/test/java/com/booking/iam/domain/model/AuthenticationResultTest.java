package com.booking.iam.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AuthenticationResult}.
 */
@DisplayName("AuthenticationResult")
class AuthenticationResultTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("success() should create successful result")
        void successShouldCreateSuccessfulResult() {
            // When
            AuthenticationResult result = AuthenticationResult.success();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isFailure()).isFalse();
            assertThat(result.failureReason()).isEmpty();
        }

        @Test
        @DisplayName("failure() should create failed result with reason")
        void failureShouldCreateFailedResultWithReason() {
            // When
            AuthenticationResult result = AuthenticationResult.failure(
                    AuthenticationFailureReason.INVALID_CREDENTIALS);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.failureReason())
                    .contains(AuthenticationFailureReason.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("failure() should throw for null reason")
        void failureShouldThrowForNullReason() {
            assertThatThrownBy(() -> AuthenticationResult.failure(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        @DisplayName("equals() should return true for same success")
        void equalsShouldReturnTrueForSameSuccess() {
            // Given
            AuthenticationResult result1 = AuthenticationResult.success();
            AuthenticationResult result2 = AuthenticationResult.success();

            // Then
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        @DisplayName("equals() should return true for same failure reason")
        void equalsShouldReturnTrueForSameFailureReason() {
            // Given
            AuthenticationResult result1 = AuthenticationResult.failure(
                    AuthenticationFailureReason.ACCOUNT_LOCKED);
            AuthenticationResult result2 = AuthenticationResult.failure(
                    AuthenticationFailureReason.ACCOUNT_LOCKED);

            // Then
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        @DisplayName("equals() should return false for different failure reasons")
        void equalsShouldReturnFalseForDifferentReasons() {
            // Given
            AuthenticationResult result1 = AuthenticationResult.failure(
                    AuthenticationFailureReason.ACCOUNT_LOCKED);
            AuthenticationResult result2 = AuthenticationResult.failure(
                    AuthenticationFailureReason.INVALID_CREDENTIALS);

            // Then
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("equals() should return false for success vs failure")
        void equalsShouldReturnFalseForSuccessVsFailure() {
            // Given
            AuthenticationResult success = AuthenticationResult.success();
            AuthenticationResult failure = AuthenticationResult.failure(
                    AuthenticationFailureReason.INVALID_CREDENTIALS);

            // Then
            assertThat(success).isNotEqualTo(failure);
        }

        @Test
        @DisplayName("hashCode() should be consistent with equals()")
        void hashCodeShouldBeConsistentWithEquals() {
            // Given
            AuthenticationResult result1 = AuthenticationResult.success();
            AuthenticationResult result2 = AuthenticationResult.success();

            // Then
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }
    }

    @Nested
    @DisplayName("String representation")
    class StringRepresentation {

        @Test
        @DisplayName("toString() should indicate success")
        void toStringShouldIndicateSuccess() {
            // Given
            AuthenticationResult result = AuthenticationResult.success();

            // Then
            assertThat(result.toString()).contains("success");
        }

        @Test
        @DisplayName("toString() should indicate failure with reason")
        void toStringShouldIndicateFailureWithReason() {
            // Given
            AuthenticationResult result = AuthenticationResult.failure(
                    AuthenticationFailureReason.ACCOUNT_LOCKED);

            // Then
            assertThat(result.toString())
                    .contains("failure")
                    .contains("ACCOUNT_LOCKED");
        }
    }
}

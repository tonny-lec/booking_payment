package com.booking.domain.iam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiPredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link User} aggregate root.
 */
@DisplayName("User")
class UserTest {

    private static final String VALID_BCRYPT_HASH = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYlQBc/G2HHe";
    private static final BiPredicate<String, String> MATCHING_PASSWORD = (raw, hash) -> true;
    private static final BiPredicate<String, String> NON_MATCHING_PASSWORD = (raw, hash) -> false;

    private Email validEmail;
    private HashedPassword validPasswordHash;

    @BeforeEach
    void setUp() {
        validEmail = Email.of("user@example.com");
        validPasswordHash = HashedPassword.of(VALID_BCRYPT_HASH);
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("create() should create a new active user with generated ID")
        void createShouldCreateNewActiveUser() {
            // When
            User user = User.create(validEmail, validPasswordHash);

            // Then
            assertThat(user.id()).isNotNull();
            assertThat(user.email()).isEqualTo(validEmail);
            assertThat(user.passwordHash()).isEqualTo(validPasswordHash);
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.failedLoginAttempts()).isZero();
            assertThat(user.createdAt()).isNotNull();
            assertThat(user.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("create() should throw NullPointerException for null email")
        void createShouldThrowExceptionForNullEmail() {
            assertThatThrownBy(() -> User.create(null, validPasswordHash))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("create() should throw NullPointerException for null password")
        void createShouldThrowExceptionForNullPassword() {
            assertThatThrownBy(() -> User.create(validEmail, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("passwordHash");
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builder should create user with all fields")
        void builderShouldCreateUserWithAllFields() {
            // Given
            UserId id = UserId.generate();
            Instant now = Instant.now();
            Instant createdAt = now.minus(Duration.ofDays(1));

            // When
            User user = User.builder()
                    .id(id)
                    .email(validEmail)
                    .passwordHash(validPasswordHash)
                    .status(UserStatus.LOCKED)
                    .failedLoginAttempts(3)
                    .lastFailedLoginAt(now)
                    .lockedUntil(now.plus(Duration.ofHours(1)))
                    .createdAt(createdAt)
                    .updatedAt(now)
                    .build();

            // Then
            assertThat(user.id()).isEqualTo(id);
            assertThat(user.email()).isEqualTo(validEmail);
            assertThat(user.status()).isEqualTo(UserStatus.LOCKED);
            assertThat(user.failedLoginAttempts()).isEqualTo(3);
            assertThat(user.lastFailedLoginAt()).isEqualTo(now);
            assertThat(user.lockedUntil()).isNotNull();
            assertThat(user.createdAt()).isEqualTo(createdAt);
            assertThat(user.updatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("builder should default status to ACTIVE")
        void builderShouldDefaultStatusToActive() {
            // When
            User user = User.builder()
                    .id(UserId.generate())
                    .email(validEmail)
                    .passwordHash(validPasswordHash)
                    .build();

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("builder should throw for negative failedLoginAttempts")
        void builderShouldThrowForNegativeFailedAttempts() {
            assertThatThrownBy(() -> User.builder()
                    .id(UserId.generate())
                    .email(validEmail)
                    .passwordHash(validPasswordHash)
                    .failedLoginAttempts(-1)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-negative");
        }
    }

    @Nested
    @DisplayName("Authentication")
    class Authentication {

        @Test
        @DisplayName("authenticate() should succeed with valid password")
        void authenticateShouldSucceedWithValidPassword() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // When
            AuthenticationResult result = user.authenticate("password", MATCHING_PASSWORD);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.failureReason()).isEmpty();
            assertThat(user.failedLoginAttempts()).isZero();
        }

        @Test
        @DisplayName("authenticate() should fail with invalid password")
        void authenticateShouldFailWithInvalidPassword() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // When
            AuthenticationResult result = user.authenticate("wrong", NON_MATCHING_PASSWORD);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.failureReason()).contains(AuthenticationFailureReason.INVALID_CREDENTIALS);
            assertThat(user.failedLoginAttempts()).isEqualTo(1);
            assertThat(user.lastFailedLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("authenticate() should reset failed attempts on success")
        void authenticateShouldResetFailedAttemptsOnSuccess() {
            // Given
            User user = User.builder()
                    .id(UserId.generate())
                    .email(validEmail)
                    .passwordHash(validPasswordHash)
                    .status(UserStatus.ACTIVE)
                    .failedLoginAttempts(3)
                    .lastFailedLoginAt(Instant.now())
                    .build();

            // When
            user.authenticate("password", MATCHING_PASSWORD);

            // Then
            assertThat(user.failedLoginAttempts()).isZero();
            assertThat(user.lastFailedLoginAt()).isNull();
        }

        @Test
        @DisplayName("authenticate() should fail for locked account")
        void authenticateShouldFailForLockedAccount() {
            // Given
            User user = User.create(validEmail, validPasswordHash);
            user.lock(Duration.ofHours(1));

            // When
            AuthenticationResult result = user.authenticate("password", MATCHING_PASSWORD);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.failureReason()).contains(AuthenticationFailureReason.ACCOUNT_LOCKED);
        }

        @Test
        @DisplayName("authenticate() should fail for suspended account")
        void authenticateShouldFailForSuspendedAccount() {
            // Given
            User user = User.create(validEmail, validPasswordHash);
            user.suspend();

            // When
            AuthenticationResult result = user.authenticate("password", MATCHING_PASSWORD);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.failureReason()).contains(AuthenticationFailureReason.ACCOUNT_NOT_ACTIVE);
        }

        @Test
        @DisplayName("authenticate() should throw for null password")
        void authenticateShouldThrowForNullPassword() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // Then
            assertThatThrownBy(() -> user.authenticate(null, MATCHING_PASSWORD))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("authenticate() should throw for null matcher")
        void authenticateShouldThrowForNullMatcher() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // Then
            assertThatThrownBy(() -> user.authenticate("password", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Auto-lock on failed attempts")
    class AutoLock {

        @Test
        @DisplayName("should lock account after max failed attempts")
        void shouldLockAccountAfterMaxFailedAttempts() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // When - fail login 5 times
            for (int i = 0; i < User.DEFAULT_MAX_FAILED_ATTEMPTS; i++) {
                user.authenticate("wrong", NON_MATCHING_PASSWORD);
            }

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.LOCKED);
            assertThat(user.isCurrentlyLocked()).isTrue();
            assertThat(user.lockedUntil()).isNotNull();
        }

        @Test
        @DisplayName("should not lock before reaching max attempts")
        void shouldNotLockBeforeMaxAttempts() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // When - fail login 4 times
            for (int i = 0; i < User.DEFAULT_MAX_FAILED_ATTEMPTS - 1; i++) {
                user.authenticate("wrong", NON_MATCHING_PASSWORD);
            }

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.failedLoginAttempts()).isEqualTo(User.DEFAULT_MAX_FAILED_ATTEMPTS - 1);
        }
    }

    @Nested
    @DisplayName("Lock/Unlock")
    class LockUnlock {

        @Test
        @DisplayName("lock() should set status to LOCKED")
        void lockShouldSetStatusToLocked() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // When
            user.lock(Duration.ofHours(1));

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.LOCKED);
            assertThat(user.lockedUntil()).isAfter(Instant.now());
            assertThat(user.isCurrentlyLocked()).isTrue();
        }

        @Test
        @DisplayName("lock() should throw for null duration")
        void lockShouldThrowForNullDuration() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // Then
            assertThatThrownBy(() -> user.lock(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lock() should throw for zero duration")
        void lockShouldThrowForZeroDuration() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // Then
            assertThatThrownBy(() -> user.lock(Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("lockIndefinitely() should lock without expiry")
        void lockIndefinitelyShouldLockWithoutExpiry() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // When
            user.lockIndefinitely();

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.LOCKED);
            assertThat(user.lockedUntil()).isNull();
            assertThat(user.isCurrentlyLocked()).isTrue();
        }

        @Test
        @DisplayName("unlock() should set status to ACTIVE")
        void unlockShouldSetStatusToActive() {
            // Given
            User user = User.create(validEmail, validPasswordHash);
            user.lock(Duration.ofHours(1));

            // When
            user.unlock();

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.lockedUntil()).isNull();
            assertThat(user.failedLoginAttempts()).isZero();
            assertThat(user.isCurrentlyLocked()).isFalse();
        }

        @Test
        @DisplayName("unlock() should be idempotent for active account")
        void unlockShouldBeIdempotentForActiveAccount() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // When
            user.unlock();

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("isCurrentlyLocked() should return false for expired lock")
        void isCurrentlyLockedShouldReturnFalseForExpiredLock() {
            // Given
            User user = User.builder()
                    .id(UserId.generate())
                    .email(validEmail)
                    .passwordHash(validPasswordHash)
                    .status(UserStatus.LOCKED)
                    .lockedUntil(Instant.now().minus(Duration.ofHours(1)))
                    .build();

            // Then
            assertThat(user.isCurrentlyLocked()).isFalse();
        }
    }

    @Nested
    @DisplayName("Suspend/Reactivate")
    class SuspendReactivate {

        @Test
        @DisplayName("suspend() should set status to SUSPENDED")
        void suspendShouldSetStatusToSuspended() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // When
            user.suspend();

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("reactivate() should set status to ACTIVE")
        void reactivateShouldSetStatusToActive() {
            // Given
            User user = User.create(validEmail, validPasswordHash);
            user.suspend();

            // When
            user.reactivate();

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.failedLoginAttempts()).isZero();
        }

        @Test
        @DisplayName("reactivate() should throw for non-suspended account")
        void reactivateShouldThrowForNonSuspendedAccount() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // Then
            assertThatThrownBy(() -> user.reactivate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("suspended");
        }
    }

    @Nested
    @DisplayName("Email/Password changes")
    class EmailPasswordChanges {

        @Test
        @DisplayName("changeEmail() should update email")
        void changeEmailShouldUpdateEmail() {
            // Given
            User user = User.create(validEmail, validPasswordHash);
            Email newEmail = Email.of("newemail@example.com");

            // When
            user.changeEmail(newEmail);

            // Then
            assertThat(user.email()).isEqualTo(newEmail);
        }

        @Test
        @DisplayName("changeEmail() should throw for null email")
        void changeEmailShouldThrowForNullEmail() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // Then
            assertThatThrownBy(() -> user.changeEmail(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("changePassword() should update password and reset failed attempts")
        void changePasswordShouldUpdatePasswordAndResetAttempts() {
            // Given
            User user = User.builder()
                    .id(UserId.generate())
                    .email(validEmail)
                    .passwordHash(validPasswordHash)
                    .status(UserStatus.ACTIVE)
                    .failedLoginAttempts(3)
                    .lastFailedLoginAt(Instant.now())
                    .build();
            HashedPassword newHash = HashedPassword.of("$2a$12$DifferentHashValueHereXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

            // When
            user.changePassword(newHash);

            // Then
            assertThat(user.passwordHash()).isEqualTo(newHash);
            assertThat(user.failedLoginAttempts()).isZero();
            assertThat(user.lastFailedLoginAt()).isNull();
        }

        @Test
        @DisplayName("changePassword() should unlock locked account")
        void changePasswordShouldUnlockLockedAccount() {
            // Given
            User user = User.create(validEmail, validPasswordHash);
            user.lock(Duration.ofHours(1));
            HashedPassword newHash = HashedPassword.of("$2a$12$DifferentHashValueHereXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

            // When
            user.changePassword(newHash);

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.lockedUntil()).isNull();
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        @DisplayName("equals() should be based on ID")
        void equalsShouldBeBasedOnId() {
            // Given
            UserId id = UserId.generate();
            User user1 = User.builder()
                    .id(id)
                    .email(validEmail)
                    .passwordHash(validPasswordHash)
                    .build();
            User user2 = User.builder()
                    .id(id)
                    .email(Email.of("different@example.com"))
                    .passwordHash(validPasswordHash)
                    .build();

            // Then
            assertThat(user1).isEqualTo(user2);
        }

        @Test
        @DisplayName("equals() should return false for different ID")
        void equalsShouldReturnFalseForDifferentId() {
            // Given
            User user1 = User.create(validEmail, validPasswordHash);
            User user2 = User.create(validEmail, validPasswordHash);

            // Then
            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("hashCode() should be consistent with equals()")
        void hashCodeShouldBeConsistentWithEquals() {
            // Given
            UserId id = UserId.generate();
            User user1 = User.builder()
                    .id(id)
                    .email(validEmail)
                    .passwordHash(validPasswordHash)
                    .build();
            User user2 = User.builder()
                    .id(id)
                    .email(validEmail)
                    .passwordHash(validPasswordHash)
                    .build();

            // Then
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("toString() should mask email")
        void toStringShouldMaskEmail() {
            // Given
            User user = User.create(validEmail, validPasswordHash);

            // When
            String result = user.toString();

            // Then
            assertThat(result).contains("User");
            assertThat(result).contains("u***@example.com");
            assertThat(result).doesNotContain("user@example.com");
        }
    }
}

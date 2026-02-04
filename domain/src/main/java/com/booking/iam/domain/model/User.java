package com.booking.iam.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Aggregate root representing a user in the IAM bounded context.
 *
 * <p>The User aggregate manages authentication state, including password verification,
 * failed login tracking, and account locking for brute-force protection.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>Email must be unique across users (enforced at repository level)</li>
 *   <li>Password hash never contains plaintext (enforced by HashedPassword)</li>
 *   <li>Failed login attempts must be non-negative</li>
 *   <li>When status is LOCKED and lockedUntil is in the future, login is blocked</li>
 * </ul>
 *
 * <h2>Behaviors</h2>
 * <ul>
 *   <li>{@link #authenticate} - Verify password and update authentication state</li>
 *   <li>{@link #lock} - Lock the account for a specified duration</li>
 *   <li>{@link #unlock} - Manually unlock the account</li>
 * </ul>
 *
 * @see UserId
 * @see Email
 * @see HashedPassword
 * @see UserStatus
 */
public class User {

    /**
     * Default maximum failed login attempts before account lock.
     */
    public static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;

    /**
     * Default lock duration after exceeding max failed attempts.
     */
    public static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(30);

    private final UserId id;
    private Email email;
    private HashedPassword passwordHash;
    private UserStatus status;
    private int failedLoginAttempts;
    private Instant lastFailedLoginAt;
    private Instant lockedUntil;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "User id must not be null");
        this.email = Objects.requireNonNull(builder.email, "User email must not be null");
        this.passwordHash = Objects.requireNonNull(builder.passwordHash, "User passwordHash must not be null");
        this.status = builder.status != null ? builder.status : UserStatus.ACTIVE;
        this.failedLoginAttempts = builder.failedLoginAttempts;
        this.lastFailedLoginAt = builder.lastFailedLoginAt;
        this.lockedUntil = builder.lockedUntil;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : this.createdAt;

        validateInvariants();
    }

    private void validateInvariants() {
        if (failedLoginAttempts < 0) {
            throw new IllegalArgumentException("Failed login attempts must be non-negative");
        }
    }

    /**
     * Creates a new User with a generated ID.
     *
     * @param email the user's email address
     * @param passwordHash the user's hashed password
     * @return a new User instance
     */
    public static User create(Email email, HashedPassword passwordHash) {
        return builder()
                .id(UserId.generate())
                .email(email)
                .passwordHash(passwordHash)
                .status(UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .build();
    }

    /**
     * Authenticates the user with the provided password.
     *
     * <p>This method handles the complete authentication flow:
     * <ol>
     *   <li>Check if the user can authenticate (status allows it)</li>
     *   <li>Check if account is currently locked</li>
     *   <li>Verify the password</li>
     *   <li>Update authentication state based on result</li>
     * </ol>
     *
     * @param rawPassword the plaintext password to verify
     * @param passwordMatcher function that compares raw password with hash
     * @return the result of the authentication attempt
     * @throws NullPointerException if rawPassword or passwordMatcher is null
     */
    public AuthenticationResult authenticate(String rawPassword, BiPredicate<String, String> passwordMatcher) {
        Objects.requireNonNull(rawPassword, "Raw password must not be null");
        Objects.requireNonNull(passwordMatcher, "Password matcher must not be null");

        // Check if account is currently locked (prioritize lock status for clearer feedback)
        if (isCurrentlyLocked()) {
            return AuthenticationResult.failure(AuthenticationFailureReason.ACCOUNT_LOCKED);
        }

        // Check if user status allows authentication (suspended, deactivated, etc.)
        if (!status.canAuthenticate()) {
            return AuthenticationResult.failure(AuthenticationFailureReason.ACCOUNT_NOT_ACTIVE);
        }

        // Verify password
        if (passwordHash.matches(rawPassword, passwordMatcher)) {
            onSuccessfulLogin();
            return AuthenticationResult.success();
        } else {
            onFailedLogin();
            return AuthenticationResult.failure(AuthenticationFailureReason.INVALID_CREDENTIALS);
        }
    }

    private void onSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
        this.updatedAt = Instant.now();
        // Domain event: UserLoggedIn would be raised here
    }

    private void onFailedLogin() {
        this.failedLoginAttempts++;
        this.lastFailedLoginAt = Instant.now();
        this.updatedAt = Instant.now();

        // Auto-lock after too many failed attempts
        if (failedLoginAttempts >= DEFAULT_MAX_FAILED_ATTEMPTS) {
            lock(DEFAULT_LOCK_DURATION);
            // Domain event: AccountLocked would be raised here
        }
        // Domain event: LoginFailed would be raised here
    }

    /**
     * Checks if the account is currently locked.
     *
     * <p>An account is considered locked if:
     * <ul>
     *   <li>Status is LOCKED, AND</li>
     *   <li>lockedUntil is set and is in the future</li>
     * </ul>
     *
     * @return true if the account is currently locked
     */
    public boolean isCurrentlyLocked() {
        if (status != UserStatus.LOCKED) {
            return false;
        }

        if (lockedUntil == null) {
            return true; // Indefinitely locked
        }

        return Instant.now().isBefore(lockedUntil);
    }

    /**
     * Locks the account for the specified duration.
     *
     * @param duration the duration to lock the account
     * @throws NullPointerException if duration is null
     * @throws IllegalArgumentException if duration is negative or zero
     */
    public void lock(Duration duration) {
        Objects.requireNonNull(duration, "Lock duration must not be null");

        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Lock duration must be positive");
        }

        this.status = UserStatus.LOCKED;
        this.lockedUntil = Instant.now().plus(duration);
        this.updatedAt = Instant.now();
    }

    /**
     * Locks the account indefinitely until manual unlock.
     */
    public void lockIndefinitely() {
        this.status = UserStatus.LOCKED;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Unlocks the account and resets failed login attempts.
     *
     * <p>This method can be called:
     * <ul>
     *   <li>Manually by an administrator</li>
     *   <li>After a password reset</li>
     *   <li>After the lock duration has expired (optional auto-unlock)</li>
     * </ul>
     */
    public void unlock() {
        if (status != UserStatus.LOCKED) {
            return; // Already unlocked or in another state
        }

        this.status = UserStatus.ACTIVE;
        this.lockedUntil = null;
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Suspends the account.
     *
     * <p>Suspended accounts require administrator intervention to reactivate.
     */
    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    /**
     * Reactivates a suspended account.
     *
     * @throws IllegalStateException if the account is not suspended
     */
    public void reactivate() {
        if (status != UserStatus.SUSPENDED) {
            throw new IllegalStateException("Can only reactivate suspended accounts");
        }

        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Changes the user's email address.
     *
     * @param newEmail the new email address
     * @throws NullPointerException if newEmail is null
     */
    public void changeEmail(Email newEmail) {
        Objects.requireNonNull(newEmail, "New email must not be null");
        this.email = newEmail;
        this.updatedAt = Instant.now();
    }

    /**
     * Changes the user's password.
     *
     * <p>This also resets failed login attempts and unlocks the account if locked.
     *
     * @param newPasswordHash the new password hash
     * @throws NullPointerException if newPasswordHash is null
     */
    public void changePassword(HashedPassword newPasswordHash) {
        Objects.requireNonNull(newPasswordHash, "New password hash must not be null");
        this.passwordHash = newPasswordHash;
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;

        // Password change unlocks the account
        if (status == UserStatus.LOCKED) {
            this.status = UserStatus.ACTIVE;
            this.lockedUntil = null;
        }

        this.updatedAt = Instant.now();
    }

    // Getters

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public HashedPassword passwordHash() {
        return passwordHash;
    }

    public UserStatus status() {
        return status;
    }

    public int failedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant lastFailedLoginAt() {
        return lastFailedLoginAt;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    // Builder

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating User instances.
     *
     * <p>Use this builder when reconstructing a User from persistence
     * or when creating a User with specific initial values.
     */
    public static class Builder {
        private UserId id;
        private Email email;
        private HashedPassword passwordHash;
        private UserStatus status;
        private int failedLoginAttempts;
        private Instant lastFailedLoginAt;
        private Instant lockedUntil;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {}

        public Builder id(UserId id) {
            this.id = id;
            return this;
        }

        public Builder email(Email email) {
            this.email = email;
            return this;
        }

        public Builder passwordHash(HashedPassword passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public Builder failedLoginAttempts(int failedLoginAttempts) {
            this.failedLoginAttempts = failedLoginAttempts;
            return this;
        }

        public Builder lastFailedLoginAt(Instant lastFailedLoginAt) {
            this.lastFailedLoginAt = lastFailedLoginAt;
            return this;
        }

        public Builder lockedUntil(Instant lockedUntil) {
            this.lockedUntil = lockedUntil;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", email=" + email.masked() + ", status=" + status + "]";
    }
}

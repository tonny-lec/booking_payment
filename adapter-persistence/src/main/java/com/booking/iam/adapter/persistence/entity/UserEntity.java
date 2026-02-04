package com.booking.iam.adapter.persistence.entity;

import com.booking.iam.domain.model.Email;
import com.booking.iam.domain.model.HashedPassword;
import com.booking.iam.domain.model.User;
import com.booking.iam.domain.model.UserId;
import com.booking.iam.domain.model.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping for IAM users.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "last_failed_login_at")
    private Instant lastFailedLoginAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    public static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.id().value();
        entity.email = user.email().value();
        entity.passwordHash = user.passwordHash().value();
        entity.status = user.status();
        entity.failedLoginAttempts = user.failedLoginAttempts();
        entity.lastFailedLoginAt = user.lastFailedLoginAt();
        entity.lockedUntil = user.lockedUntil();
        entity.createdAt = user.createdAt();
        entity.updatedAt = user.updatedAt();
        return entity;
    }

    public User toDomain() {
        return User.builder()
                .id(UserId.of(id))
                .email(Email.of(email))
                .passwordHash(HashedPassword.fromTrustedSource(passwordHash))
                .status(status)
                .failedLoginAttempts(failedLoginAttempts)
                .lastFailedLoginAt(lastFailedLoginAt)
                .lockedUntil(lockedUntil)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}

package com.booking.payment.domain.model;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.exception.BusinessRuleViolationException;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a payment lifecycle.
 */
public class Payment {

    public static final int MAX_DESCRIPTION_LENGTH = 200;
    public static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final PaymentId id;
    private final BookingId bookingId;
    private final UserId userId;
    private final Money money;
    private Integer capturedAmount;
    private Integer refundedAmount;
    private PaymentStatus status;
    private String description;
    private String gatewayTransactionId;
    private String failureReason;
    private final IdempotencyKey idempotencyKey;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id must not be null");
        this.bookingId = Objects.requireNonNull(builder.bookingId, "bookingId must not be null");
        this.userId = Objects.requireNonNull(builder.userId, "userId must not be null");
        this.money = Objects.requireNonNull(builder.money, "money must not be null");
        this.capturedAmount = builder.capturedAmount;
        this.refundedAmount = builder.refundedAmount;
        this.status = builder.status != null ? builder.status : PaymentStatus.PENDING;
        this.description = normalizeOptionalText(builder.description, MAX_DESCRIPTION_LENGTH, "description");
        this.gatewayTransactionId = normalizeOptionalText(builder.gatewayTransactionId, 255, "gatewayTransactionId");
        this.failureReason = normalizeOptionalText(builder.failureReason, MAX_FAILURE_REASON_LENGTH, "failureReason");
        this.idempotencyKey = Objects.requireNonNull(builder.idempotencyKey, "idempotencyKey must not be null");
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : this.createdAt;

        validateInvariants();
    }

    /**
     * Creates a new payment in PENDING state.
     */
    public static Payment create(
            BookingId bookingId,
            UserId userId,
            Money money,
            IdempotencyKey idempotencyKey,
            String description
    ) {
        return create(bookingId, userId, money, idempotencyKey, description, Clock.systemUTC());
    }

    /**
     * Creates a new payment in PENDING state using explicit clock.
     */
    public static Payment create(
            BookingId bookingId,
            UserId userId,
            Money money,
            IdempotencyKey idempotencyKey,
            String description,
            Clock clock
    ) {
        Objects.requireNonNull(clock, "clock must not be null");
        Instant now = Instant.now(clock);
        return builder()
                .id(PaymentId.generate())
                .bookingId(bookingId)
                .userId(userId)
                .money(money)
                .status(PaymentStatus.PENDING)
                .description(description)
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Transitions payment from PENDING to AUTHORIZED.
     */
    public void authorize(String gatewayTransactionId) {
        if (!status.isAuthorizable()) {
            throw new BusinessRuleViolationException(
                    "payment_not_authorizable",
                    "Payment cannot be authorized from status: " + status
            );
        }
        this.status = PaymentStatus.AUTHORIZED;
        this.gatewayTransactionId = normalizeRequiredText(gatewayTransactionId, 255, "gatewayTransactionId");
        this.failureReason = null;
        touch();
    }

    /**
     * Transitions payment from PENDING to FAILED.
     */
    public void fail(String reason) {
        if (!status.isAuthorizable()) {
            throw new BusinessRuleViolationException(
                    "payment_not_failurable",
                    "Payment cannot fail from status: " + status
            );
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = normalizeRequiredText(reason, MAX_FAILURE_REASON_LENGTH, "failureReason");
        touch();
    }

    /**
     * Captures an authorized payment.
     *
     * @param amount amount to capture; null means full amount
     */
    public void capture(Integer amount) {
        if (!status.isCapturable()) {
            throw new BusinessRuleViolationException(
                    "payment_not_capturable",
                    "Payment cannot be captured from status: " + status
            );
        }
        int targetAmount = amount != null ? amount : money.amount();
        if (targetAmount <= 0) {
            throw new BusinessRuleViolationException("payment_invalid_capture_amount", "Capture amount must be positive");
        }
        if (targetAmount > money.amount()) {
            throw new BusinessRuleViolationException(
                    "payment_capture_exceeds_authorized",
                    "Capture amount cannot exceed authorized amount"
            );
        }
        this.capturedAmount = targetAmount;
        this.status = PaymentStatus.CAPTURED;
        touch();
    }

    /**
     * Voids an authorized payment.
     */
    public void voidAuthorization() {
        if (status != PaymentStatus.AUTHORIZED) {
            throw new BusinessRuleViolationException(
                    "payment_not_voidable",
                    "Payment cannot be voided from status: " + status
            );
        }
        this.refundedAmount = capturedAmount != null ? capturedAmount : money.amount();
        this.status = PaymentStatus.REFUNDED;
        touch();
    }

    /**
     * Refunds a captured payment.
     *
     * @param amount amount to refund; null means full captured amount
     */
    public void refund(Integer amount) {
        if (status != PaymentStatus.CAPTURED) {
            throw new BusinessRuleViolationException(
                    "payment_not_refundable",
                    "Payment cannot be refunded from status: " + status
            );
        }
        int maxRefundable = capturedAmount != null ? capturedAmount : money.amount();
        int targetAmount = amount != null ? amount : maxRefundable;
        if (targetAmount <= 0) {
            throw new BusinessRuleViolationException("payment_invalid_refund_amount", "Refund amount must be positive");
        }
        if (targetAmount > maxRefundable) {
            throw new BusinessRuleViolationException(
                    "payment_refund_exceeds_captured",
                    "Refund amount cannot exceed captured amount"
            );
        }
        this.refundedAmount = targetAmount;
        this.status = PaymentStatus.REFUNDED;
        touch();
    }

    public boolean isOwnedBy(UserId requesterId) {
        Objects.requireNonNull(requesterId, "requesterId must not be null");
        return userId.equals(requesterId);
    }

    public PaymentId id() {
        return id;
    }

    public BookingId bookingId() {
        return bookingId;
    }

    public UserId userId() {
        return userId;
    }

    public Money money() {
        return money;
    }

    public Integer capturedAmount() {
        return capturedAmount;
    }

    public Integer refundedAmount() {
        return refundedAmount;
    }

    public PaymentStatus status() {
        return status;
    }

    public String description() {
        return description;
    }

    public String gatewayTransactionId() {
        return gatewayTransactionId;
    }

    public String failureReason() {
        return failureReason;
    }

    public IdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PaymentId id;
        private BookingId bookingId;
        private UserId userId;
        private Money money;
        private Integer capturedAmount;
        private Integer refundedAmount;
        private PaymentStatus status;
        private String description;
        private String gatewayTransactionId;
        private String failureReason;
        private IdempotencyKey idempotencyKey;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder id(PaymentId id) {
            this.id = id;
            return this;
        }

        public Builder bookingId(BookingId bookingId) {
            this.bookingId = bookingId;
            return this;
        }

        public Builder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public Builder money(Money money) {
            this.money = money;
            return this;
        }

        public Builder capturedAmount(Integer capturedAmount) {
            this.capturedAmount = capturedAmount;
            return this;
        }

        public Builder refundedAmount(Integer refundedAmount) {
            this.refundedAmount = refundedAmount;
            return this;
        }

        public Builder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder gatewayTransactionId(String gatewayTransactionId) {
            this.gatewayTransactionId = gatewayTransactionId;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder idempotencyKey(IdempotencyKey idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
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

        public Payment build() {
            return new Payment(this);
        }
    }

    private static String normalizeOptionalText(String value, int maxLength, String field) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new BusinessRuleViolationException(
                    "payment_" + field + "_too_long",
                    field + " length must be <= " + maxLength
            );
        }
        return normalized;
    }

    private static String normalizeRequiredText(String value, int maxLength, String field) {
        String normalized = normalizeOptionalText(value, maxLength, field);
        if (normalized == null) {
            throw new BusinessRuleViolationException(
                    "payment_" + field + "_required",
                    field + " must not be blank"
            );
        }
        return normalized;
    }

    private void touch() {
        this.updatedAt = Instant.now();
        validateInvariants();
    }

    private void validateInvariants() {
        if (capturedAmount != null && capturedAmount > money.amount()) {
            throw new IllegalArgumentException("capturedAmount must not exceed authorized amount");
        }
        if (refundedAmount != null && capturedAmount != null && refundedAmount > capturedAmount) {
            throw new IllegalArgumentException("refundedAmount must not exceed capturedAmount");
        }
        if (status == PaymentStatus.CAPTURED && capturedAmount == null) {
            throw new IllegalArgumentException("capturedAmount is required when status is CAPTURED");
        }
        if (status == PaymentStatus.REFUNDED && refundedAmount == null) {
            throw new IllegalArgumentException("refundedAmount is required when status is REFUNDED");
        }
        if (status == PaymentStatus.FAILED && failureReason == null) {
            throw new IllegalArgumentException("failureReason is required when status is FAILED");
        }
    }
}

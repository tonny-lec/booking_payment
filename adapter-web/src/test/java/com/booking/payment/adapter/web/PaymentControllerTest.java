package com.booking.payment.adapter.web;

import com.booking.booking.domain.model.BookingId;
import com.booking.iam.domain.model.UserId;
import com.booking.payment.application.usecase.CapturePaymentUseCase;
import com.booking.payment.application.usecase.CreatePaymentUseCase;
import com.booking.payment.application.usecase.GetPaymentUseCase;
import com.booking.payment.application.usecase.RefundPaymentUseCase;
import com.booking.payment.domain.model.IdempotencyKey;
import com.booking.payment.domain.model.Money;
import com.booking.payment.domain.model.Payment;
import com.booking.shared.adapter.web.config.ApiErrorProperties;
import com.booking.shared.adapter.web.exception.GlobalExceptionHandler;
import com.booking.shared.exception.ConflictException;
import com.booking.shared.exception.ForbiddenException;
import com.booking.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PaymentController")
class PaymentControllerTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);
    private static final String OWNER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String BOOKING_ID = "22222222-2222-2222-2222-222222222222";
    private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

    private CreatePaymentUseCase createPaymentUseCase;
    private GetPaymentUseCase getPaymentUseCase;
    private CapturePaymentUseCase capturePaymentUseCase;
    private RefundPaymentUseCase refundPaymentUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createPaymentUseCase = mock(CreatePaymentUseCase.class);
        getPaymentUseCase = mock(GetPaymentUseCase.class);
        capturePaymentUseCase = mock(CapturePaymentUseCase.class);
        refundPaymentUseCase = mock(RefundPaymentUseCase.class);

        PaymentController controller = new PaymentController(
                createPaymentUseCase,
                getPaymentUseCase,
                capturePaymentUseCase,
                refundPaymentUseCase
        );
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorProperties()))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("should return 201 with Location header when payment is created")
    void shouldReturn201WithLocationHeaderWhenPaymentIsCreated() throws Exception {
        Payment payment = authorizedPayment();
        when(createPaymentUseCase.execute(any()))
                .thenReturn(new CreatePaymentUseCase.CreatePaymentResult(payment, false));

        mockMvc.perform(post("/api/v1/payments")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/payments/" + payment.id().asString()))
                .andExpect(header().string("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(jsonPath("$.id").value(payment.id().asString()))
                .andExpect(jsonPath("$.bookingId").value(BOOKING_ID))
                .andExpect(jsonPath("$.userId").value(OWNER_ID))
                .andExpect(jsonPath("$.amount").value(10000))
                .andExpect(jsonPath("$.currency").value("JPY"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.gatewayTransactionId").value("txn_123"))
                .andExpect(jsonPath("$.idempotencyKey").value(IDEMPOTENCY_KEY));
    }

    @Test
    @DisplayName("should return 200 without Location header for idempotent replay")
    void shouldReturn200WithoutLocationHeaderForIdempotentReplay() throws Exception {
        Payment payment = authorizedPayment();
        when(createPaymentUseCase.execute(any()))
                .thenReturn(new CreatePaymentUseCase.CreatePaymentResult(payment, true));

        mockMvc.perform(post("/api/v1/payments")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(header().string("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(jsonPath("$.id").value(payment.id().asString()));
    }

    @Test
    @DisplayName("should return 400 when Idempotency-Key header is missing")
    void shouldReturn400WhenIdempotencyKeyHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .principal(() -> OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createPaymentUseCase);
    }

    @Test
    @DisplayName("should return 400 when Idempotency-Key is not a UUID")
    void shouldReturn400WhenIdempotencyKeyIsNotUuid() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createPaymentUseCase);
    }

    @Test
    @DisplayName("should return 400 when amount is not positive")
    void shouldReturn400WhenAmountIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookingId": "%s",
                                  "amount": 0,
                                  "currency": "JPY"
                                }
                                """.formatted(BOOKING_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());

        verifyNoInteractions(createPaymentUseCase);
    }

    @Test
    @DisplayName("should return 400 when currency is not an ISO 4217 code")
    void shouldReturn400WhenCurrencyIsNotIsoCode() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookingId": "%s",
                                  "amount": 10000,
                                  "currency": "ZZZ"
                                }
                                """.formatted(BOOKING_ID)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createPaymentUseCase);
    }

    @Test
    @DisplayName("should return 401 when principal name is not UUID")
    void shouldReturn401WhenPrincipalNameIsNotUuid() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .principal(() -> "anonymousUser")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("unauthorized"));

        verifyNoInteractions(createPaymentUseCase);
    }

    @Test
    @DisplayName("should return 409 when use case reports idempotency conflict")
    void shouldReturn409WhenUseCaseReportsIdempotencyConflict() throws Exception {
        when(createPaymentUseCase.execute(any()))
                .thenThrow(new ConflictException(
                        "payment_idempotency_key_conflict",
                        "Idempotency-Key was already used with a different request content"));

        mockMvc.perform(post("/api/v1/payments")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("payment_idempotency_key_conflict"));
    }

    @Test
    @DisplayName("should return 200 when getting own payment")
    void shouldReturn200WhenGettingOwnPayment() throws Exception {
        Payment payment = authorizedPayment();
        when(getPaymentUseCase.execute(any())).thenReturn(payment);

        mockMvc.perform(get("/api/v1/payments/" + payment.id().asString())
                        .principal(() -> OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.id().asString()))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    @DisplayName("should return 404 when payment does not exist")
    void shouldReturn404WhenPaymentDoesNotExist() throws Exception {
        when(getPaymentUseCase.execute(any()))
                .thenThrow(new ResourceNotFoundException("Payment", UUID.randomUUID().toString(), "Payment not found"));

        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID())
                        .principal(() -> OWNER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should return 403 when payment belongs to another user")
    void shouldReturn403WhenPaymentBelongsToAnotherUser() throws Exception {
        when(getPaymentUseCase.execute(any()))
                .thenThrow(new ForbiddenException("payment_access_denied", "Only payment owner can access payment"));

        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID())
                        .principal(() -> OWNER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("payment_access_denied"));
    }

    @Test
    @DisplayName("should return 400 when paymentId is malformed")
    void shouldReturn400WhenPaymentIdIsMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/payments/not-a-uuid")
                        .principal(() -> OWNER_ID))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getPaymentUseCase);
    }

    @Test
    @DisplayName("should return 200 when payment is captured")
    void shouldReturn200WhenPaymentIsCaptured() throws Exception {
        Payment payment = capturedPayment();
        when(capturePaymentUseCase.execute(any())).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/" + payment.id().asString() + "/capture")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 4000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.id().asString()))
                .andExpect(jsonPath("$.status").value("CAPTURED"))
                .andExpect(jsonPath("$.capturedAmount").value(4000));
    }

    @Test
    @DisplayName("should return 400 when capture paymentId is malformed")
    void shouldReturn400WhenCapturePaymentIdIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/payments/not-a-uuid/capture")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(capturePaymentUseCase);
    }

    @Test
    @DisplayName("should return 400 when capture Idempotency-Key is missing")
    void shouldReturn400WhenCaptureIdempotencyKeyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/capture")
                        .principal(() -> OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(capturePaymentUseCase);
    }

    @Test
    @DisplayName("should return 400 when capture Idempotency-Key is not UUID")
    void shouldReturn400WhenCaptureIdempotencyKeyIsNotUuid() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/capture")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(capturePaymentUseCase);
    }

    @Test
    @DisplayName("should return 400 when capture amount is not positive")
    void shouldReturn400WhenCaptureAmountIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/capture")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());

        verifyNoInteractions(capturePaymentUseCase);
    }

    @Test
    @DisplayName("should return 200 when payment is refunded")
    void shouldReturn200WhenPaymentIsRefunded() throws Exception {
        Payment payment = refundedPayment();
        when(refundPaymentUseCase.execute(any())).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/" + payment.id().asString() + "/refund")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 4000,
                                  "reason": "CUSTOMER_REQUEST",
                                  "note": "customer cancellation"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.id().asString()))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.refundedAmount").value(4000));
    }

    @Test
    @DisplayName("should return 400 when refund paymentId is malformed")
    void shouldReturn400WhenRefundPaymentIdIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/payments/not-a-uuid/refund")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRefundBody()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(refundPaymentUseCase);
    }

    @Test
    @DisplayName("should return 400 when refund Idempotency-Key is missing")
    void shouldReturn400WhenRefundIdempotencyKeyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/refund")
                        .principal(() -> OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRefundBody()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(refundPaymentUseCase);
    }

    @Test
    @DisplayName("should return 400 when refund Idempotency-Key is not UUID")
    void shouldReturn400WhenRefundIdempotencyKeyIsNotUuid() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/refund")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRefundBody()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(refundPaymentUseCase);
    }

    @Test
    @DisplayName("should return 400 when refund reason is missing")
    void shouldReturn400WhenRefundReasonIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/refund")
                        .principal(() -> OWNER_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 4000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());

        verifyNoInteractions(refundPaymentUseCase);
    }

    private static String validBody() {
        return """
                {
                  "bookingId": "%s",
                  "amount": 10000,
                  "currency": "JPY",
                  "description": "meeting room"
                }
                """.formatted(BOOKING_ID);
    }

    private static String validRefundBody() {
        return """
                {
                  "amount": 4000,
                  "reason": "CUSTOMER_REQUEST",
                  "note": "customer cancellation"
                }
                """;
    }

    private static Payment authorizedPayment() {
        Payment payment = Payment.create(
                BookingId.fromString(BOOKING_ID),
                UserId.fromString(OWNER_ID),
                Money.of(10000, "JPY"),
                IdempotencyKey.of(UUID.fromString(IDEMPOTENCY_KEY), FIXED_CLOCK),
                "meeting room",
                FIXED_CLOCK
        );
        payment.authorize("txn_123");
        return payment;
    }

    private static Payment capturedPayment() {
        Payment payment = authorizedPayment();
        payment.capture(4000);
        return payment;
    }

    private static Payment refundedPayment() {
        Payment payment = capturedPayment();
        payment.refund(4000);
        return payment;
    }
}

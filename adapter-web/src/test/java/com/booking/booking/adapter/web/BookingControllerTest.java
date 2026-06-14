package com.booking.booking.adapter.web;

import com.booking.booking.application.usecase.CancelBookingUseCase;
import com.booking.booking.application.usecase.CreateBookingUseCase;
import com.booking.booking.application.usecase.GetBookingUseCase;
import com.booking.booking.application.usecase.UpdateBookingUseCase;
import com.booking.booking.domain.model.Booking;
import com.booking.booking.domain.model.BookingId;
import com.booking.booking.domain.model.BookingStatus;
import com.booking.booking.domain.model.ResourceId;
import com.booking.booking.domain.model.TimeRange;
import com.booking.iam.domain.model.UserId;
import com.booking.shared.adapter.web.config.ApiErrorProperties;
import com.booking.shared.adapter.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BookingController")
class BookingControllerTest {

    private static final String OWNER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String BOOKING_ID = "33333333-3333-3333-3333-333333333333";
    private static final String RESOURCE_ID = "22222222-2222-2222-2222-222222222222";

    private CreateBookingUseCase createBookingUseCase;
    private GetBookingUseCase getBookingUseCase;
    private UpdateBookingUseCase updateBookingUseCase;
    private CancelBookingUseCase cancelBookingUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createBookingUseCase = mock(CreateBookingUseCase.class);
        getBookingUseCase = mock(GetBookingUseCase.class);
        updateBookingUseCase = mock(UpdateBookingUseCase.class);
        cancelBookingUseCase = mock(CancelBookingUseCase.class);

        BookingController controller = new BookingController(
                createBookingUseCase,
                getBookingUseCase,
                updateBookingUseCase,
                cancelBookingUseCase
        );
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorProperties()))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("should return 401 when principal name is not UUID")
    void shouldReturn401WhenPrincipalNameIsNotUuid() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .principal(() -> "anonymousUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": "11111111-1111-1111-1111-111111111111",
                                  "startAt": "2099-01-01T10:00:00Z",
                                  "endAt": "2099-01-01T11:00:00Z",
                                  "note": "test"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("unauthorized"));

        verifyNoInteractions(createBookingUseCase);
    }

    @Test
    @DisplayName("should return 400 when startAt is in the past")
    void shouldReturn400WhenStartAtIsInThePast() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .principal(() -> "11111111-1111-1111-1111-111111111111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": "22222222-2222-2222-2222-222222222222",
                                  "startAt": "2000-01-01T10:00:00Z",
                                  "endAt": "2099-01-01T11:00:00Z",
                                  "note": "test"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(createBookingUseCase);
    }

    @Test
    @DisplayName("should return 400 when update version is not positive")
    void shouldReturn400WhenUpdateVersionIsNotPositive() throws Exception {
        mockMvc.perform(put("/api/v1/bookings/33333333-3333-3333-3333-333333333333")
                        .principal(() -> "11111111-1111-1111-1111-111111111111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "updated",
                                  "version": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.version").exists());

        verifyNoInteractions(updateBookingUseCase);
    }

    @Test
    @DisplayName("should return 400 when update bookingId is malformed")
    void shouldReturn400WhenUpdateBookingIdIsMalformed() throws Exception {
        mockMvc.perform(put("/api/v1/bookings/not-a-uuid")
                        .principal(() -> OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "updated",
                                  "version": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(updateBookingUseCase);
    }

    @Test
    @DisplayName("should return 200 when booking is cancelled")
    void shouldReturn200WhenBookingIsCancelled() throws Exception {
        Booking booking = cancelledBooking();
        when(cancelBookingUseCase.execute(any())).thenReturn(booking);

        mockMvc.perform(delete("/api/v1/bookings/" + BOOKING_ID)
                        .principal(() -> OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BOOKING_ID))
                .andExpect(jsonPath("$.userId").value(OWNER_ID))
                .andExpect(jsonPath("$.resourceId").value(RESOURCE_ID))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.version").value(2));

        verify(cancelBookingUseCase).execute(argThat(command ->
                command.bookingId().equals(BookingId.fromString(BOOKING_ID))
                        && command.requestUserId().equals(UserId.fromString(OWNER_ID))
                        && command.reason() == null
        ));
    }

    @Test
    @DisplayName("should return 400 when delete bookingId is malformed")
    void shouldReturn400WhenDeleteBookingIdIsMalformed() throws Exception {
        mockMvc.perform(delete("/api/v1/bookings/not-a-uuid")
                        .principal(() -> OWNER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(cancelBookingUseCase);
    }

    @Test
    @DisplayName("should return 401 when delete principal name is not UUID")
    void shouldReturn401WhenDeletePrincipalNameIsNotUuid() throws Exception {
        mockMvc.perform(delete("/api/v1/bookings/" + BOOKING_ID)
                        .principal(() -> "anonymousUser"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("unauthorized"));

        verifyNoInteractions(cancelBookingUseCase);
    }

    private static Booking cancelledBooking() {
        return Booking.builder()
                .id(BookingId.fromString(BOOKING_ID))
                .userId(UserId.fromString(OWNER_ID))
                .resourceId(ResourceId.of(UUID.fromString(RESOURCE_ID)))
                .timeRange(TimeRange.fromPersisted(
                        Instant.parse("2099-01-01T10:00:00Z"),
                        Instant.parse("2099-01-01T11:00:00Z")))
                .status(BookingStatus.CANCELLED)
                .note("test")
                .version(2)
                .cancelledAt(Instant.parse("2026-03-01T00:00:00Z"))
                .createdAt(Instant.parse("2026-02-28T00:00:00Z"))
                .updatedAt(Instant.parse("2026-03-01T00:00:00Z"))
                .build();
    }
}

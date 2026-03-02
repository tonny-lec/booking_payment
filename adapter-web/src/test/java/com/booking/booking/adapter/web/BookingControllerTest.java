package com.booking.booking.adapter.web;

import com.booking.booking.application.usecase.CreateBookingUseCase;
import com.booking.booking.application.usecase.GetBookingUseCase;
import com.booking.booking.application.usecase.UpdateBookingUseCase;
import com.booking.shared.adapter.web.config.ApiErrorProperties;
import com.booking.shared.adapter.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BookingController")
class BookingControllerTest {

    private CreateBookingUseCase createBookingUseCase;
    private GetBookingUseCase getBookingUseCase;
    private UpdateBookingUseCase updateBookingUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createBookingUseCase = mock(CreateBookingUseCase.class);
        getBookingUseCase = mock(GetBookingUseCase.class);
        updateBookingUseCase = mock(UpdateBookingUseCase.class);

        BookingController controller = new BookingController(
                createBookingUseCase,
                getBookingUseCase,
                updateBookingUseCase
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
                        .principal(() -> "11111111-1111-1111-1111-111111111111")
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
}

package com.booking.booking.adapter.web.config;

import com.booking.booking.application.port.BookingRepository;
import com.booking.booking.application.usecase.CancelBookingUseCase;
import com.booking.booking.application.usecase.CreateBookingUseCase;
import com.booking.booking.application.usecase.GetBookingUseCase;
import com.booking.booking.application.usecase.UpdateBookingUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for Booking web-facing use cases.
 */
@Configuration
public class BookingUseCaseConfig {

    @Bean
    public CreateBookingUseCase createBookingUseCase(BookingRepository bookingRepository) {
        return new CreateBookingUseCase(bookingRepository);
    }

    @Bean
    public UpdateBookingUseCase updateBookingUseCase(BookingRepository bookingRepository) {
        return new UpdateBookingUseCase(bookingRepository);
    }

    @Bean
    public CancelBookingUseCase cancelBookingUseCase(BookingRepository bookingRepository) {
        return new CancelBookingUseCase(bookingRepository);
    }

    @Bean
    public GetBookingUseCase getBookingUseCase(BookingRepository bookingRepository) {
        return new GetBookingUseCase(bookingRepository);
    }
}

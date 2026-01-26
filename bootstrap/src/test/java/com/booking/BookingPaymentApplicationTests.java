package com.booking;

import com.booking.test.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * アプリケーションコンテキストのロードテスト.
 */
@SpringBootTest
@Import(PostgresTestContainerConfig.class)
class BookingPaymentApplicationTests {

    @Test
    void contextLoads() {
        // アプリケーションコンテキストが正常にロードされることを確認
    }
}

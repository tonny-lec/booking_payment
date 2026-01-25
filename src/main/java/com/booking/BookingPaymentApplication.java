package com.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 予約・決済基盤アプリケーションのエントリーポイント.
 *
 * <p>Bounded Contexts:
 * <ul>
 *   <li>IAM - 認証・認可</li>
 *   <li>Booking - 予約管理</li>
 *   <li>Payment - 決済処理</li>
 *   <li>Notification - 通知</li>
 *   <li>Audit - 監査</li>
 * </ul>
 */
@SpringBootApplication
public class BookingPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingPaymentApplication.class, args);
    }
}

package com.restaurant.reservations.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Component
public class DatabaseInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            // Drop the old flat constraint that blocked re-booking cancelled slots
            st.execute("ALTER TABLE reservations DROP CONSTRAINT IF EXISTS uk_reservation_table_date_slot");
            // Partial index — only CONFIRMED rows occupy a slot, so cancelled slots are re-bookable
            st.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_active_reservation_slot " +
                "ON reservations (table_id, reservation_date, start_time) " +
                "WHERE status = 'CONFIRMED'"
            );
            log.info("Reservation slot partial index ensured");
        } catch (Exception e) {
            log.error("Failed to apply reservation schema patch", e);
        }
    }
}

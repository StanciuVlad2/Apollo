package com.restaurant.reservations.service;

import com.restaurant.reservations.dto.DayCountData;
import com.restaurant.reservations.dto.ReservationReportData;
import com.restaurant.reservations.model.Reservation;
import com.restaurant.reservations.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationReportService {

    private final ReservationRepository reservationRepository;

    public ReservationReportData generateReport(LocalDate from, LocalDate to) {
        List<Reservation> reservations = reservationRepository.findByReservationDateBetween(from, to);

        double avgPartySize = reservations.isEmpty() ? 0.0 :
                Math.round(reservations.stream().mapToInt(Reservation::getPartySize).average().orElse(0.0) * 100.0) / 100.0;

        Map<LocalDate, List<Reservation>> byDay = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getReservationDate));

        List<DayCountData> dayCounts = byDay.entrySet().stream()
                .map(e -> {
                    List<Reservation> dayList = e.getValue();
                    double dayAvg = Math.round(dayList.stream().mapToInt(Reservation::getPartySize).average().orElse(0.0) * 100.0) / 100.0;
                    return new DayCountData(e.getKey(), dayList.size(), dayAvg);
                })
                .sorted((a, b) -> a.date().compareTo(b.date()))
                .toList();

        return new ReservationReportData(reservations.size(), avgPartySize, dayCounts);
    }
}

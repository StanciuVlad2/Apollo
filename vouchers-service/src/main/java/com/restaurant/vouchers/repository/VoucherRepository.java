package com.restaurant.vouchers.repository;

import com.restaurant.vouchers.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);
    boolean existsByOrderId(Long orderId);
    List<Voucher> findByUserIdOrderByCreatedAtDesc(Long userId);
}

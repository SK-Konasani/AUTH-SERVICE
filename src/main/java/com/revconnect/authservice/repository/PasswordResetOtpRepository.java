package com.revconnect.authservice.repository;

import com.revconnect.authservice.model.PasswordResetOtp;
import com.revconnect.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findTopByUserOrderByCreatedAtDesc(User user);
    Optional<PasswordResetOtp> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);
    List<PasswordResetOtp> findByUserAndUsedFalse(User user);
    void deleteByUser_Id(Long userId);
}
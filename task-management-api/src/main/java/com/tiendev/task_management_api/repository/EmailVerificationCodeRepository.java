package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.EmailVerificationCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM EmailVerificationCode c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    List<EmailVerificationCode> findByUserIdOrderByCreatedAtDescWithLock(@Param("userId") Long userId);

    Optional<EmailVerificationCode> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    List<EmailVerificationCode> findByUserIdAndUsedAtIsNull(Long userId);
}

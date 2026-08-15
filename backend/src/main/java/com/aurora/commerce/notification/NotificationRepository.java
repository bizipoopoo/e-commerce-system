package com.aurora.commerce.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadAtIsNull(Long userId);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
}

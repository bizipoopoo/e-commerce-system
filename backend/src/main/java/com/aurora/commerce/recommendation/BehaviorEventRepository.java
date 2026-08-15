package com.aurora.commerce.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface BehaviorEventRepository extends JpaRepository<BehaviorEvent, Long> {
    List<BehaviorEvent> findTop100ByUserIdOrderByOccurredAtDesc(Long userId);
}

package com.aurora.commerce.recommendation;

import com.aurora.commerce.catalog.CatalogFacade;
import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
class RecommendationService {

    private static final Set<String> EVENT_TYPES = Set.of("VIEW", "CLICK", "ADD_TO_CART", "PURCHASE");
    private static final Map<String, Integer> EVENT_WEIGHTS = Map.of(
            "VIEW", 1, "CLICK", 2, "ADD_TO_CART", 4, "PURCHASE", 6);

    private final BehaviorEventRepository eventRepository;
    private final CatalogFacade catalogFacade;

    RecommendationService(BehaviorEventRepository eventRepository, CatalogFacade catalogFacade) {
        this.eventRepository = eventRepository;
        this.catalogFacade = catalogFacade;
    }

    @Transactional
    void record(Long userId, String eventType, String targetType, Long targetId, String contextJson) {
        String normalizedType = eventType.toUpperCase();
        if (!EVENT_TYPES.contains(normalizedType) || !"PRODUCT".equalsIgnoreCase(targetType) || targetId == null) {
            throw new BusinessException("INVALID_BEHAVIOR_EVENT", "行为事件无效", HttpStatus.BAD_REQUEST);
        }
        eventRepository.save(new BehaviorEvent(
                userId, null, normalizedType, "PRODUCT", targetId, contextJson, Instant.now()));
    }

    @Transactional(readOnly = true)
    List<RecommendationView> recommendations(Long userId) {
        List<CatalogFacade.RecommendationCandidate> candidates = catalogFacade.recommendationCandidates();
        Map<Long, Integer> categoryWeights = categoryWeights(userId);
        return candidates.stream().map(candidate -> {
            int preference = categoryWeights.getOrDefault(candidate.categoryId(), 0);
            double score = Math.log10(candidate.salesCount() + 10) * 10
                    + (candidate.featured() ? 12 : 0) + preference * 5;
            String reason = preference > 0
                    ? "因为你最近关注了「" + candidate.categoryName() + "」"
                    : candidate.featured() ? "Aurora 编辑精选" : "正在被更多人喜欢";
            return new ScoredRecommendation(candidate, score, reason);
        }).sorted(Comparator.comparingDouble(ScoredRecommendation::score).reversed())
                .limit(8)
                .map(item -> new RecommendationView(
                        item.candidate().productId(), item.candidate().defaultSkuId(),
                        item.candidate().productName(), item.candidate().subtitle(), item.candidate().imageUrl(),
                        item.candidate().salePrice(), item.candidate().categoryName(), item.reason()
                )).toList();
    }

    private Map<Long, Integer> categoryWeights(Long userId) {
        if (userId == null) return Map.of();
        List<BehaviorEvent> events = eventRepository.findTop100ByUserIdOrderByOccurredAtDesc(userId).stream()
                .filter(event -> "PRODUCT".equals(event.targetType()) && event.targetId() != null).toList();
        Map<Long, Long> categories = catalogFacade.productCategoryIds(
                events.stream().map(BehaviorEvent::targetId).toList());
        Map<Long, Integer> weights = new HashMap<>();
        events.forEach(event -> {
            Long categoryId = categories.get(event.targetId());
            if (categoryId != null) weights.merge(
                    categoryId, EVENT_WEIGHTS.getOrDefault(event.eventType(), 1), Integer::sum);
        });
        return weights;
    }

    private record ScoredRecommendation(
            CatalogFacade.RecommendationCandidate candidate, double score, String reason
    ) {
    }

    record RecommendationView(
            Long productId, Long defaultSkuId, String productName, String subtitle,
            String imageUrl, java.math.BigDecimal salePrice, String categoryName, String reason
    ) {
    }
}

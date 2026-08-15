package com.aurora.commerce.marketing;

import com.aurora.commerce.notification.NotificationFacade;
import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MarketingFacade {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final OrderDiscountRepository discountRepository;
    private final NotificationFacade notificationFacade;

    MarketingFacade(
            CouponRepository couponRepository,
            UserCouponRepository userCouponRepository,
            OrderDiscountRepository discountRepository,
            NotificationFacade notificationFacade
    ) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.discountRepository = discountRepository;
        this.notificationFacade = notificationFacade;
    }

    @Transactional(readOnly = true)
    public List<CouponView> available(Long userId) {
        Map<Long, UserCoupon> claimed = userCouponRepository.findByUserIdOrderByClaimedAtDesc(userId).stream()
                .collect(Collectors.toMap(UserCoupon::couponId, Function.identity()));
        Instant now = Instant.now();
        return couponRepository.findByStatusOrderByCreatedAtDesc(Coupon.Status.ACTIVE).stream()
                .filter(coupon -> coupon.claimableAt(now))
                .map(coupon -> toView(coupon, claimed.get(coupon.id()), now))
                .toList();
    }

    @Transactional
    public CouponView claim(Long userId, Long couponId) {
        UserCoupon existing = userCouponRepository.findByUserIdAndCouponId(userId, couponId).orElse(null);
        if (existing != null) {
            return toView(coupon(couponId), existing, Instant.now());
        }
        Instant now = Instant.now();
        if (couponRepository.claimOne(couponId, Coupon.Status.ACTIVE, now) != 1) {
            throw new BusinessException("COUPON_NOT_CLAIMABLE", "优惠券已领完或不在领取时间", HttpStatus.CONFLICT);
        }
        Coupon coupon = coupon(couponId);
        UserCoupon userCoupon = userCouponRepository.save(new UserCoupon(userId, couponId, now));
        notificationFacade.notifyUser(
                userId, "MARKETING", "优惠券已到账", coupon.name() + " 已放入你的账户。",
                "COUPON", coupon.code());
        return toView(coupon, userCoupon, now);
    }

    @Transactional(readOnly = true)
    public List<CouponView> mine(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdOrderByClaimedAtDesc(userId);
        Map<Long, Coupon> coupons = couponRepository.findAllById(
                        userCoupons.stream().map(UserCoupon::couponId).toList()).stream()
                .collect(Collectors.toMap(Coupon::id, Function.identity()));
        Instant now = Instant.now();
        return userCoupons.stream().map(userCoupon ->
                toView(coupons.get(userCoupon.couponId()), userCoupon, now)).toList();
    }

    @Transactional(readOnly = true)
    public DiscountView preview(Long userId, Long userCouponId, BigDecimal goodsAmount) {
        if (userCouponId == null) return DiscountView.none();
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .filter(item -> item.userId().equals(userId))
                .orElseThrow(this::couponNotAvailable);
        Coupon coupon = coupon(userCoupon.couponId());
        ensureAvailable(userCoupon, coupon, Instant.now());
        return new DiscountView(userCoupon.id(), coupon.calculate(goodsAmount), coupon.name());
    }

    @Transactional
    public DiscountView lockForOrder(
            Long userId, Long userCouponId, String orderNo, BigDecimal goodsAmount
    ) {
        if (userCouponId == null) return DiscountView.none();
        OrderDiscount existing = discountRepository.findByOrderNo(orderNo).orElse(null);
        if (existing != null) {
            return new DiscountView(existing.userCouponId(), existing.discountAmount(), "优惠券");
        }
        UserCoupon userCoupon = userCouponRepository.findOwnedForUpdate(userCouponId, userId)
                .orElseThrow(this::couponNotAvailable);
        Coupon coupon = coupon(userCoupon.couponId());
        ensureAvailable(userCoupon, coupon, Instant.now());
        BigDecimal discount = coupon.calculate(goodsAmount);
        userCoupon.lock(orderNo);
        discountRepository.save(new OrderDiscount(orderNo, userCoupon.id(), discount));
        return new DiscountView(userCoupon.id(), discount, coupon.name());
    }

    @Transactional
    public void release(String orderNo) {
        OrderDiscount discount = discountRepository.findByOrderNo(orderNo).orElse(null);
        if (discount == null || discount.status() != OrderDiscount.Status.LOCKED) return;
        UserCoupon userCoupon = userCouponRepository.findByLockedOrderNo(orderNo).orElse(null);
        if (userCoupon != null) userCoupon.release(orderNo);
        discount.release();
    }

    @Transactional
    public void confirm(String orderNo) {
        OrderDiscount discount = discountRepository.findByOrderNo(orderNo).orElse(null);
        if (discount == null || discount.status() == OrderDiscount.Status.APPLIED) return;
        UserCoupon userCoupon = userCouponRepository.findById(discount.userCouponId())
                .orElseThrow(this::couponNotAvailable);
        userCoupon.use(orderNo, Instant.now());
        discount.apply();
    }

    @Transactional
    public CouponView create(CreateCouponCommand command) {
        Coupon coupon = couponRepository.save(new Coupon(
                command.code(), command.name(), command.description(), command.discountAmount(),
                command.thresholdAmount(), command.totalLimit(), command.startsAt(), command.endsAt()));
        return toView(coupon, null, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<CouponView> adminList() {
        Instant now = Instant.now();
        return couponRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(coupon -> toView(coupon, null, now)).toList();
    }

    private void ensureAvailable(UserCoupon userCoupon, Coupon coupon, Instant now) {
        if (userCoupon.status() != UserCoupon.Status.AVAILABLE || !coupon.activeAt(now)) {
            throw couponNotAvailable();
        }
    }

    private Coupon coupon(Long couponId) {
        return couponRepository.findById(couponId).orElseThrow(() ->
                new BusinessException("COUPON_NOT_FOUND", "优惠券不存在", HttpStatus.NOT_FOUND));
    }

    private CouponView toView(Coupon coupon, UserCoupon userCoupon, Instant now) {
        String status = userCoupon == null ? "UNCLAIMED" : userCoupon.status().name();
        if (now.isAfter(coupon.endsAt()) && userCoupon != null && userCoupon.status() == UserCoupon.Status.AVAILABLE) {
            status = "EXPIRED";
        }
        return new CouponView(
                coupon.id(), userCoupon == null ? null : userCoupon.id(), coupon.code(), coupon.name(),
                coupon.description(), coupon.discountAmount(), coupon.thresholdAmount(), status,
                coupon.startsAt(), coupon.endsAt()
        );
    }

    private BusinessException couponNotAvailable() {
        return new BusinessException("COUPON_NOT_AVAILABLE", "优惠券当前不可用", HttpStatus.CONFLICT);
    }

    public record DiscountView(Long userCouponId, BigDecimal discountAmount, String couponName) {
        static DiscountView none() { return new DiscountView(null, BigDecimal.ZERO.setScale(2), null); }
    }

    public record CouponView(
            Long couponId, Long userCouponId, String code, String name, String description,
            BigDecimal discountAmount, BigDecimal thresholdAmount, String status,
            Instant startsAt, Instant endsAt
    ) {
    }

    public record CreateCouponCommand(
            String code, String name, String description, BigDecimal discountAmount,
            BigDecimal thresholdAmount, int totalLimit, Instant startsAt, Instant endsAt
    ) {
    }
}

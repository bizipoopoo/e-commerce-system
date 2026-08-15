package com.aurora.commerce.marketing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
    List<Coupon> findByStatusOrderByCreatedAtDesc(Coupon.Status status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Coupon coupon set coupon.claimedCount = coupon.claimedCount + 1
             where coupon.id = :couponId and coupon.status = :status
               and coupon.startsAt <= :now and coupon.endsAt > :now
               and coupon.claimedCount < coupon.totalLimit
            """)
    int claimOne(
            @Param("couponId") Long couponId,
            @Param("status") Coupon.Status status,
            @Param("now") Instant now
    );
}

interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
    List<UserCoupon> findByUserIdOrderByClaimedAtDesc(Long userId);
    Optional<UserCoupon> findByLockedOrderNo(String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select userCoupon from UserCoupon userCoupon where userCoupon.id = :id and userCoupon.userId = :userId")
    Optional<UserCoupon> findOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}

interface OrderDiscountRepository extends JpaRepository<OrderDiscount, Long> {
    Optional<OrderDiscount> findByOrderNo(String orderNo);
}

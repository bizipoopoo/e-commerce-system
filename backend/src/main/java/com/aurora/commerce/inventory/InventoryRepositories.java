package com.aurora.commerce.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findBySkuIdIn(Collection<Long> skuIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from Inventory inventory where inventory.skuId = :skuId")
    Optional<Inventory> findBySkuIdForUpdate(@Param("skuId") Long skuId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Inventory inventory
               set inventory.reservedQuantity = inventory.reservedQuantity + :quantity,
                   inventory.version = inventory.version + 1
             where inventory.skuId = :skuId
               and inventory.totalQuantity - inventory.reservedQuantity >= :quantity
            """)
    int reserveAvailable(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Inventory inventory
               set inventory.reservedQuantity = inventory.reservedQuantity - :quantity,
                   inventory.version = inventory.version + 1
             where inventory.skuId = :skuId
               and inventory.reservedQuantity >= :quantity
            """)
    int releaseReserved(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Inventory inventory
               set inventory.totalQuantity = inventory.totalQuantity - :quantity,
                   inventory.reservedQuantity = inventory.reservedQuantity - :quantity,
                   inventory.version = inventory.version + 1
             where inventory.skuId = :skuId
               and inventory.totalQuantity >= :quantity
               and inventory.reservedQuantity >= :quantity
            """)
    int confirmReserved(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}

interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    List<InventoryReservation> findByBusinessKeyOrderBySkuIdAsc(String businessKey);
}

interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
}

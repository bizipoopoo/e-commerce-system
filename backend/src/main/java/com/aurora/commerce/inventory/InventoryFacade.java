package com.aurora.commerce.inventory;

import com.aurora.commerce.catalog.CatalogFacade;
import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryFacade {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final CatalogFacade catalogFacade;

    InventoryFacade(
            InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository,
            InventoryTransactionRepository transactionRepository,
            CatalogFacade catalogFacade
    ) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.transactionRepository = transactionRepository;
        this.catalogFacade = catalogFacade;
    }

    @Transactional(readOnly = true)
    public StockView stock(Long skuId) {
        return toView(inventoryRepository.findById(skuId).orElseThrow(() -> inventoryNotFound(skuId)));
    }

    @Transactional(readOnly = true)
    public Map<Long, StockView> stocks(Collection<Long> skuIds) {
        Map<Long, StockView> result = new LinkedHashMap<>();
        inventoryRepository.findBySkuIdIn(skuIds).stream()
                .sorted(Comparator.comparing(Inventory::skuId))
                .forEach(inventory -> result.put(inventory.skuId(), toView(inventory)));
        return result;
    }

    @Transactional(readOnly = true)
    public void ensureAvailable(Map<Long, Integer> quantities) {
        Map<Long, StockView> stocks = stocks(quantities.keySet());
        quantities.forEach((skuId, quantity) -> {
            StockView stock = stocks.get(skuId);
            if (stock == null || stock.availableQuantity() < quantity) {
                throw insufficientStock(skuId);
            }
        });
    }

    @Transactional
    public void reserve(String businessKey, Map<Long, Integer> quantities) {
        validateReservationRequest(businessKey, quantities);
        List<InventoryReservation> existing = reservationRepository
                .findByBusinessKeyOrderBySkuIdAsc(businessKey);
        if (!existing.isEmpty()) {
            if (matchesActiveReservation(existing, quantities)) {
                return;
            }
            throw new BusinessException(
                    "INVENTORY_RESERVATION_CONFLICT",
                    "业务键已经用于其他库存预占",
                    HttpStatus.CONFLICT
            );
        }

        List<Map.Entry<Long, Integer>> orderedLines = quantities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
        List<InventoryReservation> reservations = new ArrayList<>();
        for (Map.Entry<Long, Integer> line : orderedLines) {
            if (inventoryRepository.reserveAvailable(line.getKey(), line.getValue()) != 1) {
                throw insufficientStock(line.getKey());
            }
            Inventory inventory = inventoryRepository.findById(line.getKey())
                    .orElseThrow(() -> inventoryNotFound(line.getKey()));
            reservations.add(new InventoryReservation(businessKey, line.getKey(), line.getValue()));
            transactionRepository.save(new InventoryTransaction(
                    line.getKey(), "RESERVE", -line.getValue(), inventory.totalQuantity(),
                    inventory.reservedQuantity(), businessKey, "库存预占"
            ));
        }
        reservationRepository.saveAll(reservations);
    }

    @Transactional
    public void release(String businessKey) {
        List<InventoryReservation> reservations = reservationRepository
                .findByBusinessKeyOrderBySkuIdAsc(businessKey);
        for (InventoryReservation reservation : reservations.stream().filter(InventoryReservation::active).toList()) {
            if (inventoryRepository.releaseReserved(reservation.skuId(), reservation.quantity()) != 1) {
                throw new IllegalStateException("Reserved inventory is inconsistent for " + reservation.skuId());
            }
            reservation.release();
            Inventory inventory = inventoryRepository.findById(reservation.skuId())
                    .orElseThrow(() -> inventoryNotFound(reservation.skuId()));
            transactionRepository.save(new InventoryTransaction(
                    reservation.skuId(), "RELEASE", reservation.quantity(), inventory.totalQuantity(),
                    inventory.reservedQuantity(), businessKey, "释放预占"
            ));
        }
    }

    @Transactional
    public StockView setInventory(Long skuId, int totalQuantity, int warningQuantity, String reason) {
        if (!catalogFacade.skuExists(skuId)) {
            throw new BusinessException("SKU_NOT_FOUND", "SKU 不存在: " + skuId, HttpStatus.NOT_FOUND);
        }
        Inventory inventory = inventoryRepository.findBySkuIdForUpdate(skuId)
                .orElseGet(() -> new Inventory(skuId, 0, warningQuantity));
        int delta = totalQuantity - inventory.totalQuantity();
        inventory.setQuantities(totalQuantity, warningQuantity);
        Inventory saved = inventoryRepository.save(inventory);
        transactionRepository.save(new InventoryTransaction(
                skuId, "ADJUST", delta, saved.totalQuantity(), saved.reservedQuantity(),
                null, reason
        ));
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public List<StockView> allStocks(boolean warningOnly) {
        return inventoryRepository.findAll().stream()
                .filter(inventory -> !warningOnly || inventory.warning())
                .sorted(Comparator.comparing(Inventory::skuId))
                .map(this::toView)
                .toList();
    }

    private boolean matchesActiveReservation(
            List<InventoryReservation> existing,
            Map<Long, Integer> requested
    ) {
        if (existing.size() != requested.size() || existing.stream().anyMatch(line -> !line.active())) {
            return false;
        }
        return existing.stream().allMatch(line -> requested.getOrDefault(line.skuId(), -1) == line.quantity());
    }

    private void validateReservationRequest(String businessKey, Map<Long, Integer> quantities) {
        if (businessKey == null || businessKey.isBlank() || businessKey.length() > 100 || quantities.isEmpty()) {
            throw new BusinessException(
                    "INVALID_RESERVATION_REQUEST", "库存预占请求无效", HttpStatus.BAD_REQUEST);
        }
        if (quantities.values().stream().anyMatch(quantity -> quantity == null || quantity <= 0)) {
            throw new BusinessException(
                    "INVALID_RESERVATION_QUANTITY", "预占数量必须大于 0", HttpStatus.BAD_REQUEST);
        }
    }

    private StockView toView(Inventory inventory) {
        return new StockView(
                inventory.skuId(), inventory.totalQuantity(), inventory.reservedQuantity(),
                inventory.availableQuantity(), inventory.warningQuantity(), inventory.warning()
        );
    }

    private BusinessException inventoryNotFound(Long skuId) {
        return new BusinessException("INVENTORY_NOT_FOUND", "SKU 尚未配置库存: " + skuId, HttpStatus.NOT_FOUND);
    }

    private BusinessException insufficientStock(Long skuId) {
        return new BusinessException("INSUFFICIENT_STOCK", "SKU 库存不足: " + skuId, HttpStatus.CONFLICT);
    }

    public record StockView(
            Long skuId,
            int totalQuantity,
            int reservedQuantity,
            int availableQuantity,
            int warningQuantity,
            boolean warning
    ) {
    }
}

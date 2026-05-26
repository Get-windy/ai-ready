package com.qizhi.erp.stock.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 库存服务单元测试
 * 目标覆盖率: >=80%
 * 
 * @author qa-lead
 * @since 2026-04-22
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("库存服务测试")
public class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockTransactionRepository transactionRepository;

    @Mock
    private StockAlertRepository alertRepository;

    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private StockService stockService;

    private Stock testStock;
    private StockDTO testStockDTO;

    @BeforeEach
    void setUp() {
        testStock = Stock.builder()
            .id(1L)
            .productId(100L)
            .warehouseId(10L)
            .quantity(new BigDecimal("100.00"))
            .availableQuantity(new BigDecimal("90.00"))
            .reservedQuantity(new BigDecimal("10.00"))
            .minStockLevel(new BigDecimal("20.00"))
            .maxStockLevel(new BigDecimal("500.00"))
            .unit("件")
            .lastUpdated(LocalDateTime.now())
            .build();
            
        testStockDTO = StockDTO.builder()
            .id(1L)
            .productId(100L)
            .warehouseId(10L)
            .quantity(new BigDecimal("100.00"))
            .availableQuantity(new BigDecimal("90.00"))
            .unit("件")
            .build();
    }

    @Test
    @DisplayName("入库操作-成功")
    void stockIn_Success() {
        // Given
        StockInDTO inDTO = StockInDTO.builder()
            .productId(100L)
            .warehouseId(10L)
            .quantity(new BigDecimal("50.00"))
            .referenceNo("IN-20260422-001")
            .build();
            
        when(stockRepository.findByProductIdAndWarehouseId(100L, 10L))
            .thenReturn(Optional.of(testStock));
        when(stockRepository.save(any())).thenReturn(testStock);
        when(transactionRepository.save(any())).thenReturn(new StockTransaction());
        when(stockMapper.toDTO(any())).thenReturn(testStockDTO);

        // When
        StockDTO result = stockService.stockIn(inDTO);

        // Then
        assertNotNull(result);
        verify(stockRepository).save(any());
        verify(transactionRepository).save(any());
    }

    @Test
    @DisplayName("入库操作-新品入库")
    void stockIn_NewProduct() {
        // Given
        StockInDTO inDTO = StockInDTO.builder()
            .productId(200L)
            .warehouseId(10L)
            .quantity(new BigDecimal("50.00"))
            .referenceNo("IN-20260422-002")
            .build();
            
        when(stockRepository.findByProductIdAndWarehouseId(200L, 10L))
            .thenReturn(Optional.empty());
        when(stockRepository.save(any())).thenReturn(testStock);
        when(transactionRepository.save(any())).thenReturn(new StockTransaction());
        when(stockMapper.toDTO(any())).thenReturn(testStockDTO);

        // When
        StockDTO result = stockService.stockIn(inDTO);

        // Then
        assertNotNull(result);
        verify(stockRepository).save(any());
    }

    @Test
    @DisplayName("出库操作-成功")
    void stockOut_Success() {
        // Given
        StockOutDTO outDTO = StockOutDTO.builder()
            .productId(100L)
            .warehouseId(10L)
            .quantity(new BigDecimal("30.00"))
            .referenceNo("OUT-20260422-001")
            .build();
            
        when(stockRepository.findByProductIdAndWarehouseId(100L, 10L))
            .thenReturn(Optional.of(testStock));
        when(stockRepository.save(any())).thenReturn(testStock);
        when(transactionRepository.save(any())).thenReturn(new StockTransaction());
        when(stockMapper.toDTO(any())).thenReturn(testStockDTO);

        // When
        StockDTO result = stockService.stockOut(outDTO);

        // Then
        assertNotNull(result);
        verify(stockRepository).save(any());
        verify(transactionRepository).save(any());
    }

    @Test
    @DisplayName("出库操作-库存不足")
    void stockOut_InsufficientStock() {
        // Given
        StockOutDTO outDTO = StockOutDTO.builder()
            .productId(100L)
            .warehouseId(10L)
            .quantity(new BigDecimal("200.00"))
            .referenceNo("OUT-20260422-002")
            .build();
            
        when(stockRepository.findByProductIdAndWarehouseId(100L, 10L))
            .thenReturn(Optional.of(testStock));

        // When & Then
        assertThrows(InsufficientStockException.class, () -> {
            stockService.stockOut(outDTO);
        });
    }

    @Test
    @DisplayName("出库操作-库存不存在")
    void stockOut_StockNotFound() {
        // Given
        StockOutDTO outDTO = StockOutDTO.builder()
            .productId(999L)
            .warehouseId(10L)
            .quantity(new BigDecimal("10.00"))
            .build();
            
        when(stockRepository.findByProductIdAndWarehouseId(999L, 10L))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            stockService.stockOut(outDTO);
        });
    }

    @Test
    @DisplayName("查询库存-按产品ID")
    void getStockByProductId_Success() {
        // Given
        List<Stock> stocks = Arrays.asList(testStock);
        when(stockRepository.findByProductId(100L)).thenReturn(stocks);
        when(stockMapper.toDTO(any())).thenReturn(testStockDTO);

        // When
        List<StockDTO> result = stockService.getStockByProductId(100L);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("查询库存-按仓库ID")
    void getStockByWarehouseId_Success() {
        // Given
        List<Stock> stocks = Arrays.asList(testStock);
        when(stockRepository.findByWarehouseId(10L)).thenReturn(stocks);
        when(stockMapper.toDTO(any())).thenReturn(testStockDTO);

        // When
        List<StockDTO> result = stockService.getStockByWarehouseId(10L);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("库存调拨-成功")
    void transferStock_Success() {
        // Given
        StockTransferDTO transferDTO = StockTransferDTO.builder()
            .productId(100L)
            .fromWarehouseId(10L)
            .toWarehouseId(20L)
            .quantity(new BigDecimal("20.00"))
            .referenceNo("TF-20260422-001")
            .build();
            
        Stock toStock = Stock.builder()
            .id(2L)
            .productId(100L)
            .warehouseId(20L)
            .quantity(new BigDecimal("50.00"))
            .build();
            
        when(stockRepository.findByProductIdAndWarehouseId(100L, 10L))
            .thenReturn(Optional.of(testStock));
        when(stockRepository.findByProductIdAndWarehouseId(100L, 20L))
            .thenReturn(Optional.of(toStock));
        when(stockRepository.save(any())).thenReturn(testStock);
        when(transactionRepository.save(any())).thenReturn(new StockTransaction());

        // When
        stockService.transferStock(transferDTO);

        // Then
        verify(stockRepository, times(2)).save(any());
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    @DisplayName
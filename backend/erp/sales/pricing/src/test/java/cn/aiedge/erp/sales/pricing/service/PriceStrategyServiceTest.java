package cn.aiedge.erp.sales.pricing.service;

import cn.aiedge.erp.sales.pricing.dto.PriceStrategyDTO;
import cn.aiedge.erp.sales.pricing.entity.PriceStrategy;
import cn.aiedge.erp.sales.pricing.repository.PriceStrategyRepository;
import cn.aiedge.erp.sales.pricing.service.impl.PriceStrategyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 价格策略服务测试类
 */
@ExtendWith(MockitoExtension.class)
class PriceStrategyServiceTest {
    
    @Mock
    private PriceStrategyRepository priceStrategyRepository;
    
    @InjectMocks
    private PriceStrategyServiceImpl priceStrategyService;
    
    private PriceStrategyDTO testStrategyDTO;
    private PriceStrategy testStrategy;
    
    @BeforeEach
    void setUp() {
        testStrategyDTO = new PriceStrategyDTO();
        testStrategyDTO.setName("VIP客户价格策略");
        testStrategyDTO.setDescription("适用于VIP客户的特殊价格策略");
        testStrategyDTO.setStrategyType("customer_level");
        testStrategyDTO.setCustomerLevel("VIP");
        testStrategyDTO.setBasePrice(new BigDecimal("1000.00"));
        testStrategyDTO.setPriority(1);
        
        testStrategy = new PriceStrategy();
        testStrategy.setId(1L);
        testStrategy.setName("VIP客户价格策略");
        testStrategy.setDescription("适用于VIP客户的特殊价格策略");
        testStrategy.setStrategyType("customer_level");
        testStrategy.setCustomerLevel("VIP");
        testStrategy.setBasePrice(new BigDecimal("1000.00"));
        testStrategy.setPriority(1);
        testStrategy.setStatus("draft");
        testStrategy.setCreateTime(LocalDateTime.now());
        testStrategy.setUpdateTime(LocalDateTime.now());
    }
    
    @Test
    void testCreateStrategy_Success() {
        when(priceStrategyRepository.save(any(PriceStrategy.class))).thenReturn(testStrategy);
        
        PriceStrategy result = priceStrategyService.createStrategy(testStrategyDTO);
        
        assertNotNull(result);
        assertEquals("VIP客户价格策略", result.getName());
        assertEquals("draft", result.getStatus());
        verify(priceStrategyRepository, times(1)).save(any(PriceStrategy.class));
    }
    
    @Test
    void testUpdateStrategy_Success() {
        when(priceStrategyRepository.findById(1L)).thenReturn(Optional.of(testStrategy));
        when(priceStrategyRepository.save(any(PriceStrategy.class))).thenReturn(testStrategy);
        
        testStrategyDTO.setName("更新后的策略名称");
        PriceStrategy result = priceStrategyService.updateStrategy(1L, testStrategyDTO);
        
        assertNotNull(result);
        assertEquals("更新后的策略名称", result.getName());
        verify(priceStrategyRepository, times(1)).findById(1L);
        verify(priceStrategyRepository, times(1)).save(any(PriceStrategy.class));
    }
    
    @Test
    void testUpdateStrategy_NotFound() {
        when(priceStrategyRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> {
            priceStrategyService.updateStrategy(999L, testStrategyDTO);
        });
        
        verify(priceStrategyRepository, times(1)).findById(999L);
        verify(priceStrategyRepository, never()).save(any(PriceStrategy.class));
    }
    
    @Test
    void testGetStrategy_Success() {
        when(priceStrategyRepository.findById(1L)).thenReturn(Optional.of(testStrategy));
        
        PriceStrategy result = priceStrategyService.getStrategy(1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("VIP客户价格策略", result.getName());
        verify(priceStrategyRepository, times(1)).findById(1L);
    }
    
    @Test
    void testDeleteStrategy_Success() {
        when(priceStrategyRepository.findById(1L)).thenReturn(Optional.of(testStrategy));
        when(priceStrategyRepository.save(any(PriceStrategy.class))).thenReturn(testStrategy);
        
        priceStrategyService.deleteStrategy(1L);
        
        assertEquals(Integer.valueOf(1), testStrategy.getDeleted());
        verify(priceStrategyRepository, times(1)).findById(1L);
        verify(priceStrategyRepository, times(1)).save(any(PriceStrategy.class));
    }
    
    @Test
    void testListStrategies_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PriceStrategy> mockPage = new PageImpl<>(Arrays.asList(testStrategy));
        
        when(priceStrategyRepository.findAll(pageable)).thenReturn(mockPage);
        
        Page<PriceStrategy> result = priceStrategyService.listStrategies(pageable);
        
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(priceStrategyRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testActivateStrategy_Success() {
        when(priceStrategyRepository.findById(1L)).thenReturn(Optional.of(testStrategy));
        when(priceStrategyRepository.save(any(PriceStrategy.class))).thenReturn(testStrategy);
        
        PriceStrategy result = priceStrategyService.activateStrategy(1L);
        
        assertEquals("active", result.getStatus());
        verify(priceStrategyRepository, times(1)).findById(1L);
        verify(priceStrategyRepository, times(1)).save(any(PriceStrategy.class));
    }
    
    @Test
    void testDeactivateStrategy_Success() {
        when(priceStrategyRepository.findById(1L)).thenReturn(Optional.of(testStrategy));
        when(priceStrategyRepository.save(any(PriceStrategy.class))).thenReturn(testStrategy);
        
        PriceStrategy result = priceStrategyService.deactivateStrategy(1L);
        
        assertEquals("inactive", result.getStatus());
        verify(priceStrategyRepository, times(1)).findById(1L);
        verify(priceStrategyRepository, times(1)).save(any(PriceStrategy.class));
    }
    
    @Test
    void testCopyStrategy_Success() {
        PriceStrategy sourceStrategy = new PriceStrategy();
        sourceStrategy.setId(1L);
        sourceStrategy.setName("源策略");
        sourceStrategy.setDescription("源策略描述");
        sourceStrategy.setStrategyType("customer_level");
        sourceStrategy.setCustomerLevel("VIP");
        sourceStrategy.setBasePrice(new BigDecimal("1000.00"));
        sourceStrategy.setPriority(1);
        sourceStrategy.setCreateTime(LocalDateTime.now());
        
        when(priceStrategyRepository.findById(1L)).thenReturn(Optional.of(sourceStrategy));
        when(priceStrategyRepository.save(any(PriceStrategy.class))).thenAnswer(invocation -> {
            PriceStrategy saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        
        PriceStrategy result = priceStrategyService.copyStrategy(1L, "复制策略");
        
        assertNotNull(result);
        assertEquals("复制策略", result.getName());
        assertEquals("customer_level", result.getStrategyType());
        assertEquals("VIP", result.getCustomerLevel());
        verify(priceStrategyRepository, times(1)).findById(1L);
        verify(priceStrategyRepository, times(1)).save(any(PriceStrategy.class));
    }
}
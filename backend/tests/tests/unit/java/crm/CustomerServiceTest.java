package com.qizhi.crm.customer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 客户服务单元测试
 * 目标覆盖率: >=80%
 * 
 * @author qa-lead
 * @since 2026-04-22
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("客户服务测试")
public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerValidator customerValidator;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;
    private CustomerDTO testCustomerDTO;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
            .id(1L)
            .name("测试客户")
            .phone("13800138000")
            .email("test@example.com")
            .companyName("测试公司")
            .status(CustomerStatus.ACTIVE)
            .build();
            
        testCustomerDTO = CustomerDTO.builder()
            .id(1L)
            .name("测试客户")
            .phone("13800138000")
            .email("test@example.com")
            .companyName("测试公司")
            .build();
    }

    @Test
    @DisplayName("创建客户-成功场景")
    void createCustomer_Success() {
        // Given
        when(customerValidator.validate(any())).thenReturn(true);
        when(customerRepository.existsByPhone(any())).thenReturn(false);
        when(customerRepository.existsByEmail(any())).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(testCustomer);
        when(customerMapper.toDTO(any())).thenReturn(testCustomerDTO);

        // When
        CustomerDTO result = customerService.createCustomer(testCustomerDTO);

        // Then
        assertNotNull(result);
        assertEquals("测试客户", result.getName());
        assertEquals("13800138000", result.getPhone());
        verify(customerRepository).save(any());
    }

    @Test
    @DisplayName("创建客户-验证失败")
    void createCustomer_ValidationFailed() {
        // Given
        when(customerValidator.validate(any())).thenReturn(false);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            customerService.createCustomer(testCustomerDTO);
        });
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("创建客户-手机号已存在")
    void createCustomer_PhoneExists() {
        // Given
        when(customerValidator.validate(any())).thenReturn(true);
        when(customerRepository.existsByPhone("13800138000")).thenReturn(true);

        // When & Then
        assertThrows(DuplicateException.class, () -> {
            customerService.createCustomer(testCustomerDTO);
        });
    }

    @Test
    @DisplayName("根据ID查询客户-存在")
    void getCustomerById_Exists() {
        // Given
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerMapper.toDTO(testCustomer)).thenReturn(testCustomerDTO);

        // When
        CustomerDTO result = customerService.getCustomerById(1L);

        // Then
        assertNotNull(result);
        assertEquals("测试客户", result.getName());
        assertEquals("13800138000", result.getPhone());
    }

    @Test
    @DisplayName("根据ID查询客户-不存在")
    void getCustomerById_NotExists() {
        // Given
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            customerService.getCustomerById(999L);
        });
    }

    @Test
    @DisplayName("更新客户-成功")
    void updateCustomer_Success() {
        // Given
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerValidator.validate(any())).thenReturn(true);
        when(customerRepository.save(any())).thenReturn(testCustomer);
        when(customerMapper.toDTO(any())).thenReturn(testCustomerDTO);

        // When
        CustomerDTO result = customerService.updateCustomer(1L, testCustomerDTO);

        // Then
        assertNotNull(result);
        verify(customerRepository).save(any());
    }

    @Test
    @DisplayName("更新客户-客户不存在")
    void updateCustomer_NotExists() {
        // Given
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            customerService.updateCustomer(999L, testCustomerDTO);
        });
    }

    @Test
    @DisplayName("删除客户-成功")
    void deleteCustomer_Success() {
        // Given
        when(customerRepository.existsById(1L)).thenReturn(true);
        doNothing().when(customerRepository).deleteById(1L);

        // When
        customerService.deleteCustomer(1L);

        // Then
        verify(customerRepository).deleteById(1L);
    }

    @Test
    @DisplayName("删除客户-不存在")
    void deleteCustomer_NotExists() {
        // Given
        when(customerRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            customerService.deleteCustomer(999L);
        });
    }

    @Test
    @DisplayName("搜索客户-按名称")
    void searchCustomers_ByName() {
        // Given
        List<Customer> customers = Arrays.asList(testCustomer);
        when(customerRepository.findByNameContaining("测试")).thenReturn(customers);
        when(customerMapper.toDTO(any())).thenReturn(testCustomerDTO);

        // When
        List<CustomerDTO> result = customerService.searchCustomers("测试", null, null);

        // Then
        assertEquals(1, result.size());
        assertEquals("测试客户", result.get(0).getName());
    }

    @Test
    @DisplayName("搜索客户-按状态")
    void searchCustomers_ByStatus() {
        // Given
        List<Customer> customers = Arrays.asList(testCustomer);
        when(customerRepository.findByStatus(CustomerStatus.ACTIVE)).thenReturn(customers);
        when(customerMapper.toDTO(any())).thenReturn(testCustomerDTO);

        // When
        List<CustomerDTO> result = customerService.searchCustomers(null, CustomerStatus.ACTIVE, null);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("搜索客户-组合条件")
    void searchCustomers_CombinedConditions() {
        // Given
        List<Customer> customers = Arrays.asList(testCustomer);
        when(customerRepository.findByNameContainingAndStatus(any(), any())).thenReturn(customers);
        when(customerMapper.toDTO(any())).thenReturn(testCustomerDTO);

        // When
        List<CustomerDTO> result = customerService.searchCustomers("测试", CustomerStatus.ACTIVE, null);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取所有客户-分页")
    void getAllCustomers_Paginated() {
        // Given
        List<Customer> customers = Arrays.asList(testCustomer);
        Page<Customer> page = new PageImpl<>(customers);
        when(customerRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(customerMapper.toDTO(any())).thenReturn(testCustomerDTO);

        // When
        Page<CustomerDTO> result = customerService.getAllCustomers(PageRequest.of(0, 10));

        // Then
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("更新客户状态-成功")
    void updateCustomerStatus_Success() {
        // Given
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any())).thenReturn(testCustomer);
        when(customerMapper.toDTO(any())).thenReturn(testCustomerDTO);

        // When
        CustomerDTO result = customerService.updateCustomerStatus(1L, CustomerStatus.INACTIVE);

        // Then
        assertNotNull(result);
        verify(customerRepository).save(any());
    }

    @Test
    @DisplayName("批量导入客户-成功")
    void batchImportCustomers_Success() {
        // Given
        List<CustomerDTO> dtoList = Arrays.asList(testCustomerDTO);
        when(customerValidator.validate(any())).thenReturn(true);
        when(customerRepository.existsByPhone(any())).thenReturn(false);
        when(customerRepository.saveAll(any())).thenReturn(Arrays.asList(testCustomer));
        when(customerMapper.toDTO(any())).thenReturn(testCustomerDTO);

        // When
        List<CustomerDTO> result = customerService.batchImportCustomers(dtoList);

        // Then
        assertEquals(1, result.size());
        verify(customerRepository).saveAll(any());
    }

    @Test
    @DisplayName("批量导入客户-部分失败")
    void batchImportCustomers_PartialFailure() {
        // Given
        CustomerDTO invalidDTO = CustomerDTO.builder().build();
        List<CustomerDTO> dtoList = Arrays.asList(testCustomerDTO, invalidDTO);
        when(customerValidator.validate(testCustomerDTO)).thenReturn(true);
        when(customerValidator.validate(invalidDTO)).thenReturn(false);

        // When
        assertThrows(ValidationException.class, () -> {
            customerService.batchImportCustomers(dtoList);
        });
    }

    @Test
    @DisplayName("获取客户统计-成功")
    void getCustomerStatistics_Success() {
        // Given
        when(customerRepository.count()).thenReturn(100L);
        when(customerRepository.countByStatus(CustomerStatus.ACTIVE)).thenReturn(80L);
        when(customerRepository.countByStatus(CustomerStatus.INACTIVE)).thenReturn(20L);

        // When
        CustomerStatistics stats = customerService.getCustomerStatistics();

        // Then
        assertEquals(100L, stats.getTotalCount());
        assertEquals(80L, stats.getActiveCount());
        assertEquals(20L, stats.getInactiveCount());
    }
}
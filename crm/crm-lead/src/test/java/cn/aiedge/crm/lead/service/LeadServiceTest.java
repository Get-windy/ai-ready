package cn.aiedge.crm.lead.service;

import cn.aiedge.crm.lead.entity.Lead;
import cn.aiedge.crm.lead.mapper.LeadMapper;
import cn.aiedge.crm.lead.service.impl.LeadServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 线索服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadMapper leadMapper;

    @InjectMocks
    private LeadServiceImpl leadService;

    private Lead testLead;

    @BeforeEach
    void setUp() {
        testLead = new Lead();
        testLead.setTenantId(1L);
        testLead.setName("测试线索");
        testLead.setCompanyName("测试公司");
        testLead.setContactName("张三");
        testLead.setPhone("13800138000");
        testLead.setSource(1);
    }

    @Test
    @DisplayName("创建线索 - 成功")
    void testCreateLead_Success() {
        // Given
        when(leadMapper.insert(any(Lead.class))).thenReturn(1);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            // When
            Long leadId = leadService.createLead(testLead);

            // Then
            assertNotNull(leadId);
            assertEquals(0, testLead.getStatus()); // 新线索
            assertEquals(0, testLead.getFollowCount());
            assertEquals(50, testLead.getScore()); // 默认评分
            verify(leadMapper, times(1)).insert(any(Lead.class));
        }
    }

    @Test
    @DisplayName("创建线索 - 默认状态为新线索")
    void testCreateLead_DefaultStatus() {
        // Given
        when(leadMapper.insert(any(Lead.class))).thenReturn(1);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            // When
            leadService.createLead(testLead);

            // Then
            assertEquals(0, testLead.getStatus());
        }
    }

    @Test
    @DisplayName("创建线索 - 默认评分为50")
    void testCreateLead_DefaultScore() {
        // Given
        when(leadMapper.insert(any(Lead.class))).thenReturn(1);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            // When
            leadService.createLead(testLead);

            // Then
            assertEquals(50, testLead.getScore());
        }
    }
}
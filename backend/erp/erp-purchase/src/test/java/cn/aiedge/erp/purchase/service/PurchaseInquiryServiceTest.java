package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseInquiry;
import cn.aiedge.erp.purchase.enums.InquiryStatus;
import cn.aiedge.erp.purchase.mapper.PurchaseInquiryMapper;
import cn.aiedge.erp.purchase.service.impl.PurchaseInquiryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 询价单Service测试
 */
@ExtendWith(MockitoExtension.class)
class PurchaseInquiryServiceTest {

    @Mock
    private PurchaseInquiryMapper inquiryMapper;

    @InjectMocks
    private PurchaseInquiryServiceImpl inquiryService;

    @Test
    void testCreateInquiry() {
        PurchaseInquiry inquiry = new PurchaseInquiry();
        inquiry.setTitle("测试询价单");
        inquiry.setInquiryType("物资采购");
        inquiry.setDeadlineDate(LocalDateTime.now().plusDays(7));
        inquiry.setPurchaserId(1L);
        inquiry.setCreatedBy(1L);

        when(inquiryMapper.insert(any())).thenReturn(1);
        when(inquiryMapper.findById(any())).thenReturn(inquiry);

        PurchaseInquiry created = inquiryService.createInquiry(inquiry);
        
        assertNotNull(created);
        assertEquals(InquiryStatus.DRAFT, created.getStatus());
        assertNotNull(created.getInquiryNo());
        verify(inquiryMapper).insert(any());
    }

    @Test
    void testPublishInquiry() {
        PurchaseInquiry inquiry = new PurchaseInquiry();
        inquiry.setId(1L);
        inquiry.setStatus(InquiryStatus.DRAFT);

        when(inquiryMapper.findById(1L)).thenReturn(inquiry);
        when(inquiryMapper.updateStatus(anyLong(), anyString(), any())).thenReturn(1);

        PurchaseInquiry published = inquiryService.publishInquiry(1L);
        
        assertEquals(InquiryStatus.PUBLISHED, published.getStatus());
        assertNotNull(published.getPublishDate());
        verify(inquiryMapper).updateStatus(1L, InquiryStatus.PUBLISHED.name(), any());
    }

    @Test
    void testGetAllInquiries() {
        PurchaseInquiry inquiry1 = new PurchaseInquiry();
        inquiry1.setId(1L);
        inquiry1.setTitle("询价单1");

        PurchaseInquiry inquiry2 = new PurchaseInquiry();
        inquiry2.setId(2L);
        inquiry2.setTitle("询价单2");

        when(inquiryMapper.findAll()).thenReturn(Arrays.asList(inquiry1, inquiry2));

        List<PurchaseInquiry> inquiries = inquiryService.getAllInquiries();
        
        assertEquals(2, inquiries.size());
        verify(inquiryMapper).findAll();
    }

    @Test
    void testGenerateInquiryNo() {
        String inquiryNo = inquiryService.generateInquiryNo();
        
        assertNotNull(inquiryNo);
        assertTrue(inquiryNo.startsWith("INQ"));
        assertEquals(15, inquiryNo.length()); // INQ + 8位日期 + 4位随机
    }
}
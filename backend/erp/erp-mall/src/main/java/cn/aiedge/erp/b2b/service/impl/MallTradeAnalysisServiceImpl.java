package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.erp.b2b.dto.TradeAnalysisDTO;
import cn.aiedge.erp.b2b.mapper.MallTradeAnalysisMapper;
import cn.aiedge.erp.b2b.service.MallTradeAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商城交易分析服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallTradeAnalysisServiceImpl implements MallTradeAnalysisService {

    private final MallTradeAnalysisMapper tradeAnalysisMapper;

    @Override
    public TradeAnalysisDTO tradeAnalysis(String startDate, String endDate) {
        LocalDateTime startTime = parseStart(startDate);
        LocalDateTime endTime = parseEnd(endDate);

        TradeAnalysisDTO result = new TradeAnalysisDTO();
        result.setSummary(tradeAnalysisMapper.selectSummary(startTime, endTime));
        result.setDaily(tradeAnalysisMapper.selectDaily(startTime, endTime));

        List<TradeAnalysisDTO.PaymentStatusItem> distribution =
                tradeAnalysisMapper.selectPaymentStatusDistribution(startTime, endTime);
        distribution.forEach(item -> item.setPaymentStatusName(mapPaymentStatus(item.getPaymentStatus())));
        result.setPaymentStatusDistribution(distribution);
        return result;
    }

    /**
     * 支付状态映射: 0待支付,1支付中,2已支付,3部分支付,4已退款
     */
    private String mapPaymentStatus(Integer paymentStatus) {
        if (paymentStatus == null) {
            return "未知";
        }
        return switch (paymentStatus) {
            case 0 -> "待支付";
            case 1 -> "支付中";
            case 2 -> "已支付";
            case 3 -> "部分支付";
            case 4 -> "已退款";
            default -> "未知";
        };
    }

    private LocalDateTime parseStart(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim()).atStartOfDay();
        } catch (Exception e) {
            log.warn("无效的开始日期格式: {}, 将跳过该过滤", dateStr);
            return null;
        }
    }

    private LocalDateTime parseEnd(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim()).atTime(23, 59, 59);
        } catch (Exception e) {
            log.warn("无效的结束日期格式: {}, 将跳过该过滤", dateStr);
            return null;
        }
    }
}

package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.dto.StaffCommissionSummaryDTO;
import cn.aiedge.erp.marketing.entity.CommissionRecord;
import cn.aiedge.erp.marketing.mapper.CommissionRecordMapper;
import cn.aiedge.erp.marketing.service.CommissionRecordService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
public class CommissionRecordServiceImpl extends ServiceImpl<CommissionRecordMapper, CommissionRecord>
        implements CommissionRecordService {

    @Override
    public Page<StaffCommissionSummaryDTO> pageStaffSummary(long current, long size,
                                                            String startDate, String endDate, String keyword) {
        LocalDateTime startTime = parseStart(startDate);
        LocalDateTime endTime = parseEnd(endDate);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<StaffCommissionSummaryDTO> page = new Page<>(current, size);
        return (Page<StaffCommissionSummaryDTO>) baseMapper.selectStaffSummaryPage(page, startTime, endTime, kw);
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

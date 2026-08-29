package cn.aiedge.hr.service.impl;

import cn.aiedge.hr.entity.HrRecruitment;
import cn.aiedge.hr.mapper.HrRecruitmentMapper;
import cn.aiedge.hr.service.HrRecruitmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 招聘职位服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrRecruitmentServiceImpl extends ServiceImpl<HrRecruitmentMapper, HrRecruitment>
        implements HrRecruitmentService {

    @Override
    public Page<HrRecruitment> pageList(Page<HrRecruitment> page, Long tenantId,
                                         Integer status, String positionName, String channel,
                                         String startDate, String endDate) {
        LambdaQueryWrapper<HrRecruitment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrRecruitment::getTenantId, tenantId)
               .eq(status != null, HrRecruitment::getStatus, status)
               .like(positionName != null && !positionName.isEmpty(),
                     HrRecruitment::getPositionName, positionName)
               .eq(channel != null && !channel.isEmpty(),
                   HrRecruitment::getChannel, channel);

        // 日期范围筛选 - 使用 parseLocalDate 避免 PostgreSQL 类型不匹配
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(HrRecruitment::getPublishDate, parseLocalDate(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(HrRecruitment::getPublishDate, parseLocalDate(endDate));
        }

        wrapper.orderByDesc(HrRecruitment::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<HrRecruitment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrRecruitment::getId, id)
               .set(HrRecruitment::getStatus, status)
               .set(HrRecruitment::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        log.info("更新招聘职位状态: id={}, status={}", id, status);
    }

    /**
     * 安全解析日期字符串为 LocalDate，避免 PostgreSQL 类型不匹配
     */
    private LocalDate parseLocalDate(String dateString) {
        try {
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误: " + dateString + "，期望格式: yyyy-MM-dd");
        }
    }
}

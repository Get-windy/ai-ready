package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.CollectionStatsDTO;
import cn.aiedge.erp.finance.mapper.CollectionStatsMapper;
import cn.aiedge.erp.finance.service.CollectionStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 回款统计Service实现类
 * 数据源：收款单 erp_receipt，状态口径见 CollectionStatsMapper 注释
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionStatsServiceImpl implements CollectionStatsService {

    private final CollectionStatsMapper collectionStatsMapper;

    /**
     * 分组维度 -> SQL分组表达式/名称表达式 白名单
     * erp_receipt 自带 sales_person_id/sales_person_name，staff 维度直接取收款单业务员
     */
    private static final Map<String, String[]> GROUP_EXPRS = Map.of(
            "day", new String[]{"receipt_date::text", "NULL"},
            "week", new String[]{"date_trunc('week', receipt_date)::date::text", "NULL"},
            "month", new String[]{"to_char(receipt_date, 'YYYY-MM')", "NULL"},
            "staff", new String[]{"COALESCE(sales_person_id::text, '')", "COALESCE(MAX(sales_person_name), '未指定')"},
            "customer", new String[]{"COALESCE(customer_id::text, '')", "MAX(customer_name)"}
    );

    @Override
    public CollectionStatsDTO stats(LocalDate startDate, LocalDate endDate, String groupBy) {
        String dimension = StringUtils.hasText(groupBy) ? groupBy : "day";
        String[] exprs = GROUP_EXPRS.get(dimension);
        if (exprs == null) {
            throw BusinessException.badRequest("不支持的分组维度: " + groupBy + "，可选: day/week/month/staff/customer");
        }

        CollectionStatsDTO.Summary summary = collectionStatsMapper.selectSummary(startDate, endDate);
        List<CollectionStatsDTO.Detail> details = collectionStatsMapper.selectDetails(startDate, endDate, exprs[0], exprs[1]);

        CollectionStatsDTO result = new CollectionStatsDTO();
        result.setSummary(summary);
        result.setDetails(details);
        return result;
    }
}

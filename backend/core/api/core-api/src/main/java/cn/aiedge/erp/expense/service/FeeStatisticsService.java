package cn.aiedge.erp.expense.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeStatisticsQueryRequest;
import cn.aiedge.erp.expense.dto.FeeStatisticsVO;

import java.util.List;
import java.util.Map;

/**
 * 费用统计服务接口
 */
public interface FeeStatisticsService {

    /**
     * 分页查询统计台账
     */
    PageResult<FeeStatisticsVO> pageList(FeeStatisticsQueryRequest request);

    /**
     * 获取汇总统计数据
     */
    Map<String, Object> getSummaryData(Integer year, Integer month, Long departmentId);

    /**
     * 按部门统计
     */
    List<FeeStatisticsVO> getByDepartment(Integer year, Integer month);

    /**
     * 按费用类型统计
     */
    List<FeeStatisticsVO> getByExpenseType(Integer year, Integer month, Long departmentId);

    /**
     * 手动刷新统计数据
     */
    void refreshStatistics(Integer year, Integer month);
}

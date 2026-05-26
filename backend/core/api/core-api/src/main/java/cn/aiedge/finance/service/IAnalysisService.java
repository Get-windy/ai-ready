package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.DataAnalysis;
import cn.aiedge.finance.entity.AnalysisReport;
import cn.aiedge.finance.dto.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 数据分析服务接口
 */
public interface IAnalysisService {

    // 数据分析相关方法
    /**
     * 创建数据分析
     */
    Long createDataAnalysis(DataAnalysisCreateRequest request);

    /**
     * 更新数据分析
     */
    void updateDataAnalysis(DataAnalysis dataAnalysis);

    /**
     * 分页查询数据分析
     */
    Page<DataAnalysisVO> pageDataAnalyses(DataAnalysisQueryRequest request);

    /**
     * 根据ID获取数据分析详情
     */
    DataAnalysisVO getDataAnalysisById(Long id);

    /**
     * 删除数据分析
     */
    void deleteDataAnalysis(Long id);

    /**
     * 执行数据分析
     */
    void executeDataAnalysis(Long id);

    /**
     * 导出分析结果
     */
    byte[] exportAnalysisResult(Long id, String format);

    // 分析报告相关方法
    /**
     * 创建分析报告
     */
    Long createAnalysisReport(AnalysisReportCreateRequest request);

    /**
     * 更新分析报告
     */
    void updateAnalysisReport(AnalysisReport analysisReport);

    /**
     * 分页查询分析报告
     */
    Page<AnalysisReportVO> pageAnalysisReports(AnalysisReportQueryRequest request);

    /**
     * 根据ID获取分析报告详情
     */
    AnalysisReportVO getAnalysisReportById(Long id);

    /**
     * 删除分析报告
     */
    void deleteAnalysisReport(Long id);

    /**
     * 发布分析报告
     */
    void publishAnalysisReport(Long id);

    /**
     * 生成分析报告
     */
    void generateAnalysisReport(Long id);

    /**
     * 导出分析报告
     */
    byte[] exportAnalysisReport(Long id, String format);

    /**
     * 批量分析
     */
    void batchAnalyze(String analysisType, String period);
}

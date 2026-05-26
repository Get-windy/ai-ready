package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.ReportTemplate;
import cn.aiedge.finance.entity.DataSource;
import cn.aiedge.finance.entity.ChartConfig;
import cn.aiedge.finance.entity.ReportInstance;
import cn.aiedge.finance.dto.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 智能报表服务接口
 */
public interface IIntelligentReportService {

    // 报表模板管理
    /**
     * 创建报表模板
     */
    Long createReportTemplate(ReportTemplateCreateRequest request);

    /**
     * 更新报表模板
     */
    void updateReportTemplate(ReportTemplate template);

    /**
     * 分页查询报表模板
     */
    Page<ReportTemplateVO> pageReportTemplates(ReportTemplateQueryRequest request);

    /**
     * 根据ID获取报表模板详情
     */
    ReportTemplateVO getReportTemplateById(Long id);

    /**
     * 删除报表模板
     */
    void deleteReportTemplate(Long id);

    /**
     * 激活/停用报表模板
     */
    void toggleReportTemplateActive(Long id, boolean active);

    // 数据源管理
    /**
     * 创建数据源
     */
    Long createDataSource(DataSourceCreateRequest request);

    /**
     * 更新数据源
     */
    void updateDataSource(DataSource dataSource);

    /**
     * 分页查询数据源
     */
    Page<DataSourceVO> pageDataSources(DataSourceQueryRequest request);

    /**
     * 根据ID获取数据源详情
     */
    DataSourceVO getDataSourceById(Long id);

    /**
     * 删除数据源
     */
    void deleteDataSource(Long id);

    /**
     * 测试数据源连接
     */
    boolean testDataSourceConnection(Long id);

    /**
     * 预览数据源数据
     */
    Object previewDataSourceData(Long id, String query, int limit);

    // 图表配置管理
    /**
     * 创建图表配置
     */
    Long createChartConfig(ChartConfigCreateRequest request);

    /**
     * 更新图表配置
     */
    void updateChartConfig(ChartConfig chartConfig);

    /**
     * 分页查询图表配置
     */
    Page<ChartConfigVO> pageChartConfigs(ChartConfigQueryRequest request);

    /**
     * 根据ID获取图表配置详情
     */
    ChartConfigVO getChartConfigById(Long id);

    /**
     * 删除图表配置
     */
    void deleteChartConfig(Long id);

    /**
     * 获取图表数据
     */
    Object getChartData(Long id);

    // 报表实例管理
    /**
     * 生成报表实例
     */
    Long generateReportInstance(ReportInstanceCreateRequest request);

    /**
     * 更新报表实例
     */
    void updateReportInstance(ReportInstance instance);

    /**
     * 分页查询报表实例
     */
    Page<ReportInstanceVO> pageReportInstances(ReportInstanceQueryRequest request);

    /**
     * 根据ID获取报表实例详情
     */
    ReportInstanceVO getReportInstanceById(Long id);

    /**
     * 删除报表实例
     */
    void deleteReportInstance(Long id);

    /**
     * 导出报表
     */
    byte[] exportReport(Long id, String format);

    /**
     * 发布报表
     */
    void publishReport(Long id);

    /**
     * 获取报表数据
     */
    Object getReportData(Long id);

    /**
     * 执行自定义SQL查询
     */
    Object executeCustomQuery(String dataSourceId, String sql, Object[] params);
}

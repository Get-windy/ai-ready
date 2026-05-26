package cn.aiedge.finance.controller;

import cn.aiedge.finance.entity.ReportTemplate;
import cn.aiedge.finance.entity.DataSource;
import cn.aiedge.finance.entity.ChartConfig;
import cn.aiedge.finance.entity.ReportInstance;

import cn.aiedge.finance.dto.*;
import cn.aiedge.finance.service.IIntelligentReportService;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 智能报表控制器
 */
@RestController
@RequestMapping("/api/finance/intelligent-report")
@Tag(name = "智能报表管理", description = "智能报表相关操作接口")
public class IntelligentReportController {

    private final IIntelligentReportService intelligentReportService;

    public IntelligentReportController(IIntelligentReportService intelligentReportService) {
        this.intelligentReportService = intelligentReportService;
    }

    // 报表模板相关接口
    @PostMapping("/template/create")
    @Operation(summary = "创建报表模板")
    public ApiResponse<Long> createReportTemplate(@Valid @RequestBody ReportTemplateCreateRequest request) {
        Long id = intelligentReportService.createReportTemplate(request);
        return ApiResponse.success(id);
    }

    @PutMapping("/template/update")
    @Operation(summary = "更新报表模板")
    public ApiResponse<Void> updateReportTemplate(@Valid @RequestBody ReportTemplate template) {
        intelligentReportService.updateReportTemplate(template);
        return ApiResponse.success();
    }

    @PostMapping("/template/list")
    @Operation(summary = "分页查询报表模板")
    public ApiResponse<PageResult<ReportTemplateVO>> pageReportTemplates(@RequestBody ReportTemplateQueryRequest request) {
        Page<ReportTemplateVO> pageResult = intelligentReportService.pageReportTemplates(request);
        PageResult<ReportTemplateVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/template/{id}")
    @Operation(summary = "根据ID获取报表模板详情")
    public ApiResponse<ReportTemplateVO> getReportTemplateById(@PathVariable Long id) {
        ReportTemplateVO templateVO = intelligentReportService.getReportTemplateById(id);
        return ApiResponse.success(templateVO);
    }

    @DeleteMapping("/template/{id}")
    @Operation(summary = "删除报表模板")
    public ApiResponse<Void> deleteReportTemplate(@PathVariable Long id) {
        intelligentReportService.deleteReportTemplate(id);
        return ApiResponse.success();
    }

    @PostMapping("/template/toggle-active/{id}")
    @Operation(summary = "激活/停用报表模板")
    public ApiResponse<Void> toggleReportTemplateActive(@PathVariable Long id, @RequestParam boolean active) {
        intelligentReportService.toggleReportTemplateActive(id, active);
        return ApiResponse.success();
    }

    // 数据源相关接口
    @PostMapping("/datasource/create")
    @Operation(summary = "创建数据源")
    public ApiResponse<Long> createDataSource(@Valid @RequestBody DataSourceCreateRequest request) {
        Long id = intelligentReportService.createDataSource(request);
        return ApiResponse.success(id);
    }

    @PutMapping("/datasource/update")
    @Operation(summary = "更新数据源")
    public ApiResponse<Void> updateDataSource(@Valid @RequestBody DataSource dataSource) {
        intelligentReportService.updateDataSource(dataSource);
        return ApiResponse.success();
    }

    @PostMapping("/datasource/list")
    @Operation(summary = "分页查询数据源")
    public ApiResponse<PageResult<DataSourceVO>> pageDataSources(@RequestBody DataSourceQueryRequest request) {
        Page<DataSourceVO> pageResult = intelligentReportService.pageDataSources(request);
        PageResult<DataSourceVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/datasource/{id}")
    @Operation(summary = "根据ID获取数据源详情")
    public ApiResponse<DataSourceVO> getDataSourceById(@PathVariable Long id) {
        DataSourceVO dataSourceVO = intelligentReportService.getDataSourceById(id);
        return ApiResponse.success(dataSourceVO);
    }

    @DeleteMapping("/datasource/{id}")
    @Operation(summary = "删除数据源")
    public ApiResponse<Void> deleteDataSource(@PathVariable Long id) {
        intelligentReportService.deleteDataSource(id);
        return ApiResponse.success();
    }

    @PostMapping("/datasource/test-connection/{id}")
    @Operation(summary = "测试数据源连接")
    public ApiResponse<Boolean> testDataSourceConnection(@PathVariable Long id) {
        boolean success = intelligentReportService.testDataSourceConnection(id);
        return ApiResponse.success(success);
    }

    @PostMapping("/datasource/preview-data/{id}")
    @Operation(summary = "预览数据源数据")
    public ApiResponse<Object> previewDataSourceData(@PathVariable Long id, 
                                                    @RequestParam String query, 
                                                    @RequestParam(defaultValue = "100") int limit) {
        Object data = intelligentReportService.previewDataSourceData(id, query, limit);
        return ApiResponse.success(data);
    }

    // 图表配置相关接口
    @PostMapping("/chart-config/create")
    @Operation(summary = "创建图表配置")
    public ApiResponse<Long> createChartConfig(@Valid @RequestBody ChartConfigCreateRequest request) {
        Long id = intelligentReportService.createChartConfig(request);
        return ApiResponse.success(id);
    }

    @PutMapping("/chart-config/update")
    @Operation(summary = "更新图表配置")
    public ApiResponse<Void> updateChartConfig(@Valid @RequestBody ChartConfig chartConfig) {
        intelligentReportService.updateChartConfig(chartConfig);
        return ApiResponse.success();
    }

    @PostMapping("/chart-config/list")
    @Operation(summary = "分页查询图表配置")
    public ApiResponse<PageResult<ChartConfigVO>> pageChartConfigs(@RequestBody ChartConfigQueryRequest request) {
        Page<ChartConfigVO> pageResult = intelligentReportService.pageChartConfigs(request);
        PageResult<ChartConfigVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/chart-config/{id}")
    @Operation(summary = "根据ID获取图表配置详情")
    public ApiResponse<ChartConfigVO> getChartConfigById(@PathVariable Long id) {
        ChartConfigVO chartConfigVO = intelligentReportService.getChartConfigById(id);
        return ApiResponse.success(chartConfigVO);
    }

    @DeleteMapping("/chart-config/{id}")
    @Operation(summary = "删除图表配置")
    public ApiResponse<Void> deleteChartConfig(@PathVariable Long id) {
        intelligentReportService.deleteChartConfig(id);
        return ApiResponse.success();
    }

    @GetMapping("/chart-config/data/{id}")
    @Operation(summary = "获取图表数据")
    public ApiResponse<Object> getChartData(@PathVariable Long id) {
        Object data = intelligentReportService.getChartData(id);
        return ApiResponse.success(data);
    }

    // 报表实例相关接口
    @PostMapping("/instance/generate")
    @Operation(summary = "生成报表实例")
    public ApiResponse<Long> generateReportInstance(@Valid @RequestBody ReportInstanceCreateRequest request) {
        Long id = intelligentReportService.generateReportInstance(request);
        return ApiResponse.success(id);
    }

    @PutMapping("/instance/update")
    @Operation(summary = "更新报表实例")
    public ApiResponse<Void> updateReportInstance(@Valid @RequestBody ReportInstance instance) {
        intelligentReportService.updateReportInstance(instance);
        return ApiResponse.success();
    }

    @PostMapping("/instance/list")
    @Operation(summary = "分页查询报表实例")
    public ApiResponse<PageResult<ReportInstanceVO>> pageReportInstances(@RequestBody ReportInstanceQueryRequest request) {
        Page<ReportInstanceVO> pageResult = intelligentReportService.pageReportInstances(request);
        PageResult<ReportInstanceVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/instance/{id}")
    @Operation(summary = "根据ID获取报表实例详情")
    public ApiResponse<ReportInstanceVO> getReportInstanceById(@PathVariable Long id) {
        ReportInstanceVO instanceVO = intelligentReportService.getReportInstanceById(id);
        return ApiResponse.success(instanceVO);
    }

    @DeleteMapping("/instance/{id}")
    @Operation(summary = "删除报表实例")
    public ApiResponse<Void> deleteReportInstance(@PathVariable Long id) {
        intelligentReportService.deleteReportInstance(id);
        return ApiResponse.success();
    }

    @GetMapping("/instance/export/{id}")
    @Operation(summary = "导出报表")
    public ApiResponse<byte[]> exportReport(@PathVariable Long id, @RequestParam(defaultValue = "PDF") String format) {
        byte[] result = intelligentReportService.exportReport(id, format);
        return ApiResponse.success(result);
    }

    @PostMapping("/instance/publish/{id}")
    @Operation(summary = "发布报表")
    public ApiResponse<Void> publishReport(@PathVariable Long id) {
        intelligentReportService.publishReport(id);
        return ApiResponse.success();
    }

    @GetMapping("/instance/data/{id}")
    @Operation(summary = "获取报表数据")
    public ApiResponse<Object> getReportData(@PathVariable Long id) {
        Object data = intelligentReportService.getReportData(id);
        return ApiResponse.success(data);
    }

    @PostMapping("/execute-custom-query")
    @Operation(summary = "执行自定义SQL查询")
    public ApiResponse<Object> executeCustomQuery(@RequestParam String dataSourceId, 
                                                  @RequestParam String sql, 
                                                  @RequestBody(required = false) Object[] params) {
        Object result = intelligentReportService.executeCustomQuery(dataSourceId, sql, params);
        return ApiResponse.success(result);
    }
}

package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.*;
import cn.aiedge.finance.service.IAnalysisService;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据分析控制器
 */
@RestController
@RequestMapping("/api/finance/analysis")
@Tag(name = "数据分析管理", description = "数据分析相关操作接口")
public class AnalysisController {

    private final IAnalysisService analysisService;

    public AnalysisController(IAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    // 数据分析相关接口
    @PostMapping("/data-analysis/create")
    @Operation(summary = "创建数据分析")
    public ApiResponse<Long> createDataAnalysis(@Valid @RequestBody DataAnalysisCreateRequest request) {
        Long id = analysisService.createDataAnalysis(request);
        return ApiResponse.success(id);
    }

    @PostMapping("/data-analysis/list")
    @Operation(summary = "分页查询数据分析")
    public ApiResponse<PageResult<DataAnalysisVO>> pageDataAnalyses(@RequestBody DataAnalysisQueryRequest request) {
        Page<DataAnalysisVO> pageResult = analysisService.pageDataAnalyses(request);
        PageResult<DataAnalysisVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/data-analysis/{id}")
    @Operation(summary = "根据ID获取数据分析详情")
    public ApiResponse<DataAnalysisVO> getDataAnalysisById(@PathVariable Long id) {
        DataAnalysisVO analysisVO = analysisService.getDataAnalysisById(id);
        return ApiResponse.success(analysisVO);
    }

    @DeleteMapping("/data-analysis/{id}")
    @Operation(summary = "删除数据分析")
    public ApiResponse<Void> deleteDataAnalysis(@PathVariable Long id) {
        analysisService.deleteDataAnalysis(id);
        return ApiResponse.success();
    }

    @DeleteMapping("/data-analysis/batch")
    @Operation(summary = "批量删除数据分析")
    public ApiResponse<Void> batchDeleteDataAnalysis(@RequestBody List<Long> ids) {
        ids.forEach(id -> analysisService.deleteDataAnalysis(id));
        return ApiResponse.success();
    }

    @PostMapping("/data-analysis/execute/{id}")
    @Operation(summary = "执行数据分析")
    public ApiResponse<Void> executeDataAnalysis(@PathVariable Long id) {
        analysisService.executeDataAnalysis(id);
        return ApiResponse.success();
    }

    @GetMapping("/data-analysis/export/{id}")
    @Operation(summary = "导出分析结果")
    public ApiResponse<byte[]> exportAnalysisResult(@PathVariable Long id, @RequestParam(defaultValue = "PDF") String format) {
        byte[] result = analysisService.exportAnalysisResult(id, format);
        return ApiResponse.success(result);
    }

    // 分析报告相关接口
    @PostMapping("/report/create")
    @Operation(summary = "创建分析报告")
    public ApiResponse<Long> createAnalysisReport(@Valid @RequestBody AnalysisReportCreateRequest request) {
        Long id = analysisService.createAnalysisReport(request);
        return ApiResponse.success(id);
    }

    @PostMapping("/report/list")
    @Operation(summary = "分页查询分析报告")
    public ApiResponse<PageResult<AnalysisReportVO>> pageAnalysisReports(@RequestBody AnalysisReportQueryRequest request) {
        Page<AnalysisReportVO> pageResult = analysisService.pageAnalysisReports(request);
        PageResult<AnalysisReportVO> result = new PageResult<>();
        result.setRecords(pageResult.getRecords());
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getCurrent());
        result.setPageSize(pageResult.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/report/{id}")
    @Operation(summary = "根据ID获取分析报告详情")
    public ApiResponse<AnalysisReportVO> getAnalysisReportById(@PathVariable Long id) {
        AnalysisReportVO reportVO = analysisService.getAnalysisReportById(id);
        return ApiResponse.success(reportVO);
    }

    @DeleteMapping("/report/{id}")
    @Operation(summary = "删除分析报告")
    public ApiResponse<Void> deleteAnalysisReport(@PathVariable Long id) {
        analysisService.deleteAnalysisReport(id);
        return ApiResponse.success();
    }

    @DeleteMapping("/report/batch")
    @Operation(summary = "批量删除分析报告")
    public ApiResponse<Void> batchDeleteAnalysisReport(@RequestBody List<Long> ids) {
        ids.forEach(id -> analysisService.deleteAnalysisReport(id));
        return ApiResponse.success();
    }

    @PostMapping("/report/publish/{id}")
    @Operation(summary = "发布分析报告")
    public ApiResponse<Void> publishAnalysisReport(@PathVariable Long id) {
        analysisService.publishAnalysisReport(id);
        return ApiResponse.success();
    }

    @PostMapping("/report/generate/{id}")
    @Operation(summary = "生成分析报告")
    public ApiResponse<Void> generateAnalysisReport(@PathVariable Long id) {
        analysisService.generateAnalysisReport(id);
        return ApiResponse.success();
    }

    @GetMapping("/report/export/{id}")
    @Operation(summary = "导出分析报告")
    public ApiResponse<byte[]> exportAnalysisReport(@PathVariable Long id, @RequestParam(defaultValue = "PDF") String format) {
        byte[] result = analysisService.exportAnalysisReport(id, format);
        return ApiResponse.success(result);
    }

    @PostMapping("/batch-analyze")
    @Operation(summary = "批量分析")
    public ApiResponse<Void> batchAnalyze(@RequestParam String analysisType, @RequestParam String period) {
        analysisService.batchAnalyze(analysisType, period);
        return ApiResponse.success();
    }
}

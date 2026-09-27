package cn.aiedge.erp.party.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.party.entity.CustomerGrade;
import cn.aiedge.erp.party.service.CustomerGradeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 客户级别（唯一数据源 = biz_customer_grade）。
 *
 * <p>2026-09-26 收敛：原「资料 → 往来单位 → 客户 → 客户级别」子标签走的是另一套
 * {@code /api/erp/customer/level}（表 {@code erp_customer_level}，实体 CustomerLevel），
 * 与本站点各模块（商城买家账号 / 店铺设置 / 客户分析 / 客户表单与筛选下拉）读取的
 * {@code biz_customer_grade} 形成**双数据源**：在子标签新建的级别不出现在下拉里。
 * 现已统一到 {@code biz_customer_grade}，旧表与旧端点整体下线。
 */
@Tag(name = "客户等级管理", description = "客户等级CRUD接口（biz_customer_grade，全站唯一数据源）")
@RestController
@RequestMapping("/api/erp/partner/grades")
@RequiredArgsConstructor
public class CustomerGradeController {

    private final CustomerGradeService customerGradeService;

    /**
     * 客户级别下拉（只返回启用项）。
     *
     * @param gradeType 历史参数：早期级别表按 grade_type 区分客户/供应商等级，
     *                  收敛后客户级别只有一套（biz_customer_grade 无该列），此参数保留仅为兼容
     *                  既有前端调用签名，不参与过滤。
     */
    @Operation(summary = "获取客户等级列表（仅启用）")
    @SaCheckPermission("party:grades:view")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerGrade>>> getGrades(
            @RequestParam(required = false) String gradeType) {
        return ResponseEntity.ok(ApiResponse.ok(customerGradeService.listActiveGrades()));
    }

    @Operation(summary = "客户等级分页（含停用）")
    @SaCheckPermission("party:grades:view")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<IPage<CustomerGrade>>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<CustomerGrade> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(CustomerGrade::getGradeName, kw)
                              .or().like(CustomerGrade::getGradeCode, kw));
        }
        if (status != null) {
            wrapper.eq(CustomerGrade::getStatus, status);
        }
        wrapper.orderByAsc(CustomerGrade::getGradeLevel).orderByAsc(CustomerGrade::getSortOrder);
        return ResponseEntity.ok(ApiResponse.ok(customerGradeService.page(new Page<>(pageNum, pageSize), wrapper)));
    }

    @Operation(summary = "根据ID获取客户等级")
    @SaCheckPermission("party:grades:detail")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerGrade>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(customerGradeService.getById(id)));
    }

    @Operation(summary = "创建客户等级")
    @SaCheckPermission("party:grades:create")
    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> create(@RequestBody CustomerGrade grade) {
        if (grade.getGradeCode() != null && !grade.getGradeCode().isBlank()
                && customerGradeService.checkGradeCodeExists(grade.getGradeCode())) {
            throw BusinessException.badRequest("级别编码已存在：" + grade.getGradeCode());
        }
        if (grade.getStatus() == null) {
            grade.setStatus(1);
        }
        return ResponseEntity.ok(ApiResponse.ok(customerGradeService.save(grade)));
    }

    @Operation(summary = "更新客户等级")
    @SaCheckPermission("party:grades:update")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Long id, @RequestBody CustomerGrade grade) {
        if (grade.getGradeCode() != null && !grade.getGradeCode().isBlank()
                && customerGradeService.checkGradeCodeExists(grade.getGradeCode(), id)) {
            throw BusinessException.badRequest("级别编码已存在：" + grade.getGradeCode());
        }
        grade.setId(id);
        return ResponseEntity.ok(ApiResponse.ok(customerGradeService.updateById(grade)));
    }

    @Operation(summary = "删除客户等级")
    @SaCheckPermission("party:grades:delete")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Long id) {
        CustomerGrade grade = customerGradeService.getById(id);
        if (grade == null) {
            throw BusinessException.notFound("客户级别不存在");
        }
        // 引用保护：客户档案按级别名称关联（biz_party.party_level），级别被使用中不允许删除
        long used = customerGradeService.countUsedByGradeName(grade.getGradeName());
        if (used > 0) {
            throw BusinessException.badRequest(
                    "该级别已被 " + used + " 个客户使用，请改用「停用」");
        }
        return ResponseEntity.ok(ApiResponse.ok(customerGradeService.removeById(id)));
    }
}

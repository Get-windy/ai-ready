package cn.aiedge.erp.party.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.party.entity.CustomerGrade;
import cn.aiedge.erp.party.service.CustomerGradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "客户等级管理", description = "客户等级CRUD接口")
@RestController
@RequestMapping("/api/erp/partner/grades")
@RequiredArgsConstructor
public class CustomerGradeController {

    private final CustomerGradeService customerGradeService;

    @Operation(summary = "获取客户等级列表")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerGrade>>> getGrades(@RequestParam(required = false) String gradeType) {
        List<CustomerGrade> grades = customerGradeService.list();
        return ResponseEntity.ok(ApiResponse.ok(grades));
    }

    @Operation(summary = "根据ID获取客户等级")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerGrade>> getById(@PathVariable Long id) {
        CustomerGrade grade = customerGradeService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(grade));
    }

    @Operation(summary = "创建客户等级")
    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> create(@RequestBody CustomerGrade grade) {
        boolean success = customerGradeService.save(grade);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    @Operation(summary = "更新客户等级")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Long id, @RequestBody CustomerGrade grade) {
        grade.setId(id);
        boolean success = customerGradeService.updateById(grade);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    @Operation(summary = "删除客户等级")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Long id) {
        boolean success = customerGradeService.removeById(id);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }
}
package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PartnerGrade;
import cn.aiedge.erp.stock.service.PartnerGradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "往来单位等级管理")
@RestController
@RequestMapping("/api/erp/partner/grades")
@RequiredArgsConstructor
public class PartnerGradeController {

    private final PartnerGradeService partnerGradeService;

    @Operation(summary = "查询等级列表(按type筛选)")
    @GetMapping
    public Result<List<PartnerGrade>> list(@RequestParam(required = false) String gradeType) {
        if (gradeType != null) {
            return Result.ok(partnerGradeService.getByType(gradeType));
        }
        return Result.ok(partnerGradeService.lambdaQuery().eq(PartnerGrade::getDeleted, 0)
                .orderByAsc(PartnerGrade::getSortOrder).list());
    }

    @Operation(summary = "新增等级")
    @PostMapping
    public Result<Boolean> create(@RequestBody PartnerGrade grade) {
        return Result.ok(partnerGradeService.save(grade));
    }

    @Operation(summary = "更新等级")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PartnerGrade grade) {
        grade.setId(id);
        return Result.ok(partnerGradeService.updateById(grade));
    }

    @Operation(summary = "删除等级")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(partnerGradeService.removeById(id));
    }
}

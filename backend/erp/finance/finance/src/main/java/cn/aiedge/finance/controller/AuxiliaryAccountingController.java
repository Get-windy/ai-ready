package cn.aiedge.finance.controller;

import cn.aiedge.finance.entity.AuxiliaryAccounting;
import cn.aiedge.finance.enums.AuxiliaryType;
import cn.aiedge.finance.service.AuxiliaryAccountingService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance/auxiliary-accounting")
@Tag(name = "辅助核算管理", description = "辅助核算项的增删改查")
public class AuxiliaryAccountingController {
    
    @Autowired
    private AuxiliaryAccountingService auxiliaryAccountingService;
    
    @GetMapping("/list/type/{type}")
    @Operation(summary = "按类型获取辅助核算列表")
    public List<AuxiliaryAccounting> listByType(@PathVariable Integer type) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return auxiliaryAccountingService.listByType(tenantId, type);
    }
    
    @GetMapping("/page")
    @Operation(summary = "分页查询辅助核算")
    public Page<AuxiliaryAccounting> page(
            @Parameter(description = "辅助核算类型") @RequestParam(required = false) Integer auxiliaryType,
            @Parameter(description = "辅助核算编码") @RequestParam(required = false) String auxiliaryCode,
            @Parameter(description = "辅助核算名称") @RequestParam(required = false) String auxiliaryName,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<AuxiliaryAccounting> page = new Page<>(pageNum, pageSize);
        return auxiliaryAccountingService.pageList(tenantId, auxiliaryType, auxiliaryCode, auxiliaryName, page);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取辅助核算详情")
    public AuxiliaryAccounting getById(@PathVariable Long id) {
        return auxiliaryAccountingService.getById(id);
    }
    
    @PostMapping
    @Operation(summary = "创建辅助核算")
    public boolean create(@RequestBody AuxiliaryAccounting auxiliary) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        auxiliary.setTenantId(tenantId);
        return auxiliaryAccountingService.createAuxiliary(auxiliary);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新辅助核算")
    public boolean update(@PathVariable Long id, @RequestBody AuxiliaryAccounting auxiliary) {
        auxiliary.setId(id);
        return auxiliaryAccountingService.updateAuxiliary(auxiliary);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除辅助核算")
    public boolean delete(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return auxiliaryAccountingService.deleteAuxiliary(tenantId, id);
    }
    
    @PutMapping("/{id}/enable")
    @Operation(summary = "启用辅助核算")
    public boolean enable(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return auxiliaryAccountingService.enableAuxiliary(tenantId, id);
    }
    
    @PutMapping("/{id}/disable")
    @Operation(summary = "停用辅助核算")
    public boolean disable(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return auxiliaryAccountingService.disableAuxiliary(tenantId, id);
    }
    
    @PostMapping("/sync/department")
    @Operation(summary = "从部门同步辅助核算")
    public boolean syncDepartment() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return auxiliaryAccountingService.syncFromDepartment(tenantId);
    }
    
    @PostMapping("/sync/customer")
    @Operation(summary = "从客户同步辅助核算")
    public boolean syncCustomer() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return auxiliaryAccountingService.syncFromCustomer(tenantId);
    }
    
    @PostMapping("/sync/supplier")
    @Operation(summary = "从供应商同步辅助核算")
    public boolean syncSupplier() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return auxiliaryAccountingService.syncFromSupplier(tenantId);
    }
    
    @GetMapping("/types")
    @Operation(summary = "获取辅助核算类型列表")
    public List<AuxiliaryType> listTypes() {
        return List.of(AuxiliaryType.values());
    }
}
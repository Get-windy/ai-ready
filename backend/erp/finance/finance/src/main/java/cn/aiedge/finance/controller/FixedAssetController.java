package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.AssetChangeVO;
import cn.aiedge.finance.dto.AssetDepreciationVO;
import cn.aiedge.finance.dto.FixedAssetCreateDTO;
import cn.aiedge.finance.dto.FixedAssetVO;
import cn.aiedge.finance.entity.AssetChange;
import cn.aiedge.finance.entity.AssetDepreciation;
import cn.aiedge.finance.entity.AssetCategory;
import cn.aiedge.finance.entity.FixedAsset;
import cn.aiedge.finance.enums.AssetStatus;
import cn.aiedge.finance.enums.DepreciationMethod;
import cn.aiedge.finance.service.AssetCategoryService;
import cn.aiedge.finance.service.AssetChangeService;
import cn.aiedge.finance.service.AssetDepreciationService;
import cn.aiedge.finance.service.FixedAssetService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance/fixed-asset")
@Tag(name = "固定资产管理", description = "固定资产卡片、折旧计算和变动管理")
public class FixedAssetController {
    
    @Autowired
    private FixedAssetService fixedAssetService;
    
    @Autowired
    private AssetCategoryService assetCategoryService;
    
    @Autowired
    private AssetDepreciationService assetDepreciationService;
    
    @Autowired
    private AssetChangeService assetChangeService;
    
    @GetMapping("/page")
    @Operation(summary = "分页查询固定资产")
    public Page<FixedAssetVO> page(
            @Parameter(description = "资产编码") @RequestParam(required = false) String assetCode,
            @Parameter(description = "资产名称") @RequestParam(required = false) String assetName,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<FixedAsset> page = new Page<>(pageNum, pageSize);
        Page<FixedAsset> result = fixedAssetService.pageList(tenantId, assetCode, assetName, categoryId, status, departmentId, page);
        Page<FixedAssetVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }
    
    @GetMapping("/list/status/{status}")
    @Operation(summary = "按状态获取资产列表")
    public List<FixedAssetVO> listByStatus(@PathVariable Integer status) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<FixedAsset> assets = fixedAssetService.listByStatus(tenantId, status);
        return assets.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取资产详情")
    public FixedAssetVO getById(@PathVariable Long id) {
        FixedAsset asset = fixedAssetService.getById(id);
        if (asset == null) return null;
        FixedAssetVO vo = convertToVO(asset);
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<AssetChange> changes = assetChangeService.listByAssetId(tenantId, id);
        vo.setChanges(changes.stream().map(this::convertChangeToVO).collect(Collectors.toList()));
        return vo;
    }
    
    @PostMapping
    @Operation(summary = "创建固定资产")
    public boolean create(@Valid @RequestBody FixedAssetCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        FixedAsset asset = new FixedAsset();
        BeanUtils.copyProperties(dto, asset);
        asset.setTenantId(tenantId);
        if (asset.getResidualValue() == null) {
            asset.setResidualValue(BigDecimal.ZERO);
        }
        if (asset.getStartDepreciationDate() == null) {
            asset.setStartDepreciationDate(dto.getAcquisitionDate().plusMonths(1));
        }
        return fixedAssetService.createAsset(asset);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新固定资产")
    public boolean update(@PathVariable Long id, @Valid @RequestBody FixedAssetCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        FixedAsset asset = fixedAssetService.getById(id);
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        BeanUtils.copyProperties(dto, asset);
        return fixedAssetService.updateAsset(asset);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除固定资产")
    public boolean delete(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return fixedAssetService.deleteAsset(tenantId, id);
    }
    
    @PutMapping("/{id}/transfer")
    @Operation(summary = "资产转移")
    public boolean transfer(@PathVariable Long id,
            @RequestParam Long toDepartmentId,
            @RequestParam(required = false) Long toLocationId,
            @RequestParam(required = false) Long toCustodianId,
            @RequestParam String reason) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return fixedAssetService.transfer(tenantId, id, toDepartmentId, toLocationId, toCustodianId, reason);
    }
    
    @PutMapping("/{id}/dispose")
    @Operation(summary = "资产处置")
    public boolean dispose(@PathVariable Long id,
            @RequestParam(required = false) BigDecimal disposeValue,
            @RequestParam String reason) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return fixedAssetService.dispose(tenantId, id, disposeValue, reason);
    }
    
    @PutMapping("/{id}/scrap")
    @Operation(summary = "资产报废")
    public boolean scrap(@PathVariable Long id, @RequestParam String reason) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return fixedAssetService.scrap(tenantId, id, reason);
    }
    
    @GetMapping("/summary")
    @Operation(summary = "获取资产汇总")
    public Map<String, BigDecimal> getSummary() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return fixedAssetService.getAssetSummary(tenantId);
    }
    
    @GetMapping("/generate-code")
    @Operation(summary = "生成资产编码")
    public String generateCode() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return fixedAssetService.generateAssetCode(tenantId);
    }
    
    @GetMapping("/depreciation/page")
    @Operation(summary = "分页查询折旧记录")
    public Page<AssetDepreciationVO> pageDepreciation(
            @Parameter(description = "期间") @RequestParam(required = false) String period,
            @Parameter(description = "资产编码") @RequestParam(required = false) String assetCode,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<AssetDepreciation> page = new Page<>(pageNum, pageSize);
        Page<AssetDepreciation> result = assetDepreciationService.pageList(tenantId, period, assetCode, status, page);
        Page<AssetDepreciationVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::convertDepreciationToVO).collect(Collectors.toList()));
        return voPage;
    }
    
    @GetMapping("/depreciation/summary/{period}")
    @Operation(summary = "获取期间折旧汇总")
    public Map<String, Object> getDepreciationSummary(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return assetDepreciationService.getDepreciationSummary(tenantId, period);
    }
    
    @PostMapping("/depreciation/run/{period}")
    @Operation(summary = "执行期间折旧")
    public boolean runDepreciation(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return fixedAssetService.runDepreciation(tenantId, period);
    }
    
    @PostMapping("/depreciation/post/{period}")
    @Operation(summary = "记账折旧")
    public boolean postDepreciation(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return fixedAssetService.postDepreciation(tenantId, period);
    }
    
    @GetMapping("/category/tree")
    @Operation(summary = "获取资产分类树")
    public List<AssetCategory> getCategoryTree() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return assetCategoryService.buildTree(tenantId);
    }
    
    @GetMapping("/depreciation-methods")
    @Operation(summary = "获取折旧方法列表")
    public List<DepreciationMethod> listDepreciationMethods() {
        return List.of(DepreciationMethod.values());
    }
    
    @GetMapping("/statuses")
    @Operation(summary = "获取资产状态列表")
    public List<AssetStatus> listStatuses() {
        return List.of(AssetStatus.values());
    }
    
    private FixedAssetVO convertToVO(FixedAsset asset) {
        FixedAssetVO vo = new FixedAssetVO();
        BeanUtils.copyProperties(asset, vo);
        DepreciationMethod method = DepreciationMethod.fromCode(asset.getDepreciationMethod());
        vo.setDepreciationMethodName(method != null ? method.getName() : "");
        AssetStatus status = AssetStatus.fromCode(asset.getStatus());
        vo.setStatusName(status != null ? status.getName() : "");
        vo.setRemainingMonths(asset.getUsefulLife() * 12 - asset.getUsedMonths());
        return vo;
    }
    
    private AssetChangeVO convertChangeToVO(AssetChange change) {
        AssetChangeVO vo = new AssetChangeVO();
        BeanUtils.copyProperties(change, vo);
        return vo;
    }
    
    private AssetDepreciationVO convertDepreciationToVO(AssetDepreciation dep) {
        AssetDepreciationVO vo = new AssetDepreciationVO();
        BeanUtils.copyProperties(dep, vo);
        vo.setStatusName(dep.getStatus() == 1 ? "已记账" : "未记账");
        return vo;
    }
}
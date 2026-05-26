package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.VoucherCreateDTO;
import cn.aiedge.finance.dto.VoucherVO;
import cn.aiedge.finance.entity.AccountSubject;
import cn.aiedge.finance.entity.Voucher;
import cn.aiedge.finance.entity.VoucherEntry;
import cn.aiedge.finance.enums.VoucherStatus;
import cn.aiedge.finance.enums.VoucherType;
import cn.aiedge.finance.mapper.AccountSubjectMapper;
import cn.aiedge.finance.service.VoucherService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance/voucher")
@Tag(name = "凭证管理", description = "凭证的录入、审核、记账")
public class VoucherController {
    
    @Autowired
    private VoucherService voucherService;
    
    @Autowired
    private AccountSubjectMapper accountSubjectMapper;
    
    @GetMapping("/page")
    @Operation(summary = "分页查询凭证")
    public Page<VoucherVO> page(
            @Parameter(description = "会计期间") @RequestParam(required = false) String period,
            @Parameter(description = "凭证号") @RequestParam(required = false) String voucherNo,
            @Parameter(description = "凭证类型") @RequestParam(required = false) Integer voucherType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<Voucher> page = new Page<>(pageNum, pageSize);
        Page<Voucher> result = voucherService.pageList(tenantId, period, voucherNo, voucherType, status, startDate, endDate, page);
        Page<VoucherVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }
    
    @GetMapping("/list/{period}")
    @Operation(summary = "按期间获取凭证列表")
    public List<VoucherVO> listByPeriod(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<Voucher> vouchers = voucherService.listByPeriod(tenantId, period);
        return vouchers.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取凭证详情")
    public VoucherVO getById(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Voucher voucher = voucherService.getDetail(tenantId, id);
        return voucher != null ? convertToVOWithEntries(voucher) : null;
    }
    
    @PostMapping
    @Operation(summary = "创建凭证")
    public boolean create(@Valid @RequestBody VoucherCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Voucher voucher = new Voucher();
        BeanUtils.copyProperties(dto, voucher);
        voucher.setTenantId(tenantId);
        List<VoucherEntry> entries = new ArrayList<>();
        if (dto.getEntries() != null) {
            for (VoucherCreateDTO.VoucherEntryDTO entryDTO : dto.getEntries()) {
                VoucherEntry entry = new VoucherEntry();
                BeanUtils.copyProperties(entryDTO, entry);
                AccountSubject subject = accountSubjectMapper.selectById(entryDTO.getSubjectId());
                if (subject != null) {
                    entry.setSubjectCode(subject.getSubjectCode());
                    entry.setSubjectName(subject.getSubjectName());
                }
                entry.setTenantId(tenantId);
                entries.add(entry);
            }
        }
        voucher.setEntries(entries);
        return voucherService.createVoucher(voucher);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新凭证")
    public boolean update(@PathVariable Long id, @Valid @RequestBody VoucherCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Voucher voucher = voucherService.getById(id);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        BeanUtils.copyProperties(dto, voucher);
        List<VoucherEntry> entries = new ArrayList<>();
        if (dto.getEntries() != null) {
            for (VoucherCreateDTO.VoucherEntryDTO entryDTO : dto.getEntries()) {
                VoucherEntry entry = new VoucherEntry();
                BeanUtils.copyProperties(entryDTO, entry);
                AccountSubject subject = accountSubjectMapper.selectById(entryDTO.getSubjectId());
                if (subject != null) {
                    entry.setSubjectCode(subject.getSubjectCode());
                    entry.setSubjectName(subject.getSubjectName());
                }
                entry.setTenantId(tenantId);
                entries.add(entry);
            }
        }
        voucher.setEntries(entries);
        return voucherService.updateVoucher(voucher);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除凭证")
    public boolean delete(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return voucherService.deleteVoucher(tenantId, id);
    }
    
    @PutMapping("/{id}/submit")
    @Operation(summary = "提交审核")
    public boolean submit(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return voucherService.submitForReview(tenantId, id);
    }
    
    @PutMapping("/{id}/approve")
    @Operation(summary = "审核通过")
    public boolean approve(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return voucherService.approve(tenantId, id);
    }
    
    @PutMapping("/{id}/reject")
    @Operation(summary = "审核驳回")
    public boolean reject(@PathVariable Long id, @RequestParam String reason) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return voucherService.reject(tenantId, id, reason);
    }
    
    @PutMapping("/{id}/post")
    @Operation(summary = "记账")
    public boolean post(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return voucherService.post(tenantId, id);
    }
    
    @PutMapping("/{id}/void")
    @Operation(summary = "作废凭证")
    public boolean voidVoucher(@PathVariable Long id, @RequestParam String reason) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return voucherService.voidVoucher(tenantId, id, reason);
    }
    
    @PutMapping("/{id}/print")
    @Operation(summary = "打印凭证")
    public boolean print(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return voucherService.print(tenantId, id);
    }
    
    @GetMapping("/generate-no")
    @Operation(summary = "生成凭证号")
    public String generateVoucherNo(@RequestParam String period, @RequestParam(required = false) String word) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return voucherService.generateVoucherNo(tenantId, period, word);
    }
    
    @GetMapping("/check-balance/{id}")
    @Operation(summary = "检查凭证借贷平衡")
    public boolean checkBalance(@PathVariable Long id) {
        Voucher voucher = voucherService.getById(id);
        return voucher != null && voucherService.checkBalance(voucher);
    }
    
    @GetMapping("/period-summary/{period}")
    @Operation(summary = "获取期间凭证汇总")
    public Map<String, BigDecimal> getPeriodSummary(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return voucherService.getPeriodSummary(tenantId, period);
    }
    
    @GetMapping("/types")
    @Operation(summary = "获取凭证类型列表")
    public List<VoucherType> listTypes() {
        return List.of(VoucherType.values());
    }
    
    @GetMapping("/statuses")
    @Operation(summary = "获取凭证状态列表")
    public List<VoucherStatus> listStatuses() {
        return List.of(VoucherStatus.values());
    }
    
    private VoucherVO convertToVO(Voucher voucher) {
        VoucherVO vo = new VoucherVO();
        BeanUtils.copyProperties(voucher, vo);
        VoucherType type = VoucherType.fromCode(voucher.getVoucherType());
        vo.setVoucherTypeName(type != null ? type.getName() : "");
        VoucherStatus status = VoucherStatus.fromCode(voucher.getStatus());
        vo.setStatusName(status != null ? status.getName() : "");
        BigDecimal totalDebit = voucher.getTotalDebit() != null ? voucher.getTotalDebit() : BigDecimal.ZERO;
        BigDecimal totalCredit = voucher.getTotalCredit() != null ? voucher.getTotalCredit() : BigDecimal.ZERO;
        vo.setIsBalanced(totalDebit.compareTo(totalCredit) == 0);
        return vo;
    }
    
    private VoucherVO convertToVOWithEntries(Voucher voucher) {
        VoucherVO vo = convertToVO(voucher);
        if (voucher.getEntries() != null) {
            List<VoucherVO.VoucherEntryVO> entryVOs = new ArrayList<>();
            for (VoucherEntry entry : voucher.getEntries()) {
                VoucherVO.VoucherEntryVO entryVO = new VoucherVO.VoucherEntryVO();
                BeanUtils.copyProperties(entry, entryVO);
                entryVOs.add(entryVO);
            }
            vo.setEntries(entryVOs);
        }
        return vo;
    }
}
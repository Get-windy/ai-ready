package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.AccountPeriodCreateDTO;
import cn.aiedge.finance.dto.AccountPeriodVO;
import cn.aiedge.finance.entity.AccountPeriod;
import cn.aiedge.finance.enums.PeriodStatus;
import cn.aiedge.finance.service.AccountPeriodService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance/account-period")
@Tag(name = "会计期间管理", description = "会计期间的开账、结账和年终结转")
public class AccountPeriodController {
    
    @Autowired
    private AccountPeriodService accountPeriodService;
    
    @GetMapping("/current")
    @Operation(summary = "获取当前会计期间")
    public AccountPeriodVO getCurrentPeriod() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        AccountPeriod period = accountPeriodService.getCurrentPeriod(tenantId);
        return period != null ? convertToVO(period) : null;
    }
    
    @GetMapping("/list")
    @Operation(summary = "获取所有会计期间")
    public List<AccountPeriodVO> listAll() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<AccountPeriod> periods = accountPeriodService.listAll(tenantId);
        return periods.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @GetMapping("/list/year/{year}")
    @Operation(summary = "按年份获取会计期间")
    public List<AccountPeriodVO> listByYear(@PathVariable Integer year) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<AccountPeriod> periods = accountPeriodService.listByYear(tenantId, year);
        return periods.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @GetMapping("/page")
    @Operation(summary = "分页查询会计期间")
    public Page<AccountPeriodVO> page(
            @Parameter(description = "年份") @RequestParam(required = false) Integer year,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<AccountPeriod> page = new Page<>(pageNum, pageSize);
        Page<AccountPeriod> result = accountPeriodService.pageList(tenantId, year, status, page);
        Page<AccountPeriodVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取会计期间详情")
    public AccountPeriodVO getById(@PathVariable Long id) {
        AccountPeriod period = accountPeriodService.getById(id);
        return period != null ? convertToVO(period) : null;
    }
    
    @PostMapping
    @Operation(summary = "创建会计期间")
    public boolean create(@Valid @RequestBody AccountPeriodCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        AccountPeriod period = new AccountPeriod();
        BeanUtils.copyProperties(dto, period);
        period.setTenantId(tenantId);
        return accountPeriodService.createPeriod(period);
    }
    
    @PostMapping("/init-year/{year}")
    @Operation(summary = "初始化年度会计期间")
    public boolean initYear(@PathVariable Integer year) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return accountPeriodService.initYearPeriods(tenantId, year);
    }
    
    @PutMapping("/{id}/open")
    @Operation(summary = "开账")
    public boolean open(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        AccountPeriod period = accountPeriodService.getById(id);
        if (period == null) {
            throw new RuntimeException("会计期间不存在");
        }
        return accountPeriodService.openPeriod(tenantId, period.getYear(), period.getMonth());
    }
    
    @PutMapping("/{id}/close")
    @Operation(summary = "结账")
    public boolean close(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        AccountPeriod period = accountPeriodService.getById(id);
        if (period == null) {
            throw new RuntimeException("会计期间不存在");
        }
        return accountPeriodService.closePeriod(tenantId, period.getYear(), period.getMonth());
    }
    
    @PutMapping("/{id}/set-current")
    @Operation(summary = "设为当前期间")
    public boolean setCurrent(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return accountPeriodService.setCurrentPeriod(tenantId, id);
    }
    
    @PostMapping("/year-end-close/{year}")
    @Operation(summary = "年终结账")
    public boolean yearEndClose(@PathVariable Integer year) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return accountPeriodService.yearEndClose(tenantId, year);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除会计期间")
    public boolean delete(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return accountPeriodService.deletePeriod(tenantId, id);
    }
    
    private AccountPeriodVO convertToVO(AccountPeriod period) {
        AccountPeriodVO vo = new AccountPeriodVO();
        BeanUtils.copyProperties(period, vo);
        PeriodStatus status = PeriodStatus.fromCode(period.getStatus());
        vo.setStatusName(status != null ? status.getName() : "");
        return vo;
    }
}
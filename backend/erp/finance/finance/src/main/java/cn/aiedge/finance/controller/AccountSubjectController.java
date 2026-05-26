package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.AccountSubjectCreateDTO;
import cn.aiedge.finance.dto.AccountSubjectVO;
import cn.aiedge.finance.entity.AccountSubject;
import cn.aiedge.finance.enums.AccountDirection;
import cn.aiedge.finance.enums.AccountType;
import cn.aiedge.finance.service.AccountSubjectService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance/account-subject")
@Tag(name = "会计科目管理", description = "会计科目的增删改查和树形结构")
public class AccountSubjectController {
    
    @Autowired
    private AccountSubjectService accountSubjectService;
    
    @GetMapping("/tree")
    @Operation(summary = "获取科目树形结构")
    public List<AccountSubjectVO> getTree() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<AccountSubject> tree = accountSubjectService.buildTree(tenantId);
        return convertToVOTree(tree);
    }
    
    @GetMapping("/list/enabled")
    @Operation(summary = "获取所有启用的科目")
    public List<AccountSubjectVO> listEnabled() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<AccountSubject> subjects = accountSubjectService.listAllEnabled(tenantId);
        return subjects.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @GetMapping("/list/leaf")
    @Operation(summary = "获取所有末级科目")
    public List<AccountSubjectVO> listLeaf() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<AccountSubject> subjects = accountSubjectService.listLeafSubjects(tenantId);
        return subjects.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @GetMapping("/list/type/{type}")
    @Operation(summary = "按类型获取科目列表")
    public List<AccountSubjectVO> listByType(@PathVariable Integer type) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<AccountSubject> subjects = accountSubjectService.listByType(tenantId, type);
        return subjects.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @GetMapping("/page")
    @Operation(summary = "分页查询科目")
    public Page<AccountSubjectVO> page(
            @Parameter(description = "科目编码") @RequestParam(required = false) String subjectCode,
            @Parameter(description = "科目名称") @RequestParam(required = false) String subjectName,
            @Parameter(description = "科目类型") @RequestParam(required = false) Integer subjectType,
            @Parameter(description = "科目级别") @RequestParam(required = false) Integer level,
            @Parameter(description = "启用状态") @RequestParam(required = false) Integer enabled,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<AccountSubject> page = new Page<>(pageNum, pageSize);
        Page<AccountSubject> result = accountSubjectService.pageList(tenantId, subjectCode, subjectName, subjectType, level, enabled, page);
        Page<AccountSubjectVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取科目详情")
    public AccountSubjectVO getById(@PathVariable Long id) {
        AccountSubject subject = accountSubjectService.getById(id);
        return subject != null ? convertToVO(subject) : null;
    }
    
    @PostMapping
    @Operation(summary = "创建科目")
    public boolean create(@Valid @RequestBody AccountSubjectCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        AccountSubject subject = new AccountSubject();
        BeanUtils.copyProperties(dto, subject);
        subject.setTenantId(tenantId);
        return accountSubjectService.createSubject(subject);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新科目")
    public boolean update(@PathVariable Long id, @Valid @RequestBody AccountSubjectCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        AccountSubject subject = accountSubjectService.getById(id);
        if (subject == null) {
            throw new RuntimeException("科目不存在");
        }
        BeanUtils.copyProperties(dto, subject);
        return accountSubjectService.updateSubject(subject);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除科目")
    public boolean delete(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return accountSubjectService.deleteSubject(tenantId, id);
    }
    
    @PutMapping("/{id}/enable")
    @Operation(summary = "启用科目")
    public boolean enable(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return accountSubjectService.enableSubject(tenantId, id);
    }
    
    @PutMapping("/{id}/disable")
    @Operation(summary = "停用科目")
    public boolean disable(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return accountSubjectService.disableSubject(tenantId, id);
    }
    
    @PostMapping("/init-standard")
    @Operation(summary = "初始化标准科目体系")
    public boolean initStandard() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return accountSubjectService.initStandardSubjects(tenantId);
    }
    
    private AccountSubjectVO convertToVO(AccountSubject subject) {
        AccountSubjectVO vo = new AccountSubjectVO();
        BeanUtils.copyProperties(subject, vo);
        AccountType type = AccountType.fromCode(subject.getSubjectType());
        vo.setSubjectTypeName(type != null ? type.getName() : "");
        AccountDirection direction = AccountDirection.fromCode(subject.getBalanceDirection());
        vo.setBalanceDirectionName(direction != null ? direction.getName() : "");
        return vo;
    }
    
    private List<AccountSubjectVO> convertToVOTree(List<AccountSubject> tree) {
        List<AccountSubjectVO> voTree = new ArrayList<>();
        for (AccountSubject subject : tree) {
            AccountSubjectVO vo = convertToVO(subject);
            if (subject.getChildren() != null && !subject.getChildren().isEmpty()) {
                vo.setChildren(convertToVOTree(subject.getChildren()));
            }
            voTree.add(vo);
        }
        return voTree;
    }
}
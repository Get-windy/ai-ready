package cn.aiedge.erp.finance.expensedoc.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.base.entity.SysDept;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysDeptMapper;
import cn.aiedge.base.service.SysConfigService;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.erp.finance.expensedoc.dto.ApprovalProcessDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ApprovalSubmitDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApprovalDetailVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApprovalQuery;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApproverConfigDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApproverSuggestionVO;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseApproval;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseDoc;
import cn.aiedge.erp.finance.expensedoc.mapper.ExpenseApprovalMapper;
import cn.aiedge.erp.finance.expensedoc.service.ExpenseApprovalService;
import cn.aiedge.erp.finance.expensedoc.service.ExpenseDocService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 费用审批 Service实现
 *
 * P0 红线（单一口径）：审批复用《费用单》(erp_expense_doc) 状态机与审批字段，不另建审批引擎/审批主表；
 *                     过程留痕落 erp_expense_approval，可经 /records 追溯。
 * 审批链：部门审批(1) → 财务审批(2) → 总经理审批(3) → 审批通过；任一环节可驳回。
 * 审批人隔离：单据在审时由 current_approver_id 指向本级审批人，「仅看我的待办」按其过滤。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseApprovalServiceImpl extends ServiceImpl<ExpenseApprovalMapper, ExpenseApproval>
        implements ExpenseApprovalService {

    /** 审批状态：未提交 / 审批中 / 审批通过 / 审批驳回 */
    private static final int AS_UNSUBMITTED = 0;
    private static final int AS_APPROVING = 1;
    private static final int AS_APPROVED = 2;
    private static final int AS_REJECTED = 3;

    /** 费用单状态：草稿 */
    private static final int ST_DRAFT = 0;

    private static final int DEFAULT_TOTAL_LEVEL = 3;
    private static final int MAX_TOTAL_LEVEL = 3;

    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";

    /** 审批人自动指派来源 */
    private static final String SOURCE_DEPT_LEADER = "DEPT_LEADER";
    private static final String SOURCE_CONFIG = "CONFIG";
    private static final String SOURCE_NONE = "NONE";

    /** 审批人配置键（存系统参数 sys_config，group=expense_approval） */
    private static final String CFG_GROUP = "expense_approval";
    private static final String CFG_LEVEL1 = "expense.approval.level1.approverId";
    private static final String CFG_LEVEL2 = "expense.approval.level2.approverId";
    private static final String CFG_LEVEL3 = "expense.approval.level3.approverId";

    private final ExpenseDocService expenseDocService;
    private final SysConfigService sysConfigService;
    private final SysUserService sysUserService;
    private final SysDeptMapper sysDeptMapper;

    @Override
    public Page<ExpenseDoc> pagePending(ExpenseApprovalQuery query, Long loginUserId) {
        if (query == null) {
            query = new ExpenseApprovalQuery();
        }
        LambdaQueryWrapper<ExpenseDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseDoc::getDeleted, 0);
        // 待审批工作台默认只显示「审批中」；可切换查看未提交/已通过/已驳回
        wrapper.eq(ExpenseDoc::getApprovalStatus,
                query.getApprovalStatus() != null ? query.getApprovalStatus() : AS_APPROVING);
        if (query.getDocNo() != null && !query.getDocNo().isEmpty()) {
            wrapper.like(ExpenseDoc::getDocNo, query.getDocNo());
        }
        if (query.getPartnerName() != null && !query.getPartnerName().isEmpty()) {
            wrapper.like(ExpenseDoc::getPartnerName, query.getPartnerName());
        }
        if (query.getHandlerName() != null && !query.getHandlerName().isEmpty()) {
            wrapper.like(ExpenseDoc::getHandlerName, query.getHandlerName());
        }
        if (query.getDeptName() != null && !query.getDeptName().isEmpty()) {
            wrapper.like(ExpenseDoc::getDeptName, query.getDeptName());
        }
        if (query.getCreatorName() != null && !query.getCreatorName().isEmpty()) {
            wrapper.like(ExpenseDoc::getCreatorName, query.getCreatorName());
        }
        if (query.getCurrentApproverName() != null && !query.getCurrentApproverName().isEmpty()) {
            wrapper.like(ExpenseDoc::getCurrentApproverName, query.getCurrentApproverName());
        }
        if (query.getSummary() != null && !query.getSummary().isEmpty()) {
            wrapper.like(ExpenseDoc::getSummary, query.getSummary());
        }
        if (query.getExpenseType() != null) {
            wrapper.eq(ExpenseDoc::getExpenseType, query.getExpenseType());
        }
        if (query.getDateStart() != null) {
            wrapper.ge(ExpenseDoc::getDocDate, query.getDateStart());
        }
        if (query.getDateEnd() != null) {
            wrapper.le(ExpenseDoc::getDocDate, query.getDateEnd());
        }
        // 审批人隔离：仅看我的待办（未指派的单据不进入任何人的个人待办）
        if (Boolean.TRUE.equals(query.getOnlyMine()) && loginUserId != null) {
            wrapper.eq(ExpenseDoc::getCurrentApproverId, loginUserId);
        }
        wrapper.orderByDesc(ExpenseDoc::getSubmitTime).orderByDesc(ExpenseDoc::getDocNo);
        return expenseDocService.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDoc submit(ApprovalSubmitDTO dto, Long operatorId, String operatorName) {
        ExpenseDoc doc = requireDoc(dto.getDocId());
        if (nvl(doc.getStatus()) != ST_DRAFT) {
            throw new BusinessException("已记账/已取消的费用单不能提交审批");
        }
        int approvalStatus = nvl(doc.getApprovalStatus());
        if (approvalStatus == AS_APPROVING) {
            throw new BusinessException("费用单已在审批中，不能重复提交");
        }
        if (approvalStatus == AS_APPROVED) {
            throw new BusinessException("费用单已审批通过，不能重复提交");
        }
        int totalLevel = dto.getTotalLevel() != null && dto.getTotalLevel() > 0
                ? Math.min(dto.getTotalLevel(), MAX_TOTAL_LEVEL) : DEFAULT_TOTAL_LEVEL;
        // 自动指派：未指定第一级审批人时按「部门负责人 → 审批人配置」解析
        Long firstApproverId = dto.getApproverId();
        String firstApproverName = dto.getApproverName();
        if (firstApproverId == null) {
            ExpenseApproverSuggestionVO suggestion = resolveApprover(1, doc);
            firstApproverId = suggestion.getApproverId();
            firstApproverName = suggestion.getApproverName();
        }
        if (firstApproverId == null) {
            throw new BusinessException("请选择第一级审批人（可在「审批人配置」中预设各级默认审批人，实现自动指派）");
        }

        String previousStatus = stateText(doc);
        doc.setApprovalStatus(AS_APPROVING);
        doc.setApprovalLevel(1);
        doc.setTotalApprovalLevel(totalLevel);
        doc.setCurrentApproverId(firstApproverId);
        doc.setCurrentApproverName(firstApproverName);
        doc.setSubmitTime(LocalDateTime.now());
        doc.setRejectReason(null);
        expenseDocService.updateById(doc);

        writeRecord(doc, 1, operatorId, operatorName, ACTION_SUBMIT, "提交审批", previousStatus, stateText(doc));
        log.info("费用单提交审批: docNo={}, totalLevel={}, 一级审批人={}", doc.getDocNo(), totalLevel, firstApproverName);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDoc process(ApprovalProcessDTO dto, Long operatorId, String operatorName) {
        ExpenseDoc doc = requireDoc(dto.getDocId());
        if (nvl(doc.getApprovalStatus()) != AS_APPROVING) {
            throw new BusinessException("该费用单不在审批中，不能审批");
        }
        if (nvl(doc.getStatus()) != ST_DRAFT) {
            throw new BusinessException("已记账的费用单不能审批");
        }
        // 审批人隔离：已指派本级审批人时，仅该审批人可处理
        if (doc.getCurrentApproverId() != null && operatorId != null
                && !doc.getCurrentApproverId().equals(operatorId)) {
            throw new BusinessException("该单据当前由「" + nvl(doc.getCurrentApproverName(), "其他审批人")
                    + "」审批，您无权处理");
        }

        String action = dto.getAction() != null ? dto.getAction().trim().toUpperCase() : "";
        int level = nvl(doc.getApprovalLevel()) > 0 ? doc.getApprovalLevel() : 1;
        int totalLevel = nvl(doc.getTotalApprovalLevel()) > 0 ? doc.getTotalApprovalLevel() : DEFAULT_TOTAL_LEVEL;
        String previousStatus = stateText(doc);
        String comment = dto.getComment() != null ? dto.getComment().trim() : null;

        if (ACTION_APPROVE.equals(action)) {
            if (level >= totalLevel) {
                // 末级通过 → 审批通过，等待记账（记账即费用支付：扣账户 + 资金流水 + 凭证）
                doc.setApprovalStatus(AS_APPROVED);
                doc.setCurrentApproverId(null);
                doc.setCurrentApproverName(null);
                doc.setRejectReason(null);
            } else {
                // 自动指派：未指定下一级审批人时按审批人配置解析（部门负责人仅用于第一级）
                Long nextApproverId = dto.getNextApproverId();
                String nextApproverName = dto.getNextApproverName();
                if (nextApproverId == null) {
                    ExpenseApproverSuggestionVO suggestion = resolveApprover(level + 1, doc);
                    nextApproverId = suggestion.getApproverId();
                    nextApproverName = suggestion.getApproverName();
                }
                if (nextApproverId == null) {
                    throw new BusinessException("请选择下一级（" + levelName(level + 1)
                            + "）审批人（可在「审批人配置」中预设各级默认审批人，实现自动指派）");
                }
                doc.setApprovalLevel(level + 1);
                doc.setCurrentApproverId(nextApproverId);
                doc.setCurrentApproverName(nextApproverName);
            }
        } else if (ACTION_REJECT.equals(action)) {
            if (comment == null || comment.isEmpty()) {
                throw new BusinessException("驳回原因必填");
            }
            doc.setApprovalStatus(AS_REJECTED);
            doc.setRejectReason(comment);
            doc.setCurrentApproverId(null);
            doc.setCurrentApproverName(null);
        } else {
            throw new BusinessException("不支持的审批动作: " + dto.getAction());
        }

        expenseDocService.updateById(doc);
        writeRecord(doc, level, operatorId, operatorName, action, comment, previousStatus, stateText(doc));
        log.info("费用单审批处理: docNo={}, level={}, action={}, 结果={}",
                doc.getDocNo(), level, action, stateText(doc));
        return doc;
    }

    @Override
    public List<ExpenseApproval> records(Long docId) {
        return list(new LambdaQueryWrapper<ExpenseApproval>()
                .eq(ExpenseApproval::getExpenseDocId, docId)
                .eq(ExpenseApproval::getDeleted, 0)
                .orderByAsc(ExpenseApproval::getApprovalTime)
                .orderByAsc(ExpenseApproval::getId));
    }

    @Override
    public ExpenseApprovalDetailVO detail(Long docId) {
        ExpenseApprovalDetailVO vo = new ExpenseApprovalDetailVO();
        vo.setDoc(expenseDocService.getDetail(docId));
        vo.setRecords(records(docId));
        return vo;
    }

    // ── 审批人自动指派 ──

    @Override
    public ExpenseApproverConfigDTO getApproverConfig() {
        ExpenseApproverConfigDTO dto = new ExpenseApproverConfigDTO();
        dto.setLevel1ApproverId(configApproverId(1));
        dto.setLevel2ApproverId(configApproverId(2));
        dto.setLevel3ApproverId(configApproverId(3));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveApproverConfig(ExpenseApproverConfigDTO dto) {
        saveConfigApprover(1, dto != null ? dto.getLevel1ApproverId() : null);
        saveConfigApprover(2, dto != null ? dto.getLevel2ApproverId() : null);
        saveConfigApprover(3, dto != null ? dto.getLevel3ApproverId() : null);
        log.info("费用审批人配置已保存: level1={}, level2={}, level3={}",
                dto != null ? dto.getLevel1ApproverId() : null,
                dto != null ? dto.getLevel2ApproverId() : null,
                dto != null ? dto.getLevel3ApproverId() : null);
    }

    @Override
    public List<ExpenseApproverSuggestionVO> suggestApprovers(Long docId, Integer totalLevel) {
        ExpenseDoc doc = docId != null ? expenseDocService.getById(docId) : null;
        int total = totalLevel != null && totalLevel > 0
                ? Math.min(totalLevel, MAX_TOTAL_LEVEL) : DEFAULT_TOTAL_LEVEL;
        List<ExpenseApproverSuggestionVO> list = new ArrayList<>();
        for (int level = 1; level <= total; level++) {
            list.add(resolveApprover(level, doc));
        }
        return list;
    }

    /**
     * 解析某级别的建议审批人：
     * 第一级 → 费用单所属部门的负责人（sys_dept.leader，按姓名匹配用户）；无部门/负责人时回退审批人配置。
     * 第二/三级 → 审批人配置。
     */
    private ExpenseApproverSuggestionVO resolveApprover(int level, ExpenseDoc doc) {
        ExpenseApproverSuggestionVO vo = new ExpenseApproverSuggestionVO();
        vo.setLevel(level);
        if (level == 1 && doc != null && doc.getDeptId() != null) {
            Long leaderId = findDeptLeaderId(doc.getDeptId());
            if (leaderId != null) {
                vo.setApproverId(leaderId);
                vo.setApproverName(userName(leaderId));
                vo.setSource(SOURCE_DEPT_LEADER);
                return vo;
            }
        }
        Long configId = configApproverId(level);
        if (configId != null) {
            vo.setApproverId(configId);
            vo.setApproverName(userName(configId));
            vo.setSource(SOURCE_CONFIG);
            return vo;
        }
        vo.setSource(SOURCE_NONE);
        return vo;
    }

    /** 读取某级别配置的默认审批人ID */
    private Long configApproverId(int level) {
        try {
            String raw = sysConfigService.getValue(configKey(level), null);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return Long.valueOf(raw.trim());
        } catch (Exception e) {
            log.warn("读取费用审批人配置失败: level={}, error={}", level, e.getMessage());
            return null;
        }
    }

    /** 保存/清除某级别配置的默认审批人 */
    private void saveConfigApprover(int level, Long approverId) {
        String key = configKey(level);
        if (approverId == null) {
            sysConfigService.setValue(key, "");
            return;
        }
        sysConfigService.setValue(key, String.valueOf(approverId), "number", CFG_GROUP,
                "费用审批 " + levelName(level) + "审批默认审批人ID");
    }

    private String configKey(int level) {
        switch (level) {
            case 1:
                return CFG_LEVEL1;
            case 2:
                return CFG_LEVEL2;
            case 3:
                return CFG_LEVEL3;
            default:
                return "expense.approval.level" + level + ".approverId";
        }
    }

    /** 部门负责人（sys_dept.leader 姓名 → 用户ID），取不到返回 null */
    private Long findDeptLeaderId(Long deptId) {
        try {
            SysDept dept = sysDeptMapper.selectById(deptId);
            if (dept == null || dept.getLeader() == null || dept.getLeader().isBlank()) {
                return null;
            }
            String leaderName = dept.getLeader().trim();
            List<SysUser> users = sysUserService.list(new LambdaQueryWrapper<SysUser>()
                    .and(w -> w.eq(SysUser::getNickname, leaderName)
                            .or().eq(SysUser::getRealName, leaderName)
                            .or().eq(SysUser::getUsername, leaderName))
                    .orderByAsc(SysUser::getId)
                    .last("LIMIT 1"));
            return users.isEmpty() ? null : users.get(0).getId();
        } catch (Exception e) {
            log.warn("解析部门负责人失败: deptId={}, error={}", deptId, e.getMessage());
            return null;
        }
    }

    /** 用户展示名：昵称 → 真实姓名 → 用户名 → ID */
    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            SysUser user = sysUserService.getById(userId);
            if (user != null) {
                if (user.getNickname() != null && !user.getNickname().isEmpty()) {
                    return user.getNickname();
                }
                if (user.getRealName() != null && !user.getRealName().isEmpty()) {
                    return user.getRealName();
                }
                if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                    return user.getUsername();
                }
            }
        } catch (Exception e) {
            log.warn("查询审批人名称失败: userId={}, error={}", userId, e.getMessage());
        }
        return String.valueOf(userId);
    }

    // ── 内部方法 ──

    private ExpenseDoc requireDoc(Long docId) {
        if (docId == null) {
            throw new BusinessException("费用单ID不能为空");
        }
        ExpenseDoc doc = expenseDocService.getById(docId);
        if (doc == null) {
            throw new BusinessException("费用单不存在: " + docId);
        }
        return doc;
    }

    private void writeRecord(ExpenseDoc doc, int level, Long operatorId, String operatorName,
                             String action, String comment, String previousStatus, String currentStatus) {
        ExpenseApproval record = new ExpenseApproval();
        record.setExpenseDocId(doc.getId());
        record.setDocNo(doc.getDocNo());
        record.setApprovalLevel(level);
        record.setApproverId(operatorId);
        record.setApproverName(operatorName != null ? operatorName : "系统");
        record.setApprovalAction(action);
        record.setApprovalComment(comment);
        record.setApprovalTime(LocalDateTime.now());
        record.setPreviousStatus(previousStatus);
        record.setCurrentStatus(currentStatus);
        record.setTenantId(doc.getTenantId() != null ? doc.getTenantId() : 1L);
        record.setDeleted(0);
        save(record);
    }

    /** 审批级别名称：1-部门 2-财务 3-总经理 */
    private String levelName(int level) {
        switch (level) {
            case 1:
                return "部门";
            case 2:
                return "财务";
            case 3:
                return "总经理";
            default:
                return "第" + level + "级";
        }
    }

    /** 审批状态文案（含级别进度，用于审批记录前后状态留痕） */
    private String stateText(ExpenseDoc doc) {
        int approvalStatus = nvl(doc.getApprovalStatus());
        int level = nvl(doc.getApprovalLevel());
        int totalLevel = nvl(doc.getTotalApprovalLevel());
        switch (approvalStatus) {
            case AS_APPROVING:
                return levelName(level) + "审批中(" + level + "/" + totalLevel + ")";
            case AS_APPROVED:
                return "审批通过";
            case AS_REJECTED:
                return "审批驳回";
            default:
                return "未提交";
        }
    }

    private int nvl(Integer value) {
        return value != null ? value : 0;
    }

    private String nvl(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }
}

package cn.aiedge.erp.finance.bankaccount.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.bankaccount.dto.BankAccountDTO;
import cn.aiedge.erp.finance.bankaccount.dto.BankAccountQueryDTO;
import cn.aiedge.erp.finance.bankaccount.service.BankAccountService;
import cn.aiedge.erp.finance.mapper.FinanceAccountMapper;
import cn.aiedge.erp.finance.model.entity.FinanceAccount;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 银行账户（资金账户）Service 实现
 *
 * 口径：银行账户 = finance_account 的资金账户视图（科目编号/科目名称/账户类型/商城线下转账收款），
 * 与《支付账户》同源，禁止另建重复表。
 *
 * 树形：同级按 parent_id 挂接（「显示层次结构」），后端按深度优先展开为有序平铺列表后分页，
 * 保证 ql361 的「树形展示 + 分页」行为一致（每行带 level / hasChildren 供前端缩进与文件夹图标）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankAccountServiceImpl implements BankAccountService {

    private final FinanceAccountMapper financeAccountMapper;

    /** 预置账户编码前缀（系统预置账户不允许改编号/删除） */
    private static final Set<String> RESERVED_CODES = Set.of("1001", "1002");

    // ==================== 查询 ====================

    @Override
    public Page<BankAccountDTO> page(BankAccountQueryDTO query) {
        BankAccountQueryDTO q = query != null ? query : new BankAccountQueryDTO();
        List<BankAccountDTO> ordered = loadOrdered(q);

        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();
        int from = Math.min((pageNum - 1) * pageSize, ordered.size());
        int to = Math.min(from + pageSize, ordered.size());

        Page<BankAccountDTO> page = new Page<>(pageNum, pageSize, ordered.size());
        page.setRecords(new ArrayList<>(ordered.subList(from, to)));
        return page;
    }

    @Override
    public List<BankAccountDTO> list(BankAccountQueryDTO query) {
        return loadOrdered(query != null ? query : new BankAccountQueryDTO());
    }

    @Override
    public BankAccountDTO getById(Long id) {
        FinanceAccount entity = financeAccountMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("银行账户不存在: " + id);
        }
        BankAccountDTO dto = toDTO(entity);
        if (entity.getParentId() != null) {
            FinanceAccount parent = financeAccountMapper.selectById(entity.getParentId());
            if (parent != null) {
                dto.setParentName(parent.getAccountName());
            }
        }
        return dto;
    }

    @Override
    public List<BankAccountDTO> options() {
        BankAccountQueryDTO q = new BankAccountQueryDTO();
        q.setStatus(1);
        q.setShowTree(1);
        return loadOrdered(q);
    }

    /**
     * 按查询条件取全量匹配数据，并按「是否层次结构」排序：
     *   showTree=1 → 深度优先（父在前，子缩进），同级按 sortNo、subjectCode 排序
     *   showTree!=1 → 按 subjectCode 平铺排序
     */
    private List<BankAccountDTO> loadOrdered(BankAccountQueryDTO q) {
        List<FinanceAccount> rows = financeAccountMapper.selectList(buildWrapper(q));
        List<BankAccountDTO> dtos = rows.stream().map(this::toDTO).collect(Collectors.toList());

        if (q.getShowTree() == null || q.getShowTree() != 1) {
            dtos.sort(Comparator.comparing(d -> d.getSubjectCode() == null ? "" : d.getSubjectCode()));
            return dtos;
        }

        Map<Long, List<BankAccountDTO>> childrenByParent = new HashMap<>();
        Map<Long, BankAccountDTO> byId = new HashMap<>();
        for (BankAccountDTO dto : dtos) {
            byId.put(dto.getId(), dto);
        }
        List<BankAccountDTO> roots = new ArrayList<>();
        for (BankAccountDTO dto : dtos) {
            Long pid = dto.getParentId();
            // 父不在结果集内（被条件过滤掉）时提升为根，避免节点丢失
            if (pid != null && byId.containsKey(pid)) {
                childrenByParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(dto);
            } else {
                roots.add(dto);
            }
        }
        Comparator<BankAccountDTO> siblingOrder = Comparator
                .comparing((BankAccountDTO d) -> d.getSortNo() == null ? Integer.MAX_VALUE : d.getSortNo())
                .thenComparing(d -> d.getSubjectCode() == null ? "" : d.getSubjectCode());
        roots.sort(siblingOrder);

        List<BankAccountDTO> ordered = new ArrayList<>(dtos.size());
        Deque<BankAccountDTO> stack = new ArrayDeque<>();
        Deque<Integer> levelStack = new ArrayDeque<>();
        for (int i = roots.size() - 1; i >= 0; i--) {
            stack.push(roots.get(i));
            levelStack.push(1);
        }
        Set<Long> visited = new HashSet<>();
        while (!stack.isEmpty()) {
            BankAccountDTO cur = stack.pop();
            Integer level = levelStack.pop();
            if (!visited.add(cur.getId())) {
                continue; // 防御脏数据造成的环
            }
            cur.setLevel(level);
            List<BankAccountDTO> children = childrenByParent.getOrDefault(cur.getId(), List.of());
            cur.setHasChildren(!children.isEmpty());
            ordered.add(cur);
            List<BankAccountDTO> sorted = new ArrayList<>(children);
            sorted.sort(siblingOrder);
            for (int i = sorted.size() - 1; i >= 0; i--) {
                stack.push(sorted.get(i));
                levelStack.push(level + 1);
            }
        }
        return ordered;
    }

    private LambdaQueryWrapper<FinanceAccount> buildWrapper(BankAccountQueryDTO q) {
        LambdaQueryWrapper<FinanceAccount> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            wrapper.and(w -> w.like(FinanceAccount::getSubjectCode, kw)
                    .or().like(FinanceAccount::getAccountName, kw)
                    .or().like(FinanceAccount::getBankName, kw)
                    .or().like(FinanceAccount::getBankAccount, kw)
                    .or().like(FinanceAccount::getEasyCode, kw)
                    .or().like(FinanceAccount::getBriefName, kw));
        }
        if (q.getAccountType() != null) {
            wrapper.eq(FinanceAccount::getAccountType, q.getAccountType());
        }
        if (q.getAccountLevel() != null) {
            wrapper.eq(FinanceAccount::getAccountLevel, q.getAccountLevel());
        }
        if (StringUtils.hasText(q.getCurrency())) {
            wrapper.eq(FinanceAccount::getCurrency, q.getCurrency().trim());
        }
        if (q.getMallTransferEnabled() != null) {
            wrapper.eq(FinanceAccount::getMallTransferEnabled, q.getMallTransferEnabled());
        }
        if (q.getStatus() != null) {
            wrapper.eq(FinanceAccount::getStatus, q.getStatus());
        } else if (q.getShowDisabled() == null || q.getShowDisabled() != 1) {
            // 「显示停用」未勾选：仅启用数据（对标 ql361 默认行为）
            wrapper.eq(FinanceAccount::getStatus, 1);
        }
        if (q.getParentId() != null) {
            if (q.getParentId() == 0L) {
                wrapper.isNull(FinanceAccount::getParentId);
            } else {
                wrapper.eq(FinanceAccount::getParentId, q.getParentId());
            }
        }
        return wrapper;
    }

    // ==================== 写操作 ====================

    @Override
    @Transactional
    public BankAccountDTO create(BankAccountDTO dto) {
        String code = dto.getSubjectCode() == null ? "" : dto.getSubjectCode().trim();
        if (!StringUtils.hasText(code)) {
            throw BusinessException.badRequest("银行编号不能为空");
        }
        if (!StringUtils.hasText(dto.getAccountName())) {
            throw BusinessException.badRequest("银行全称不能为空");
        }
        assertCodeAvailable(code, null);
        assertParentValid(dto.getParentId(), null);

        FinanceAccount entity = new FinanceAccount();
        applyToEntity(entity, dto);
        entity.setSubjectCode(code);
        if (entity.getAccountType() == null) {
            entity.setAccountType(1);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getMallTransferEnabled() == null) {
            entity.setMallTransferEnabled(0);
        }
        if (entity.getIsSystem() == null) {
            entity.setIsSystem(0);
        }
        if (entity.getBalance() == null) {
            entity.setBalance(BigDecimal.ZERO);
        }
        if (!StringUtils.hasText(entity.getCurrency())) {
            entity.setCurrency("CNY");
        }
        financeAccountMapper.insert(entity);
        log.info("新增银行账户: id={}, code={}, name={}", entity.getId(), entity.getSubjectCode(), entity.getAccountName());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public BankAccountDTO update(Long id, BankAccountDTO dto) {
        FinanceAccount entity = financeAccountMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("银行账户不存在: " + id);
        }
        boolean preset = isPreset(entity);
        String newCode = dto.getSubjectCode() == null ? null : dto.getSubjectCode().trim();

        if (preset && StringUtils.hasText(newCode) && !newCode.equals(entity.getSubjectCode())) {
            throw BusinessException.badRequest("系统预置账户不允许修改银行编号");
        }
        if (StringUtils.hasText(newCode) && !newCode.equals(entity.getSubjectCode())) {
            assertCodeAvailable(newCode, id);
        }
        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw BusinessException.badRequest("上级账户不能是自身");
            }
            assertParentValid(dto.getParentId(), id);
            if (collectDescendantIds(id).contains(dto.getParentId())) {
                throw BusinessException.badRequest("上级账户不能是自己的下级");
            }
        }

        applyToEntity(entity, dto);
        if (StringUtils.hasText(newCode)) {
            entity.setSubjectCode(newCode);
        }
        financeAccountMapper.updateById(entity);
        log.info("修改银行账户: id={}, code={}, name={}", id, entity.getSubjectCode(), entity.getAccountName());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FinanceAccount entity = financeAccountMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("银行账户不存在: " + id);
        }
        if (isPreset(entity)) {
            throw BusinessException.badRequest("系统预置账户不允许删除");
        }
        Long childCount = financeAccountMapper.selectCount(
                new LambdaQueryWrapper<FinanceAccount>().eq(FinanceAccount::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw BusinessException.badRequest("该账户存在下级账户，无法删除");
        }
        if (entity.getBalance() != null && entity.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw BusinessException.badRequest("该账户余额不为 0，无法删除");
        }
        // ⚠️ 必须用 UpdateWrapper 显式 SET deleted_flag：
        //    MyBatis-Plus 的 updateById 会把 @TableLogic 字段从 SET 子句中剔除，导致「删除成功但未落库」。
        financeAccountMapper.update(null, new LambdaUpdateWrapper<FinanceAccount>()
                .eq(FinanceAccount::getId, id)
                .set(FinanceAccount::getDeletedFlag, 1));
        log.info("删除银行账户: id={}, code={}", id, entity.getSubjectCode());
    }

    @Override
    @Transactional
    public BankAccountDTO updateStatus(Long id, Integer status) {
        FinanceAccount entity = financeAccountMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("银行账户不存在: " + id);
        }
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.badRequest("状态取值非法，仅支持 0-停用 / 1-启用");
        }
        entity.setStatus(status);
        financeAccountMapper.updateById(entity);
        log.info("{}银行账户: id={}, code={}", status == 1 ? "启用" : "停用", id, entity.getSubjectCode());
        return toDTO(entity);
    }

    @Override
    public String nextCode(Long parentId) {
        String parentCode = null;
        if (parentId != null && parentId > 0) {
            FinanceAccount parent = financeAccountMapper.selectById(parentId);
            if (parent == null) {
                throw BusinessException.notFound("上级账户不存在: " + parentId);
            }
            parentCode = parent.getSubjectCode();
        }
        List<FinanceAccount> siblings = financeAccountMapper.selectList(
                new LambdaQueryWrapper<FinanceAccount>()
                        .select(FinanceAccount::getSubjectCode)
                        .eq(parentId != null && parentId > 0, FinanceAccount::getParentId, parentId)
                        .isNull(parentId == null || parentId <= 0, FinanceAccount::getParentId));

        Set<String> taken = siblings.stream()
                .map(FinanceAccount::getSubjectCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (StringUtils.hasText(parentCode)) {
            // 子账户：父编号 + 两位序号（1002 → 100201、100202 …）
            for (int seq = 1; seq <= 999; seq++) {
                String candidate = parentCode + String.format("%02d", seq);
                if (!taken.contains(candidate)) {
                    return candidate;
                }
            }
            throw BusinessException.badRequest("同级账户数量已达上限");
        }
        // 顶级：从 1001 起取未被占用的最小四位编号
        for (int code = 1001; code <= 9999; code++) {
            String candidate = String.valueOf(code);
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
        throw BusinessException.badRequest("顶级账户数量已达上限");
    }

    // ==================== 内部工具 ====================

    private void applyToEntity(FinanceAccount entity, BankAccountDTO dto) {
        if (dto.getAccountName() != null) entity.setAccountName(dto.getAccountName().trim());
        if (dto.getAccountType() != null) entity.setAccountType(dto.getAccountType());
        if (dto.getBankName() != null) entity.setBankName(dto.getBankName());
        if (dto.getBankAccount() != null) entity.setBankAccount(dto.getBankAccount());
        if (dto.getAccountHolder() != null) entity.setAccountHolder(dto.getAccountHolder());
        if (dto.getEasyCode() != null) entity.setEasyCode(dto.getEasyCode());
        if (dto.getBriefName() != null) entity.setBriefName(dto.getBriefName());
        if (dto.getQrcodeUrl() != null) entity.setQrcodeUrl(dto.getQrcodeUrl());
        if (dto.getMallTransferEnabled() != null) entity.setMallTransferEnabled(dto.getMallTransferEnabled());
        if (dto.getParentId() != null) entity.setParentId(dto.getParentId());
        if (dto.getCurrency() != null) entity.setCurrency(dto.getCurrency());
        if (dto.getAccountLevel() != null) entity.setAccountLevel(dto.getAccountLevel());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getSortNo() != null) entity.setSortNo(dto.getSortNo());
        if (dto.getRemark() != null) entity.setRemark(dto.getRemark());
    }

    private void assertCodeAvailable(String code, Long excludeId) {
        Long count = financeAccountMapper.selectCount(new LambdaQueryWrapper<FinanceAccount>()
                .eq(FinanceAccount::getSubjectCode, code)
                .ne(excludeId != null, FinanceAccount::getId, excludeId));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("银行编号已存在: " + code);
        }
    }

    private void assertParentValid(Long parentId, Long selfId) {
        if (parentId == null || parentId <= 0) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw BusinessException.badRequest("上级账户不能是自身");
        }
        if (financeAccountMapper.selectById(parentId) == null) {
            throw BusinessException.notFound("上级账户不存在: " + parentId);
        }
    }

    private Set<Long> collectDescendantIds(Long rootId) {
        List<FinanceAccount> all = financeAccountMapper.selectList(
                new LambdaQueryWrapper<FinanceAccount>().select(FinanceAccount::getId, FinanceAccount::getParentId));
        Map<Long, List<Long>> childMap = new HashMap<>();
        for (FinanceAccount a : all) {
            if (a.getParentId() != null) {
                childMap.computeIfAbsent(a.getParentId(), k -> new ArrayList<>()).add(a.getId());
            }
        }
        Set<Long> result = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootId);
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            for (Long child : childMap.getOrDefault(cur, List.of())) {
                if (result.add(child)) {
                    stack.push(child);
                }
            }
        }
        return result;
    }

    /** 系统预置账户：显式标记，或标准资金科目 1001/1002 */
    private boolean isPreset(FinanceAccount entity) {
        return (entity.getIsSystem() != null && entity.getIsSystem() == 1)
                || RESERVED_CODES.contains(entity.getSubjectCode());
    }

    private BankAccountDTO toDTO(FinanceAccount entity) {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setId(entity.getId());
        dto.setSubjectCode(entity.getSubjectCode());
        dto.setAccountName(entity.getAccountName());
        dto.setAccountType(entity.getAccountType());
        dto.setBankName(entity.getBankName());
        dto.setBankAccount(entity.getBankAccount());
        dto.setAccountHolder(entity.getAccountHolder());
        dto.setEasyCode(entity.getEasyCode());
        dto.setBriefName(entity.getBriefName());
        dto.setQrcodeUrl(entity.getQrcodeUrl());
        dto.setMallTransferEnabled(entity.getMallTransferEnabled() == null ? 0 : entity.getMallTransferEnabled());
        dto.setParentId(entity.getParentId());
        dto.setBalance(entity.getBalance());
        dto.setCurrency(entity.getCurrency());
        dto.setAccountLevel(entity.getAccountLevel());
        dto.setStatus(entity.getStatus());
        dto.setIsSystem(entity.getIsSystem() == null ? 0 : entity.getIsSystem());
        dto.setSortNo(entity.getSortNo());
        dto.setRemark(entity.getRemark());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }
}

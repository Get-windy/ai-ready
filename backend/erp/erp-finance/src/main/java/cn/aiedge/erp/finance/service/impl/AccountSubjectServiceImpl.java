package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.dto.AccountSubjectQuery;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.mapper.VoucherItemMapper;
import cn.aiedge.erp.finance.model.entity.AccountSubject;
import cn.aiedge.erp.finance.model.entity.VoucherItem;
import cn.aiedge.erp.finance.service.AccountSubjectService;
import cn.aiedge.erp.finance.support.AccountSubjectTreeBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 会计科目Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountSubjectServiceImpl implements AccountSubjectService {

    private final AccountSubjectMapper accountSubjectMapper;
    private final VoucherItemMapper voucherItemMapper;

    @Override
    public List<AccountSubjectDTO> getAll() {
        return search(null);
    }

    @Override
    public List<AccountSubjectDTO> search(AccountSubjectQuery query) {
        AccountSubjectQuery q = query != null ? query : new AccountSubjectQuery();

        LambdaQueryWrapper<AccountSubject> wrapper = new LambdaQueryWrapper<>();
        if (q.getSubjectType() != null) {
            wrapper.eq(AccountSubject::getSubjectType, q.getSubjectType());
        }
        if (q.getDirection() != null) {
            wrapper.eq(AccountSubject::getDirection, q.getDirection());
        }
        if (q.getParentId() != null) {
            wrapper.eq(AccountSubject::getParentId, q.getParentId());
        }
        if (!q.includeDisabledOrDefault()) {
            wrapper.eq(AccountSubject::getIsEnabled, true);
        }
        String keyword = q.getKeyword() == null ? null : q.getKeyword().trim();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(AccountSubject::getSubjectCode, keyword)
                    .or().like(AccountSubject::getSubjectName, keyword)
                    .or().like(AccountSubject::getMnemonicCode, keyword));
        }
        wrapper.orderByAsc(AccountSubject::getSubjectCode);

        List<AccountSubject> subjects = accountSubjectMapper.selectList(wrapper);
        List<AccountSubjectDTO> dtos = subjects.stream().map(this::toDTO).collect(Collectors.toList());
        fillDisplayNames(dtos);
        return dtos;
    }

    @Override
    public AccountSubjectDTO getById(Long id) {
        AccountSubject subject = accountSubjectMapper.selectById(id);
        if (subject == null) {
            throw BusinessException.notFound("会计科目不存在: " + id);
        }
        AccountSubjectDTO dto = toDTO(subject);
        fillDisplayNames(List.of(dto));
        return dto;
    }

    @Override
    @Transactional
    public AccountSubjectDTO create(AccountSubjectDTO dto) {
        if (dto.getSubjectCode() == null || dto.getSubjectCode().trim().isEmpty()) {
            throw BusinessException.badRequest("科目编号不能为空");
        }
        if (dto.getSubjectName() == null || dto.getSubjectName().trim().isEmpty()) {
            throw BusinessException.badRequest("科目名称不能为空");
        }
        dto.setSubjectCode(dto.getSubjectCode().trim());
        dto.setSubjectName(dto.getSubjectName().trim());

        // 科目编号全局唯一
        accountSubjectMapper.findBySubjectCode(dto.getSubjectCode())
                .ifPresent(s -> {
                    throw BusinessException.badRequest("科目编号已存在: " + dto.getSubjectCode());
                });

        AccountSubject parent = null;
        if (dto.getParentId() != null) {
            parent = accountSubjectMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw BusinessException.notFound("上级科目不存在: " + dto.getParentId());
            }
        }

        // 科目分类沿上级继承；无上级时取入参（页面在「科目分类」下新增，已确定分类）
        Integer subjectType = parent != null ? parent.getSubjectType() : dto.getSubjectType();
        if (subjectType == null) {
            throw BusinessException.badRequest("请先在左侧科目分类下选择分类后再新增");
        }
        // 余额方向未指定时继承上级
        Integer direction = dto.getDirection() != null
                ? dto.getDirection()
                : (parent != null && parent.getDirection() != null ? parent.getDirection() : 1);

        AccountSubject entity = toEntity(dto);
        entity.setSubjectType(subjectType);
        entity.setDirection(direction);
        entity.setLevel(parent != null && parent.getLevel() != null ? parent.getLevel() + 1 : 1);
        entity.setIsLeaf(true);
        entity.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : true);
        entity.setFullName(resolveFullName(entity, parent, dto.getFullName()));
        accountSubjectMapper.insert(entity);

        // 上级由叶子变为非叶子
        if (parent != null && Boolean.TRUE.equals(parent.getIsLeaf())) {
            parent.setIsLeaf(false);
            accountSubjectMapper.updateById(parent);
        }
        log.info("创建会计科目: id={}, code={}, name={}", entity.getId(), entity.getSubjectCode(), entity.getSubjectName());
        return getById(entity.getId());
    }

    @Override
    @Transactional
    public AccountSubjectDTO update(Long id, AccountSubjectDTO dto) {
        AccountSubject entity = accountSubjectMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("会计科目不存在: " + id);
        }

        // 科目编号唯一（排除自身）
        if (dto.getSubjectCode() != null && !dto.getSubjectCode().trim().isEmpty()
                && !dto.getSubjectCode().trim().equals(entity.getSubjectCode())) {
            String newCode = dto.getSubjectCode().trim();
            accountSubjectMapper.findBySubjectCode(newCode)
                    .ifPresent(s -> {
                        throw BusinessException.badRequest("科目编号已存在: " + newCode);
                    });
            entity.setSubjectCode(newCode);
        }

        // 上级科目校验：不能是自身，也不能是自身的下级（避免成环）
        AccountSubject parent = null;
        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw BusinessException.badRequest("上级科目不能是自身");
            }
            parent = accountSubjectMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw BusinessException.notFound("上级科目不存在: " + dto.getParentId());
            }
            if (collectCodeWithDescendants(id).contains(parent.getSubjectCode())) {
                throw BusinessException.badRequest("上级科目不能是自身的下级科目");
            }
            entity.setParentId(dto.getParentId());
            entity.setSubjectType(parent.getSubjectType());
            entity.setLevel(parent.getLevel() != null ? parent.getLevel() + 1 : 1);
        }

        if (dto.getSubjectName() != null && !dto.getSubjectName().trim().isEmpty()) {
            entity.setSubjectName(dto.getSubjectName().trim());
        }
        if (dto.getSubjectType() != null && dto.getParentId() == null) {
            entity.setSubjectType(dto.getSubjectType());
        }
        if (dto.getDirection() != null) entity.setDirection(dto.getDirection());
        if (dto.getIsEnabled() != null) entity.setIsEnabled(dto.getIsEnabled());
        if (dto.getMnemonicCode() != null) entity.setMnemonicCode(dto.getMnemonicCode().trim());
        // 核算项可清空：页面每次保存都整体提交该字段，null 即「不核算」
        entity.setAuxiliaryTypeId(dto.getAuxiliaryTypeId());
        if (dto.getRemark() != null) entity.setRemark(dto.getRemark());

        entity.setFullName(resolveFullName(entity, parent, dto.getFullName()));
        accountSubjectMapper.updateById(entity);
        // updateById 默认忽略 null 字段，清空「核算项」需显式 set null
        if (dto.getAuxiliaryTypeId() == null) {
            accountSubjectMapper.update(null, new LambdaUpdateWrapper<AccountSubject>()
                    .eq(AccountSubject::getId, id)
                    .set(AccountSubject::getAuxiliaryTypeId, null));
        }
        // 上级改名/改编号后，下级「科目全名」按新链路重算，保证科目全名口径一致
        refreshDescendantFullNames(id);
        log.info("更新会计科目: id={}, code={}, name={}", id, entity.getSubjectCode(), entity.getSubjectName());
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AccountSubject entity = accountSubjectMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("会计科目不存在: " + id);
        }

        // 有子科目不可删除
        List<AccountSubject> children = accountSubjectMapper.findByParentId(id);
        if (!children.isEmpty()) {
            throw BusinessException.badRequest("该科目下有子科目，无法删除");
        }
        // 已被凭证引用不可删除（保证凭证/总账/报表口径一致）
        long used = voucherItemMapper.selectCount(
                new LambdaQueryWrapper<cn.aiedge.erp.finance.model.entity.VoucherItem>()
                        .eq(cn.aiedge.erp.finance.model.entity.VoucherItem::getSubjectId, id));
        if (used > 0) {
            throw BusinessException.badRequest("该科目已被 " + used + " 条凭证分录引用，无法删除，请改为停用");
        }

        // 逻辑删除须走 deleteById：updateById 不会写入 @TableLogic 字段（deleted_flag）
        accountSubjectMapper.deleteById(id);
        // 父科目若无其它子科目，恢复为叶子
        if (entity.getParentId() != null) {
            AccountSubject parent = accountSubjectMapper.selectById(entity.getParentId());
            if (parent != null && accountSubjectMapper.findByParentId(parent.getId()).isEmpty()) {
                parent.setIsLeaf(true);
                accountSubjectMapper.updateById(parent);
            }
        }
        log.info("删除会计科目: id={}, code={}", id, entity.getSubjectCode());
    }

    @Override
    public List<AccountSubjectDTO> getTree() {
        return getTree(null);
    }

    @Override
    public List<AccountSubjectDTO> getTree(AccountSubjectQuery query) {
        AccountSubjectQuery q = query != null ? query : new AccountSubjectQuery();
        List<AccountSubjectDTO> flat = search(q);
        if (!q.hierarchicalOrDefault()) {
            return flat;
        }
        return buildTree(flat);
    }

    @Override
    public List<AccountSubjectDTO> getByType(Integer subjectType) {
        AccountSubjectQuery q = new AccountSubjectQuery();
        q.setSubjectType(subjectType);
        return search(q);
    }

    @Override
    @Transactional
    public AccountSubjectDTO enable(Long id, boolean enabled) {
        AccountSubject entity = accountSubjectMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("会计科目不存在: " + id);
        }
        entity.setIsEnabled(enabled);
        accountSubjectMapper.updateById(entity);
        log.info("{}会计科目: id={}, code={}", enabled ? "启用" : "禁用", id, entity.getSubjectCode());
        return getById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            delete(id);
        }
        log.info("批量删除会计科目: ids={}", ids);
    }

    @Override
    public List<String> collectCodeWithDescendants(Long subjectId) {
        if (subjectId == null) {
            return List.of();
        }
        AccountSubject root = accountSubjectMapper.selectById(subjectId);
        if (root == null || root.getSubjectCode() == null || root.getSubjectCode().isEmpty()) {
            return List.of();
        }
        List<AccountSubject> all = accountSubjectMapper.selectList(null);
        Map<Long, List<AccountSubject>> childrenByParent = all.stream()
                .filter(s -> s.getParentId() != null)
                .collect(Collectors.groupingBy(AccountSubject::getParentId));

        List<String> codes = new ArrayList<>();
        Deque<AccountSubject> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            AccountSubject current = stack.pop();
            if (current.getSubjectCode() != null && !current.getSubjectCode().isEmpty()) {
                codes.add(current.getSubjectCode());
            }
            for (AccountSubject child : childrenByParent.getOrDefault(current.getId(), List.of())) {
                stack.push(child);
            }
        }
        return codes;
    }

    // ======== 内部工具 ========

    @Override
    public List<FinanceAuxiliaryTypeDTO> getAuxTypeOptions() {
        List<FinanceAuxiliaryTypeDTO> options = new ArrayList<>();
        for (Map<String, Object> row : accountSubjectMapper.selectAuxTypeOptions()) {
            Map<String, Object> lower = new HashMap<>();
            row.forEach((k, v) -> lower.put(k == null ? null : k.toLowerCase(), v));
            Object id = lower.get("id");
            FinanceAuxiliaryTypeDTO dto = new FinanceAuxiliaryTypeDTO();
            dto.setId(id instanceof Number n ? n.longValue() : null);
            dto.setTypeCode(lower.get("typecode") != null ? String.valueOf(lower.get("typecode")) : null);
            dto.setTypeName(lower.get("typename") != null ? String.valueOf(lower.get("typename")) : null);
            dto.setEnabled(true);
            options.add(dto);
        }
        return options;
    }

    /**
     * 重算指定科目全部下级的「科目全名」（上级改名后链路同步）
     */
    private void refreshDescendantFullNames(Long rootId) {
        AccountSubject root = accountSubjectMapper.selectById(rootId);
        if (root == null) {
            return;
        }
        List<AccountSubject> all = accountSubjectMapper.selectList(null);
        Map<Long, List<AccountSubject>> byParent = all.stream()
                .filter(s -> s.getParentId() != null)
                .collect(Collectors.groupingBy(AccountSubject::getParentId));
        Deque<AccountSubject> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            AccountSubject parent = queue.poll();
            String parentFull = parent.getFullName() == null || parent.getFullName().isEmpty()
                    ? parent.getSubjectName() : parent.getFullName();
            for (AccountSubject child : byParent.getOrDefault(parent.getId(), List.of())) {
                String derived = (parentFull == null ? "" : parentFull + "/") + child.getSubjectName();
                if (!derived.equals(child.getFullName())) {
                    child.setFullName(derived);
                    accountSubjectMapper.updateById(child);
                }
                queue.add(child);
            }
        }
    }

    /**
     * 科目全名：显式传入优先；否则「上级科目全名/名称 + 本科目名称」拼装
     */
    private String resolveFullName(AccountSubject entity, AccountSubject parent, String inputFullName) {
        if (inputFullName != null && !inputFullName.trim().isEmpty()) {
            return inputFullName.trim();
        }
        String self = entity.getSubjectName() == null ? "" : entity.getSubjectName();
        if (parent == null) {
            return self;
        }
        String parentFull = parent.getFullName();
        if (parentFull == null || parentFull.isEmpty()) {
            parentFull = parent.getSubjectName();
        }
        return parentFull == null || parentFull.isEmpty() ? self : parentFull + "/" + self;
    }

    /**
     * 回填「上级科目编码/名称」与「核算项名称」，供列表展示
     */
    private void fillDisplayNames(List<AccountSubjectDTO> dtos) {
        if (dtos.isEmpty()) {
            return;
        }
        Map<Long, AccountSubjectDTO> byId = dtos.stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(AccountSubjectDTO::getId, d -> d, (a, b) -> a, HashMap::new));

        // 上级可能不在结果集内，按需补齐
        List<Long> missingParentIds = dtos.stream()
                .map(AccountSubjectDTO::getParentId)
                .filter(Objects::nonNull)
                .filter(pid -> !byId.containsKey(pid))
                .distinct()
                .collect(Collectors.toList());
        if (!missingParentIds.isEmpty()) {
            for (AccountSubject parent : accountSubjectMapper.selectBatchIds(missingParentIds)) {
                byId.put(parent.getId(), toDTO(parent));
            }
        }
        for (AccountSubjectDTO dto : dtos) {
            AccountSubjectDTO parent = byId.get(dto.getParentId());
            if (parent != null) {
                dto.setParentCode(parent.getSubjectCode());
                dto.setParentName(parent.getSubjectName());
            }
        }

        // 核算项名称（辅助核算类型）
        List<Long> typeIds = dtos.stream()
                .map(AccountSubjectDTO::getAuxiliaryTypeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!typeIds.isEmpty()) {
            Map<Long, String> typeNames = new HashMap<>();
            for (FinanceAuxiliaryTypeDTO type : getAuxTypeOptions()) {
                if (typeIds.contains(type.getId())) {
                    typeNames.put(type.getId(), type.getTypeName());
                }
            }
            for (AccountSubjectDTO dto : dtos) {
                if (dto.getAuxiliaryTypeId() != null) {
                    dto.setAuxiliaryTypeName(typeNames.get(dto.getAuxiliaryTypeId()));
                }
            }
        }
    }

    /**
     * 由扁平列表构建树：父节点不在集合内的节点自动升为顶层
     * （与「费用类型」等科目视图共用 {@link AccountSubjectTreeBuilder}，不重复实现建树逻辑）
     */
    private List<AccountSubjectDTO> buildTree(List<AccountSubjectDTO> flat) {
        return AccountSubjectTreeBuilder.build(flat);
    }

    // ======== DTO <-> Entity 转换 ========

    private AccountSubjectDTO toDTO(AccountSubject entity) {
        AccountSubjectDTO dto = new AccountSubjectDTO();
        dto.setId(entity.getId());
        dto.setSubjectCode(entity.getSubjectCode());
        dto.setSubjectName(entity.getSubjectName());
        dto.setParentId(entity.getParentId());
        dto.setLevel(entity.getLevel());
        dto.setSubjectType(entity.getSubjectType());
        dto.setDirection(entity.getDirection());
        dto.setIsLeaf(entity.getIsLeaf());
        dto.setIsEnabled(entity.getIsEnabled());
        dto.setMnemonicCode(entity.getMnemonicCode());
        dto.setFullName(entity.getFullName());
        dto.setAuxiliaryTypeId(entity.getAuxiliaryTypeId());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    private AccountSubject toEntity(AccountSubjectDTO dto) {
        AccountSubject entity = new AccountSubject();
        entity.setSubjectCode(dto.getSubjectCode());
        entity.setSubjectName(dto.getSubjectName());
        entity.setParentId(dto.getParentId());
        entity.setLevel(dto.getLevel());
        entity.setSubjectType(dto.getSubjectType());
        entity.setDirection(dto.getDirection());
        entity.setIsLeaf(dto.getIsLeaf());
        entity.setIsEnabled(dto.getIsEnabled());
        entity.setMnemonicCode(dto.getMnemonicCode());
        entity.setFullName(dto.getFullName());
        entity.setAuxiliaryTypeId(dto.getAuxiliaryTypeId());
        entity.setRemark(dto.getRemark());
        return entity;
    }
}

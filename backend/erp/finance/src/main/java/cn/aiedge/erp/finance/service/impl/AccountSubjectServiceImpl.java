package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.model.entity.AccountSubject;
import cn.aiedge.erp.finance.service.AccountSubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    @Override
    public List<AccountSubjectDTO> getAll() {
        List<AccountSubject> subjects = accountSubjectMapper.selectList(null);
        return subjects.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AccountSubjectDTO getById(Long id) {
        AccountSubject subject = accountSubjectMapper.selectById(id);
        if (subject == null) {
            throw new RuntimeException("会计科目不存在: " + id);
        }
        return toDTO(subject);
    }

    @Override
    @Transactional
    public AccountSubjectDTO create(AccountSubjectDTO dto) {
        // 验证科目编码唯一性
        accountSubjectMapper.findBySubjectCode(dto.getSubjectCode())
                .ifPresent(s -> {
                    throw new RuntimeException("科目编码已存在: " + dto.getSubjectCode());
                });

        // 验证上级科目存在
        if (dto.getParentId() != null) {
            AccountSubject parent = accountSubjectMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new RuntimeException("上级科目不存在: " + dto.getParentId());
            }
        }

        AccountSubject entity = toEntity(dto);
        entity.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : true);
        accountSubjectMapper.insert(entity);
        log.info("创建会计科目: id={}, code={}, name={}", entity.getId(), entity.getSubjectCode(), entity.getSubjectName());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public AccountSubjectDTO update(Long id, AccountSubjectDTO dto) {
        AccountSubject entity = accountSubjectMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("会计科目不存在: " + id);
        }

        // 验证编码唯一性（排除自身）
        if (dto.getSubjectCode() != null && !dto.getSubjectCode().equals(entity.getSubjectCode())) {
            accountSubjectMapper.findBySubjectCode(dto.getSubjectCode())
                    .ifPresent(s -> {
                        throw new RuntimeException("科目编码已存在: " + dto.getSubjectCode());
                    });
        }

        // 验证上级科目存在且不能是自己
        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw new RuntimeException("上级科目不能是自身");
            }
            AccountSubject parent = accountSubjectMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new RuntimeException("上级科目不存在: " + dto.getParentId());
            }
        }

        if (dto.getSubjectCode() != null) entity.setSubjectCode(dto.getSubjectCode());
        if (dto.getSubjectName() != null) entity.setSubjectName(dto.getSubjectName());
        if (dto.getParentId() != null) entity.setParentId(dto.getParentId());
        if (dto.getLevel() != null) entity.setLevel(dto.getLevel());
        if (dto.getSubjectType() != null) entity.setSubjectType(dto.getSubjectType());
        if (dto.getDirection() != null) entity.setDirection(dto.getDirection());
        if (dto.getIsLeaf() != null) entity.setIsLeaf(dto.getIsLeaf());
        if (dto.getIsEnabled() != null) entity.setIsEnabled(dto.getIsEnabled());
        if (dto.getRemark() != null) entity.setRemark(dto.getRemark());

        accountSubjectMapper.updateById(entity);
        log.info("更新会计科目: id={}, code={}, name={}", id, entity.getSubjectCode(), entity.getSubjectName());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AccountSubject entity = accountSubjectMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("会计科目不存在: " + id);
        }

        // 检查是否有子科目
        List<AccountSubject> children = accountSubjectMapper.findByParentId(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("该科目下有子科目，无法删除");
        }

        entity.setDeletedFlag(1);
        accountSubjectMapper.updateById(entity);
        log.info("删除会计科目: id={}, code={}", id, entity.getSubjectCode());
    }

    @Override
    public List<AccountSubjectDTO> getTree() {
        List<AccountSubject> allSubjects = accountSubjectMapper.selectList(null);
        List<AccountSubjectDTO> allDTOs = allSubjects.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        // 构建父子映射
        Map<Long, List<AccountSubjectDTO>> parentMap = allDTOs.stream()
                .filter(d -> d.getParentId() != null)
                .collect(Collectors.groupingBy(AccountSubjectDTO::getParentId));

        // 为每个节点设置子节点
        for (AccountSubjectDTO dto : allDTOs) {
            List<AccountSubjectDTO> children = parentMap.getOrDefault(dto.getId(), new ArrayList<>());
            children.sort(Comparator.comparing(AccountSubjectDTO::getSubjectCode));
            dto.setChildren(children);
        }

        // 返回顶级节点（parentId为null的）
        return allDTOs.stream()
                .filter(d -> d.getParentId() == null)
                .sorted(Comparator.comparing(AccountSubjectDTO::getSubjectCode))
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountSubjectDTO> getByType(Integer subjectType) {
        List<AccountSubject> subjects = accountSubjectMapper.findBySubjectType(subjectType);
        return subjects.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountSubjectDTO enable(Long id, boolean enabled) {
        AccountSubject entity = accountSubjectMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("会计科目不存在: " + id);
        }
        entity.setIsEnabled(enabled);
        accountSubjectMapper.updateById(entity);
        log.info("{}会计科目: id={}, code={}", enabled ? "启用" : "禁用", id, entity.getSubjectCode());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            AccountSubject entity = accountSubjectMapper.selectById(id);
            if (entity == null) {
                throw new RuntimeException("会计科目不存在: " + id);
            }
            List<AccountSubject> children = accountSubjectMapper.findByParentId(id);
            if (!children.isEmpty()) {
                throw new RuntimeException("科目包含子科目，无法删除: " + entity.getSubjectCode());
            }
        }
        accountSubjectMapper.deleteBatchIds(ids);
        log.info("批量删除会计科目: ids={}", ids);
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
        entity.setRemark(dto.getRemark());
        return entity;
    }
}

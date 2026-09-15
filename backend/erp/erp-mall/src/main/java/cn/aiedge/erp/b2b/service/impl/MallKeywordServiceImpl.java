package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.mapper.MallKeywordMapper;
import cn.aiedge.erp.b2b.model.MallKeyword;
import cn.aiedge.erp.b2b.service.MallKeywordService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 商城搜索关键词服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallKeywordServiceImpl implements MallKeywordService {

    private final MallKeywordMapper mallKeywordMapper;
    private final SecurityContext securityContext;

    private Long getCurrentTenantId() {
        return securityContext.getCurrentTenantId();
    }

    @Override
    public IPage<MallKeyword> pageKeywords(Integer pageNum, Integer pageSize, String keyword, Integer keywordType, Integer status,
                                           String remark) {
        LambdaQueryWrapper<MallKeyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallKeyword::getTenantId, getCurrentTenantId());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(MallKeyword::getKeyword, keyword);
        }
        if (keywordType != null) {
            wrapper.eq(MallKeyword::getKeywordType, keywordType);
        }
        if (status != null) {
            wrapper.eq(MallKeyword::getStatus, status);
        }
        // 备注模糊查询（原先前端「备注」列只能展示、无法按备注筛选）
        if (remark != null && !remark.isEmpty()) {
            wrapper.like(MallKeyword::getRemark, remark);
        }
        wrapper.orderByAsc(MallKeyword::getSort).orderByDesc(MallKeyword::getCreateTime);
        return mallKeywordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createKeyword(MallKeyword keyword) {
        keyword.setId(null);
        keyword.setTenantId(getCurrentTenantId());
        keyword.setCreateBy(StpUtil.getLoginIdAsLong());
        if (keyword.getKeyword() == null || keyword.getKeyword().isBlank()) {
            throw new BusinessException("关键词不能为空");
        }
        keyword.setKeyword(keyword.getKeyword().trim());
        if (keyword.getKeywordType() == null) {
            keyword.setKeywordType(1);
        }
        if (keyword.getStatus() == null) {
            keyword.setStatus(1);
        }
        assertNotDuplicate(keyword.getKeyword(), null);
        mallKeywordMapper.insert(keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateKeyword(MallKeyword keyword) {
        mustGet(keyword.getId());
        // tenant_id 由系统维护，不允许修改
        keyword.setTenantId(null);
        if (keyword.getKeyword() != null) {
            if (keyword.getKeyword().isBlank()) {
                throw new BusinessException("关键词不能为空");
            }
            keyword.setKeyword(keyword.getKeyword().trim());
            assertNotDuplicate(keyword.getKeyword(), keyword.getId());
        }
        keyword.setUpdateBy(StpUtil.getLoginIdAsLong());
        keyword.setUpdateTime(LocalDateTime.now());
        mallKeywordMapper.updateById(keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKeyword(Long id) {
        mustGet(id);
        mallKeywordMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态仅支持 1=启用 0=禁用");
        }
        MallKeyword keyword = mustGet(id);
        keyword.setStatus(status);
        keyword.setUpdateBy(StpUtil.getLoginIdAsLong());
        keyword.setUpdateTime(LocalDateTime.now());
        mallKeywordMapper.updateById(keyword);
    }

    /**
     * 同租户下关键词唯一（应用层预检，数据库另有 uk_mall_keyword_tenant_kw 兜底）
     */
    private void assertNotDuplicate(String keyword, Long excludeId) {
        LambdaQueryWrapper<MallKeyword> wrapper = new LambdaQueryWrapper<MallKeyword>()
                .eq(MallKeyword::getTenantId, getCurrentTenantId())
                .eq(MallKeyword::getKeyword, keyword);
        if (excludeId != null) {
            wrapper.ne(MallKeyword::getId, excludeId);
        }
        if (mallKeywordMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("关键词已存在: " + keyword);
        }
    }

    private MallKeyword mustGet(Long id) {
        MallKeyword keyword = id == null ? null : mallKeywordMapper.selectById(id);
        if (keyword == null || !keyword.getTenantId().equals(getCurrentTenantId())) {
            throw BusinessException.notFound("关键词不存在");
        }
        return keyword;
    }
}

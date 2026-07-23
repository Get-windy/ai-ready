package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.mapper.MallNoticeMapper;
import cn.aiedge.erp.b2b.model.MallNotice;
import cn.aiedge.erp.b2b.service.MallNoticeService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商城公告服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallNoticeServiceImpl implements MallNoticeService {

    private final MallNoticeMapper mallNoticeMapper;
    private final SecurityContext securityContext;

    private Long getCurrentTenantId() {
        return securityContext.getCurrentTenantId();
    }

    @Override
    public IPage<MallNotice> pageNotices(Integer pageNum, Integer pageSize, String title, Integer noticeType, Integer status) {
        LambdaQueryWrapper<MallNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallNotice::getTenantId, getCurrentTenantId());
        if (title != null && !title.isEmpty()) {
            wrapper.like(MallNotice::getTitle, title);
        }
        if (noticeType != null) {
            wrapper.eq(MallNotice::getNoticeType, noticeType);
        }
        if (status != null) {
            wrapper.eq(MallNotice::getStatus, status);
        }
        wrapper.orderByAsc(MallNotice::getSort).orderByDesc(MallNotice::getCreateTime);
        return mallNoticeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public MallNotice getNotice(Long id) {
        return mustGet(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotice(MallNotice notice) {
        notice.setId(null);
        notice.setTenantId(getCurrentTenantId());
        notice.setCreateBy(StpUtil.getLoginIdAsLong());
        if (notice.getStatus() == null) {
            notice.setStatus(MallNotice.STATUS_DRAFT);
        }
        if (notice.getNoticeType() == null) {
            notice.setNoticeType(1);
        }
        // 已发布状态只能由发布动作进入
        if (notice.getStatus() == MallNotice.STATUS_PUBLISHED) {
            notice.setPublishTime(LocalDateTime.now());
        } else {
            notice.setPublishTime(null);
        }
        mallNoticeMapper.insert(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(MallNotice notice) {
        mustGet(notice.getId());
        // tenant_id/publish_time 由系统维护，不允许编辑接口直接修改
        notice.setTenantId(null);
        notice.setPublishTime(null);
        notice.setUpdateBy(StpUtil.getLoginIdAsLong());
        notice.setUpdateTime(LocalDateTime.now());
        mallNoticeMapper.updateById(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        mustGet(id);
        mallNoticeMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNotice(Long id) {
        MallNotice notice = mustGet(id);
        notice.setStatus(MallNotice.STATUS_PUBLISHED);
        notice.setPublishTime(LocalDateTime.now());
        notice.setUpdateBy(StpUtil.getLoginIdAsLong());
        notice.setUpdateTime(LocalDateTime.now());
        mallNoticeMapper.updateById(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineNotice(Long id) {
        MallNotice notice = mustGet(id);
        if (notice.getStatus() != MallNotice.STATUS_PUBLISHED) {
            throw new BusinessException("仅已发布的公告可以下线");
        }
        notice.setStatus(MallNotice.STATUS_OFFLINE);
        notice.setUpdateBy(StpUtil.getLoginIdAsLong());
        notice.setUpdateTime(LocalDateTime.now());
        mallNoticeMapper.updateById(notice);
    }

    @Override
    public List<MallNotice> listPublished(Integer limit) {
        Page<MallNotice> page = new Page<>(1, limit == null || limit <= 0 ? 10 : limit);
        return mallNoticeMapper.selectPage(page, new LambdaQueryWrapper<MallNotice>()
                        .eq(MallNotice::getTenantId, getCurrentTenantId())
                        .eq(MallNotice::getStatus, MallNotice.STATUS_PUBLISHED)
                        .orderByAsc(MallNotice::getSort)
                        .orderByDesc(MallNotice::getPublishTime))
                .getRecords();
    }

    private MallNotice mustGet(Long id) {
        MallNotice notice = id == null ? null : mallNoticeMapper.selectById(id);
        if (notice == null || !notice.getTenantId().equals(getCurrentTenantId())) {
            throw BusinessException.notFound("公告不存在");
        }
        return notice;
    }
}

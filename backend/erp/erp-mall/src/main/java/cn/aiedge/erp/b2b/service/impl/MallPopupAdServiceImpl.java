package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.mapper.MallPopupAdMapper;
import cn.aiedge.erp.b2b.model.MallPopupAd;
import cn.aiedge.erp.b2b.service.MallPopupAdService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 弹窗广告服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallPopupAdServiceImpl extends ServiceImpl<MallPopupAdMapper, MallPopupAd>
        implements MallPopupAdService {

    private final SecurityContext securityContext;

    private Long getCurrentTenantId() {
        return securityContext.getCurrentTenantId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        MallPopupAd ad = mustGet(id);
        if (ad.getStatus() != MallPopupAd.STATUS_DRAFT) {
            throw new BusinessException("仅草稿状态的广告可以发布");
        }
        ad.setStatus(MallPopupAd.STATUS_ACTIVE);
        ad.setUpdateBy(securityContext.getCurrentUserId());
        ad.setUpdateTime(LocalDateTime.now());
        updateById(ad);
        log.info("弹窗广告已发布, id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        MallPopupAd ad = mustGet(id);
        if (ad.getStatus() != MallPopupAd.STATUS_ACTIVE) {
            throw new BusinessException("仅投放中的广告可以下线");
        }
        ad.setStatus(MallPopupAd.STATUS_OFFLINE);
        ad.setUpdateBy(securityContext.getCurrentUserId());
        ad.setUpdateTime(LocalDateTime.now());
        updateById(ad);
        log.info("弹窗广告已下线, id={}", id);
    }

    /**
     * 按 ID 查找并校验租户归属，找不到或租户不匹配时抛出异常
     */
    private MallPopupAd mustGet(Long id) {
        MallPopupAd ad = getById(id);
        if (ad == null || !ad.getTenantId().equals(getCurrentTenantId())) {
            throw BusinessException.notFound("弹窗广告不存在");
        }
        return ad;
    }
}

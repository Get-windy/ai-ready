package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.entity.LoyaltyCoupon;
import cn.aiedge.erp.marketing.mapper.LoyaltyCouponMapper;
import cn.aiedge.erp.marketing.service.LoyaltyCouponService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class LoyaltyCouponServiceImpl extends ServiceImpl<LoyaltyCouponMapper, LoyaltyCoupon>
        implements LoyaltyCouponService {

    @Override
    public List<LoyaltyCoupon> listAvailable(Long partnerId) {
        LocalDateTime now = LocalDateTime.now();
        return list(new LambdaQueryWrapper<LoyaltyCoupon>()
                .eq(LoyaltyCoupon::getPartnerId, partnerId)
                .eq(LoyaltyCoupon::getStatus, "UNUSED")
                .and(w -> w
                        .isNull(LoyaltyCoupon::getExpirationDate)
                        .or().ge(LoyaltyCoupon::getExpirationDate, now)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(Long couponId) {
        LoyaltyCoupon coupon = getById(couponId);
        if (coupon != null && "UNUSED".equals(coupon.getStatus())) {
            coupon.setStatus("USED");
            coupon.setUsedTime(LocalDateTime.now());
            updateById(coupon);
        }
    }
}

package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.entity.LoyaltyCoupon;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface LoyaltyCouponService extends IService<LoyaltyCoupon> {
    List<LoyaltyCoupon> listAvailable(Long memberId);
    void useCoupon(Long couponId);
}

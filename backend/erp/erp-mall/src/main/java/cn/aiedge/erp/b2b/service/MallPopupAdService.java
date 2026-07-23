package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.model.MallPopupAd;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 弹窗广告服务接口
 */
public interface MallPopupAdService extends IService<MallPopupAd> {

    /**
     * 发布弹窗广告（草稿→投放中）
     */
    void publish(Long id);

    /**
     * 下线弹窗广告（投放中→已下架）
     */
    void offline(Long id);
}

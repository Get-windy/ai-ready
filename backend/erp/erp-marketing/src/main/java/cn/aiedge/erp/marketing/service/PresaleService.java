package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.entity.Presale;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PresaleService extends IService<Presale> {

    /**
     * 发布预售活动（校验时间窗口与库存）
     */
    void publish(Long id);

    /**
     * 取消预售活动
     */
    void cancel(Long id);

    /**
     * 将已到期的进行中预售活动置为已结束
     */
    void finishExpired();
}

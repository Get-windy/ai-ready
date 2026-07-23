package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.entity.FlashSale;
import cn.aiedge.erp.marketing.entity.FlashSaleOrder;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface FlashSaleService extends IService<FlashSale> {

    /**
     * 发布秒杀场次（校验时间窗口与库存）
     */
    void publish(Long id);

    /**
     * 取消秒杀场次
     */
    void cancel(Long id);

    /**
     * 分页查询场次参与记录（先按参与记录累计回写 sold_count）
     */
    IPage<FlashSaleOrder> pageParticipants(Long sessionId, Integer pageNum, Integer pageSize);

    /**
     * 按参与记录累计回写场次 sold_count
     */
    void recalcSoldCount(Long sessionId);

    /**
     * 将已到期的已发布场次置为已结束
     */
    void finishExpired();
}

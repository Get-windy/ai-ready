package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.PurchasePriceTrackQueryDTO;
import cn.aiedge.erp.purchase.dto.PurchasePriceTrackSaveDTO;
import cn.aiedge.erp.purchase.dto.PurchasePriceTrendVO;
import cn.aiedge.erp.purchase.entity.PurchasePriceTrack;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 采购价格跟踪Service
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PurchasePriceTrackService {

    /**
     * 分页查询价格跟踪列表（商品×往来单位最近一条）
     */
    IPage<PurchasePriceTrack> page(PurchasePriceTrackQueryDTO dto);

    /**
     * 新增"价格折扣"（同商品×往来单位×同日已存在则更新，否则插入新价格点）
     */
    PurchasePriceTrack saveTrack(PurchasePriceTrackSaveDTO dto);

    /**
     * 修改价格记录
     */
    PurchasePriceTrack updateTrack(Long id, PurchasePriceTrackSaveDTO dto);

    /**
     * 删除价格记录
     */
    void deleteTrack(Long id);

    /**
     * 批量删除
     */
    void batchDelete(List<Long> ids);

    /**
     * 某商品价格趋势（按采购日期升序）
     */
    List<PurchasePriceTrendVO> trend(Long productId);
}

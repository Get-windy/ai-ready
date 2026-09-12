package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.SalePriceTrackQueryDTO;
import cn.aiedge.erp.sale.dto.SalePriceTrackSaveDTO;
import cn.aiedge.erp.sale.dto.SalePriceTrendVO;
import cn.aiedge.erp.sale.entity.SalePriceTrack;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 销售价格跟踪Service
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SalePriceTrackService {

    /**
     * 分页查询价格跟踪列表（商品×往来单位最近一条）
     */
    IPage<SalePriceTrack> page(SalePriceTrackQueryDTO dto);

    /**
     * 新增"价格折扣"（同商品×往来单位×同日已存在则更新，否则插入新价格点）
     */
    SalePriceTrack saveTrack(SalePriceTrackSaveDTO dto);

    /**
     * 修改价格记录
     */
    SalePriceTrack updateTrack(Long id, SalePriceTrackSaveDTO dto);

    /**
     * 删除价格记录
     */
    void deleteTrack(Long id);

    /**
     * 批量删除
     */
    void batchDelete(List<Long> ids);

    /**
     * 某商品价格趋势（按销售日期升序）
     */
    List<SalePriceTrendVO> trend(Long productId);
}

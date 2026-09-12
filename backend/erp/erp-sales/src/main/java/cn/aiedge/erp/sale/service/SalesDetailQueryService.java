package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.SalesDetailQueryDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 销售明细查询服务接口
 */
public interface SalesDetailQueryService {

    /**
     * 分页查询销售明细（96列，按明细行分页）
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    Page<Map<String, Object>> pageDetail(SalesDetailQueryDTO queryDTO);

    /**
     * 不分页查询销售明细（96列）——导出用，与 pageDetail 共用同一套过滤与组装口径
     */
    List<Map<String, Object>> listDetail(SalesDetailQueryDTO queryDTO);

    /**
     * 最近成交价聚合（对标：商品×往来单位最近成交价，含条码/规格/型号/产地/最后修改时间）
     */
    Page<Map<String, Object>> pageRecentPriceAgg(SalesDetailQueryDTO queryDTO);
}

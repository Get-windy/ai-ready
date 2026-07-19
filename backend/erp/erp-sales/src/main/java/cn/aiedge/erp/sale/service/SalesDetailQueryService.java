package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.SalesDetailQueryDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;

/**
 * 销售明细查询服务接口
 */
public interface SalesDetailQueryService {

    /**
     * 分页查询销售明细（96列）
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    Page<Map<String, Object>> pageDetail(SalesDetailQueryDTO queryDTO);
}

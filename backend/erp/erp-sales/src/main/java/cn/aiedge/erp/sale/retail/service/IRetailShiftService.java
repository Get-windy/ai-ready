package cn.aiedge.erp.sale.retail.service;

import cn.aiedge.erp.sale.retail.entity.RetailOrder;
import cn.aiedge.erp.sale.retail.entity.RetailShift;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public interface IRetailShiftService extends IService<RetailShift> {

    /**
     * 开班：收银员/仓库/期初现金，校验该收银员无未交班班次
     */
    RetailShift openShift(Long cashierId, String cashierName, Long warehouseId,
                          BigDecimal openingCash, String remark);

    /**
     * 交班：录入实点现金，自动汇总该班次时段内已结算零售单，
     * 计算应收现金与长短款
     */
    RetailShift closeShift(Long shiftId, BigDecimal closingCash, String remark);

    /**
     * 查询收银员当前营业中班次（无则返回 null）
     */
    RetailShift getCurrent(Long cashierId);

    /**
     * 历史班次分页
     */
    IPage<RetailShift> pageShifts(Page<RetailShift> page, Long cashierId, Integer status,
                                  String dateStart, String dateEnd);

    /**
     * 班次详情（含汇总明细：班次内已结算零售单列表）
     */
    ShiftDetailVO getDetail(Long id);

    @Data
    class ShiftDetailVO {
        private RetailShift shift;
        private List<RetailOrder> orders;
    }
}

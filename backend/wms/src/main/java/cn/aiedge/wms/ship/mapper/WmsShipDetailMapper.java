package cn.aiedge.wms.ship.mapper;

import cn.aiedge.wms.entity.WmsShipDetail;
import cn.aiedge.wms.ship.dto.ShipQuery;
import cn.aiedge.wms.ship.dto.WmsShipDetailPageVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WmsShipDetailMapper extends BaseMapper<WmsShipDetail> {

    /**
     * 按明细分页查询发货单（JOIN 发货单头补单号/仓库/客户等冗余信息）.
     */
    @Select("<script>"
            + "SELECT d.id, d.ship_id, d.line_no, d.product_id, d.product_code, d.product_name, d.product_spec, d.product_unit,"
            + "       d.expected_quantity, d.scanned_quantity, d.confirmed_quantity, d.batch_no, d.serial_no, d.location_id, d.location_code,"
            + "       d.status, d.remark,"
            + "       t.task_no, t.source_order_no, t.warehouse_id, t.warehouse_name, t.customer_name, t.carrier_name, t.tracking_no, t.status AS task_status, t.create_time"
            + " FROM wms_ship_detail d"
            + " LEFT JOIN wms_ship_task t ON d.ship_id = t.id"
            + "<where>"
            + "<if test='q.keyword != null and q.keyword != \"\"'>"
            + "  AND (t.task_no LIKE CONCAT('%', #{q.keyword}, '%') OR t.source_order_no LIKE CONCAT('%', #{q.keyword}, '%'))"
            + "</if>"
            + "<if test='q.sourceOrderNo != null and q.sourceOrderNo != \"\"'>"
            + "  AND t.source_order_no LIKE CONCAT('%', #{q.sourceOrderNo}, '%')"
            + "</if>"
            + "<if test='q.warehouseId != null'>"
            + "  AND t.warehouse_id = #{q.warehouseId}"
            + "</if>"
            + "<if test='q.warehouseName != null and q.warehouseName != \"\"'>"
            + "  AND t.warehouse_name LIKE CONCAT('%', #{q.warehouseName}, '%')"
            + "</if>"
            + "<if test='q.customerName != null and q.customerName != \"\"'>"
            + "  AND t.customer_name LIKE CONCAT('%', #{q.customerName}, '%')"
            + "</if>"
            + "<if test='q.carrierName != null and q.carrierName != \"\"'>"
            + "  AND t.carrier_name LIKE CONCAT('%', #{q.carrierName}, '%')"
            + "</if>"
            + "<if test='q.trackingNo != null and q.trackingNo != \"\"'>"
            + "  AND t.tracking_no LIKE CONCAT('%', #{q.trackingNo}, '%')"
            + "</if>"
            + "<if test='q.status != null'>"
            + "  AND d.status = #{q.status}"
            + "</if>"
            + "<if test='q.productName != null and q.productName != \"\"'>"
            + "  AND d.product_name LIKE CONCAT('%', #{q.productName}, '%')"
            + "</if>"
            + "<if test='q.productCode != null and q.productCode != \"\"'>"
            + "  AND d.product_code LIKE CONCAT('%', #{q.productCode}, '%')"
            + "</if>"
            + "<if test='q.startDate != null and q.startDate != \"\"'>"
            + "  AND t.create_time &gt;= #{q.startDate}"
            + "</if>"
            + "<if test='q.endDate != null and q.endDate != \"\"'>"
            + "  AND t.create_time &lt;= CONCAT(#{q.endDate}, ' 23:59:59')"
            + "</if>"
            + "</where>"
            + " ORDER BY d.id DESC"
            + "</script>")
    IPage<WmsShipDetailPageVO> selectDetailPage(IPage<WmsShipDetailPageVO> page, @Param("q") ShipQuery query);
}

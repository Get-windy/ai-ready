package cn.aiedge.dms.task.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配送任务 → 出库单号反查（供《发货查询》固定项「配送状态 / 配送线路」使用）
 *
 * <p>「配送状态 / 配送线路」是配送任务（`dms_task`）的执行属性，不是销售出库单自身的列；
 * 本查询按 `source_bill_no`（来源单据号 = 出库单号）反查出单号集合，
 * 再由《发货查询》把它作为 `outboundNos` 传给销售出库单分页接口做精确过滤。
 * 这样 DMS 的配送语义留在 DMS 侧，销售模块不需要 JOIN DMS 表结构。</p>
 *
 * @author AI-Ready Team
 */
@Mapper
public interface OutboundDeliveryLookupMapper {

    /**
     * 反查命中的来源单据号（去重，按单号排序，最多 limit 条）
     *
     * @param statuses 配送任务状态集合（对外三值已由 Service 展开为执行态集合）；空=不限
     * @param routeId  配送线路档案ID；空=不限
     * @param limit    上限（超出即为截断，由调用方提示收窄条件）
     */
    @Select("<script>"
            + "SELECT DISTINCT t.source_bill_no AS sourceBillNo FROM dms_task t "
            + "WHERE t.deleted = 0 AND t.source_bill_no IS NOT NULL AND t.source_bill_no &lt;&gt; '' "
            + "<if test=\"statuses != null and statuses.size() > 0\">"
            + "  AND t.status IN <foreach collection='statuses' item='s' open='(' separator=',' close=')'>#{s}</foreach> "
            + "</if>"
            + "<if test='routeId != null'> AND t.route_id = #{routeId} </if>"
            + "ORDER BY t.source_bill_no ASC LIMIT #{limit}"
            + "</script>")
    List<String> findSourceBillNos(@Param("statuses") List<Integer> statuses,
                                   @Param("routeId") Long routeId,
                                   @Param("limit") int limit);
}

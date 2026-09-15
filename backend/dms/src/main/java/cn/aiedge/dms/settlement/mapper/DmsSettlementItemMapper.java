package cn.aiedge.dms.settlement.mapper;

import cn.aiedge.dms.settlement.entity.DmsSettlementItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配送结算单明细 Mapper
 */
@Mapper
public interface DmsSettlementItemMapper extends BaseMapper<DmsSettlementItem> {

    /**
     * 查询已被「已确认/已推送」结算单占用（结算过）的任务ID
     *
     * <p>生成结算单时用于**防止同一任务被重复结算**（如先按配送员、后又按渠道结算同一单）；
     * 草稿（status=0）不占用，允许重建。</p>
     */
    @Select("<script>"
            + "SELECT DISTINCT i.task_id FROM dms_settlement_item i "
            + "JOIN dms_settlement s ON s.id = i.settlement_id AND s.deleted = 0 AND s.status &gt;= 1 "
            + "WHERE i.deleted = 0 AND i.task_id IN "
            + "<foreach collection='taskIds' item='t' open='(' separator=',' close=')'>#{t}</foreach>"
            + "</script>")
    List<Long> selectSettledTaskIds(@Param("taskIds") List<Long> taskIds);
}

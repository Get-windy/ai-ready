package cn.aiedge.erp.marketing.mapper;

import cn.aiedge.erp.marketing.dto.ShareSummaryRow;
import cn.aiedge.erp.marketing.entity.ShareRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShareRecordMapper extends BaseMapper<ShareRecord> {

    /**
     * 按分享对象聚合的分享统计（「我要推广」各物料 Tab 的 5 个统计列）。
     * 传 {@code sharerId} 即只看「我」的分享（我的推广 Tab 口径）。
     */
    @Select("""
            <script>
            SELECT target_id                              AS target_id,
                   MAX(share_time)                        AS last_share_time,
                   COUNT(*)                               AS share_count,
                   COALESCE(SUM(view_count), 0)           AS view_count,
                   COALESCE(SUM(viewer_count), 0)         AS viewer_count,
                   COALESCE(SUM(receive_count), 0)        AS receive_count
            FROM mkt_share_record
            WHERE deleted = 0
              AND tenant_id = #{tenantId}
              AND share_type = #{shareType}
              <if test="sharerId != null"> AND sharer_id = #{sharerId} </if>
            GROUP BY target_id
            </script>
            """)
    List<ShareSummaryRow> selectSummaryByTarget(@Param("tenantId") Long tenantId,
                                                @Param("shareType") String shareType,
                                                @Param("sharerId") Long sharerId);
}

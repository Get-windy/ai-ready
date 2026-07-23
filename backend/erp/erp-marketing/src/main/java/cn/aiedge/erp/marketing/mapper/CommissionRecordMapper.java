package cn.aiedge.erp.marketing.mapper;

import cn.aiedge.erp.marketing.dto.StaffCommissionSummaryDTO;
import cn.aiedge.erp.marketing.entity.CommissionRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface CommissionRecordMapper extends BaseMapper<CommissionRecord> {

    /**
     * 员工提成汇总分页: 按 referrer_id (推荐人=员工/业务员) 聚合,
     * 左联 sys_user 取姓名 (sys_user 为全局租户拦截器忽略表, 无需额外处理)
     */
    @Select("""
            <script>
            SELECT r.referrer_id,
              COALESCE(u.real_name, u.nickname, u.username) AS staff_name,
              COUNT(*) AS record_count,
              COUNT(DISTINCT r.order_id) AS order_count,
              COALESCE(SUM(r.order_amount), 0) AS total_order_amount,
              COALESCE(SUM(r.commission_amount), 0) AS total_commission_amount,
              COALESCE(SUM(CASE WHEN r.status = 'PAID' THEN r.commission_amount ELSE 0 END), 0) AS settled_commission_amount,
              SUM(CASE WHEN r.status = 'PAID' THEN 1 ELSE 0 END) AS settled_count,
              COALESCE(SUM(CASE WHEN r.status IN ('DRAFT', 'CONFIRMED') THEN r.commission_amount ELSE 0 END), 0) AS unsettled_commission_amount,
              SUM(CASE WHEN r.status IN ('DRAFT', 'CONFIRMED') THEN 1 ELSE 0 END) AS unsettled_count
            FROM erp_commission_record r
            LEFT JOIN sys_user u ON u.id = r.referrer_id AND u.deleted = 0
            WHERE r.deleted = 0
              AND r.referrer_id IS NOT NULL
            <if test="startTime != null">
              AND r.create_time &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              AND r.create_time &lt;= #{endTime}
            </if>
            <if test="keyword != null and keyword != ''">
              AND COALESCE(u.real_name, u.nickname, u.username) ILIKE CONCAT('%', #{keyword}, '%')
            </if>
            GROUP BY r.referrer_id, u.real_name, u.nickname, u.username
            ORDER BY total_commission_amount DESC
            </script>
            """)
    IPage<StaffCommissionSummaryDTO> selectStaffSummaryPage(Page<StaffCommissionSummaryDTO> page,
                                                            @Param("startTime") LocalDateTime startTime,
                                                            @Param("endTime") LocalDateTime endTime,
                                                            @Param("keyword") String keyword);
}

package cn.aiedge.erp.printing.mapper;

import cn.aiedge.erp.printing.entity.v2.SysScreenshotTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysScreenshotTaskMapper extends BaseMapper<SysScreenshotTask> {

    /**
     * 查询已超时的 MANUAL_CONFIRM 截图任务
     * 关联 print_task 和 chain_item 获取截图确认超时时间
     * 条件：ss.status=PENDING AND ci.screenshot_mode='MANUAL_CONFIRM'
     *       AND NOW() > ss.created_at + ci.screenshot_confirm_timeout 秒
     */
    @Select("""
            SELECT ss.* FROM sys_screenshot_task ss
            JOIN sys_print_task t ON ss.screenshot_id = t.screenshot_id
            JOIN sys_print_chain_item ci ON t.chain_item_id = ci.item_key::bigint
            WHERE ss.status = 'PENDING'
              AND ci.screenshot_mode = 'MANUAL_CONFIRM'
              AND ci.screenshot_confirm_timeout IS NOT NULL
              AND EXTRACT(EPOCH FROM NOW() - ss.created_at) > ci.screenshot_confirm_timeout
            LIMIT #{limit}
            """)
    List<SysScreenshotTask> selectExpiredManualConfirmTasks(@Param("limit") int limit);
}

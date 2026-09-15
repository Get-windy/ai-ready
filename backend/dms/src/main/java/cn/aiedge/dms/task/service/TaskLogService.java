package cn.aiedge.dms.task.service;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.task.entity.DmsTaskLog;
import cn.aiedge.dms.task.mapper.DmsTaskLogMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 调度审计服务（《调度任务开发文档》§3.6 工程约束 3）
 *
 * <p>独立成服务（不放在 TaskService）以免 DispatchService ↔ TaskService 互相依赖成环。
 * 审计写入**永不抛出**：留痕失败不能打断调度主流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskLogService {

    private final DmsTaskLogMapper taskLogMapper;
    private final DmsRiderMapper riderMapper;
    private final SysUserMapper sysUserMapper;

    /** 各动作的中文名（前端列表直接展示，避免前后端两套映射） */
    private static final Map<String, String> ACTION_TEXT = Map.of(
            DmsTaskLog.ACTION_ASSIGN, "指派",
            DmsTaskLog.ACTION_REASSIGN, "改派",
            DmsTaskLog.ACTION_UNASSIGN, "取消指派",
            DmsTaskLog.ACTION_CANCEL, "取消任务",
            DmsTaskLog.ACTION_EXCEPTION, "标记异常",
            DmsTaskLog.ACTION_AUTO_ASSIGN, "自动调度",
            DmsTaskLog.ACTION_ESCALATE, "超时升级重派",
            DmsTaskLog.ACTION_BATCH_ASSIGN, "批量指派",
            DmsTaskLog.ACTION_BATCH_CANCEL, "批量取消",
            DmsTaskLog.ACTION_CHANNEL, "渠道回传");

    /**
     * 写一条调度审计
     *
     * @param taskId        任务ID
     * @param taskNo        任务编号快照
     * @param action        动作（见 {@link DmsTaskLog} 常量）
     * @param fromRiderId   变更前配送员（可空）
     * @param toRiderId     变更后配送员（可空）
     * @param reason        原因/说明（可空）
     */
    public void record(Long taskId, String taskNo, String action,
                       Long fromRiderId, Long toRiderId, String reason) {
        try {
            DmsTaskLog entity = new DmsTaskLog();
            entity.setTaskId(taskId);
            entity.setTaskNo(taskNo);
            entity.setAction(action);
            entity.setActionText(ACTION_TEXT.getOrDefault(action, action));
            entity.setFromRiderId(fromRiderId);
            entity.setFromRiderName(riderName(fromRiderId));
            entity.setToRiderId(toRiderId);
            entity.setToRiderName(riderName(toRiderId));
            entity.setReason(truncate(reason));
            Long operatorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
            entity.setOperatorId(operatorId);
            entity.setOperatorName(operatorId == null ? "系统" : currentUserName(operatorId));
            taskLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("调度审计写入失败: taskId={}, action={}", taskId, action, e);
        }
    }

    /** 任务审计时间线（倒序，最近的在最前） */
    public List<DmsTaskLog> listByTask(Long taskId) {
        return taskLogMapper.selectList(new LambdaQueryWrapper<DmsTaskLog>()
                .eq(DmsTaskLog::getTaskId, taskId)
                .orderByDesc(DmsTaskLog::getCreateTime)
                .orderByDesc(DmsTaskLog::getId));
    }

    private String riderName(Long riderId) {
        if (riderId == null) {
            return null;
        }
        DmsRider rider = riderMapper.selectById(riderId);
        return rider == null ? null : rider.getRealName();
    }

    /** 当前登录用户真实姓名（昵称优先，其次用户名） */
    private String currentUserName(Long userId) {
        try {
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null) {
                if (StringUtils.hasText(user.getNickname())) {
                    return user.getNickname();
                }
                if (StringUtils.hasText(user.getUsername())) {
                    return user.getUsername();
                }
            }
        } catch (Exception e) {
            log.warn("查询审计操作人姓名失败: userId={}", userId, e);
        }
        return String.valueOf(userId);
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 500 ? text : text.substring(0, 500);
    }
}

package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_group_buy_participant")
public class GroupBuyParticipant {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long activityId;
    private String groupId;
    private Long partnerId;
    private String userName;
    private Integer quantity;
    private Long orderId;
    private String orderStatus;
    private Integer isCreator;
    private String groupStatus;
    private LocalDateTime joinTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 弹窗广告
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_popup_ad")
public class MallPopupAd extends BaseEntity {

    /** 状态: 0=草稿 1=投放中 2=已结束 3=已下架 */
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_FINISHED = 2;
    public static final int STATUS_OFFLINE = 3;

    /** 广告标题 */
    private String title;

    /** 广告图片 URL */
    private String imageUrl;

    /** 点击跳转链接 */
    private String linkUrl;

    /** 展示方式: once=仅首次 everyday=每日 once_per_session=每次会话 */
    private String showType;

    /** 目标用户: all=全部 member=会员 new=新用户 */
    private String targetUser;

    /** 投放开始时间 */
    private LocalDateTime startTime;

    /** 投放结束时间 */
    private LocalDateTime endTime;

    /** 排序（数字越小越靠前） */
    private Integer sort;

    /** 状态: 0=草稿 1=投放中 2=已结束 3=已下架 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建人姓名（对标「商城弹窗广告」页「创建人」列，写入时快照） */
    @com.baomidou.mybatisplus.annotation.TableField(updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private String creatorName;
}

package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商城公告
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_notice")
public class MallNotice extends BaseEntity {

    /** 状态: 0=草稿 1=已发布 2=已下线 */
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_OFFLINE = 2;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 公告类型: 1=公告 2=活动 3=系统 */
    private Integer noticeType;

    /** 状态: 0=草稿 1=已发布 2=已下线 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 排序 */
    private Integer sort;
}

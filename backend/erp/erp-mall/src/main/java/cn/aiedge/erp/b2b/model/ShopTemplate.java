package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商城页面模板
 */
@Data
@TableName("shop_template")
public class ShopTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 模板名称 */
    private String templateName;

    /** 模板编码 */
    private String templateCode;

    /** 缩略图 */
    private String thumbnail;

    /** 描述 */
    private String description;

    /** 模板配置JSON */
    private String configJson;

    /** 是否默认 */
    private Integer isDefault;

    /** 1启用 0停用 */
    private Integer status;

    // ========================================================================
    // 行业模板库（Flyway V11.366.0__Add_Mall_Shop_Decoration_Storage.sql）
    // 页面：商城装修 → 装修模板 → 模板库（对标实测 14 行业，「引用」= 复制到本租户）
    //
    // ⚠️ 本表**无 tenant_id 列**（V6.4.0 建表即无，全库无 ALTER 追加），
    //    故「库」条目（isLibrary=1）为**平台共享**数据；「引用」= 复制出一条
    //    isLibrary=0 的行，沿用既有 GET /template/list 的可见性口径（不新增隔离列，
    //    避免改变既有页面语义）。见开发文档「未实现/未闭环清单」。
    // ⚠️ 行业模板的**内部布局内容未实测**，库条目的 configJson 一律为空，
    //    禁止臆造 14 个假模板结构。
    // ========================================================================

    /** 【行业模板库】行业编码（⚠️ 对标未实测，本实现口径：office/apparel/…/jewelry） */
    private String industryCode;

    /** 【行业模板库】行业名称（**对标实测逐字**：办公用品/服装鞋帽/化妆用品/机械机电/家电数码/母婴用品/汽修汽配/生鲜农贸/食品快消品/手机通讯/通用行业/五金建材/医药制品/珠宝钟表） */
    private String industryName;

    /** 【行业模板库】1=库条目（平台共享，供「模板库」Tab 展示与引用） 0=本租户「我的模板」 */
    private Integer isLibrary;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}

package cn.aiedge.erp.b2b.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商城商品标签（{@code erp_mall_tag}）**只读**映射。
 *
 * <p><b>跨模块说明</b>：该表的**权威写入方在 erp-stock**
 * （{@code cn.aiedge.erp.stock.controller.MallTagController @RequestMapping("/api/erp/mall-tag")}，
 * 即管理端「商品上架 → 商品标签」）。erp-mall 只做**只读引用**，不提供任何写路径。
 * 之所以在本模块再建一个映射（而不是依赖 erp-stock），是因为 erp-mall 的 pom 只依赖
 * {@code core-base} 与 {@code erp-partner} —— 引入 erp-stock 会形成模块环
 * （erp-stock → ... → erp-mall 的商品视图已在用）。这与 {@link ErpProductMall}
 * 读 {@code v_mall_product} 是同一处置。</p>
 *
 * <p><b>用途</b>：C 端「分类页顶部商品标签栏」与「商品卡角标」——
 * 商品的标签以逗号分隔的 {@code tag_code} 存在 {@code erp_product.mall_tags}，
 * 前端拿本表把 code 翻译成标签名。</p>
 *
 * <p>本表**有** {@code tenant_id}，由租户插件按会话租户自动过滤，无需登记忽略清单。</p>
 */
@Data
@TableName("erp_mall_tag")
public class MallTagOption implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 标签名（如「荐」「HOT」） */
    private String tagName;

    /** 标签编码（TAG_1…TAG_n），商品 mall_tags 里存的就是它 */
    private String tagCode;

    private Integer sortOrder;

    /** 1=启用 / 0=停用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

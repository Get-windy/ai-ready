package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商城收货地址（{@code mall_address}）。
 *
 * <p><b>2026-09-26 按真表结构重写，并改为**不继承** {@link BaseEntity}</b>，原因有二：</p>
 * <ol>
 *   <li>原实体映射的 {@code customer_id / province / city / district / detail_address / label}
 *       在表里**一个都不存在**（表只有 {@code user_id / region / address}）⇒
 *       {@code /v1/mall/user/addresses} 增删改查全部 500（真机：{@code 字段 "customer_id" 不存在}）。</li>
 *   <li>{@code BaseEntity} 带 {@code create_by / update_by}，而本表**没有这两列**
 *       （V9.34.0 建表即如此；全库 229/572 张表同样没有 ⇒ 这两列不是全局约定）
 *       ⇒ 修完字段名后紧接着报 {@code 字段 "create_by" 不存在}。
 *       MP 的字段清单来自实体，故只能在自己类里声明**实际存在的列**。</li>
 * </ol>
 * <p>权威结构见 {@code V9.34.0__Backfill_Missing_Tables.sql}。</p>
 */
@Data
@TableName("mall_address")
public class MallAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 归属用户 = 当前登录的商城买家（shop_user.id） */
    private Long userId;

    /** 收货人 */
    private String consignee;

    /** 联系电话 */
    private String phone;

    /** 省市区（一个字符串，如「广东省深圳市南山区」—— 表结构如此，不做拆分猜测） */
    private String region;

    /** 详细地址（街道门牌等） */
    private String address;

    /** 是否默认地址：**0/1 的整数**（表列类型是 integer，不是 boolean） */
    private Integer isDefault;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

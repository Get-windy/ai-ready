package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商城搜索关键词
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_keyword")
public class MallKeyword extends BaseEntity {

    /** 关键词 */
    private String keyword;

    /** 关键词类型: 1=热门 2=置顶 3=屏蔽 */
    private Integer keywordType;

    /** 排序 */
    private Integer sort;

    /** 状态: 1=启用 0=禁用 */
    private Integer status;
}

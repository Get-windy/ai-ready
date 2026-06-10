package cn.aiedge.erp.printing.entity.v2;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_print_chain")
public class SysPrintChain {

    @TableId(type = IdType.AUTO)
    private Long chainId;

    private Long tenantId;

    private String pageCode;

    private String chainName;

    private String description;

    private String status;

    private Integer sortOrder;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}

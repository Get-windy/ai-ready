package cn.aiedge.erp.printing.entity.v2;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_print_chain_item")
public class SysPrintChainItem {

    @TableId(type = IdType.AUTO)
    private Long itemId;

    private Long chainId;

    private Integer stepOrder;

    private Long templateId;

    private Long clientId;

    private String printerName;

    /**
     * 截图模式: DISABLED / MANUAL_CONFIRM / AUTO_CONFIRM
     */
    private String screenshotMode;

    private Integer screenshotConfirmTimeout;

    /**
     * 截图自定义配置 JSON：可自定义截图内容、布局、水印等
     */
    private String screenshotConfigJson;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

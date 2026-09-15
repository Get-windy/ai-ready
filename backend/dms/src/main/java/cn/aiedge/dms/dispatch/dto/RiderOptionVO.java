package cn.aiedge.dms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 配送员选项（区域分包绑定的「配送员」选择器数据源）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "配送员选项")
public class RiderOptionVO {

    private Long id;

    @Schema(description = "配送员编号")
    private String riderNo;

    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态：0-离线 1-空闲 2-忙碌 3-休息")
    private Integer status;

    @Schema(description = "实名认证状态：1-已通过")
    private Integer verifyStatus;
}

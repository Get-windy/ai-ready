package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

/**
 * 打印客户端登录响应 VO
 */
@Data
public class PrintClientLoginVO {

    /** 访问令牌 */
    private String accessToken;

    /** 令牌类型 */
    private String tokenType = "Bearer";

    /** 租户 ID */
    private Long tenantId;

    /** 租户名称 */
    private String tenantName;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 客户端信息（未注册时 clientId=null） */
    private ClientInfo client;

    @Data
    public static class ClientInfo {
        private Long clientId;
        private String clientName;
        private String clientCode;
        private String authKey;
        private String status;
    }
}

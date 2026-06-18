package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地理编码请求（地址 → 坐标）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeRequest {

    /** 地址（如：北京市朝阳区建国路88号） */
    private String address;

    /** 城市（可选，辅助解析） */
    private String city;
}

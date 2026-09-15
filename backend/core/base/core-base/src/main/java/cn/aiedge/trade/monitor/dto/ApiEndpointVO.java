package cn.aiedge.trade.monitor.dto;

import java.util.List;

/**
 * 开放接口目录项（「快速联调」接口分组树的一个叶子）
 *
 * @param group      分组键（order / inventory / sync / product / health）
 * @param groupLabel 分组中文名
 * @param key        接口键（联调发送时回传，避免前端拼接路径）
 * @param name       接口中文名
 * @param method     HTTP 方法
 * @param path       路径模板（含 {pathVar}）
 * @param description 说明
 * @param params     参数定义
 */
public record ApiEndpointVO(
        String group,
        String groupLabel,
        String key,
        String name,
        String method,
        String path,
        String description,
        List<ApiEndpointParamVO> params) {
}

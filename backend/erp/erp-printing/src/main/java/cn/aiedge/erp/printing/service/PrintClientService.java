package cn.aiedge.erp.printing.service;

import cn.aiedge.erp.printing.dto.v2.PrintClientRegisterRequest;
import cn.aiedge.erp.printing.dto.v2.PrintClientVO;
import cn.aiedge.erp.printing.entity.v2.SysPrintClient;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface PrintClientService {

    PrintClientVO register(PrintClientRegisterRequest request, Long tenantId, Long userId);

    PrintClientVO updateClient(Long clientId, PrintClientRegisterRequest request, Long tenantId);

    PrintClientVO getClient(Long clientId, Long tenantId);

    SysPrintClient getClientEntity(Long clientId, Long tenantId);

    Page<PrintClientVO> listClients(Integer page, Integer size, String status, Long tenantId);

    void deleteClient(Long clientId, Long tenantId);

    String resetAuthKey(Long clientId, Long tenantId);

    void updateStatus(Long clientId, String status, Long tenantId);

    void processHeartbeat(Long clientId, String authKey, String clientVersion,
                          String defaultPrinter, String clientIp);

    /**
     * 根据机器标识查找客户端
     */
    SysPrintClient findByMachineId(String machineId, Long tenantId);

    /**
     * 注册客户端（带机器标识）
     */
    SysPrintClient registerClient(PrintClientRegisterRequest request, Long tenantId, Long userId, String machineId);

    /**
     * 客户端 API 鉴权：验证 auth_key 是否匹配
     */
    SysPrintClient authenticate(Long clientId, String authKey);
}

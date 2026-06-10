package cn.aiedge.erp.printing.service.impl;

import cn.aiedge.erp.printing.dto.v2.PrintClientRegisterRequest;
import cn.aiedge.erp.printing.dto.v2.PrintClientVO;
import cn.aiedge.erp.printing.entity.v2.SysPrintClient;
import cn.aiedge.erp.printing.mapper.SysPrintClientMapper;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.printing.config.PrintingRabbitConfig;
import cn.aiedge.erp.printing.service.PrintClientService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrintClientServiceImpl implements PrintClientService {

    private final SysPrintClientMapper clientMapper;
    private final AmqpAdmin amqpAdmin;

    @Override
    @Transactional
    public PrintClientVO register(PrintClientRegisterRequest request, Long tenantId, Long userId) {
        // 校验重名
        LambdaQueryWrapper<SysPrintClient> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(SysPrintClient::getTenantId, tenantId)
                .eq(SysPrintClient::getClientName, request.getClientName());
        if (clientMapper.selectCount(nameCheck) > 0) {
            throw BusinessException.badRequest("客户端名称已存在: " + request.getClientName());
        }

        SysPrintClient client = new SysPrintClient();
        client.setTenantId(tenantId);
        client.setClientName(request.getClientName());
        client.setClientCode(generateClientCode());
        client.setAuthKey(generateAuthKey());
        client.setStatus("OFFLINE");
        client.setClientVersion(request.getClientVersion());
        client.setCreatedBy(userId);
        clientMapper.insert(client);

        // 注册时自动声明客户端消息队列
        declareClientQueue(client.getClientId());

        return toVO(client);
    }

    @Override
    @Transactional
    public PrintClientVO updateClient(Long clientId, PrintClientRegisterRequest request, Long tenantId) {
        SysPrintClient client = clientMapper.selectById(clientId);
        if (client == null || !client.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("客户端不存在");
        }

        // 校验重名（排除自身）
        LambdaQueryWrapper<SysPrintClient> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(SysPrintClient::getTenantId, tenantId)
                .eq(SysPrintClient::getClientName, request.getClientName())
                .ne(SysPrintClient::getClientId, clientId);
        if (clientMapper.selectCount(nameCheck) > 0) {
            throw BusinessException.badRequest("客户端名称已存在: " + request.getClientName());
        }

        client.setClientName(request.getClientName());
        client.setClientVersion(request.getClientVersion());
        clientMapper.updateById(client);

        return toVO(clientMapper.selectById(clientId));
    }

    @Override
    public PrintClientVO getClient(Long clientId, Long tenantId) {
        SysPrintClient client = clientMapper.selectById(clientId);
        if (client == null || !client.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("客户端不存在");
        }
        return toVO(client);
    }

    @Override
    public SysPrintClient getClientEntity(Long clientId, Long tenantId) {
        SysPrintClient client = clientMapper.selectById(clientId);
        if (client == null || !client.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("客户端不存在");
        }
        return client;
    }

    @Override
    public Page<PrintClientVO> listClients(Integer page, Integer size, String status, Long tenantId) {
        Page<SysPrintClient> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SysPrintClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPrintClient::getTenantId, tenantId);
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(SysPrintClient::getStatus, status);
        }
        wrapper.orderByDesc(SysPrintClient::getCreatedAt);

        Page<SysPrintClient> clientPage = clientMapper.selectPage(pageObj, wrapper);
        Page<PrintClientVO> voPage = new Page<>(clientPage.getCurrent(), clientPage.getSize(), clientPage.getTotal());
        voPage.setRecords(clientPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public void deleteClient(Long clientId, Long tenantId) {
        SysPrintClient client = clientMapper.selectById(clientId);
        if (client == null || !client.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("客户端不存在");
        }
        clientMapper.deleteById(clientId);
    }

    @Override
    @Transactional
    public String resetAuthKey(Long clientId, Long tenantId) {
        SysPrintClient client = clientMapper.selectById(clientId);
        if (client == null || !client.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("客户端不存在");
        }
        String newKey = generateAuthKey();
        client.setAuthKey(newKey);
        clientMapper.updateById(client);
        return newKey;
    }

    @Override
    @Transactional
    public void updateStatus(Long clientId, String status, Long tenantId) {
        SysPrintClient client = clientMapper.selectById(clientId);
        if (client == null || !client.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("客户端不存在");
        }
        client.setStatus(status);
        if ("ONLINE".equals(status)) {
            client.setLastHeartbeat(LocalDateTime.now());
        }
        clientMapper.updateById(client);
    }

    @Override
    public SysPrintClient findByMachineId(String machineId, Long tenantId) {
        if (StrUtil.isBlank(machineId)) return null;
        return clientMapper.selectByMachineId(machineId, tenantId);
    }

    @Override
    @Transactional
    public SysPrintClient registerClient(PrintClientRegisterRequest request, Long tenantId, Long userId, String machineId) {
        // 校验重名
        LambdaQueryWrapper<SysPrintClient> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(SysPrintClient::getTenantId, tenantId)
                .eq(SysPrintClient::getClientName, request.getClientName());
        if (clientMapper.selectCount(nameCheck) > 0) {
            throw BusinessException.badRequest("客户端名称已存在: " + request.getClientName());
        }

        SysPrintClient client = new SysPrintClient();
        client.setTenantId(tenantId);
        client.setClientName(request.getClientName());
        client.setClientCode(generateClientCode());
        client.setAuthKey(generateAuthKey());
        client.setStatus("OFFLINE");
        client.setClientVersion(request.getClientVersion());
        client.setMachineId(StrUtil.isNotBlank(machineId) ? machineId : null);
        client.setCreatedBy(userId);
        clientMapper.insert(client);

        // 注册时自动声明客户端消息队列
        declareClientQueue(client.getClientId());

        return client;
    }

    @Override
    @Transactional
    public void processHeartbeat(Long clientId, String authKey, String clientVersion,
                                  String defaultPrinter, String clientIp) {
        SysPrintClient client = clientMapper.selectById(clientId);
        if (client == null) {
            throw BusinessException.notFound("客户端不存在");
        }
        if (!client.getAuthKey().equals(authKey)) {
            throw BusinessException.unauthorized("认证密钥无效");
        }

        String prevStatus = client.getStatus();
        client.setStatus("ONLINE");
        client.setLastHeartbeat(LocalDateTime.now());
        if (StrUtil.isNotBlank(clientVersion)) {
            client.setClientVersion(clientVersion);
        }
        if (StrUtil.isNotBlank(defaultPrinter)) {
            client.setDefaultPrinter(defaultPrinter);
        }
        if (StrUtil.isNotBlank(clientIp)) {
            client.setClientIp(clientIp);
        }
        clientMapper.updateById(client);

        // 客户端上线时声明队列（防止之前未注册）
        if (!"ONLINE".equals(prevStatus)) {
            declareClientQueue(clientId);
        }
    }

    @Override
    public SysPrintClient authenticate(Long clientId, String authKey) {
        SysPrintClient client = clientMapper.selectById(clientId);
        if (client == null || !client.getAuthKey().equals(authKey)) {
            throw BusinessException.unauthorized("客户端认证失败");
        }
        return client;
    }

    /**
     * 声明客户端专用消息队列
     * 确保 RabbitMQ 中存在该客户端的队列和绑定
     */
    private void declareClientQueue(Long clientId) {
        try {
            PrintingRabbitConfig.declareClientQueue(amqpAdmin, clientId);
            log.info("已声明客户端队列: clientId={}", clientId);
        } catch (Exception e) {
            log.warn("声明客户端队列失败: clientId={}", clientId, e);
        }
    }

    private String generateClientCode() {
        return "PC" + IdUtil.fastSimpleUUID().substring(0, 12).toUpperCase();
    }

    private String generateAuthKey() {
        return DigestUtil.md5Hex(IdUtil.fastUUID() + System.nanoTime()).substring(8, 40);
    }

    private PrintClientVO toVO(SysPrintClient client) {
        PrintClientVO vo = new PrintClientVO();
        vo.setClientId(client.getClientId());
        vo.setTenantId(client.getTenantId());
        vo.setClientName(client.getClientName());
        vo.setClientCode(client.getClientCode());
        vo.setAuthKey(client.getAuthKey());
        vo.setStatus(client.getStatus());
        vo.setLastHeartbeat(client.getLastHeartbeat());
        vo.setClientIp(client.getClientIp());
        vo.setClientVersion(client.getClientVersion());
        vo.setDefaultPrinter(client.getDefaultPrinter());
        vo.setCreatedAt(client.getCreatedAt());
        return vo;
    }
}

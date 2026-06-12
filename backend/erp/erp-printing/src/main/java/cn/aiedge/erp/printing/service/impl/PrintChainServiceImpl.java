package cn.aiedge.erp.printing.service.impl;

import cn.aiedge.erp.printing.dto.v2.*;
import cn.aiedge.erp.printing.entity.v2.*;
import cn.aiedge.erp.printing.mapper.*;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.printing.service.PrintChainService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrintChainServiceImpl implements PrintChainService {

    private final SysPrintChainMapper chainMapper;
    private final SysPrintChainItemMapper chainItemMapper;
    private final SysPrintTemplateMapper templateMapper;
    private final SysPrintClientMapper clientMapper;

    @Override
    @Transactional
    public PrintChainVO createChain(PrintChainCreateRequest request, Long tenantId, Long userId) {
        // 校验重名
        LambdaQueryWrapper<SysPrintChain> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(SysPrintChain::getTenantId, tenantId)
                .eq(SysPrintChain::getChainName, request.getChainName());
        if (chainMapper.selectCount(nameCheck) > 0) {
            throw BusinessException.badRequest("链路名称已存在: " + request.getChainName());
        }

        // 创建链路
        SysPrintChain chain = new SysPrintChain();
        chain.setTenantId(tenantId);
        chain.setPageCode(request.getPageCode());
        chain.setChainName(request.getChainName());
        chain.setDescription(request.getDescription());
        chain.setStatus("ACTIVE");
        chain.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        chain.setCreatedBy(userId);
        chainMapper.insert(chain);

        // 批量创建明细
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            saveItems(chain.getChainId(), request.getItems(), tenantId, userId);
        }

        return getChainDetail(chain.getChainId(), tenantId);
    }

    @Override
    @Transactional
    public PrintChainVO updateChain(Long chainId, PrintChainCreateRequest request, Long tenantId, Long userId) {
        SysPrintChain chain = chainMapper.selectById(chainId);
        if (chain == null || !chain.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("链路不存在");
        }

        // 校验重名（排除自身）
        LambdaQueryWrapper<SysPrintChain> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(SysPrintChain::getTenantId, tenantId)
                .eq(SysPrintChain::getChainName, request.getChainName())
                .ne(SysPrintChain::getChainId, chainId);
        if (chainMapper.selectCount(nameCheck) > 0) {
            throw BusinessException.badRequest("链路名称已存在: " + request.getChainName());
        }

        chain.setPageCode(request.getPageCode());
        chain.setChainName(request.getChainName());
        chain.setDescription(request.getDescription());
        if (request.getSortOrder() != null) {
            chain.setSortOrder(request.getSortOrder());
        }
        chainMapper.updateById(chain);

        // 全量替换明细项（删旧插新）
        LambdaQueryWrapper<SysPrintChainItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysPrintChainItem::getChainId, chainId);
        chainItemMapper.delete(deleteWrapper);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            saveItems(chainId, request.getItems(), tenantId, userId);
        }

        return getChainDetail(chainId, tenantId);
    }

    private void saveItems(Long chainId, List<ChainItemRequest> items, Long tenantId, Long userId) {
        for (ChainItemRequest item : items) {
            // 校验模板和客户端属于同一租户
            SysPrintTemplate template = templateMapper.selectById(item.getTemplateId());
            if (template == null || !template.getTenantId().equals(tenantId)) {
                throw BusinessException.notFound("模板不存在或不属于当前租户: " + item.getTemplateId());
            }
            SysPrintClient client = clientMapper.selectById(item.getClientId());
            if (client == null || !client.getTenantId().equals(tenantId)) {
                throw BusinessException.notFound("客户端不存在或不属于当前租户: " + item.getClientId());
            }

            SysPrintChainItem chainItem = new SysPrintChainItem();
            chainItem.setChainId(chainId);
            chainItem.setStepOrder(item.getStepOrder());
            chainItem.setTemplateId(item.getTemplateId());
            chainItem.setClientId(item.getClientId());
            chainItem.setPrinterName(item.getPrinterName());
            chainItem.setScreenshotMode(item.getScreenshotMode() != null ? item.getScreenshotMode() : "DISABLED");
            chainItem.setScreenshotConfirmTimeout(item.getScreenshotConfirmTimeout() != null ? item.getScreenshotConfirmTimeout() : 300);
            chainItem.setScreenshotConfigJson(item.getScreenshotConfigJson());
            chainItem.setCreatedBy(userId);
            chainItemMapper.insert(chainItem);
        }
    }

    @Override
    public PrintChainVO getChainDetail(Long chainId, Long tenantId) {
        SysPrintChain chain = chainMapper.selectById(chainId);
        if (chain == null || !chain.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("链路不存在");
        }

        PrintChainVO vo = toVO(chain);

        // 查询明细（按 step_order 排序）
        List<SysPrintChainItem> items = chainItemMapper.selectByChainIdOrdered(chainId);
        List<ChainItemVO> itemVOs = items.stream().map(this::toItemVO).collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }

    @Override
    public SysPrintChain getChainById(Long chainId, Long tenantId) {
        SysPrintChain chain = chainMapper.selectById(chainId);
        if (chain == null || !chain.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("链路不存在");
        }
        return chain;
    }

    @Override
    public Page<PrintChainVO> listChains(Integer page, Integer size, String pageCode, Long tenantId) {
        Page<SysPrintChain> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SysPrintChain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPrintChain::getTenantId, tenantId);
        if (StrUtil.isNotBlank(pageCode)) {
            wrapper.eq(SysPrintChain::getPageCode, pageCode);
        }
        wrapper.orderByAsc(SysPrintChain::getSortOrder)
                .orderByDesc(SysPrintChain::getCreatedAt);

        Page<SysPrintChain> chainPage = chainMapper.selectPage(pageObj, wrapper);

        // 转换为 VO（不含明细）
        Page<PrintChainVO> voPage = new Page<>(chainPage.getCurrent(), chainPage.getSize(), chainPage.getTotal());
        voPage.setRecords(chainPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional
    public void deleteChain(Long chainId, Long tenantId) {
        SysPrintChain chain = chainMapper.selectById(chainId);
        if (chain == null || !chain.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("链路不存在");
        }
        // 级联删除明细
        LambdaQueryWrapper<SysPrintChainItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(SysPrintChainItem::getChainId, chainId);
        chainItemMapper.delete(itemWrapper);
        chainMapper.deleteById(chainId);
    }

    @Override
    @Transactional
    public void updateChainStatus(Long chainId, String status, Long tenantId) {
        SysPrintChain chain = chainMapper.selectById(chainId);
        if (chain == null || !chain.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("链路不存在");
        }
        chain.setStatus(status);
        chainMapper.updateById(chain);
    }

    @Override
    public List<PrintChainVO> listByPageCode(String pageCode, Long tenantId) {
        LambdaQueryWrapper<SysPrintChain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPrintChain::getTenantId, tenantId)
                .eq(SysPrintChain::getPageCode, pageCode)
                .eq(SysPrintChain::getStatus, "ACTIVE")
                .orderByAsc(SysPrintChain::getSortOrder);
        return chainMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private PrintChainVO toVO(SysPrintChain chain) {
        PrintChainVO vo = new PrintChainVO();
        vo.setChainId(chain.getChainId());
        vo.setTenantId(chain.getTenantId());
        vo.setPageCode(chain.getPageCode());
        vo.setChainName(chain.getChainName());
        vo.setDescription(chain.getDescription());
        vo.setStatus(chain.getStatus());
        vo.setSortOrder(chain.getSortOrder());
        vo.setCreatedAt(chain.getCreatedAt());
        vo.setUpdatedAt(chain.getUpdatedAt());
        return vo;
    }

    private ChainItemVO toItemVO(SysPrintChainItem item) {
        ChainItemVO vo = new ChainItemVO();
        vo.setItemId(item.getItemId());
        vo.setChainId(item.getChainId());
        vo.setStepOrder(item.getStepOrder());
        vo.setTemplateId(item.getTemplateId());
        vo.setClientId(item.getClientId());
        vo.setPrinterName(item.getPrinterName());
        vo.setScreenshotMode(item.getScreenshotMode());
        vo.setScreenshotConfirmTimeout(item.getScreenshotConfirmTimeout());
        vo.setScreenshotConfigJson(item.getScreenshotConfigJson());
        vo.setCreatedAt(item.getCreatedAt());

        // 填充关联名称
        SysPrintTemplate template = templateMapper.selectById(item.getTemplateId());
        if (template != null) {
            vo.setTemplateName(template.getTemplateName());
            vo.setPageCode(template.getPageCode());
        }
        SysPrintClient client = clientMapper.selectById(item.getClientId());
        if (client != null) {
            vo.setClientName(client.getClientName());
        }

        return vo;
    }
}

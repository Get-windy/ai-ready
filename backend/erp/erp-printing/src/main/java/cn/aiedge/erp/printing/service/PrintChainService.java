package cn.aiedge.erp.printing.service;

import cn.aiedge.erp.printing.dto.v2.PrintChainCreateRequest;
import cn.aiedge.erp.printing.dto.v2.PrintChainVO;
import cn.aiedge.erp.printing.entity.v2.SysPrintChain;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface PrintChainService {

    PrintChainVO createChain(PrintChainCreateRequest request, Long tenantId, Long userId);

    PrintChainVO updateChain(Long chainId, PrintChainCreateRequest request, Long tenantId, Long userId);

    PrintChainVO getChainDetail(Long chainId, Long tenantId);

    SysPrintChain getChainById(Long chainId, Long tenantId);

    Page<PrintChainVO> listChains(Integer page, Integer size, String pageCode, Long tenantId);

    void deleteChain(Long chainId, Long tenantId);

    void updateChainStatus(Long chainId, String status, Long tenantId);

    List<PrintChainVO> listByPageCode(String pageCode, Long tenantId);
}

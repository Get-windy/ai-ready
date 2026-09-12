package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.PaymentChannelDTO;
import cn.aiedge.erp.finance.dto.PaymentChannelQuery;
import cn.aiedge.erp.finance.dto.PaymentChannelVO;
import cn.aiedge.erp.finance.model.entity.PaymentChannel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 支付渠道Service接口（资料 → 支付管理 → 支付渠道）
 */
public interface PaymentChannelService extends IService<PaymentChannel> {

    /** 多条件分页查询 */
    Page<PaymentChannelVO> page(PaymentChannelQuery query);

    /** 条件列表（导出复用；status 不传=全部） */
    List<PaymentChannelVO> list(PaymentChannelQuery query);

    /** 详情（含支付方式回填） */
    PaymentChannelVO detail(Long id);

    /** 新增 */
    PaymentChannelVO create(PaymentChannelDTO dto);

    /** 修改（渠道编码不可变更） */
    PaymentChannelVO update(Long id, PaymentChannelDTO dto);

    /** 删除 */
    void delete(Long id);

    /** 启用/停用（status 仅 0/1） */
    PaymentChannelVO updateStatus(Long id, Integer status);

    /** Excel 导入（真实落库） */
    Map<String, Object> importExcel(MultipartFile file);
}

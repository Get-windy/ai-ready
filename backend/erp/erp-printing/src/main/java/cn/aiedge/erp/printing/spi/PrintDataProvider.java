package cn.aiedge.erp.printing.spi;

import java.util.Map;

/**
 * 打印数据装配器（SPI）。
 *
 * 每个「可打印单据」实现一个，由 Spring 收进 {@link PrintDataProviderRegistry}。
 * 打印时后端按 pageCode 找到它，自己从库里把单据头 + 明细组装成**标准打印数据包**。
 *
 * 为什么要有这一层：
 * 早先由页面把整张实体（SaleOrder / SaleOutbound …）交给前端渲染接口，
 * 各页字段名不同 ⇒ 每个页面的模板都得重配一遍 field；
 * 页面想打印也必须自己懂「怎么挑模板、怎么调渲染」。装配器把这两件事都收到后端，
 * 模板面对的永远是同一套标准字段，新页面接入 = 写一个装配器 + 建一个模板，前端零改动。
 *
 * 数据包契约（模板里 field 引用的就是这些键）：
 * <ul>
 *   <li>单据头：平铺键值，如 {@code orderNo / orderDate / customerName / billAmount}</li>
 *   <li>明细：固定键 {@code items}，值为 {@code List<Map>}，
 *       每行如 {@code lineNo / productName / specification / unit / quantity / unitPrice / lineAmount}</li>
 *   <li>合计：单行能算的放明细行字段；需跨行汇总的放顶层（如 {@code totalQuantity}）</li>
 * </ul>
 */
public interface PrintDataProvider {

    /**
     * 页面编码。必须与 {@code sys_print_template.page_code}、
     * 前端 {@code <PrintDialog page-code="…">} 三处完全一致。
     */
    String pageCode();

    /**
     * 装配打印数据。
     *
     * @param documentId 单据主键
     * @return 标准打印数据包；单据不存在时返回 {@code null}，由调用方抛业务异常
     */
    Map<String, Object> load(Long documentId);

    /** 单据编号，用于打印留痕与预览标题；取不到返回 null */
    default String documentNo(Long documentId) {
        return null;
    }
}

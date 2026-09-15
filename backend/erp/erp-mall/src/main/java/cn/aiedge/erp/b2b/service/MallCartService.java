package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.b2b.dto.CartAddRequest;
import cn.aiedge.erp.b2b.dto.CartDTO;

import java.util.List;

public interface MallCartService {

    List<CartDTO> getCart();

    /**
     * 管理端分页查询购物车（真实分页 SQL，非当前页口径）
     *
     * @param customerId     会员ID（精确，可空）
     * @param memberKeyword  会员关键字（店铺用户名/昵称/手机号/公司名，模糊，可空）
     * @param productKeyword 商品关键字（商品名称/商品编码，模糊，可空）
     */
    PageResult<CartDTO> pageCart(Integer pageNum, Integer pageSize, Long customerId,
                                 String memberKeyword, String productKeyword);

    CartDTO addToCart(CartAddRequest request);

    CartDTO updateCartItem(Long id, CartAddRequest request);

    void removeFromCart(Long id);

    /**
     * 批量删除购物车项（逻辑删除）
     *
     * @return 实际删除条数
     */
    int removeBatch(List<Long> ids);

    void clearCart();

    void checkStock();
}

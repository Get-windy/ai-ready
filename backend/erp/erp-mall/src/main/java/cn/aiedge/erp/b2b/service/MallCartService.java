package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.CartAddRequest;
import cn.aiedge.erp.b2b.dto.CartDTO;

import java.util.List;

public interface MallCartService {

    List<CartDTO> getCart();

    CartDTO addToCart(CartAddRequest request);

    CartDTO updateCartItem(Long id, CartAddRequest request);

    void removeFromCart(Long id);

    void clearCart();

    void checkStock();
}

package com.atguigu.gmall0416.service;

import com.atguigu.gmall0416.bean.CartInfo;

import java.util.List;

public interface CartService {
    // 方法：一个是返回值，一个是参数 addToCart(CartInfo cartInfo );
    void  addToCart(String skuId,String userId,Integer skuNum);

    List<CartInfo> getCartList(String userId);
    // skuId:redis---field， userId：redis：key。
    //   void  addToCart(String skuId, String userId, CartInfo cartInfo);
    List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId);
    //  商品选中状态
    void checkCart(String skuId, String isChecked, String userId);
    // 获取redis 中选中的商品列表
    List<CartInfo> getCartCheckedList(String userId);
}

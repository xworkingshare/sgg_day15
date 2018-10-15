package com.atguigu.gmall0416.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0416.bean.CartInfo;
import com.atguigu.gmall0416.bean.SkuInfo;
import com.atguigu.gmall0416.config.CookieUtil;
import com.atguigu.gmall0416.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

// 操作cookie 中的购物车数据 业务逻辑跟 redis 中业务的
@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;

    @Reference
    private ManageService manageService;
    // 未登录添加
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, Integer skuNum){
        // 都需要判断当前购物车是否存
        String cartJson  = CookieUtil.getCookieValue(request, cookieCartName, true);
        boolean ifExist=false;
        List<CartInfo> cartInfoList  = new ArrayList<>();
        if (cartJson!=null && cartJson.length()>0){

           // 将字符串转换对象
          cartInfoList  = JSON.parseArray(cartJson, CartInfo.class);
           // 对该集合进行循环遍历
           for (CartInfo cartInfo : cartInfoList) {
               if (cartInfo.getSkuId().equals(skuId)){
                    // 数量加+skuNum
                   cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                   // 从新放入cookie！
                   ifExist=true;
               }
           }
       }
       if (!ifExist){
           //把商品信息取出来，新增到购物车
           SkuInfo skuInfo = manageService.getSkuInfo(skuId);
           CartInfo cartInfo=new CartInfo();

           cartInfo.setSkuId(skuId);
           cartInfo.setCartPrice(skuInfo.getPrice());
           cartInfo.setSkuPrice(skuInfo.getPrice());
           cartInfo.setSkuName(skuInfo.getSkuName());
           cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

           cartInfo.setUserId(userId);
           cartInfo.setSkuNum(skuNum);

           cartInfoList.add(cartInfo);

       }
        // 将集合数据从新放入cookie 中
        CookieUtil.setCookie(request,response,cookieCartName,JSON.toJSONString(cartInfoList),COOKIE_CART_MAXAGE,true);
    }

    public List<CartInfo> getCartList(HttpServletRequest request) {
        // 直接从cookie 取值
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        if (cookieValue!=null && cookieValue.length()>0){
            // cookieValue 转换成集合对象
            List<CartInfo> cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            return cartInfoList;
        }
        return null;
    }

    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }
    // 选中状态更新
    public  void  checkCart(HttpServletRequest request,HttpServletResponse response,String skuId,String isChecked){
        // 取得cookie 中所有的数据
        List<CartInfo> cartList = getCartList(request);
        if (cartList!=null && cartList.size()>0){
            for (CartInfo cartInfo : cartList) {
                // 页面传递过来的商品Id 跟cookie 中 的商品Id一致，则将商品的选中状态赋给cookie
                if (skuId.equals(cartInfo.getSkuId())){
                    cartInfo.setIsChecked(isChecked);
                }
            }
        }
        // 将集合重新存入cookie 中
        CookieUtil.setCookie(request,response,cookieCartName,JSON.toJSONString(cartList),COOKIE_CART_MAXAGE,true);

    }
}

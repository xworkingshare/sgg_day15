package com.atguigu.gmall0416.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0416.bean.CartInfo;
import com.atguigu.gmall0416.bean.SkuInfo;
import com.atguigu.gmall0416.cart.constant.CartConst;
import com.atguigu.gmall0416.cart.mapper.CartInfoMapper;
import com.atguigu.gmall0416.config.RedisUtil;
import com.atguigu.gmall0416.service.CartService;
import com.atguigu.gmall0416.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        // 先判断购物车中是否有该商品 -- skuId 取得数据
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfo.setSkuId(skuId);
        // 查询一下： select * from cart_info where skuId = ? and userId = ?
        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfo);
        // 声明一个变量
        // CartInfo cartInfoRedis = null;
        // 如果有商品 数量+1
        if (cartInfoExist!=null){
            // 数量+skuNum
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            // 更新数据库
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
            // 修改缓存
            // cartInfoRedis = cartInfoExist;
        }else {
            // 没有商品，新增
            // CartInfo 中所有信息来自于SkuInfo --
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            // 放入数据库
            cartInfoMapper.insertSelective(cartInfo1);
            // 修改缓存
            cartInfoExist=cartInfo1;
        }
        // 放入缓存。。。
        Jedis jedis = redisUtil.getJedis();

        // 放入数据
        // 定义key：user:userId:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+ CartConst.USER_CART_KEY_SUFFIX;
        // hset(key,field,value)  key:user:userId:cart  field:skuId  value:cartInfo
        jedis.hset(userCartKey,skuId, JSON.toJSONString(cartInfoExist));

        // 过期时间：根据用户的过期时间来设置
        // redis 怎么获取key的过期时间 ttl(key);
        String userInfoKey = CartConst.USER_KEY_PREFIX+userId+ CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey,ttl.intValue());

    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        // 1.从redis 中取得数据
        Jedis jedis = redisUtil.getJedis();
        // key：user:userId:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+ CartConst.USER_CART_KEY_SUFFIX;
        //        String cartJson = jedis.hget(userCartKey,userId);
        //       hash : 取值方式：将hash中的所有值一次性全部出去哪个方法？
        List<String> cartJsons  = jedis.hvals(userCartKey);
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 判断遍历
        if (cartJsons!=null && cartJsons.size()>0){
            for (String cartJson : cartJsons) {
                // cartJson 对应的是每一个skuId 的值， 将cartJson 转换成我们的cartInfo对象
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            // 做一个排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    // String 分类常用 6 length(); equals(); indexOf(); lastIndexOf();trim(): subString(); compareTo()比较大小;
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else {
            // 缓存中没有数据 --- db：
            List<CartInfo> cartInfoListDB = loadCartCache(userId);
            return cartInfoListDB;
        }
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId) {
        // 获取redis 中的数据
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        //  cartListFromCookie 循环匹配 [skuId] 有同样的商品数量相加
        for (CartInfo cartInfoCK : cartListFromCookie) {
            boolean isMatch =false;
            for (CartInfo cartInfoDB : cartInfoList) {
                    if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())){
                        cartInfoDB.setSkuNum(cartInfoDB.getSkuNum()+cartInfoCK.getSkuNum());
                        // 更新数据库
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                        isMatch = true;
                    }
            }
            // 不相等的情况 当前数据库中没有cookie 的是商品
            if (!isMatch){
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        // 以上代码只操作数据库，将数据库变为最新的数据，仅此而已！
        // 以下代码是根据当前的用户从新查询最新数据并同步redis！
        List<CartInfo> infoList = loadCartCache(userId);
        // 有么有判读isisChecked ？
        for (CartInfo cartInfoDB : infoList) {
            // 循环cookie
            for (CartInfo infoCK : cartListFromCookie) {
                // 有相同的商品
                if (cartInfoDB.getSkuId().equals(infoCK.getSkuId())){
                    // 判断cookie 中的商品是否选中，如何有选中的，更新数据库。
                    if ("1".equals(infoCK.getIsChecked())){
                        // 从新给db 对象赋值为选中状态.
                        cartInfoDB.setIsChecked("1");
                        // 从新调用一下checkCart
                        checkCart(cartInfoDB.getSkuId(), infoCK.getIsChecked(), userId);
                    }
                }
            }
        }

        return infoList;
    }
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        // 改变之前所有购物车中商品的状态
        // 准备redis ，准备key
        Jedis jedis = redisUtil.getJedis();
        // user:userId:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 将redis 中的数据取出来 hset(key,field,value) hget(key,field);
        String cartJson  = jedis.hget(userCartKey, skuId);
        // 将redis 中的字符串转换成对象
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        if (cartInfo!=null){
            cartInfo.setIsChecked(isChecked);
        }
        // 将修改完成的对象从新给redis
        jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfo));


        // === 新创建一个key 用来存储被选中的商品
        // user:userId:checked
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        // 放入被选中的购物车商品
        if ("1".equals(isChecked)){
            // 选中则添加
            jedis.hset(userCheckedKey,skuId,JSON.toJSONString(cartInfo));
        }else {
            // 状态为0 ，没有选中，则删除
            jedis.hdel(userCheckedKey,skuId);
        }
        jedis.close();

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
         // 什么一个 List<CartInfo> 对象
        List<CartInfo> newCartList = new ArrayList<>();
        // redis
        Jedis jedis = redisUtil.getJedis();
        // key
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        // 取得redis 中的所有数据
        List<String> cartCheckedList  = jedis.hvals(userCheckedKey);
        // 循环遍历被选中的商品列表
        for (String cartJson : cartCheckedList) {
            // cartJson 是每一个商品，将其字符串转换为对象
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            newCartList.add(cartInfo);
        }
        // 返回新的集合
        return newCartList;
    }

    private List<CartInfo> loadCartCache(String userId) {
        //  根据userId 查询 cartInfo ，但是有可能会出现商品价格不一致的情况
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList==null || cartInfoList.size()==0){
            return null;
        }
        // 将其放入redis
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        // 迭代：hset(key,field,value);  field key:skuId value:单个cartInfo的字符串 : v
        Map<String,String> map = new HashMap<>(cartInfoList.size());
        for (CartInfo cartInfo : cartInfoList) {
            // 将其对象变为字符串
            String newCartInfo = JSON.toJSONString(cartInfo);
            map.put(cartInfo.getSkuId(),newCartInfo);
        }
        // hmset:表示一次存入多个值
        jedis.hmset(userCartKey,map);
        jedis.close();
        return  cartInfoList;
    }
}

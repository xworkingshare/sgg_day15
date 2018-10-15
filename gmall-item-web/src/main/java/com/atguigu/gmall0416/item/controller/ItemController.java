package com.atguigu.gmall0416.item.controller;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0416.bean.SkuInfo;
import com.atguigu.gmall0416.bean.SkuSaleAttrValue;
import com.atguigu.gmall0416.bean.SpuSaleAttr;
import com.atguigu.gmall0416.config.LoginRequire;
import com.atguigu.gmall0416.service.ListService;
import com.atguigu.gmall0416.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;
    @RequestMapping("{skuId}.html")
//    @LoginRequire(autoRedirect = true)
    public String index(@PathVariable(value = "skuId") String skuId, Model model, HttpServletRequest request){
        // 商品详细信息，是根据页面传递过来的商品Id 进行查找！动态，如何变成动态？
        // springMVC 讲的！
        System.out.println("skuId="+skuId);
        // 根据skuId 进行查找数据 ，数据库那张表？skuInfo,调用后台manageService
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        // 存入，给前台使用,skuInfo 中应该包含skuImage数据
        request.setAttribute("skuInfo",skuInfo);
        // 查询销售属性，以及销售属性值
        List<SpuSaleAttr> spuSaleAttrList = manageService.selectSpuSaleAttrListCheckBySku(skuInfo);
        // 根据skuId=skuInfo--spuId。因为spu下关联着销售属性，以及销售属性值。

        // 页面显示！
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);
        // 做拼接字符串的功能
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

//        # 108|110 30 108|110 32 109|111 31 == 在java代码中拼写。
//        String jsonKey = "";
//        HashMap map = new HashMap(); 5 0-4
//        map.put("108|110",30);
//        map.put("109|111",31);

        String jsonKey = "";
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
            // 取得集合中的数据
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);
            if (jsonKey.length()!=0){
                jsonKey+="|";
            }
            // jsonKey+=108 jsonKey+=108|110
            jsonKey+=skuSaleAttrValue.getSaleAttrValueId();
            // 什么时候将jsonKey 重置！什么时候结束拼接
            if ((i+1)==skuSaleAttrValueListBySpu.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId()) ){
                map.put(jsonKey,skuSaleAttrValue.getSkuId());
                jsonKey="";
            }
        }
        // 转换json字符串
        String valuesSkuJson = JSON.toJSONString(map);
        System.out.println("valuesSkuJson="+valuesSkuJson);
        request.setAttribute("valuesSkuJson",valuesSkuJson);

        // 调用热度排名
        listService.incrHotScore(skuId);
        return "item";
    }
}

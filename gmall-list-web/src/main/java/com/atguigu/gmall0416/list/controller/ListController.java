package com.atguigu.gmall0416.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0416.bean.BaseAttrInfo;
import com.atguigu.gmall0416.bean.BaseAttrValue;
import com.atguigu.gmall0416.bean.SkuLsParams;
import com.atguigu.gmall0416.bean.SkuLsResult;
import com.atguigu.gmall0416.service.ListService;
import com.atguigu.gmall0416.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;
    @RequestMapping("list.html")
    public String list(SkuLsParams skuLsParams, HttpServletRequest request, Model model){
        // 设置每页显示的条数
        skuLsParams.setPageSize(3);

        SkuLsResult skuLsResult = listService.search(skuLsParams);
//        skuLsParams.setPageSize(3); 失败！ 走的是默认值


        // 对象转换成字符串
         String s = JSON.toJSONString(skuLsResult);
        System.out.println(s);


        // 从es 中取得 平台属性值的Id 集合
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        // foreach 循环不能对集合同时又两个操作！ itco :迭代器

        // 用平台属性值的Id 进行查询 平台属性值的名称，查询平台属性名。  manageService.getAttrList(String catalog3Id);
        List<BaseAttrInfo> attrList =  manageService.getAttrList(attrValueIdList);

        // 制作url 连接
       String urlParam = makeUrlParam(skuLsParams);

       // 面包屑功能：声明一个集合
        List<BaseAttrValue> baseAttrValuesList = new ArrayList<>();

        // 过滤重复属性值 循环attrList --  skuLsParams.getValueId() 页面得到的valueId 比较结果如何相同，则将数据进行remove？
        // 集合-- 能否在遍历的过程中进行删除集合中的数据？
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            // 取得平台属性对象
            BaseAttrInfo baseAttrInfo = iterator.next();
            // 平台属性值 集合
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            // 循环比较
            for (BaseAttrValue baseAttrValue : attrValueList) {
                // 取得平台属性值对象
                baseAttrValue.setUrlParam(urlParam);
                // 从http://list.gmall.com/list.html?keyword=%E5%B0%8F%E7%B1%B3&valueId=13&valueId=80&valueId=80&valueId=13&valueId=80
                if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
                    // 取得url 中得到每一个平台属性值Id
                    for (String valueId : skuLsParams.getValueId()) {
                        // 如果平台属性值Id 跟 点击的平台属性值Id 相同则删除
                        if (valueId.equals(baseAttrValue.getId())){
                            iterator.remove();
                            // 取出属性名：属性值 将其添加到集合中，在页面进行循环显示！
                            BaseAttrValue baseAttrValueSelected = new BaseAttrValue();
                            // （属性名：属性值）看做一个整体赋给setValueName();
                            baseAttrValueSelected.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            // 做一个去重复
                            String makeUrlParam = makeUrlParam(skuLsParams, valueId);
                            // 面包屑中的URL赋值！
                            baseAttrValueSelected.setUrlParam(makeUrlParam);
                            baseAttrValuesList.add(baseAttrValueSelected);
                        }
                    }
                }
            }
        }

       // 将urlParam 保存到页面属性值的href 属性中！
        request.setAttribute("urlParam",urlParam);
        // 保存关键字keyword
        request.setAttribute("keyword",skuLsParams.getKeyword());

        // 将平台属性名集合存储，然后前台显示即可！
        request.setAttribute("attrList",attrList);

//        var totalPage = [[${totalPages}]];
//        var pageNo = [[${pageNo}]];

        request.setAttribute("totalPages",skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());
        // 存储面包屑集合
        request.setAttribute("baseAttrValuesList",baseAttrValuesList);
        // 将skuLsInfo 列表进行保存
        request.setAttribute("skuLsInfoList",skuLsResult.getSkuLsInfoList());
       /*返回试图的名称*/
       return "list";

    }
    // 制作url 的方法
    private String makeUrlParam(SkuLsParams skuLsParams,String ... excludeValueIds) {
//        http://list.gmall.com/list.html?keyword=小米&catalog3Id=61&valueId=13&pageNo=1&pageSize=10
//        参数传递 一个参数，多个参数：
//        一个参数：连接后面加？
//        多个参数：第一个是？第二个后续都是&连接。
        String urlParam = "";
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            urlParam+="keyword="+skuLsParams.getKeyword();
        }
        // 注意：&
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            if (urlParam.length()>0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        /*
        * 数组长度：.length;
        * 字符串长度：.length();
        * 集合：.size();
        * 文件：.length();
        * */
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环匹配
//            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
//                String valueId = skuLsParams.getValueId()[i];
//                if (excludeValueIds!=null && excludeValueIds.length>0){
//                    // 为啥是0 因为我们每次点击的时候，只能点击一个过滤属性
//                    String excludeValueId = excludeValueIds[0];
//                    if (valueId.equals(excludeValueId)){
//                        // break continue return；
//                        continue;
//                    }
//                }
//                if (urlParam.length()>0){
//                    urlParam+="&";
//                }
//                urlParam+="valueId="+valueId;
//            }
            // skuLsParams.getValueId();
            for (String valueId : skuLsParams.getValueId()) {
                if (excludeValueIds!=null && excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if (valueId.equals(excludeValueId)){
                        continue;
                    }
                }
                if (urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }
        return urlParam;
    }


}

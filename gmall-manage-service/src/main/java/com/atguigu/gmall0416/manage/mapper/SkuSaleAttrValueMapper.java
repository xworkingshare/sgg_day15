package com.atguigu.gmall0416.manage.mapper;

import com.atguigu.gmall0416.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    // 根据spuId查询销售属性值Id
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu (String spuId);
}

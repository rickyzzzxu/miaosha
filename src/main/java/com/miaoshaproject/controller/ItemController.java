package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobject.ItemVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.response.CommonRetrunType;
import com.miaoshaproject.service.CacheService;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.ItemModel;
import com.sun.tools.javac.jvm.CRTable;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller("/item")
@RequestMapping("/item")
@CrossOrigin(originPatterns = "*", allowCredentials = "true", allowedHeaders = "*")
public class ItemController extends BaseController{

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PromoService promoService;

    //创建商品的接口
    @RequestMapping(value = "/create",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonRetrunType createItem(@RequestParam(name = "title") String title,
                                       @RequestParam(name = "description") String description,
                                       @RequestParam(name = "price") BigDecimal price,
                                       @RequestParam(name = "imgUrl") String imgUrl,
                                       @RequestParam(name = "stock") Integer stock
                                       ) throws BusinessException {
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setPrice(price);
        itemModel.setDescription(description);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn  = itemService.createItem(itemModel);
        ItemVO itemVO = this.convertItemVOFromModel(itemModelForReturn);
        return CommonRetrunType.create(itemVO);
    }

    private ItemVO convertItemVOFromModel(ItemModel itemModel){
        if (itemModel == null){
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel,itemVO);
        if (itemModel.getPromoModel() != null){
            //说明有正在进行或即将进行的秒杀活动
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPrompItemPrice());
        }else {
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }

    @RequestMapping(value = "/publishpromo",method = {RequestMethod.GET})
    @ResponseBody
    public CommonRetrunType publishPromo(@RequestParam(name = "id") Integer id){
        promoService.publishPromo(id);
        return CommonRetrunType.create(null);
    }

    //商品详情页浏览
    @RequestMapping(value = "/get",method = {RequestMethod.GET})
    @ResponseBody
    public CommonRetrunType getItem(@RequestParam(name = "id") Integer id){
        ItemModel itemModel = null;

        //先从本地缓存中查找
        itemModel = (ItemModel) cacheService.getCommonCache("item_"+id);
        //若本地缓存中不存在，去redis中查找
        if (itemModel == null){
            //从redis中查找对应的itemModel
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_"+id);
            if (itemModel == null){
                //redis中不存在再去mysql中找
                itemModel = itemService.getItemById(id);
                //将数据放到redis中
                redisTemplate.opsForValue().set("item_"+id,itemModel);
                redisTemplate.expire("item_"+id,10, TimeUnit.MINUTES);
            }
            //将数据放入本地缓存中
            cacheService.setCommonCache("item_"+id,itemModel);
        }


        ItemVO itemVO = this.convertItemVOFromModel(itemModel);
        return CommonRetrunType.create(itemVO);
    }

    //商品列表页面浏览
    @RequestMapping(value = "/list",method = {RequestMethod.GET})
    @ResponseBody
    public CommonRetrunType listItem(){
        List<ItemModel> itemModels = itemService.listItem();
        //使用Stream api将List<ItemModel>转化为List<ItemVO>
        List<ItemVO> itemVOS = itemModels.stream().map(itemModel -> {
            ItemVO itemVO = this.convertItemVOFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonRetrunType.create(itemVOS);
    }
}

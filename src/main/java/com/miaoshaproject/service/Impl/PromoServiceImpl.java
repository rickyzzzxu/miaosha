package com.miaoshaproject.service.Impl;

import com.miaoshaproject.dao.PromoDOMapper;
import com.miaoshaproject.dataobject.PromoDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.PromoModel;
import com.miaoshaproject.service.model.UserModel;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PromoServiceImpl implements PromoService {
    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        PromoModel promoModel = this.convertPromoModelFromPromoDO(promoDO);
        if (promoModel == null){
            return null;
        }
        //判断当前时间秒杀活动是正在进行还是即将开始
        if (promoModel.getStartDate().isAfterNow()){//秒杀活动还未开始
            promoModel.setStatus(1);
        }else if (promoModel.getEndDate().isBeforeNow()){//秒杀活动已结束
            promoModel.setStatus(3);
        }else {//秒杀活动正在进行
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        //通过活动id获取活动
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO.getItemId() == null || promoDO.getItemId().intValue() == 0){
            return;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());
        //将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(),itemModel.getStock());
        //将秒杀大闸的数字限制到redis内
        //大闸的数字为当前秒杀商品库存的5倍
        redisTemplate.opsForValue().set("promo_door_count_"+promoId,itemModel.getStock().intValue() * 5);
    }

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        PromoModel promoModel = this.convertPromoModelFromPromoDO(promoDO);
        if (promoModel == null){
            return null;
        }
        //判断当前时间秒杀活动是正在进行还是即将开始
        if (promoModel.getStartDate().isAfterNow()){//秒杀活动还未开始
            promoModel.setStatus(1);
        }else if (promoModel.getEndDate().isBeforeNow()){//秒杀活动已结束
            promoModel.setStatus(3);
        }else {//秒杀活动正在进行
            promoModel.setStatus(2);
        }

        //只有status为2时，秒杀活动才开始，才允许获得令牌
        if (promoModel.getStatus().intValue() != 2){
            return null;
        }

        //判断item信息是否存在
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null){
            return null;
        }

        //判断user信息是否存在
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel == null){
            return null;
        }

        //获取秒杀大闸的count数量
        long result = redisTemplate.opsForValue().increment("promo_door_count_"+promoId, -1);
        if (result < 0){
            return null;
        }

        //生成token并存入redis内，设置5分钟有效期
        String token = UUID.randomUUID().toString().replace("-","");
        redisTemplate.opsForValue().set("promo_token_"+promoId+"_userId_"+userId+"_itemId_"+itemId,token);
        redisTemplate.expire("promo_token_"+promoId+"_userId_"+userId+"_itemId_"+itemId,5, TimeUnit.MINUTES);
        return token;
    }

    private PromoModel convertPromoModelFromPromoDO(PromoDO promoDO){
        if (promoDO == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        promoModel.setPrompItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}

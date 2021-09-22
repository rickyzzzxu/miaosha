package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.OrderModel;

public interface OrderService {
    OrderModel createModel(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException;

    String generateOrderId();
}

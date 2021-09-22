package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.UserModel;

public interface UserService {

    //通过id获取对象
    UserModel getUserById(Integer id);

    void regiser(UserModel userModel) throws BusinessException;

    //通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);

    /***
     *
     * @param telephone：用户注册手机
     * @param encrptPassword：用户加密后的密码
     * @throws BusinessException
     */
    UserModel vaildateLogin(String telephone, String encrptPassword) throws BusinessException;
}

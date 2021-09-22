package com.miaoshaproject.controller;

import com.alibaba.druid.util.StringUtils;
import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonRetrunType;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(originPatterns = "*", allowCredentials = "true", allowedHeaders = "*")
public class UserController extends BaseController{
    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private RedisTemplate redisTemplate;

    //用户登录接口
    @RequestMapping(value = "/login",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonRetrunType login(@RequestParam(name="telephone") String telephone,
                                  @RequestParam(name="password") String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        //入参校验，用户手机号和密码不能为空
        if (org.apache.commons.lang3.StringUtils.isEmpty(telephone) ||
                org.apache.commons.lang3.StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VAILDATION_ERROR);
        }

        //用户登录
        UserModel userModel = userService.vaildateLogin(telephone,this.EncodeByMd5(password));

        //将登录凭证加到用户登录成功的session内

        //修改成若用户登录成功，则将用户信息和登录凭证一起放入redis中

        //生成登录凭证token，UUID
        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-","");
        //建立token和用户登录态的联系
        redisTemplate.opsForValue().set(uuidToken,userModel);
        redisTemplate.expire(uuidToken,1, TimeUnit.HOURS);
//        this.request.getSession().setAttribute("IS_LOGIN",true);
//        this.request.getSession().setAttribute("LOGIN_USER",userModel);
        return CommonRetrunType.create(uuidToken);
    }

    //用户注册接口
    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonRetrunType register(@RequestParam(name="telephone") String telephone,
                                     @RequestParam(name="otpCode") String otpCode,
                                     @RequestParam(name="name") String name,
                                     @RequestParam(name="gender") Integer gender,
                                     @RequestParam(name="age") Integer age,
                                     @RequestParam(name="password") String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //验证手机号和对应的otpCode相符合
        String inSessionOtpCode = (String) request.getSession().getAttribute(telephone);
        //这个equals方法内部实现了判空操作
        if (!StringUtils.equals(inSessionOtpCode,otpCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VAILDATION_ERROR,"短信验证码不符合");
        }
        //用户注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setAge(age);
        userModel.setGender(gender);
        userModel.setTelephone(telephone);
        userModel.setRegisterMode("byphone");
        //对密码进行加密
        userModel.setEncrptPassword(this.EncodeByMd5(password));
        userService.regiser(userModel);
        return CommonRetrunType.create(null);
    }

    public String EncodeByMd5(String str) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();
        //加密字符串
        String newStr = base64Encoder.encode(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }

    //用户获取otp短信接口
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonRetrunType getOtp(@RequestParam(name="telephone") String telephone){
        //按照一定的规则生成otp验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);//生成[0,bound)区间内的int型随机数
        randomInt += 10000;//生成[0,bound)区间内的int型随机数
        String otpCode = String.valueOf(randomInt);
        //将otp验证码和用户手机号关联,使用httpsession的方式绑定用户手机号和otpCode
        request.getSession().setAttribute(telephone,otpCode);
        //将otp验证码通过短信发送给用户（省略）
        System.out.println("telephone:"+telephone+" otpCode:"+otpCode);

        return CommonRetrunType.create(null);
    }


    @RequestMapping("/get")
    @ResponseBody
    public CommonRetrunType gerUser(@RequestParam(name="id") Integer id) throws BusinessException {
        //调用service层的服务获取对应id的用户对象并返回给前端页面
        UserModel userModel = userService.getUserById(id);

        if (userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
        //将核心领域模型转换为可供UI使用的viewobjext
        UserVO userVO = convertFormModel(userModel);
        //返回通用对象
        return CommonRetrunType.create(userVO);
    }

    private UserVO convertFormModel(UserModel userModel){
        if (userModel == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }


}

package com.miaoshaproject.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.mq.MqProducer;
import com.miaoshaproject.response.CommonRetrunType;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

@Controller("order")
@RequestMapping("/order")
@CrossOrigin(originPatterns = "*", allowCredentials = "true", allowedHeaders = "*")
public class
OrderController extends BaseController{
    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(20);

        orderCreateRateLimiter.create(300);
    }

    //生成验证码
    @RequestMapping(value = "/generateverifycode",method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public void generateVerifyCode(HttpServletResponse response) throws BusinessException, IOException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录，不能生成验证码");
        }
        //获取用户登录信息
        UserModel userModel = (UserModel)redisTemplate.opsForValue().get(token);
        if (userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录，不能生成验证码");
        }
        Map<String,Object> map = CodeUtil.generateCodeAndPic();

        //将验证码与用户信息绑定到redis中
        redisTemplate.opsForValue().set("verify_code_"+userModel.getId(),map.get("code"));
        redisTemplate.expire("verify_code_"+userModel.getId(),10,TimeUnit.MINUTES);

        ImageIO.write((RenderedImage) map.get("codePic"), "jpeg", response.getOutputStream());



    }




    //生成秒杀令牌
    @RequestMapping(value = "/generatetoken",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonRetrunType generateYoken(@RequestParam(name = "itemId")Integer itemId,
                                        @RequestParam(name = "promoId")Integer promoId,
                                          @RequestParam(name = "verifyCode")String verifyCode) throws BusinessException {
        //根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //获取用户登录信息
        UserModel userModel = (UserModel)redisTemplate.opsForValue().get(token);
        if (userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //通过verifyCode验证验证码的有效性
        String verifyCodeInRedis = (String) redisTemplate.opsForValue().get("verify_code_"+userModel.getId());
        if (StringUtils.isEmpty(verifyCodeInRedis)){
            throw new BusinessException(EmBusinessError.PARAMETER_VAILDATION_ERROR,"请求非法");
        }
        if (!verifyCodeInRedis.equalsIgnoreCase(verifyCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VAILDATION_ERROR,"验证码错误");
        }


        //获取秒杀令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());
        if(promoToken == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VAILDATION_ERROR,"令牌生成失败");
        }

        return CommonRetrunType.create(promoToken);

    }

    //封装下单请求
    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonRetrunType createOrder(@RequestParam(name = "itemId")Integer itemId,
                                        @RequestParam(name = "amount")Integer amount,
                                        @RequestParam(name = "promoId",required = false)Integer promoId,
                                        @RequestParam(name = "promoToken",required = false)String promoToken) throws BusinessException {

        if (!orderCreateRateLimiter.tryAcquire()){
            throw new BusinessException(EmBusinessError.RATELIMIT);
        }

        //Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //获取用户登录信息
        UserModel userModel = (UserModel)redisTemplate.opsForValue().get(token);
        if (userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        //OrderModel orderModel = orderService.createModel(userModel.getId(),itemId,promoId,amount);

        //校验秒杀令牌
        if (promoId != null){
            String promoTokenInRedis = (String) redisTemplate.opsForValue().get("promo_token_"+promoId+"_userId_"+userModel.getId()+"_itemId_"+itemId);
            if (promoTokenInRedis == null){
                throw new BusinessException(EmBusinessError.PARAMETER_VAILDATION_ERROR,"秒杀令牌校验失败");
            }
            if (!StringUtils.equals(promoTokenInRedis,promoToken)){
                throw new BusinessException(EmBusinessError.PARAMETER_VAILDATION_ERROR,"秒杀令牌校验失败");
            }
        }

        //初始化流水之前，先判断商品是否售罄，若对应的售罄key存在，则直接抛出库存不足异常
        if (redisTemplate.hasKey("promo_item_stock_invaild_"+itemId)){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        //同步调用线程池的submit方法
        //拥塞窗口为20的等待队列，用于队列泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //初始化流水
                String stockLogId = itemService.initStockLog(itemId,amount);


                boolean orderResult = mqProducer.transactionAsyncReduceStock(userModel.getId(),itemId,promoId,amount,stockLogId);
                if (!orderResult){
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR,"下单失败");
                }
                return null;
            }
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }


        return CommonRetrunType.create(null);
    }
}

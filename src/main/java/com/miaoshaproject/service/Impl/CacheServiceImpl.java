package com.miaoshaproject.service.Impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.miaoshaproject.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;


@Service
public class CacheServiceImpl implements CacheService {

    private Cache<String,Object> commonCache = null;

    @PostConstruct
    public void init(){
        commonCache = CacheBuilder.newBuilder()
                //设置缓存容器的初始容量为10
                .initialCapacity(10)
                //设置缓存中最多放100个key，超过后会使用LRU策略移除缓存项
                .maximumSize(100)
                //设置写缓存60秒后过期
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }
    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key,value);
    }

    @Override
    public Object getCommonCache(String key) {
        //如果存在就返回value，否则返回null
        return commonCache.getIfPresent(key);
    }
}

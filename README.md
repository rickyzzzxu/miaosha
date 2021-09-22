# 高并发电商秒杀项目

#### 介绍
使用java做的高并发电商秒杀项目
技术栈：springboot，mybatis，redis，rocketmq等

#### 软件架构
springboot+mybatis做常规的电商功能，如登录，注册等\
redis用作缓存，缓存热点信息，如用户登录的token信息，商品的详情，库存信息等\
rocketmq异步更新数据库，保证数据与缓存的一致性\
数据库使用mysql




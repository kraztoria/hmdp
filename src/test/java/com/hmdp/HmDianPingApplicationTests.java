package com.hmdp;

import com.alibaba.fastjson.JSON;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.ArrayList;

@SpringBootTest
@RunWith(SpringRunner.class)
public class HmDianPingApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ShopMapper shopMapper;

    @Test
    public void testJson() {
        String jsonString = JSON.toJSONString(new ArrayList<String>());
        redisTemplate.opsForValue().set("test", jsonString);
        System.out.println(redisTemplate.opsForValue().get("test"));
    }

    @Test
    public void testLogicExpire() {
        Shop shop = shopMapper.selectById(1);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusMinutes(RedisConstants.CACHE_SHOP_TTL));

        redisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + 1, JSON.toJSONString(redisData));
    }
}

package com.hmdp;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;

@SpringBootTest
@RunWith(SpringRunner.class)
public class HmDianPingApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void testJson() {
        String jsonString = JSON.toJSONString(new ArrayList<String>());
        redisTemplate.opsForValue().set("test", jsonString);
        System.out.println(redisTemplate.opsForValue().get("test"));
    }
}

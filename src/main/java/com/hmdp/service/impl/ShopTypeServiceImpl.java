package com.hmdp.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    private final StringRedisTemplate redisTemplate;

    private final ShopTypeMapper shopTypeMapper;

    @Override
    public Result queryTypeList() {
        // 查询缓存
        String cacheKey = RedisConstants.CACHE_SHOP_TYPE_KEY + "list";
        String shopTypesJson = redisTemplate.opsForValue().get(cacheKey);

        List<ShopType> shopTypes;
        // 缓存中不存在
        if (StringUtils.isEmpty(shopTypesJson)) {
            log.info("[loadCache] 缓存中不存在店铺类型 {}", cacheKey);
            shopTypes = loadCache(cacheKey);
        } else {
            // 缓存中存在
            log.info("[loadCache] 店铺类型缓存命中 {}", cacheKey);

            // 反序列化数据
            shopTypes = JSON.parseArray(shopTypesJson, ShopType.class);

            // 更新缓存时间
            // redisTemplate.expire(cacheKey, RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        }

        return Result.ok(shopTypes);
    }

    /**
     * 从数据库中查询店铺类型并加载进缓存中
     *
     * @param cacheKey 缓存 key
     * @return 店铺类型列表 如果没有任何店铺类型则返回空链表
     */
    private List<ShopType> loadCache(String cacheKey) {
        log.info("[loadCache] 从数据库中查询店铺类型 {}", cacheKey);

        // 从数据库中查询数据
        List<ShopType> shopTypes = shopTypeMapper.selectList(new LambdaQueryWrapper<ShopType>().orderByAsc(ShopType::getSort));

        // 序列化数据
        String shopTypesJson = JSON.toJSONString(shopTypes);

        // 将数据加载进缓存中
        redisTemplate.opsForValue().set(cacheKey, shopTypesJson, RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        log.info("[loadCache] 成功将店铺类型加载进缓存中 {}", shopTypesJson);

        return shopTypes;
    }
}

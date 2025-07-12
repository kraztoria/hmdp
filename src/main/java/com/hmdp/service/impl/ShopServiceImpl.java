package com.hmdp.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final StringRedisTemplate redisTemplate;

    private final ShopMapper shopMapper;

    @Override
    public Result queryShopById(Long id) {
        return queryWithMutex(id);
    }

    // 缓存击穿 互斥锁版
    private Result queryWithMutex(Long id) {
        // 构建缓存 key
        String cacheKey = RedisConstants.CACHE_SHOP_KEY + id;

        // 查询缓存
        String shopJson = redisTemplate.opsForValue().get(cacheKey);

        // 判断是否命中缓存
        if (!StringUtils.isEmpty(shopJson)) {
            return Result.ok(JSON.parseObject(shopJson, Shop.class));
        }

        // 获取分布式锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean locked = tryLock(lockKey);

        try {
            // 没获取到
            if (!locked) {
                TimeUnit.MILLISECONDS.sleep(50);
                return queryWithMutex(id);
            }

            // 未命中缓存，查询数据库
            Shop shop = shopMapper.selectById(id);
            if (null == shop) {
                return Result.fail("店铺不存在");
            }

            // 加载缓存
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

            // 返回
            return Result.ok(shop);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);

        } finally {
            unlock(lockKey);
        }
    }

    // 缓存穿透
    private Result queryWithPassThrough(Long id) {
        // 构建缓存 key
        String cacheKey = RedisConstants.CACHE_SHOP_KEY + id;

        // 从 redis 中查询缓存
        String shopMessage = redisTemplate.opsForValue().get(cacheKey);
        Shop shop;

        if (StringUtils.isEmpty(shopMessage)) {
            // 判断是空缓存还是没有缓存
            if (shopMessage != null) {
                // 空缓存
                log.warn("[queryShopById] 命中空缓存 店铺id: {}", id);
                return Result.fail("店铺不存在");
            }

            // 缓存中没有对应店铺数据，查询数据库并加载缓存
            log.info("[queryShopById] 缓存中没有店铺数据 {}，查询数据库并加载缓存", id);
            shop = loadCache(id, cacheKey);
            if (null == shop) return Result.fail("店铺不存在");
        } else {
            // 缓存命中
            log.info("[queryShopById] 店铺 {} 缓存命中 {}", id, shopMessage);
            shop = JSON.parseObject(shopMessage, Shop.class);

            // 更新缓存时间
            // redisTemplate.expire(cacheKey, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            log.warn("[updateShop] 店铺id不能为空");
            return Result.fail("店铺id不能为空");
        }

        // 更新数据库信息
        shopMapper.updateById(shop);

        // 删除缓存
        redisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);

        log.info("[updateShop] 成功更新店铺 {} 信息并删除缓存", id);

        return Result.ok();
    }

    /**
     * 从数据库中查询店铺信息并加载进缓存中
     *
     * @param id       店铺id
     * @param cacheKey 缓存 key
     * @return 店铺数据 如果店铺不存在则返回 null
     */
    private Shop loadCache(Long id, String cacheKey) {
        log.info("[loadCache] 从数据库中将店铺 {} 加载进缓存", id);

        // 从数据库中查询对应数据
        Shop shop = shopMapper.selectById(id);

        // 不存在对应的店铺
        if (null == shop) {
            log.warn("[loadCache] 店铺 {} 不存在", id);

            // 将空数据写入缓存中 避免缓存穿透
            redisTemplate.opsForValue().set(cacheKey, SystemConstants.EMPTY_STRING, RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);

            return null;
        }

        // 序列化店铺数据
        String shopMessage = JSON.toJSONString(shop);

        // 将数据加载进缓存中
        redisTemplate.opsForValue().set(cacheKey, shopMessage, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        log.info("[loadCache] 成功将店铺 {} 信息加载进缓存中 {}", id, shopMessage);

        // 返回店铺数据
        return shop;
    }

    private boolean tryLock(String lockKey) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, SystemConstants.EMPTY_STRING, RedisConstants.LOCK_SHOP_TTL, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(locked);
    }

    private void unlock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
}

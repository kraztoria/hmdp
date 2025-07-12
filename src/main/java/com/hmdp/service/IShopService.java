package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据id查询店铺信息
     *
     * @param id 店铺id
     * @return 店铺信息
     */
    Result queryShopById(Long id);

    /**
     * 更新店铺信息
     *
     * @param shop 店铺信息
     * @return 结果
     */
    Result updateShop(Shop shop);
}

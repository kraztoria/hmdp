package com.hmdp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自定义线程池配置属性
 *
 * @author Kraztoria rotroutine@163.com
 * @since 2025/7/12 19:08
 */
@Data
@ConfigurationProperties(prefix = "hmdp.thread-pool")
public class ThreadPoolConfigProperties {

    private int corePoolSize = 8;          // 核心线程数（默认值）

    private int maxPoolSize = 16;          // 最大线程数（默认值）

    private int queueCapacity = 32;       // 队列容量（默认值）

    private int keepAliveSeconds = 60;     // 空闲线程存活时间（默认值）单位：秒

}

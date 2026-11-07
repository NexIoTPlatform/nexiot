/// *
// *
// * Copyright (c) 2025, NexIoT. All Rights Reserved.
// *
// * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
// * @Author: gitee.com/NexIoT
// * @Email: wo8335224@gmail.com
// * @Wechat: outlookFil
// *
// *
// */
//
// package cn.universal.web.config;
//
// import io.netty.channel.EventLoopGroup;
// import io.netty.channel.nio.NioEventLoopGroup;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.reactive.config.EnableWebFlux;
// import org.springframework.web.reactive.config.WebFluxConfigurer;
// import reactor.core.scheduler.Scheduler;
// import reactor.core.scheduler.Schedulers;
//
/// **
// * 响应式编程配置 - Spring Boot 3.5 新特性
// *
// * 响应式编程优势：
// * 1. 非阻塞I/O：提高并发处理能力
// * 2. 背压处理：自动处理流量控制
// * 3. 资源效率：减少线程占用
// * 4. 适合IoT：大量设备连接和消息处理
// */
// @Slf4j
// @Configuration
// @EnableWebFlux
// public class ReactiveConfig implements WebFluxConfigurer {
//
//    /**
//     * 虚拟线程调度器 - 用于响应式流处理
//     */
//    @Bean("virtualThreadScheduler")
//    public Scheduler virtualThreadScheduler() {
//        return Schedulers.fromExecutorService(
//            java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()
//        );
//    }
//
//    /**
//     * 事件循环组 - 用于Netty服务器
//     */
//    @Bean("eventLoopGroup")
//    public EventLoopGroup eventLoopGroup() {
//        // 使用虚拟线程优化事件循环
//        return new NioEventLoopGroup(
//            Runtime.getRuntime().availableProcessors(),
//            r -> {
//                Thread t = Thread.ofVirtual()
//                    .name("netty-eventloop-", 0)
//                    .unstarted(r);
//                t.setDaemon(true);
//                return t;
//            }
//        );
//    }
//
//    /**
//     * 响应式WebSocket配置
//     */
//    @Bean
//    public WebSocketConfig webSocketConfig() {
//        return new WebSocketConfig();
//    }
//
//    /**
//     * WebSocket配置类
//     */
//    public static class WebSocketConfig {
//        // WebSocket相关配置
//        // 可以添加WebSocket处理器、拦截器等
//    }
// }

/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.cache.example;

import cn.universal.cache.annotation.MultiLevelCacheable;
import cn.universal.cache.strategy.CacheStrategy;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 多级缓存使用示例
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@Service
public class MultiLevelCacheExample {

  private final Map<String, User> userDatabase = new HashMap<>();
  private final Map<String, Product> productDatabase = new HashMap<>();

  public MultiLevelCacheExample() {
    // 初始化示例数据
    userDatabase.put("user1", new User("user1", "张三", 25));
    userDatabase.put("user2", new User("user2", "李四", 30));
    userDatabase.put("user3", new User("user3", "王五", 28));

    productDatabase.put("prod1", new Product("prod1", "iPhone 15", 5999.0));
    productDatabase.put("prod2", new Product("prod2", "MacBook Pro", 12999.0));
    productDatabase.put("prod3", new Product("prod3", "AirPods Pro", 1999.0));
  }

  /** 基本使用示例 L1缓存5分钟，L2缓存1小时 */
  @MultiLevelCacheable(
      cacheNames = "user_info",
      key = "#userId",
      l1Expire = 300,
      l2Expire = 3600,
      strategy = CacheStrategy.WRITE_THROUGH)
  public User getUserById(String userId) {
    log.info("从数据库查询用户: {}", userId);
    return userDatabase.get(userId);
  }

  /** 条件缓存示例 只有当userId不为空且结果不为null时才缓存 */
  @MultiLevelCacheable(
      cacheNames = "user_info",
      key = "#userId",
      condition = "#userId != null",
      unless = "#result == null",
      l1Expire = 600,
      l2Expire = 7200)
  public User getUserByIdWithCondition(String userId) {
    log.info("从数据库查询用户（条件缓存）: {}", userId);
    return userDatabase.get(userId);
  }

  /** 产品信息缓存示例 使用WRITE_BEHIND策略，异步写入L2缓存 */
  @MultiLevelCacheable(
      cacheNames = "product_info",
      key = "#productId",
      l1Expire = 1800,
      l2Expire = 86400,
      strategy = CacheStrategy.WRITE_BEHIND)
  public Product getProductById(String productId) {
    log.info("从数据库查询产品: {}", productId);
    return productDatabase.get(productId);
  }

  /** 复杂键生成示例 使用多个参数生成缓存键 */
  @MultiLevelCacheable(
      cacheNames = "user_product",
      key = "#userId + ':' + #productId",
      l1Expire = 900,
      l2Expire = 3600)
  public UserProduct getUserProduct(String userId, String productId) {
    log.info("从数据库查询用户产品关系: {} - {}", userId, productId);
    User user = userDatabase.get(userId);
    Product product = productDatabase.get(productId);
    if (user != null && product != null) {
      return new UserProduct(user, product);
    }
    return null;
  }

  /** 只使用L2缓存的示例 适合内存受限的场景 */
  @MultiLevelCacheable(
      cacheNames = "product_list",
      key = "'all_products'",
      l1Expire = 0, // 禁用L1缓存
      l2Expire = 1800,
      strategy = CacheStrategy.WRITE_AROUND)
  public Map<String, Product> getAllProducts() {
    log.info("从数据库查询所有产品");
    return new HashMap<>(productDatabase);
  }

  /** 用户实体类 */
  public static class User {

    private String id;
    private String name;
    private int age;

    public User(String id, String name, int age) {
      this.id = id;
      this.name = name;
      this.age = age;
    }

    // Getters and Setters
    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }

    @Override
    public String toString() {
      return "User{id='" + id + "', name='" + name + "', age=" + age + "}";
    }
  }

  /** 产品实体类 */
  public static class Product {

    private String id;
    private String name;
    private double price;

    public Product(String id, String name, double price) {
      this.id = id;
      this.name = name;
      this.price = price;
    }

    // Getters and Setters
    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public double getPrice() {
      return price;
    }

    public void setPrice(double price) {
      this.price = price;
    }

    @Override
    public String toString() {
      return "Product{id='" + id + "', name='" + name + "', price=" + price + "}";
    }
  }

  /** 用户产品关系实体类 */
  public static class UserProduct {

    private User user;
    private Product product;

    public UserProduct(User user, Product product) {
      this.user = user;
      this.product = product;
    }

    // Getters and Setters
    public User getUser() {
      return user;
    }

    public void setUser(User user) {
      this.user = user;
    }

    public Product getProduct() {
      return product;
    }

    public void setProduct(Product product) {
      this.product = product;
    }

    @Override
    public String toString() {
      return "UserProduct{user=" + user + ", product=" + product + "}";
    }
  }
}

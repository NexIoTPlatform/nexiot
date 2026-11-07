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

package cn.universal.rule.fence.utils;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.List;

/**
 * todo @Author gitee.com/NexIoT
 *
 * @since 2023/8/8 9:56
 */
public class RegionUtil {

  private static double EARTH_RADIUS = 6378137;

  // private static double EARTH_SEA = 1.852;海里

  private static double rad(double d) {
    return d * Math.PI / 180.0;
  }

  /**
   * 判断是否在多边形区域内
   *
   * @param pointLon 要判断的点的横坐标 经度
   * @param pointLat 要判断的点的纵坐标 维度
   * @param lon 区域各顶点的横坐标数组
   * @param lat 区域各顶点的纵坐标数组
   * @return
   */
  public static boolean isInPolygon(double pointLon, double pointLat, double[] lon, double[] lat) {
    // 将要判断的横纵坐标组成一个点
    Point2D.Double point = new Point2D.Double(pointLon, pointLat);
    // 将区域各顶点的横纵坐标放到一个点集合里面
    List<Point2D.Double> pointList = new ArrayList<Double>();
    double x = 0.0, y = 0.0;
    for (int i = 0; i < lon.length; i++) {
      x = lon[i];
      y = lat[i];
      Point2D.Double polygonPoint = new Point2D.Double(x, y);
      pointList.add(polygonPoint);
    }
    return check(point, pointList);
  }

  /**
   * @param point 要判断的点的横纵坐标
   * @param polygon 组成的顶点坐标集合
   * @return
   */
  private static boolean check(Point2D.Double point, List<Point2D.Double> polygon) {
    java.awt.geom.GeneralPath peneralPath = new java.awt.geom.GeneralPath();

    Point2D.Double first = polygon.get(0);
    // 通过移动到指定坐标（以双精度指定），将一个点添加到路径中
    peneralPath.moveTo(first.x, first.y);
    polygon.remove(0);
    for (Point2D.Double d : polygon) {
      // 通过绘制一条从当前坐标到新指定坐标（以双精度指定）的直线，将一个点添加到路径中。
      peneralPath.lineTo(d.x, d.y);
    }
    // 将几何多边形封闭
    peneralPath.lineTo(first.x, first.y);
    peneralPath.closePath();
    // 测试指定的 Point2D 是否在 Shape 的边界内。
    return peneralPath.contains(point);
  }

  /**
   * 通过经纬度获取距离(单位：米)
   *
   * @param lat1 纬度1
   * @param lng1 经度1
   * @param lat2 纬度2
   * @param lng2 经度2
   * @return 距离
   */
  public static double getDistance(double lat1, double lng1, double lat2, double lng2) {
    double radLat1 = rad(lat1);
    double radLat2 = rad(lat2);
    double a = radLat1 - radLat2;
    double b = rad(lng1) - rad(lng2);
    double s =
        2
            * Math.asin(
                Math.sqrt(
                    Math.pow(Math.sin(a / 2), 2)
                        + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
    s = s * EARTH_RADIUS;
    s = Math.round(s * 10000d) / 10000d;
    // double len=s/EARTH_SEA;
    // s = s / 1000;
    return s;
  }

  /**
   * @param lng1 经度
   * @param lng2 经度
   * @param lat1 纬度
   * @param lat2 纬度
   * @param radius 判断一个点是否在圆形区域内,比较坐标点与圆心的距离是否小于半径
   */
  public static boolean isInCircle(
      double lng1, double lat1, double lng2, double lat2, double radius) {
    double distance = getDistance(lat1, lng1, lat2, lng2);
    if (distance > radius) {
      return false;
    } else {
      return true;
    }
  }
}

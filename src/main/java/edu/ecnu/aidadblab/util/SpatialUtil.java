package edu.ecnu.aidadblab.util;

import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.constant.AngleType;
import edu.ecnu.aidadblab.constant.LocationComponent;
import edu.ecnu.aidadblab.data.model.Angle;

public class SpatialUtil {

    public static JSONObject calculateCircle(JSONObject v1, JSONObject v2) {
        JSONObject circle = new JSONObject();
        circle.put(LocationComponent.LATITUDE,
                (v1.getDoubleValue(LocationComponent.LATITUDE) + v2.getDoubleValue(LocationComponent.LATITUDE)) / 2d
        );
        circle.put(LocationComponent.LONGITUDE,
                (v1.getDoubleValue(LocationComponent.LONGITUDE) + v2.getDoubleValue(LocationComponent.LONGITUDE)) / 2d
        );
        circle.put(LocationComponent.RADIUS, calculateDistance(circle, v1));
        return circle;
    }

    public static JSONObject calculateCircle(JSONObject v1, JSONObject v2, JSONObject v3) {
        double x1 = v1.getDoubleValue(LocationComponent.LONGITUDE);
        double y1 = v1.getDoubleValue(LocationComponent.LATITUDE);
        double x2 = v2.getDoubleValue(LocationComponent.LONGITUDE);
        double y2 = v2.getDoubleValue(LocationComponent.LATITUDE);
        double x3 = v3.getDoubleValue(LocationComponent.LONGITUDE);
        double y3 = v3.getDoubleValue(LocationComponent.LATITUDE);

        double a = x1 - x2;
        double b = y1 - y2;
        double c = x1 - x3;
        double d = y1 - y3;
        double e = ((x1 * x1 - x2 * x2) + (y1 * y1 - y2 * y2)) / 2;
        double f = ((x1 * x1 - x3 * x3) + (y1 * y1 - y3 * y3)) / 2;
        double det = b * c - a * d;

        if (Math.abs(det) < 1e-7) {
            //Three points collinear
            return null;
        }

        JSONObject circle = new JSONObject();
        circle.put(LocationComponent.LATITUDE, -(d * e - b * f) / det);
        circle.put(LocationComponent.LONGITUDE, -(a * f - c * e) / det);
        circle.put(LocationComponent.RADIUS, calculateDistance(circle, v1));

        return circle;
    }

    public static double calculateDistance(JSONObject v1, JSONObject v2) {
        double x1 = v1.getDoubleValue(LocationComponent.LONGITUDE);
        double y1 = v1.getDoubleValue(LocationComponent.LATITUDE);
        double x2 = v2.getDoubleValue(LocationComponent.LONGITUDE);
        double y2 = v2.getDoubleValue(LocationComponent.LATITUDE);

        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    public static Angle[] getInOutAngle(JSONObject vi, JSONObject vj, double radius) {
        double x1 = vi.getDoubleValue(LocationComponent.LONGITUDE);
        double y1 = vi.getDoubleValue(LocationComponent.LATITUDE);
        double x2 = vj.getDoubleValue(LocationComponent.LONGITUDE);
        double y2 = vj.getDoubleValue(LocationComponent.LATITUDE);
        if (x1 == x2 && y1 == y2) {
            return null;
            //throw new IllegalArgumentException("vi, vj can not be the same point");
        }

        Angle inAngle = new Angle(AngleType.IN);
        Angle outAngle = new Angle(AngleType.OUT);

        if (y1 == y2) {
            double d = calculateDistance(vi, vj);
            double angle = Math.toDegrees(Math.asin(d / 2 / radius));
            if (x2 > x1) {
                inAngle.angleDegree = angle;
                outAngle.angleDegree = -angle;
            } else {
                inAngle.angleDegree = 180 + angle;
                outAngle.angleDegree = 180 - angle;
            }
        } else if (x1 == x2) {
            double d = calculateDistance(vi, vj);
            double angle = Math.toDegrees(Math.acos(d / 2 / radius));
            if (y2 > y1) {
                inAngle.angleDegree = 180 - angle;
                outAngle.angleDegree = angle;
            } else {
                inAngle.angleDegree = 360 - angle;
                outAngle.angleDegree = 180 + angle;
            }
        } else {
            double c1 = (x2 * x2 - x1 * x1 + y2 * y2 - y1 * y1) / (2 * (x2 - x1));
            double c2 = (y2 - y1) / (x2 - x1);  //斜率
            double A = (c2 * c2 + 1);
            double B = (2 * x1 * c2 - 2 * c1 * c2 - 2 * y1);
            double C = x1 * x1 - 2 * x1 * c1 + c1 * c1 + y1 * y1 - radius * radius;

            double temp = Math.sqrt(B * B - 4 * A * C);
            if (Double.isNaN(temp)) {
                return null;
            }
            double y3 = (-B + temp) / (2 * A);
            double x3 = c1 - c2 * y3;
            double y4 = (-B - temp) / (2 * A);
            double x4 = c1 - c2 * y4;
            double angleUpper, angleLower, yUpper, yLower, xUpper, xLower;
            if (x3 < x4) {
                xLower = x3;
                xUpper = x4;
                yLower = y3;
                yUpper = y4;
            } else {
                xLower = x4;
                xUpper = x3;
                yLower = y4;
                yUpper = y3;
            }
            double zLower = Math.sqrt((xLower - x1) * (xLower - x1) + (yLower - y1) * (yLower - y1));
            angleLower = Math.toDegrees(Math.acos(Math.abs(xLower - x1) / zLower));
            double zUpper = Math.sqrt((xUpper - x1) * (xUpper - x1) + (yUpper - y1) * (yUpper - y1));
            angleUpper = Math.toDegrees(Math.acos(Math.abs(xUpper - x1) / zUpper));

            //vj 在第一象限
            if (x2 >= x1 && y2 >= y1) {
                //都在第一象限 11
                if (xUpper >= x1 && yUpper >= y1 && xLower >= x1 && yLower >= y1) {
                    inAngle.angleDegree = angleLower;
                    outAngle.angleDegree = angleUpper;
                } // 12
                else if (xUpper >= x1 && yUpper >= y1 && xLower <= x1 && yLower >= y1) {
                    inAngle.angleDegree = 180 - angleLower;
                    outAngle.angleDegree = angleUpper;
                } // 14
                else if (xUpper >= x1 && yUpper <= y1 && xLower >= x1 && yLower >= y1) {
                    inAngle.angleDegree = angleLower;
                    outAngle.angleDegree = -angleUpper;
                } // 24
                else if (xUpper >= x1 && yUpper <= y1 && xLower <= x1 && yLower >= y1) {
                    inAngle.angleDegree = 180 - angleLower;
                    outAngle.angleDegree = -angleUpper;
                } else {
                    Console.log("{} {} {} {} {} {}", x1, y1, xLower, yLower, xUpper, yUpper);
                    throw new IllegalArgumentException();
                }
            }//vj 在第二象限
            else if (x2 <= x1 && y2 >= y1) {
                // 12
                if (xUpper >= x1 && yUpper >= y1 && xLower <= x1 && yLower >= y1) {
                    inAngle.angleDegree = 180 - angleLower;
                    outAngle.angleDegree = angleUpper;
                }
                // 22
                else if (xUpper <= x1 && yUpper >= y1 && xLower <= x1 && yLower >= y1) {
                    inAngle.angleDegree = 180 - angleLower;
                    outAngle.angleDegree = 180 - angleUpper;
                } // 23
                else if (xUpper <= x1 && yUpper >= y1 && xLower <= x1 && yLower <= y1) {
                    inAngle.angleDegree = 180 + angleLower;
                    outAngle.angleDegree = 180 - angleUpper;
                }// 13
                else if (xUpper >= x1 && yUpper >= y1 && xLower <= x1 && yLower <= y1) {
                    inAngle.angleDegree = 180 + angleLower;
                    outAngle.angleDegree = angleUpper;
                } else {
                    Console.log("{} {} {} {} {} {}", x1, y1, xLower, yLower, xUpper, yUpper);
                    throw new IllegalArgumentException();
                }
            }//vj 在第三象限
            else if (x2 <= x1 && y2 <= y1) {
                // 23
                if (xUpper <= x1 && yUpper <= y1 && xLower <= x1 && yLower >= y1) {
                    inAngle.angleDegree = 180 + angleUpper;
                    outAngle.angleDegree = 180 - angleLower;
                }// 33
                else if (xUpper <= x1 && yUpper <= y1 && xLower <= x1 && yLower <= y1) {
                    inAngle.angleDegree = 180 + angleUpper;
                    outAngle.angleDegree = 180 + angleLower;
                }// 34
                else if (xUpper >= x1 && yUpper <= y1 && xLower <= x1 && yLower <= y1) {
                    inAngle.angleDegree = 360 - angleUpper;
                    outAngle.angleDegree = 180 + angleLower;
                }//24
                else if (xUpper >= x1 && yUpper <= y1 && xLower <= x1 && yLower >= y1) {
                    inAngle.angleDegree = 180 - angleLower;
                    outAngle.angleDegree = -angleUpper;
                } else {
                    Console.log("{} {} {} {} {} {}", x1, y1, xLower, yLower, xUpper, yUpper);
                    throw new IllegalArgumentException();
                }
            }//vj 在第四象限
            else {
                // 34
                if (xUpper >= x1 && yUpper <= y1 && xLower <= x1 && yLower <= y1) {
                    inAngle.angleDegree = 360 - angleUpper;
                    outAngle.angleDegree = 180 + angleLower;
                }// 44
                else if (xUpper >= x1 && yUpper <= y1 && xLower >= x1 && yLower <= y1) {
                    inAngle.angleDegree = 360 - angleUpper;
                    outAngle.angleDegree = 360 - angleLower;
                }// 41
                else if (xUpper >= x1 && yUpper >= y1 && xLower >= x1 && yLower <= y1) {
                    inAngle.angleDegree = 360 + angleUpper;
                    outAngle.angleDegree = 360 - angleLower;
                }// 13
                else if (xUpper >= x1 && yUpper >= y1 && xLower <= x1 && yLower <= y1) {
                    inAngle.angleDegree = 360 + angleUpper;
                    outAngle.angleDegree = 180 + angleLower;
                } else {
                    Console.log("{} {} {} {} {} {}", x1, y1, xLower, yLower, xUpper, yUpper);
                    throw new IllegalArgumentException();
                }
            }
        }

        //Console.log("inAngle: {} outAngle: {}", inAngle.angleDegree, outAngle.angleDegree);

        return new Angle[]{inAngle, outAngle};
    }

}

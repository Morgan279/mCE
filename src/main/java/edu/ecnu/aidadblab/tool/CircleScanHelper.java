package edu.ecnu.aidadblab.tool;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.constant.AngleType;
import edu.ecnu.aidadblab.constant.LocationComponent;
import edu.ecnu.aidadblab.data.model.Angle;
import edu.ecnu.aidadblab.data.model.SKECCenter;
import lombok.NoArgsConstructor;

import java.util.List;

public class CircleScanHelper {

    public static List<SKECCenter> getCircleCenters(JSONObject v, JSONObject u, double dRadius) {
        Node p1 = new Node(v);
        Node p2 = new Node(u);
        Node center1 = new Node();
        Node center2 = new Node();

        double k = 0.0, k_verticle = 0.0;
        double mid_x = 0.0, mid_y = 0.0;
        double a = 1.0;
        double b = 1.0;
        double c = 1.0;
        k = (p2.y_ - p1.y_) / (p2.x_ - p1.x_);
        if (k == 0) {
            center1.x_ = (p1.x_ + p2.x_) / 2.0;
            center2.x_ = (p1.x_ + p2.x_) / 2.0;
            center1.y_ = p1.y_ + Math.sqrt(dRadius * dRadius - (p1.x_ - p2.x_) * (p1.x_ - p2.x_) / 4.0);
            center2.y_ = p2.y_ - Math.sqrt(dRadius * dRadius - (p1.x_ - p2.x_) * (p1.x_ - p2.x_) / 4.0);
        } else {
            k_verticle = -1.0 / k;
            mid_x = (p1.x_ + p2.x_) / 2.0;
            mid_y = (p1.y_ + p2.y_) / 2.0;
            a = 1.0 + k_verticle * k_verticle;
            b = -2 * mid_x - k_verticle * k_verticle * (p1.x_ + p2.x_);
            c = mid_x * mid_x + k_verticle * k_verticle * (p1.x_ + p2.x_) * (p1.x_ + p2.x_) / 4.0 -
                    (dRadius * dRadius - ((mid_x - p1.x_) * (mid_x - p1.x_) + (mid_y - p1.y_) * (mid_y - p1.y_)));
            double factor = Math.sqrt(b * b - 4 * a * c);
            center1.x_ = (-1.0 * b + factor) / (2 * a);
            center2.x_ = (-1.0 * b - factor) / (2 * a);
            center1.y_ = yCoordinates(mid_x, mid_y, k_verticle, center1.x_);
            center2.y_ = yCoordinates(mid_x, mid_y, k_verticle, center2.x_);
        }

        SKECCenter skecCenter1 = new SKECCenter();
        SKECCenter skecCenter2 = new SKECCenter();
        skecCenter1.center = center1.toJSON();
        skecCenter2.center = center2.toJSON();

//        if (compareAngle(v, skecCenter1.center, skecCenter2.center) > 0) {
//            skecCenter1.type = AngleType.OUT;
//            skecCenter2.type = AngleType.IN;
//        } else {
//            skecCenter1.type = AngleType.IN;
//            skecCenter2.type = AngleType.OUT;
//        }

        if ((center1.y_ >= 0 && center2.y_ >= 0) || (center1.x_ < 0 && center2.y_ > 0)) {
            skecCenter1.type = AngleType.OUT;
            skecCenter2.type = AngleType.IN;
        } else {
            skecCenter1.type = AngleType.IN;
            skecCenter2.type = AngleType.OUT;
        }

        return CollUtil.newArrayList(skecCenter1, skecCenter2);
    }

    public static Angle[] getInOutAngle(JSONObject v, JSONObject u, double diameter) {
        Node a = new Node(v);
        Node b = new Node(u);
        double dist = euclideanDistance(a, b);
        double x = b.x_ - a.x_;
        double y = b.y_ - a.y_;
        double baseAngle = Math.atan2(y, x);
        double alpha = Math.acos(dist / diameter);
        Angle inAngle = new Angle(AngleType.IN);
        Angle outAngle = new Angle(AngleType.OUT);
        inAngle.angleDegree = normalizeAngle(baseAngle - alpha);
        outAngle.angleDegree = normalizeAngle(baseAngle + alpha);
        return new Angle[]{inAngle, outAngle};
    }

    private static double normalizeAngle(double angle) {
        while (angle < 0)
            angle += 2 * Math.PI;
        while (angle > Math.PI * 2)
            angle -= 2 * Math.PI;
        return angle;
    }

    public static int compareAngle(JSONObject v, JSONObject u1, JSONObject u2) {
        Node node1 = new Node(u1);
        Node node2 = new Node(u2);
        Node axisNodeAdvanced = new Node(v);
        if (node1.x_ == axisNodeAdvanced.x_ && node1.y_ == axisNodeAdvanced.y_) {
            return 1;
        }
        if (node2.x_ == axisNodeAdvanced.x_ && node2.y_ == axisNodeAdvanced.y_) {
            return -1;
        }
        Node tmpNode1 = new Node();
        Node tmpNode2 = new Node();
        tmpNode1.x_ = node1.x_ - axisNodeAdvanced.x_;
        tmpNode1.y_ = node1.y_ - axisNodeAdvanced.y_;
        tmpNode2.x_ = node2.x_ - axisNodeAdvanced.x_;
        tmpNode2.y_ = node2.y_ - axisNodeAdvanced.y_;
        if (cross(tmpNode1, tmpNode2) == 0) {
            return Double.compare(euclideanDistance(tmpNode2, axisNodeAdvanced), euclideanDistance(tmpNode1, axisNodeAdvanced));
        }

        return cross(tmpNode1, tmpNode2) > 0 ? 1 : -1;
    }

    private static double yCoordinates(double x, double y, double k, double x0) {
        return k * x0 - k * x + y;
    }


    private static double euclideanDistance(Node node1, Node node2) {
        return Math.sqrt((node1.x_ - node2.x_) * (node1.x_ - node2.x_) + (node1.y_ - node2.y_) * (node1.y_ - node2.y_));
    }

    private static double cross(Node node1, Node node2) {
        return node1.x_ * node2.y_ - node1.y_ * node2.x_;
    }

    @NoArgsConstructor
    private static class Node {
        public Node(JSONObject location) {
            this.x_ = location.getDoubleValue(LocationComponent.LONGITUDE);
            this.y_ = location.getDoubleValue(LocationComponent.LATITUDE);
        }

        public double x_;

        public double y_;

        public JSONObject toJSON() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(LocationComponent.LONGITUDE, x_);
            jsonObject.put(LocationComponent.LATITUDE, y_);
            return jsonObject;
        }
    }
}

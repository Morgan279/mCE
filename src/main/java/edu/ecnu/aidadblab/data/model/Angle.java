package edu.ecnu.aidadblab.data.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Angle {

    public Angle(int angleType) {
        this.angleType = angleType;
    }

    public double angleDegree;

    public int angleType;

}

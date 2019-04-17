package org.softwire.training.gasmon.model;

import com.google.common.base.MoreObjects;

public class Location {

    private double x;
    private double y;
    private String id;

    public Location(double x, double y, String id) {
        this.x = x;
        this.y = y;
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getId() {
        return id;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
                .add("ID", id)
                .add("x", x)
                .add("y", y)
                .toString();
    }
}

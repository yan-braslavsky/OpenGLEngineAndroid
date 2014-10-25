package com.example.yan_home.openglengineandroid.util.math;

/**
 * Created by Yan-Home on 10/3/2014.
 */
public class MathUtils {

    public static final float randomInRange(float min, float max) {
        return (float) (Math.random() < 0.5 ? ((1 - Math.random()) * (max - min) + min) : (Math.random() * (max - min) + min));
    }

    public static void rotatePointAroundOrigin(Vector2 point, Vector2 origin, float angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double newX = origin.getX() + (point.getX() - origin.getX()) * Math.cos(angleRadians) - (point.getY() - origin.getY()) * Math.sin(angleRadians);
        double newY = origin.getY() + (point.getX() - origin.getX()) * Math.sin(angleRadians) + (point.getY() - origin.getY()) * Math.cos(angleRadians);
        point.setX((float) newX);
        point.setY((float) newY);
    }

}

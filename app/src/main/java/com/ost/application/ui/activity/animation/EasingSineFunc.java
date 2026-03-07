package com.ost.application.ui.activity.animation;
public class EasingSineFunc {

    private static EasingSineFunc instance;

    public static EasingSineFunc getInstance() {
        if (instance == null) {
            instance = new EasingSineFunc();
        }
        return instance;
    }
    public float easeInOut(float t, float b, float c, float d) {
        t /= d / 2;
        if (t < 1) return (float) (c / 2 * (Math.sin(Math.PI * t / 2)) + b);
        t--;
        return (float) (-c / 2 * (Math.cos(Math.PI * t / 2) - 2) + b);
    }
}
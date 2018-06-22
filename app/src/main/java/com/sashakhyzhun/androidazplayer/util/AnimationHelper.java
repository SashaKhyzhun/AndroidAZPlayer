package com.sashakhyzhun.androidazplayer.util;

import android.view.View;
import android.view.ViewPropertyAnimator;

public class AnimationHelper {

    private static final int DISPLAY_Y_OFFSET = 250;
    private static final int DISPLAY_X_OFFSET = 150;
    private static final int VELOCITY_OFFSET = 150;

    private int maxX;
    private int maxY;

    public AnimationHelper(int maxX, int maxY) {
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public void swipeToRightUpperCorner(View playerView, double velocity) {
        playerAnim(playerView, velocity,
                maxX / 2 - DISPLAY_X_OFFSET,
                -maxY / 2 + DISPLAY_Y_OFFSET)
                .start();
    }

    public void swipeToRightDownCorner(View playerView, double velocity) {
        playerAnim(playerView, velocity,
                maxX / 2 - DISPLAY_X_OFFSET,
                maxY / 2 - DISPLAY_Y_OFFSET)
                .start();
    }

    public void swipeToLeftUpperCorner(View playerView, double velocity) {
        playerAnim(playerView, velocity,
                -maxX / 2 + DISPLAY_X_OFFSET,
                -maxY / 2 + DISPLAY_Y_OFFSET)
                .start();
    }

    public void swipeToLeftDownCorner(View playerView, double velocity) {
        playerAnim(playerView, velocity,
                -maxX / 2 + DISPLAY_X_OFFSET,
                maxY / 2 - DISPLAY_Y_OFFSET)
                .start();
    }

    public void swipeToHorizontalEdge(View playerView, double velocity, boolean toRight) {
        playerAnim(playerView, velocity)
                .translationX(toRight ? maxX / 2 - DISPLAY_X_OFFSET : -maxX / 2 + DISPLAY_X_OFFSET)
                .start();
    }

    public void swipeToVerticalEdge(View playerView, double velocity, boolean toTop) {
        playerAnim(playerView, velocity)
                .translationY(toTop ? -maxY / 2 + DISPLAY_Y_OFFSET : maxY / 2 - DISPLAY_Y_OFFSET)
                .start();
    }

    private ViewPropertyAnimator playerAnim(View playerView, double velocity,
                                            float transX, float transY) {
        return playerView.animate()
                .translationX(transX)
                .translationY(transY)
                .setDuration(convertVelocity(velocity));
    }

    private ViewPropertyAnimator playerAnim(View playerView, double velocity) {
        return playerView.animate()
                .setDuration(convertVelocity(velocity));
    }

    private static long convertVelocity(double velocity) {
        if (velocity < 2) velocity = 2;
        if (velocity > 8) velocity = 8;
        long speedLong = (long) velocity;
        return ((10 - speedLong) * 100 - VELOCITY_OFFSET);
    }





}

package com.sashakhyzhun.androidazplayer.util;

import android.view.View;
import android.view.ViewPropertyAnimator;

public class AnimationHelper {

    private static final int VELOCITY_OFFSET = 150;
    private static final int offsetX = 20;
    private int maxX;
    private int maxY;

    //todo: replace manual offset with '+-view.getHeight() / +-view.getWidth()'
    public AnimationHelper(int maxX, int maxY) {
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public void swipeToRightUpperCorner(View playerView, double velocity) {
        playerAnim(playerView, velocity,
                maxX / 2 - playerView.getWidth() / 2 - offsetX,
                -maxY / 2 + playerView.getHeight())
                .start();
    }

    public void swipeToRightDownCorner(View playerView, double velocity) {
        playerAnim(playerView, velocity,
                maxX / 2 - playerView.getWidth() / 2 - offsetX,
                maxY / 2 - playerView.getHeight())
                .start();
    }

    public void swipeToLeftUpperCorner(View playerView, double velocity) {
        playerAnim(playerView, velocity,
                -maxX / 2 + playerView.getWidth() / 2 + offsetX,
                -maxY / 2 + playerView.getHeight())
                .start();
    }

    public void swipeToLeftDownCorner(View playerView, double velocity) {
        playerAnim(playerView, velocity,
                -maxX / 2 + playerView.getWidth() / 2 + offsetX,
                maxY / 2 - playerView.getHeight())
                .start();
    }

    public void swipeToHorizontalEdge(View playerView, double velocity, boolean toRight) {
        playerAnim(playerView, velocity)
                .translationX(toRight
                        ? maxX / 2 - playerView.getWidth() / 2
                        : -maxX / 2 + playerView.getWidth() / 2)
                .start();
    }

    public void swipeToVerticalEdge(View playerView, double velocity, boolean toTop) {
        playerAnim(playerView, velocity)
                .translationY(toTop
                        ? -maxY / 2 + playerView.getHeight()
                        : maxY / 2 - playerView.getHeight())
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

package com.sashakhyzhun.androidazplayer.ui.custom;

import android.app.Application;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;

import javax.security.auth.login.LoginException;

import static com.sashakhyzhun.androidazplayer.util.Constants.shitHappens;
import static com.sashakhyzhun.androidazplayer.util.DeviceUtil.dimension;

public class AzButton extends View implements View.OnClickListener, View.OnTouchListener {

    private static final int DISPLAY_Y_OFFSET = 250;
    private static final int DISPLAY_X_OFFSET = 150;

    private PLAYER_STATE mState = PLAYER_STATE.NORMAL;
    private boolean isFetchingAnimRunning = false;
    private float START_ANGLE_POINT = 90;

    private int angleArch;
    private float oldValueX = 0;
    private float oldValueY = 0;
    private float oldViewX = 0;
    private float oldViewY = 0;

    // size of the player
    private int preferedWidth = 0;
    private int _preferedWidthDP = 100;
    private int preferedPadding = 0;

    private Point trianglePoint;
    private Paint trianglePaint;
    private GestureDetector gestureDetector;
    private OnClickListener mListener;
    private MediaPlayer mMediaPlayer;
    private Paint fetchingCircleLineAround;
    private Paint pauseViewInsideRect;
    private Paint playerButtonPaint;
    private RectF playerRectArch;
    private Rect playerRectAngle;

    private int maxX = 0;
    private int maxY = 0;


    public enum PLAYER_STATE {
        PLAYING,
        PAUSE,
        FETCHING,
        NORMAL,
        COMPLETED
    }

    public AzButton(Context context) {
        super(context);
        init(context, null);
    }

    public AzButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AzButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        Display display = null;
        if (wm != null) {
            display = wm.getDefaultDisplay();
            Point displayPoint = new Point();
            display.getSize(displayPoint);
            maxX = displayPoint.x;
            maxY = displayPoint.y;
        }


        //gestureListener = new GestureListener();
        gestureDetector = new GestureDetector(context, new SingleTapConfirm(this));

        // onClicks
        setOnClickListener(this);
        setOnTouchListener(this);

        // display sizes
        preferedWidth = (int) dimension(context, _preferedWidthDP);
        preferedPadding = (int) dimension(context, 4);

        // set up the layout params
        setLayoutParams(new LinearLayout.LayoutParams(_preferedWidthDP, _preferedWidthDP));
        setPadding(preferedPadding, preferedPadding, preferedPadding, preferedPadding);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { setElevation(10F); }

        // create the Player Main view
        playerButtonPaint = new Paint();
        playerButtonPaint.setAntiAlias(true);
        playerButtonPaint.setColor(Color.BLACK);

        // create the TRIANGLE inside the player
        trianglePaint = new Paint();
        trianglePaint.setColor(Color.WHITE);
        trianglePaint.setStyle(Paint.Style.FILL);
        trianglePoint = new Point();

        // create the PAUSE inside the player
        pauseViewInsideRect = new Paint();
        pauseViewInsideRect.setColor(Color.GREEN);

        // create the ROTATE ANIMATION as it is
        RotateAnimation rotateAnim = new RotateAnimation(
                0,
                360,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        );
        rotateAnim.setDuration(1000);
        rotateAnim.setRepeatCount(Animation.INFINITE);
        rotateAnim.setRepeatMode(Animation.RESTART);
        rotateAnim.setInterpolator(new LinearInterpolator());

        // create the FETCHING paint around the player like 'loading'
        fetchingCircleLineAround = new Paint();
        fetchingCircleLineAround.setAntiAlias(true);
        fetchingCircleLineAround.setStyle(Paint.Style.STROKE);
        fetchingCircleLineAround.setStrokeWidth(dimension(context, 2));
        fetchingCircleLineAround.setColor(Color.YELLOW);

        // size 228x228 4example
        playerRectArch = new RectF(0, 0, 0, 0);
        playerRectAngle = new Rect(0, 0, 0, 0);

        // Initial Angle (optional, it can be zero)
        angleArch = 120;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int usableWidth = preferedWidth;
        int usableHeight = preferedWidth;

        // make it circle
        int mRadius = Math.min(usableWidth, usableHeight) / 2;
        int mCircleX = (usableWidth / 2);
        int mCircleY = (usableHeight / 2);

        canvas.drawCircle(mCircleX, mCircleY, mRadius, playerButtonPaint);

        switch (getState()) {
            case NORMAL:
            case PAUSE:
            case COMPLETED:

                int triangleWidth = (usableWidth / 2) - preferedPadding;

                // triangle sizes
                trianglePoint.x = mCircleX - (triangleWidth / 2) + preferedPadding;
                trianglePoint.y = mCircleY - (triangleWidth / 2);

                Path trianglePath = getInsideTrianglePath(trianglePoint, triangleWidth);
                canvas.drawPath(trianglePath, trianglePaint);

                if (getState() == PLAYER_STATE.PAUSE) {

                    playerRectArch.left = preferedPadding / 2;
                    playerRectArch.top = preferedPadding / 2;
                    playerRectArch.right = (preferedWidth) - preferedPadding / 2;
                    playerRectArch.bottom = preferedWidth - preferedPadding / 2;

                    canvas.drawArc(playerRectArch, START_ANGLE_POINT, angleArch, false, fetchingCircleLineAround);
                }
                break;
            case PLAYING:

                int width = (usableWidth / 3) - preferedPadding;

                int x = mCircleX - width;
                int y = mCircleY - width;

                playerRectAngle.top = y;
                playerRectAngle.left = x + (preferedPadding / 4);
                playerRectAngle.right = mCircleX - (preferedPadding / 2);
                playerRectAngle.bottom = mCircleY + width;
                canvas.drawRect(playerRectAngle, pauseViewInsideRect);

                playerRectAngle.top = y;
                playerRectAngle.left = mCircleX + (preferedPadding / 2);
                playerRectAngle.right = mCircleX + (mCircleX - x) - (preferedPadding / 4);
                playerRectAngle.bottom = mCircleY + width;
                canvas.drawRect(playerRectAngle, pauseViewInsideRect);

                playerRectArch.left = preferedPadding / 2;
                playerRectArch.top = preferedPadding / 2;
                playerRectArch.right = preferedWidth - preferedPadding / 2;
                playerRectArch.bottom = preferedWidth - preferedPadding / 2;

                canvas.drawArc(playerRectArch, START_ANGLE_POINT, angleArch, false, fetchingCircleLineAround);

                break;
            case FETCHING:

                playerRectArch.left = preferedPadding / 2;
                playerRectArch.top = preferedPadding / 2;
                playerRectArch.right = preferedWidth - preferedPadding / 2;
                playerRectArch.bottom = preferedWidth - preferedPadding / 2;

                canvas.drawArc(playerRectArch, START_ANGLE_POINT, angleArch, false, fetchingCircleLineAround);
                break;
        }


    }

    private Path getInsideTrianglePath(Point point1, int width) {
        Point point2 = new Point(point1.x, point1.y + width);
        Point point3 = new Point(point1.x + width, point1.y + (width / 2));
        Path path = new Path();
        path.moveTo(point1.x, point1.y);
        path.lineTo(point2.x, point2.y);
        path.lineTo(point3.x, point3.y);
        return path;
    }

    public PLAYER_STATE getState() {
        return mState;
    }

    public void setState(PLAYER_STATE state) {
        mState = state;
        switch (state) {
            case FETCHING:
                Log.d("AzButton", "state_fetching = true");
            case PLAYING:
                isFetchingAnimRunning = false;
                startPlayingAnim();
                break;
            case COMPLETED:
                isFetchingAnimRunning = false;
                angleArch = 120;
                START_ANGLE_POINT = 90;
                break;
            default:
                isFetchingAnimRunning = false;
             break;
        }
        //If you want to re draw your view from UI Thread you can call invalidate() method.
        //If you want to re draw your view from Non UI Thread you can call postInvalidate() method.
        invalidate();
    }

    private void startFetchingAnim() {
        if (!isFetchingAnimRunning) {
            return;
        }
        Handler h = new Handler();
        h.postDelayed(() -> {
            if (angleArch >= 350) {
                angleArch = 0;
            }
            angleArch += 10;
            if (START_ANGLE_POINT >= 350) {
                START_ANGLE_POINT = 0;
            }
            START_ANGLE_POINT += 10;
            invalidate();
            startFetchingAnim();
        }, 50);

    }

    public void startFetching() {
        setState(PLAYER_STATE.FETCHING);
        if (!isFetchingAnimRunning) {
            isFetchingAnimRunning = !isFetchingAnimRunning;
            startFetchingAnim();
        }
    }

    public void startPlayingAnim() {
        if (getState() != PLAYER_STATE.PLAYING || mMediaPlayer == null) {
            return;
        }
        Handler h = new Handler();
        h.postDelayed(() -> {

            angleArch = (mMediaPlayer.getCurrentPosition() * 360) / mMediaPlayer.getDuration();
            Log.d("Duration A", String.valueOf(mMediaPlayer.getDuration()));
            Log.d("Duration C", String.valueOf(mMediaPlayer.getCurrentPosition()));
            Log.d("Duration", String.valueOf(angleArch));

            START_ANGLE_POINT = 0;
            invalidate();
            startPlayingAnim();
        }, 1000);

    }


    /**
     * listeners
     */

    @Override
    public void onClick(View v) {
        //Log.i("AzButton", "onClick");
    }

    private class SingleTapConfirm extends SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 1;
        private static final int SWIPE_VELOCITY_THRESHOLD = 1;
        private View playerView;

        SingleTapConfirm(View playerView) {
            this.playerView = playerView;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

//                if (Math.abs(diffX) > Math.abs(diffY)) {
//                    if (diffX > 0) {
//                        swipeToLeftUpperCorner(playerView);
//                    } else {
//                        swipeToRightUpperCorner(playerView);
//                    }
//                    return true;
//                }
//
//                if (Math.abs(diffX) < Math.abs(diffY)) {
//                    if (diffY > 0) {
//                        swipeToRightDownCorner(playerView);
//                    } else {
//                        swipeToLeftDownCorner(playerView);
//                    }
//                    return true;
//                }

//                // GOING TO THE RIGHT | UP
//                if (Math.abs(diffX) > 0 && Math.abs(diffY) > 0) {
//                    swipeToRightUpperCorner(playerView);
//                }
//                // GOING TO THE RIGHT | DOWN
//                if (Math.abs(diffX) > 0 && Math.abs(diffY) < 0) {
//                    swipeToRightDownCorner(playerView);
//                }
//                // GOING TO THE LEFT | UP
//                if (Math.abs(diffX) < 0 && Math.abs(diffY) > 0) {
//                    swipeToLeftUpperCorner(playerView);
//                }
//                // // GOING TO THE LEFT | DOWN
//                if (Math.abs(diffX) < 0 && Math.abs(diffY) < 0) {
//                    swipeToLeftDownCorner(playerView);
//                }
//                else {
//                    Log.i("onFling", "onFling | ELSE BLOCK");
//                }
//                Log.d("onFling", "e1.getX = " + e1.getX());
//                Log.d("onFling", "e2.getX = " + e2.getX());
//                Log.d("onFling", "e1.getY = " + e1.getY());
//                Log.d("onFling", "e2.getY = " + e2.getY());
//                Log.d("onFling", "diffX = " + diffX);
//                Log.d("onFling", "diffY = " + diffY);
//                Log.d("onFling", "abs(diffX) = " + Math.abs(diffX));
//                Log.d("onFling", "abs(diffY) = " + Math.abs(diffY));
//                Log.d("onFling", "abs(velocityX) = " + Math.abs(velocityX));
//                Log.d("onFling", "abs(velocityY) = " + Math.abs(velocityY));
//                Log.d("onFling", "*****************************");

            } catch (Exception exception) {
                exception.printStackTrace();
                Log.e(Application.class.getSimpleName(), shitHappens);
            }
            return false;
        }

    }

    public void setClickedListener(OnClickListener listener) {
        mListener = listener;
    }

    public void setMediaPlayer(MediaPlayer mp) {
        mMediaPlayer = mp;
    }

//    @Override
//    public boolean onTouch(View view, MotionEvent event) {
//
////        if (gestureDetector.onTouchEvent(event)) {
////            Log.d("onTouch", "gestureDetector.onTouchEvent(event)");
////            if (mListener != null) {
////                mListener.onClick(view);
////            }
////            return false;
////        }
//
//
//        float oldX = event.getX();
//        float oldY = event.getY();
//
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                oldValueX = view.getX() - event.getRawX();
//                oldValueY = view.getY() - event.getRawY();
//                oldX = event.getX();
//                oldY = event.getY();
//                Log.d("ACTION_DOWN", "oldX = " + oldX);
//                Log.d("ACTION_DOWN", "oldY = " + oldY);
//                break;
//            case MotionEvent.ACTION_MOVE:
//                view.animate()
//                        .x(event.getRawX() + oldValueX)
//                        .y(event.getRawY() + oldValueY)
//                        .setDuration(0)
//                        .start();
//                break;
//            case MotionEvent.ACTION_UP:
//                float newX = view.getX() - event.getRawX();
//                float newY = view.getY() - event.getRawY();
//                Log.d("ACTION_UP", "newX = " + newX);
//                Log.d("ACTION_UP", "newY = " + newY);
//
////                Log.d("ActionUp", "old x = " + e1X);
////                Log.d("ActionUp", "new X = " + event.getX());
////                Log.d("ActionUp", "-------------------------------");
////                Log.d("ActionUp", "old Y = " + e1Y);
////                Log.d("ActionUp", "new Y = " + event.getY());
//                float diffX = newX - oldX;
//                float diffY = newY - oldY;
////                Log.d("ActionUp", "-------------------------------");
////                Log.d("ActionUp", "diff X = " + diffX);
////                Log.d("ActionUp", "diff Y = " + diffY);
//
//
//                break;
//            default:
//                break;
//        }
//
//        return true;
//    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldValueX = view.getX() - event.getRawX();
                oldValueY = view.getY() - event.getRawY();
                oldViewX = view.getX();
                oldViewY = view.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                view.animate()
                        .x(event.getRawX() + oldValueX)
                        .y(event.getRawY() + oldValueY)
                        .setDuration(0)
                        .start();
                break;
            case MotionEvent.ACTION_UP:
                float lastX = view.getX();
                float lastY = view.getY();
                Log.d("ACTION_UP", "old x = " + oldViewX);
                Log.d("ACTION_UP", "new x = " + lastX);
                Log.d("ACTION_UP", "old y = " + oldViewY);
                Log.d("ACTION_UP", "new y = " + lastY);
                Log.d("ACTION_UP", "---------------------");

                Log.d("ACTION_UP", "abs X = " + (Math.abs(lastX - oldViewX)));
                Log.d("ACTION_UP", "abs Y = " + (Math.abs(lastY - oldViewY)));


                if (lastX > oldViewX && Math.abs(lastX - oldViewX) > 100) { // to right

                    Log.i("actionUp", "движемся вправо");

                    if (lastY > oldViewY) { // to top
                        Log.i("actionUp", "и вниз");


                    } else if (lastY < oldViewY) { // to bottom

                        Log.i("actionUp", "и вверх");

                    }

                } else if (lastX < oldViewX && Math.abs(lastX - oldViewX) > 100) { // to left

                    Log.i("actionUp", "движемся влево");

                    if (lastY > oldViewY) { // to top

                        Log.i("actionUp", "и вниз");

                    } else if (lastY < oldViewY) { // to bottom

                        Log.i("actionUp", "и вверх");

                    }
                }

//                // GOING TO THE RIGHT
//                if (Math.abs(oldViewX - lastX) > 200) {
//                    // AND UP
//                    if (Math.abs(oldViewY - lastY) > 200) {
//                        swipeToRightUpperCorner(view);
//                    }
//                    // OR DOWN
//                    else if (Math.abs(oldViewY - lastY) < 200) {
//                        swipeToRightDownCorner(view);
//                    }
//                }
//
//                // GOING TO THE LEFT
//                if (Math.abs(oldViewX - lastX) < 200) {
//                    // AND UP
//                    if (Math.abs(oldViewY - lastY) > 0) {
//                        swipeToLeftUpperCorner(view);
//                    }
//                    // OR DOWN
//                    else if (Math.abs(oldViewY - lastY) < 0) {
//                        swipeToLeftDownCorner(view);
//                    }
//                }

                break;
        }

        return true;
    }


    public void swipeToRightUpperCorner(View playerView) {
        Log.d("onFling", "swipeToRightUpperCorner");
        playerView.animate()
                .translationX(maxX / 2 - DISPLAY_X_OFFSET)
                .translationY(-maxY / 2 + DISPLAY_Y_OFFSET)
                .setDuration(400)
                .start();
    }

    public void swipeToRightDownCorner(View playerView) {
        Log.d("onFling", "swipeToRightDownCorner");
        playerView.animate()
                .translationX(maxX / 2 - DISPLAY_X_OFFSET)
                .translationY(maxY / 2 + DISPLAY_Y_OFFSET)
                .setDuration(100)
                .start();
    }

    public void swipeToLeftUpperCorner(View playerView) {
        Log.d("onFling", "swipeToLeftUpperCorner");
//        playerView.animate()
//                .translationX(0)
//                .translationY(0)
//                .setDuration(100)
//                .start();
    }

    public void swipeToLeftDownCorner(View playerView){
        Log.d("onFling", "swipeToLeftDownCorner");
//        playerView.animate()
//                .translationX(0)
//                .translationY(0)
//                .setDuration(100)
//                .start();
    }





}

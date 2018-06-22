package com.sashakhyzhun.androidazplayer.ui.custom;

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

import java.text.DecimalFormat;

import static com.sashakhyzhun.androidazplayer.util.DeviceUtil.dimension;

/**
 * This code is not the best solution ever that can handle any case.
 * I had requirements, I did related solution.
 * Something required a much more time for a better result (like animation smoothing).
 * Everything might be improved & polished for better performance or smoother animations.
 */
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
    private long timerStart = 0;


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
        gestureDetector = new GestureDetector(context, new SingleTapConfirm());

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
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

    public void setClickedListener(OnClickListener listener) {
        mListener = listener;
    }

    public void setMediaPlayer(MediaPlayer mp) {
        mMediaPlayer = mp;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldValueX = view.getX() - event.getRawX();
                oldValueY = view.getY() - event.getRawY();
                oldViewX = view.getX();
                oldViewY = view.getY();
                timerStart = System.currentTimeMillis();
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

                long timerEnd = System.currentTimeMillis();
                double distance = Math.sqrt((lastX-oldViewX) * (lastX-oldViewX) + (lastY-oldViewY) * (lastY-oldViewY));
                double velocity = (distance / (timerEnd - timerStart));

                if (lastX > oldViewX && Math.abs(lastX - oldViewX) > 300) { // to right
                    if (lastY > oldViewY) { // and top
                        swipeToRightDownCorner(view, velocity);
                    } else if (lastY < oldViewY) { // and bottom
                        swipeToRightUpperCorner(view, velocity);
                    }
                } else if (lastX < oldViewX && Math.abs(lastX - oldViewX) > 300) { // to left
                    if (lastY > oldViewY) { // and  top
                        swipeToLeftDownCorner(view, velocity);
                    } else if (lastY < oldViewY) { // and  bottom
                        swipeToLeftUpperCorner(view, velocity);
                    }
                }

                break;
        }

        return true;
    }

    private static long convertVelocity(double velocity) {
        DecimalFormat df = new DecimalFormat("####0.00");
        long speedLong = (long) velocity;
        return ((10 - speedLong) * 100);
    }

    public void swipeToRightUpperCorner(View playerView, double velocity) {
        Log.d("onFling", "swipeToRightUpperCorner");
        playerView.animate()
                .translationX(maxX / 2 - DISPLAY_X_OFFSET)
                .translationY(-maxY / 2 + DISPLAY_Y_OFFSET)
                .setDuration(convertVelocity(velocity))
                .start();
    }

    public void swipeToRightDownCorner(View playerView, double velocity) {
        Log.d("onFling", "swipeToRightDownCorner");
        playerView.animate()
                .translationX(maxX / 2 - DISPLAY_X_OFFSET)
                .translationY(maxY / 2 - DISPLAY_Y_OFFSET)
                .setDuration(convertVelocity(velocity))
                .start();
    }

    public void swipeToLeftUpperCorner(View playerView, double velocity) {
        Log.d("onFling", "swipeToLeftUpperCorner");
        playerView.animate()
                .translationX(-maxX / 2 + DISPLAY_X_OFFSET)
                .translationY(-maxY / 2 + DISPLAY_Y_OFFSET)
                .setDuration(convertVelocity(velocity))
                .start();
    }

    public void swipeToLeftDownCorner(View playerView, double velocity) {
        Log.d("onFling", "swipeToLeftDownCorner");
        playerView.animate()
                .translationX(-maxX / 2 + DISPLAY_X_OFFSET)
                .translationY(maxY / 2 - DISPLAY_Y_OFFSET)
                .setDuration(convertVelocity(velocity))
                .start();
    }





}

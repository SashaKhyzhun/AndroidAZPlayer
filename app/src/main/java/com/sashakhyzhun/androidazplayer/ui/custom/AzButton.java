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

import com.sashakhyzhun.androidazplayer.util.AnimationHelper;

import static com.sashakhyzhun.androidazplayer.util.Constants.ANDROID_ACTION_BAR_OFFSET_Y;
import static com.sashakhyzhun.androidazplayer.util.DeviceUtil.dimension;

public class AzButton extends View implements View.OnClickListener, View.OnTouchListener {

    private PLAYER_STATE mState = PLAYER_STATE.NORMAL;
    private boolean isFetchingAnimRunning = false;
    private float START_ANGLE_POINT = 90;

    private int angleArch;
    private float oldValueX = 0;
    private float oldValueY = 0;
    private float oldX = 0;
    private float oldY = 0;

    private int prefWidth = 0;
    private int prefPadding = 0;

    private GestureDetector gestureDetector;
    private AnimationHelper animHelper;
    private OnClickListener mListener;
    private MediaPlayer mMediaPlayer;
    private Point trianglePoint;
    private Paint trianglePaint;
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

    @Override
    @SuppressWarnings("SuspiciousNameCombination")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int usableWidth = prefWidth;
        int usableHeight = prefWidth;

        int mRadius = Math.min(usableWidth, usableHeight) / 2;
        int mCircleX = (usableWidth / 2);
        int mCircleY = (usableHeight / 2);
        canvas.drawCircle(mCircleX, mCircleY, mRadius, playerButtonPaint);

        switch (getState()) {
            case NORMAL:
            case PAUSE:
            case COMPLETED:
                int triangleWidth = (usableWidth / 2) - prefPadding;
                trianglePoint.x = mCircleX - (triangleWidth / 2) + prefPadding;
                trianglePoint.y = mCircleY - (triangleWidth / 2);

                Path trianglePath = getInsideTrianglePath(trianglePoint, triangleWidth);
                canvas.drawPath(trianglePath, trianglePaint);

                if (getState() == PLAYER_STATE.PAUSE) {
                    playerRectArch.left = prefPadding / 2;
                    playerRectArch.top = prefPadding / 2;
                    playerRectArch.right = (prefWidth) - prefPadding / 2;
                    playerRectArch.bottom = prefWidth - prefPadding / 2;
                    canvas.drawArc(playerRectArch, START_ANGLE_POINT, angleArch, false, fetchingCircleLineAround);
                }
                break;
            case PLAYING:
                int width = (usableWidth / 3) - prefPadding;
                int x = mCircleX - width;
                int y = mCircleY - width;

                playerRectAngle.top = y;
                playerRectAngle.left = x + (prefPadding / 4);
                playerRectAngle.right = mCircleX - (prefPadding / 2);
                playerRectAngle.bottom = mCircleY + width;
                canvas.drawRect(playerRectAngle, pauseViewInsideRect);

                playerRectAngle.top = y;
                playerRectAngle.left = mCircleX + (prefPadding / 2);
                playerRectAngle.right = mCircleX + (mCircleX - x) - (prefPadding / 4);
                playerRectAngle.bottom = mCircleY + width;
                canvas.drawRect(playerRectAngle, pauseViewInsideRect);

                playerRectArch.left = prefPadding / 2;
                playerRectArch.top = prefPadding / 2;
                playerRectArch.right = prefWidth - prefPadding / 2;
                playerRectArch.bottom = prefWidth - prefPadding / 2;
                canvas.drawArc(playerRectArch, START_ANGLE_POINT, angleArch, false, fetchingCircleLineAround);
                break;
            case FETCHING:
                playerRectArch.left = prefPadding / 2;
                playerRectArch.top = prefPadding / 2;
                playerRectArch.right = prefWidth - prefPadding / 2;
                playerRectArch.bottom = prefWidth - prefPadding / 2;
                canvas.drawArc(playerRectArch, START_ANGLE_POINT, angleArch, false, fetchingCircleLineAround);
                break;
        }


    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            if (mListener != null) {
                mListener.onClick(view);
            }
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldValueX = view.getX() - event.getRawX();
                oldValueY = view.getY() - event.getRawY();
                oldX = view.getX();
                oldY = view.getY();
                timerStart = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                view.animate()
                        .x(event.getRawX() + oldValueX)
                        .y(event.getRawY() + oldValueY)
                        .setDuration(0)
                        .start();

                handleViewOutOfScreenPosition(view);
                break;
            case MotionEvent.ACTION_UP:
                float newX = view.getX();
                float newY = view.getY();

                long timerEnd = System.currentTimeMillis();
                double distance = Math.sqrt((newX - oldX) * (newX - oldX) + (newY - oldY) * (newY - oldY));
                double velocity = (distance / (timerEnd - timerStart));
                boolean offsetX = Math.abs(newX - oldX) > 200;
                boolean offsetY = Math.abs(newY - oldY) > 350;

                if (newX > oldX && offsetX) {
                    handleRightSwipe(newY, offsetY, view, velocity);
                    break;
                } else if (newX < oldX && offsetX) {
                    handleLeftSwipe(newY, offsetY, view, velocity);
                    break;
                } else if (newY < oldY && offsetY) {
                    handleTopSwipe(newX, offsetX, view, velocity);
                    break;
                } else if (newY > oldY && offsetY) {
                    handleBottomSwipe(newX, offsetX, view, velocity);
                    break;
                }


                break;
        }

        return true;
    }

    public void setClickedListener(OnClickListener listener) {
        mListener = listener;
    }

    public void setMediaPlayer(MediaPlayer mp) {
        mMediaPlayer = mp;
    }

    public void setState(PLAYER_STATE state) {
        mState = state;
        switch (state) {
            case FETCHING:
                break;
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
        invalidate();
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
            START_ANGLE_POINT = 0;
            invalidate();
            startPlayingAnim();
        }, 1000);

    }

    public PLAYER_STATE getState() {
        return mState;
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

        animHelper = new AnimationHelper(maxX, maxY);
        gestureDetector = new GestureDetector(context, new SingleTapConfirm());

        setOnClickListener(this);
        setOnTouchListener(this);

        int prefWidthDP = 100;
        prefWidth = (int) dimension(context, prefWidthDP);
        prefPadding = (int) dimension(context, 4);

        setLayoutParams(new LinearLayout.LayoutParams(prefWidthDP, prefWidthDP));
        setPadding(prefPadding, prefPadding, prefPadding, prefPadding);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(10F);
        }

        playerButtonPaint = new Paint();
        playerButtonPaint.setAntiAlias(true);
        playerButtonPaint.setColor(Color.BLACK);

        trianglePaint = new Paint();
        trianglePaint.setColor(Color.WHITE);
        trianglePaint.setStyle(Paint.Style.FILL);
        trianglePoint = new Point();

        pauseViewInsideRect = new Paint();
        pauseViewInsideRect.setColor(Color.GREEN);

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

        fetchingCircleLineAround = new Paint();
        fetchingCircleLineAround.setAntiAlias(true);
        fetchingCircleLineAround.setStyle(Paint.Style.STROKE);
        fetchingCircleLineAround.setStrokeWidth(dimension(context, 2));
        fetchingCircleLineAround.setColor(Color.YELLOW);

        playerRectArch = new RectF(0, 0, 0, 0);
        playerRectAngle = new Rect(0, 0, 0, 0);
        angleArch = 120;
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

    private void handleViewOutOfScreenPosition(View view) {
        if (view.getX() <= 0) view.animate().x(0).start();
        if (view.getY() <= 0) view.animate().y(0).start();
        if (view.getX() >= maxX - view.getWidth()) view.animate().x(maxX - view.getWidth()).start();
        if (view.getY() >= maxY - view.getHeight() - ANDROID_ACTION_BAR_OFFSET_Y)
            view.animate().y(maxY - view.getHeight() - ANDROID_ACTION_BAR_OFFSET_Y).start();
    }

    private void handleRightSwipe(float newY, boolean offsetY, View view, double velocity) {
        if (newY < oldY && offsetY) {
            animHelper.swipeToRightUpperCorner(view, velocity);
        } else if (newY > oldY && offsetY) {
            animHelper.swipeToRightDownCorner(view, velocity);
        } else {
            animHelper.swipeToHorizontalEdge(view, velocity, true);
        }
    }

    private void handleLeftSwipe(float newY, boolean offsetY, View view, double velocity) {
        if (newY < oldY && offsetY) {
            animHelper.swipeToLeftUpperCorner(view, velocity);
        } else if (newY > oldY && offsetY) {
            animHelper.swipeToLeftDownCorner(view, velocity);
        } else {
            animHelper.swipeToHorizontalEdge(view, velocity, false);
        }
    }

    private void handleTopSwipe(float newX, boolean offsetX, View view, double velocity) {
        if (newX > oldX && offsetX) {
            animHelper.swipeToRightUpperCorner(view, velocity);
        } else if (newX < oldX && offsetX) {
            animHelper.swipeToLeftUpperCorner(view, velocity);
        } else {
            animHelper.swipeToVerticalEdge(view, velocity, true);
        }
    }

    private void handleBottomSwipe(float newX, boolean offsetX, View view, double velocity) {
        if (newX > oldX && offsetX) {
            animHelper.swipeToRightDownCorner(view, velocity);
        } else if (newX < oldX && offsetX) {
            animHelper.swipeToLeftDownCorner(view, velocity);
        } else {
            animHelper.swipeToVerticalEdge(view, velocity, false);
        }
    }

    private class SingleTapConfirm extends SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

}

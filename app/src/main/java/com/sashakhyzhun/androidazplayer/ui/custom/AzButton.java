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
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;

import static com.sashakhyzhun.androidazplayer.util.DeviceUtil.dipToPixels;

public class AzButton extends View implements View.OnClickListener, View.OnTouchListener {

    private boolean isFetchingAnimRunning = false;
    private float START_ANGLE_POINT = 90;

    private int directionSOUTH = 1;
    private int directionNORTH = 2;
    private int directionEAST = 3;
    private int directionWEST = 4;

    private int angleArch;
    private float oldValueX = 0;
    private float oldValueY = 0;

    // size of the player
    private int preferedWidth = 0;
    private int _preferedWidthDP = 56;
    private int preferedPadding = 0;

    private BUTTON_STATE mState = BUTTON_STATE.STATE_NORMAL;

    private Point trianglePoint;
    private Paint trianglePaint;
    private GestureDetector gestureDetector;
    private OnClickListener mListener;
    private MediaPlayer mMediaPlayer;
    private Paint paintArch;
    private Paint paintRect;
    private Paint paint;
    private RectF rectArch;
    private Rect rectangle;




    public enum BUTTON_STATE {
        STATE_PLAYING,
        STATE_PAUSE,
        STATE_FETCHING,
        STATE_NORMAL,
        STATE_COMPLETED
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

    //todo: check it
    private void init(Context context, AttributeSet attrs) {
        gestureDetector = new GestureDetector(context, new SingleTapConfirm());
        setOnClickListener(this);
        setOnTouchListener(this);

        preferedWidth = (int) dipToPixels(context, _preferedWidthDP);
        preferedPadding = (int) dipToPixels(context, 4);
        setLayoutParams(new LinearLayout.LayoutParams(_preferedWidthDP, _preferedWidthDP));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(10F);
        }
        setPadding(preferedPadding, preferedPadding, preferedPadding, preferedPadding);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);

        trianglePaint = new Paint();
        trianglePaint.setColor(Color.WHITE);
        trianglePaint.setStyle(Paint.Style.FILL);
        trianglePoint = new Point();

        // create the Paint and set its color
        paintRect = new Paint();
        paintRect.setColor(Color.WHITE);

        rectangle = new Rect(0, 0, 0, 0);

        Paint paintFetching = new Paint();
        paintFetching.setStyle(Paint.Style.STROKE);
        paintFetching.setColor(Color.WHITE);
        paintFetching.setStrokeWidth(dipToPixels(context, 2));


        paintFetching.setAlpha(255);
        paintFetching.setAntiAlias(true);

        RotateAnimation rotateAnim = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);

        rotateAnim.setDuration(1000);
        rotateAnim.setRepeatCount(Animation.INFINITE);
        rotateAnim.setRepeatMode(Animation.RESTART);
        rotateAnim.setInterpolator(new LinearInterpolator());

        paintArch = new Paint();
        paintArch.setAntiAlias(true);
        paintArch.setStyle(Paint.Style.STROKE);
        paintArch.setStrokeWidth(dipToPixels(context, 2));
        paintArch.setColor(Color.WHITE);

        //size 200x200 example
        rectArch = new RectF(0, 0, 0, 0);

        //Initial Angle (optional, it can be zero)
        angleArch = 120;

    }

    @Override //todo: check it
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int usableWidth = preferedWidth;
        int usableHeight = preferedWidth;

        int mRadius = Math.min(usableWidth, usableHeight) / 2;
        int mCircleX = (usableWidth / 2);
        int mCircleY = (usableHeight / 2);

        canvas.drawCircle(mCircleX, mCircleY, mRadius, paint);

        if (getState() == BUTTON_STATE.STATE_NORMAL || getState() == BUTTON_STATE.STATE_PAUSE || getState() == BUTTON_STATE.STATE_COMPLETED) {

            int triangleWidth = (usableWidth / 2) - preferedPadding;

            trianglePoint.x = mCircleX - (triangleWidth / 2) + preferedPadding;
            trianglePoint.y = mCircleY - (triangleWidth / 2);
            Path trianglePath = getInsideTriangleCoordinates(trianglePoint, triangleWidth, directionWEST);

            canvas.drawPath(trianglePath, trianglePaint);

            if (getState() == BUTTON_STATE.STATE_PAUSE) {

                rectArch.left = preferedPadding / 2;
                rectArch.top = preferedPadding / 2;
                rectArch.right = preferedWidth - preferedPadding / 2;
                rectArch.bottom = preferedWidth - preferedPadding / 2;

                canvas.drawArc(rectArch, START_ANGLE_POINT, angleArch, false, paintArch);
            }
        } else if (getState() == BUTTON_STATE.STATE_PLAYING) {

            int width = (usableWidth / 3) - preferedPadding;

            int x = mCircleX - width;
            int y = mCircleY - width;

            rectangle.top = y;
            rectangle.left = x + (preferedPadding / 4);
            rectangle.right = mCircleX - (preferedPadding / 2);
            rectangle.bottom = mCircleY + width;
            canvas.drawRect(rectangle, paintRect);

            //second
            rectangle.top = y;
            rectangle.left = mCircleX + (preferedPadding / 2);
            rectangle.right = mCircleX + (mCircleX - x) - (preferedPadding / 4);
            rectangle.bottom = mCircleY + width;
            canvas.drawRect(rectangle, paintRect);

            rectArch.left = preferedPadding / 2;
            rectArch.top = preferedPadding / 2;
            rectArch.right = preferedWidth - preferedPadding / 2;
            rectArch.bottom = preferedWidth - preferedPadding / 2;

            canvas.drawArc(rectArch, START_ANGLE_POINT, angleArch, false, paintArch);

        } else if (getState() == BUTTON_STATE.STATE_FETCHING) {

            rectArch.left = preferedPadding / 2;
            rectArch.top = preferedPadding / 2;
            rectArch.right = preferedWidth - preferedPadding / 2;
            rectArch.bottom = preferedWidth - preferedPadding / 2;

            canvas.drawArc(rectArch, START_ANGLE_POINT, angleArch, false, paintArch);
        }


    }

    //todo: check it
    private Path getInsideTriangleCoordinates(Point p1, int width, int direction) {
        Point p2 = null, p3 = null;

        if (direction == directionNORTH) {
            p2 = new Point(p1.x + width, p1.y);
            p3 = new Point(p1.x + (width / 2), p1.y - width);
        } else if (direction == directionSOUTH) {
            p2 = new Point(p1.x + width, p1.y);
            p3 = new Point(p1.x + (width / 2), p1.y + width);
        } else if (direction == directionEAST) {
            p2 = new Point(p1.x, p1.y + width);
            p3 = new Point(p1.x - width, p1.y + (width / 2));
        } else if (direction == directionWEST) {
            p2 = new Point(p1.x, p1.y + width);
            p3 = new Point(p1.x + width, p1.y + (width / 2));
        }

        Path path = new Path();
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);

        return path;
    }

    public BUTTON_STATE getState() {
        return mState;
    }

    public void setState(BUTTON_STATE state) {
        mState = state;
        switch (state) {
            case STATE_PLAYING:
                isFetchingAnimRunning = false;
                startPlayingAnim();
                break;
            case STATE_COMPLETED:
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
            if (angleArch >= 350)
                angleArch = 0;
            angleArch += 10;
            if (START_ANGLE_POINT >= 350)
                START_ANGLE_POINT = 0;
            START_ANGLE_POINT += 10;
            invalidate();
            startFetchingAnim();
        }, 50);

    }

    public void startFetching() {
        setState(AzButton.BUTTON_STATE.STATE_FETCHING);
        if (!isFetchingAnimRunning) {
            isFetchingAnimRunning = !isFetchingAnimRunning;
            startFetchingAnim();
        }
    }

    public void startPlayingAnim() {
        if (getState() != BUTTON_STATE.STATE_PLAYING || mMediaPlayer == null) {
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
    public void onClick(View v) { Log.i("AzButton", "onClick"); }

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
        if (gestureDetector.onTouchEvent(event)) {
            if (mListener != null) {
                mListener.onClick(view);
            }
            return true;
        } else {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    oldValueX = view.getX() - event.getRawX();
                    oldValueY = view.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if ((event.getRawY() + oldValueY) < getRootView().getY()
                            ||
                            (event.getRawY() + oldValueY) > (getRootView().getY()
                                                + getRootView().getHeight()
                                                - view.getHeight()
                                                - dipToPixels(view.getContext(), 24))) {
                        return false;
                    }
                    view.animate()
                            .x(event.getRawX() + oldValueX)
                            .y(event.getRawY() + oldValueY)
                            .setDuration(0)
                            .start();

                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("ACTION_UP", "!!!!!");

                    break;
                default:
                    break;
            }
            return false;
        }
    }



}

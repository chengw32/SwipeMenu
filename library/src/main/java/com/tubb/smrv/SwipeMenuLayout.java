package com.tubb.smrv;

import android.content.Context;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;

/**
 * 
 * @author baoyz
 * @date 2014-8-23
 * 
 */
public class SwipeMenuLayout extends FrameLayout {

	private static final int STATE_CLOSE = 0;
	private static final int STATE_OPEN = 1;

	private int mSwipeDirection;

	private View mContentView;
	private SwipeMenuView mMenuView;
	private int mDownX;
	private int state = STATE_CLOSE;
	private GestureDetectorCompat mGestureDetector;
	private OnGestureListener mGestureListener;
	private boolean isFling;
	private ScrollerCompat mOpenScroller;
	private ScrollerCompat mCloseScroller;
	private int mBaseX;
	private Interpolator mCloseInterpolator;
	private Interpolator mOpenInterpolator;
    private OnClickListener onClickListener;
    private ViewConfiguration mViewConfiguration;
	private boolean swipeEnable = true;
    private int mMinFlingDistance = 3; // dip

	public SwipeMenuLayout(View contentView, SwipeMenuView menuView) {
		this(contentView, menuView, null, null);
	}

	public SwipeMenuLayout(View contentView, SwipeMenuView menuView,
						   Interpolator closeInterpolator, Interpolator openInterpolator) {
		super(contentView.getContext());
		mCloseInterpolator = closeInterpolator;
		mOpenInterpolator = openInterpolator;
		mContentView = contentView;
		mMenuView = menuView;
		mMenuView.setLayout(this);
        mViewConfiguration = ViewConfiguration.get(contentView.getContext());
		init();
	}

	 private SwipeMenuLayout(Context context, AttributeSet attrs, int
	 defStyle) {
	    super(context, attrs, defStyle);
	 }

	private SwipeMenuLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private SwipeMenuLayout(Context context) {
		super(context);
	}

	public void setPosition(RecyclerView.ViewHolder vh) {
		mMenuView.setPosition(vh);
	}

	public void setSwipeDirection(int swipeDirection) {
		mSwipeDirection = swipeDirection;
	}

    @SuppressWarnings("deprecation")
    private void init() {
        setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        mGestureListener = new SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                isFling = false;
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {

                if(velocityX > mViewConfiguration.getScaledMinimumFlingVelocity() || velocityY > mViewConfiguration.getScaledMinimumFlingVelocity())
                    isFling = true;
//                Log.e("XX", "isFling:"+isFling);
                return isFling;
            }
        };
        mGestureDetector = new GestureDetectorCompat(getContext(),
                mGestureListener);

        // mScroller = ScrollerCompat.create(getContext(), new
        // BounceInterpolator());
        if (mCloseInterpolator != null) {
            mCloseScroller = ScrollerCompat.create(getContext(),
					mCloseInterpolator);
        } else {
            mCloseScroller = ScrollerCompat.create(getContext());
        }
        if (mOpenInterpolator != null) {
            mOpenScroller = ScrollerCompat.create(getContext(),
					mOpenInterpolator);
        } else {
            mOpenScroller = ScrollerCompat.create(getContext());
        }

        LayoutParams contentParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mContentView.setLayoutParams(contentParams);
        if (mContentView.getId() < 1) {
            mContentView.setId(R.id.sm_content_view_id);
        }

        mMenuView.setId(R.id.sm_menu_view_id);
        mMenuView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.MATCH_PARENT));

        addView(mContentView);
        addView(mMenuView);

		// in android 2.x, MenuView height is MATCH_PARENT is not work.

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						setMenuHeight(mContentView.getHeight());
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
							getViewTreeObserver().removeOnGlobalLayoutListener(this);
						else
							getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}
				});
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}

    public boolean onSwipe(MotionEvent event) {
		mGestureDetector.onTouchEvent(event);
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mDownX = (int) event.getX();
			isFling = false;
			break;
		case MotionEvent.ACTION_MOVE:
			// Log.i("byz", "downX = " + mDownX + ", moveX = " + event.getX());
			int dis = (int) (mDownX - event.getX());
			if (state == STATE_OPEN) {
				dis += mMenuView.getWidth() * mSwipeDirection;;
			}
			swipe(dis);
			break;
		case MotionEvent.ACTION_UP:
			if ((isFling || Math.abs(mDownX - event.getX()) > (mMenuView.getWidth() / 3)) &&
					Math.signum(mDownX - event.getX()) == mSwipeDirection) {
				// open
//                Log.e("XX", "open");
				smoothOpenMenu();
			} else {
				// close
//                Log.e("XX", "close");
				smoothCloseMenu();
				return false;
			}
			break;
		}
		return true;
	}

	public boolean isOpen() {
		return state == STATE_OPEN;
	}

	private void swipe(int dis) {
		if (Math.signum(dis) != mSwipeDirection) {
			dis = 0;
		} else if (Math.abs(dis) > mMenuView.getWidth()) {
			dis = mMenuView.getWidth() * mSwipeDirection;
            state = STATE_OPEN;
		}

		mContentView.layout(-dis, mContentView.getTop(),
				mContentView.getWidth() -dis, getMeasuredHeight());

		if (mSwipeDirection == SwipeMenuRecyclerView.DIRECTION_LEFT) {

			mMenuView.layout(mContentView.getWidth() - dis, mMenuView.getTop(),
					mContentView.getWidth() + mMenuView.getWidth() - dis,
					mMenuView.getBottom());
		} else {
			mMenuView.layout(-mMenuView.getWidth() - dis, mMenuView.getTop(),
					- dis, mMenuView.getBottom());
		}
	}

	@Override
	public void computeScroll() {
		if (state == STATE_OPEN) {
			if (mOpenScroller.computeScrollOffset()) {
				swipe(mOpenScroller.getCurrX()*mSwipeDirection);
				postInvalidate();
			}
		} else {
			if (mCloseScroller.computeScrollOffset()) {
				swipe((mBaseX - mCloseScroller.getCurrX())*mSwipeDirection);
				postInvalidate();
			}
		}
	}

	public void smoothCloseMenu() {
		state = STATE_CLOSE;
		if (mSwipeDirection == SwipeMenuRecyclerView.DIRECTION_LEFT) {
			mBaseX = -mContentView.getLeft();

			mCloseScroller.startScroll(0, 0, mMenuView.getWidth(), 0, 500);
		} else {
			mBaseX = mMenuView.getRight();
			mCloseScroller.startScroll(0, 0, mMenuView.getWidth(), 0, 500);
		}
		postInvalidate();
	}

	public void smoothOpenMenu() {
		state = STATE_OPEN;
		if (mSwipeDirection == SwipeMenuRecyclerView.DIRECTION_LEFT) {
			mOpenScroller.startScroll(-mContentView.getLeft(), 0, mMenuView.getWidth(), 0, 500);
		} else {
			mOpenScroller.startScroll(mContentView.getLeft(), 0, mMenuView.getWidth(), 0, 500);
		}
		postInvalidate();
	}

	public void closeMenu() {
		if (mCloseScroller.computeScrollOffset()) {
			mCloseScroller.abortAnimation();
		}
		if (state == STATE_OPEN) {
			state = STATE_CLOSE;
			swipe(0);
		}
	}

	public void openMenu() {
		if (state == STATE_CLOSE) {
			state = STATE_OPEN;
			swipe(mMenuView.getWidth()*mSwipeDirection);
		}
	}

	public View getContentView() {
		return mContentView;
	}

	public SwipeMenuView getMenuView() {
		return mMenuView;
	}

	private int dp2px(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				getContext().getResources().getDisplayMetrics());
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		mMenuView.measure(MeasureSpec.makeMeasureSpec(0,
				MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(
				getMeasuredHeight(), MeasureSpec.EXACTLY));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		mContentView.layout(0, 0, getMeasuredWidth(),
                mContentView.getMeasuredHeight());
		if (mSwipeDirection == SwipeMenuRecyclerView.DIRECTION_LEFT) {
			mMenuView.layout(getMeasuredWidth(), 0,
					getMeasuredWidth() + mMenuView.getMeasuredWidth(),
					mContentView.getMeasuredHeight());
		} else {
			mMenuView.layout(-mMenuView.getMeasuredWidth(), 0,
					0, mContentView.getMeasuredHeight());
		}
	}

	public void setMenuHeight(int measuredHeight) {
		LayoutParams params = (LayoutParams) mMenuView.getLayoutParams();
		if (params.height != measuredHeight) {
			params.height = measuredHeight;
			mMenuView.setLayoutParams(mMenuView.getLayoutParams());
		}
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		super.setOnClickListener(l);
        onClickListener = l;
	}

    public OnClickListener getOnClickListener() {
        return onClickListener;
    }

    public void setSwipeEnable(boolean swipeEnable) {
        this.swipeEnable = swipeEnable;
    }

    public boolean isSwipeEnable() {
        return swipeEnable;
    }
}

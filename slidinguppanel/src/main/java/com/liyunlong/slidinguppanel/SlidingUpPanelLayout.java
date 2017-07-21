package com.liyunlong.slidinguppanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 滑动面板视图
 * <ul>
 * <strong>使用方法：</strong>
 * <li>使用{@link SlidingUpPanelLayout}作为滑动面板视图布局的根元素
 * <li>该布局只能有两个元素，第一个子视图为主视图，第二个视图为滑动面板
 * <li>该布局可以通过{@code android:gravity}属性或{@link #setGravity(int)}方法设置为{@code top}或{@code bottom}
 * <li>主视图的宽度和高度应该设置为{@code match_parent}
 * <li>滑动布局的宽度应该设置为{@code match_parent}，高度应该设置为{@code match_parent}或{@code wrap_content}或最大理想高度
 * <li>滑动布局的高度如果想要设置为屏幕的百分比，可以将高度设置为{@code match_parent}并指定{@code layout_weight}属性，有效值为(0,1)
 * <li>该布局可以通过{@code umanoDragView}属性或{@link #setDragView(View)}方法指定一个特定视图来限制拖动区域
 * <li>默认情况下，整个滑动面板将充当拖动区域并拦截拦截和拖动事件
 * </ul>
 *
 * @author liyunlong
 * @date 2017/7/20 10:41
 */
public class SlidingUpPanelLayout extends ViewGroup {

    private static final String TAG = SlidingUpPanelLayout.class.getSimpleName();

    /**
     * 默认未定义的值
     */
    private static final int DEFAULT_UNDEFINED = -1; // dp;
    /**
     * 默认滑动面板高度(单位：dp)
     */
    private static final int DEFAULT_PANEL_HEIGHT = 68; // dp;

    /**
     * 默认在滑动时面板可以停止的锚点
     */
    private static final float DEFAULT_ANCHOR_POINT = 1.0f; // In relative %

    /**
     * 默认阴影的高度(单位：dp)
     */
    private static final int DEFAULT_SHADOW_HEIGHT = 4; // dp;
    /**
     * 默认主视图视差偏移量
     */
    private static final int DEFAULT_PARALLAX_OFFSET = 0;
    /**
     * 默认蒙层颜色(80%灰色)
     */
    private static final int DEFAULT_FADE_COLOR = 0x99000000;

    /**
     * 默认最低快速滑动的阀值
     */
    private static final int DEFAULT_MIN_FLING_VELOCITY = 400; // dips per second
    /**
     * 默认是否在mMainview(主视图)上加一层蒙层
     */
    private static final boolean DEFAULT_OVERLAY_FLAG = false;
    /**
     * Default is set to true for clip panel for performance reasons
     */
    private static final boolean DEFAULT_CLIP_PANEL_FLAG = true;
    /**
     * 用于在Bundle中存储滑动状态的标记
     */
    public static final String SLIDING_STATE = "slidingState";
    /**
     * 用于在Bundle中存储父布局状态的标记
     */
    public static final String SUPER_STATE = "superState";
    /**
     * 默认组件初始状态
     */
    private static final PanelState DEFAULT_SLIDE_STATE = PanelState.COLLAPSED;
    /**
     * 滑动面板的高度(单位：px)
     */
    private int mPanelHeight = DEFAULT_UNDEFINED;
    /**
     * 最低快速滑动的阀值
     */
    private int mMinFlingVelocity = DEFAULT_MIN_FLING_VELOCITY;
    /**
     * 蒙层颜色
     */
    private int mCoveredFadeColor = DEFAULT_FADE_COLOR;
    /**
     * 用于滑动时主视图蒙层的画笔
     */
    private final Paint mCoveredFadePaint = new Paint();
    /**
     * 用于绘制阴影的Drawable
     */
    private final Drawable mShadowDrawable;
    /**
     * 阴影的高度(单位：px)
     */
    private int mShadowHeight = DEFAULT_UNDEFINED;
    /**
     * 主视图视差偏移量
     */
    private int mParallaxOffset = DEFAULT_UNDEFINED;
    /**
     * 设置面板是否覆盖内容(默认为false)
     */
    private boolean mOverlayContent = DEFAULT_OVERLAY_FLAG;
    /**
     * 滑动面板是否向上滑动为展开
     */
    private boolean mIsSlidingUp;
    /**
     * 主视图是否被裁剪到滑动面板的顶部
     */
    private boolean mClipPanel = DEFAULT_CLIP_PANEL_FLAG;
    /**
     * 可以滑动的子视图(可以没有)
     */
    private View mSlideableView;
    /**
     * 主视图(一般是第一个索引的子视图)
     */
    private View mMainView;
    /**
     * 拖动视图(如果有则Panel只能通过这个视图拖动，否则整个Panel可用于拖动)
     */
    private View mDragView;
    /**
     * mDragView的资源ID(如果有则Panel只能通过这个视图拖动，否则整个Panel可用于拖动)
     */
    private int mDragViewResId = DEFAULT_UNDEFINED;
    /**
     * 滚动视图(如果设置则滚动将在面板和视图之间在必要时进行转换)
     */
    private View mScrollableView;
    /**
     * mScrollableView的资源ID(如果设置则滚动将在面板和视图之间在必要时进行转换)
     */
    private int mScrollableViewResId = DEFAULT_UNDEFINED;
    /**
     * 确定滚动视图当前的滚动位置辅助类
     */
    private ScrollableViewHelper mScrollableViewHelper = new ScrollableViewHelper();
    /**
     * 当前滑动状态
     */
    private PanelState mSlideState = DEFAULT_SLIDE_STATE;
    /**
     * 如果当前滑动状态是{@link PanelState#DRAGGING}，则将存储最后一个非拖动状态
     */
    private PanelState mLastNotDraggingSlideState = DEFAULT_SLIDE_STATE;
    /**
     * 滑动面板从展开位置的偏移量(有效值范围[0,1])
     * <br>0表示折叠状态，1表示展开状态
     */
    private float mSlideOffset;
    /**
     * 滑动面板可以滑动的距离(单位：px)
     */
    private int mSlideRange;
    /**
     * 在滑动时面板可以停止的锚点(有效值范围[0,1])
     */
    private float mAnchorPoint = 1.f;
    /**
     * 是否不能继续拖动(面板视图被锁定到内部滚动或防止拖动的另一个条件)
     */
    private boolean mIsUnableToDrag;
    /**
     * 是否启用滑动功能的标志
     */
    private boolean mIsTouchEnabled;

    private float mPrevMotionY;
    private float mInitialMotionX;
    private float mInitialMotionY;
    /**
     * 是否正在处理滚动视图的触摸事件
     */
    private boolean mIsScrollableViewHandlingTouch = false;
    /**
     * 蒙层点击事件监听
     */
    private OnClickListener mFadeOnClickListener;
    /**
     * Panel滑动动作监听集合
     */
    private final List<PanelSlideListener> mPanelSlideListeners = new CopyOnWriteArrayList<>();
    /**
     * 用于处理滑动的细节的辅助类
     */
    private final ViewDragHelper mDragHelper;
    /**
     * Stores whether or not the pane was expanded the last time it was slideable.
     * If expand/collapse operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    private boolean mFirstLayout = true;
    /**
     * 绘制主视图和蒙层的矩形区域
     */
    private final Rect mTmpRect = new Rect();

    /**
     * 默认定义要解析的属性
     */
    private static final int[] DEFAULT_ATTRS = new int[]{
            android.R.attr.gravity
    };

    /**
     * 滑动面板状态
     */
    public enum PanelState {
        /**
         * 展开
         */
        EXPANDED,
        /**
         * 折叠(默认状态)
         */
        COLLAPSED,
        /**
         * 锚点
         */
        ANCHORED,
        /**
         * 隐藏
         */
        HIDDEN,
        /**
         * 拖动
         */
        DRAGGING
    }

    /**
     * Panel滑动事件监听器
     */
    public interface PanelSlideListener {
        /**
         * 当滑动面板的位置改变(即产生有效的滑动距离)时调用该方法
         *
         * @param panel       被移动的子视图(滑动面板)
         * @param slideOffset 滑动面板在其范围内的偏移量(有效值范围[0,1])
         */
        void onPanelSlide(View panel, float slideOffset);

        /**
         * 当滑动面板状态改变时调用
         *
         * @param panel         被移动的子视图(滑动面板)
         * @param previousState 之前的状态
         * @param cunrentState  当前状态
         */
        void onPanelStateChanged(View panel, PanelState previousState, PanelState cunrentState);
    }

    /**
     * Panel滑动事件监听器的空实现(如果只需要监听一个方法可以使用这个监听器)
     */
    public static class SimplePanelSlideListener implements PanelSlideListener {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
        }

        @Override
        public void onPanelStateChanged(View panel, PanelState previousState, PanelState cunrentState) {
        }
    }

    public SlidingUpPanelLayout(Context context) {
        this(context, null);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // 兼容一些Android提供的可视化工具做的处理
//        if (isInEditMode()) {
//            mShadowDrawable = null;
//            mDragHelper = null;
//            return;
//        }

        Interpolator scrollerInterpolator = null;
        if (attrs != null) {
            // 解析系统属性
            TypedArray defAttrs = context.obtainStyledAttributes(attrs, DEFAULT_ATTRS);
            if (defAttrs != null) {
                int gravity = defAttrs.getInt(0, Gravity.BOTTOM);
                setGravity(gravity);
                defAttrs.recycle();
            }

            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SlidingUpPanelLayout);
            // 解析自定义的属性
            if (typedArray != null) {
                mPanelHeight = typedArray.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_umanoPanelHeight, DEFAULT_UNDEFINED);
                mShadowHeight = typedArray.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_umanoShadowHeight, DEFAULT_UNDEFINED);
                mParallaxOffset = typedArray.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_umanoParallaxOffset, DEFAULT_UNDEFINED);

                mMinFlingVelocity = typedArray.getInt(R.styleable.SlidingUpPanelLayout_umanoFlingVelocity, DEFAULT_MIN_FLING_VELOCITY);
                mCoveredFadeColor = typedArray.getColor(R.styleable.SlidingUpPanelLayout_umanoFadeColor, DEFAULT_FADE_COLOR);

                mDragViewResId = typedArray.getResourceId(R.styleable.SlidingUpPanelLayout_umanoDragView, DEFAULT_UNDEFINED);
                mScrollableViewResId = typedArray.getResourceId(R.styleable.SlidingUpPanelLayout_umanoScrollableView, DEFAULT_UNDEFINED);

                mOverlayContent = typedArray.getBoolean(R.styleable.SlidingUpPanelLayout_umanoOverlay, DEFAULT_OVERLAY_FLAG);
                mClipPanel = typedArray.getBoolean(R.styleable.SlidingUpPanelLayout_umanoClipPanel, DEFAULT_CLIP_PANEL_FLAG);

                mAnchorPoint = typedArray.getFloat(R.styleable.SlidingUpPanelLayout_umanoAnchorPoint, DEFAULT_ANCHOR_POINT);

                mSlideState = PanelState.values()[typedArray.getInt(R.styleable.SlidingUpPanelLayout_umanoInitialState, DEFAULT_SLIDE_STATE.ordinal())];

                int interpolatorResId = typedArray.getResourceId(R.styleable.SlidingUpPanelLayout_umanoScrollInterpolator, DEFAULT_UNDEFINED);
                if (interpolatorResId != DEFAULT_UNDEFINED) {
                    scrollerInterpolator = AnimationUtils.loadInterpolator(context, interpolatorResId);
                }
                typedArray.recycle();
            }
        }
        // 若在xml未定义某些属性，会在此初始化值
        final float density = context.getResources().getDisplayMetrics().density;
        if (mPanelHeight == DEFAULT_UNDEFINED) {
            mPanelHeight = (int) (DEFAULT_PANEL_HEIGHT * density + 0.5f);
        }
        if (mShadowHeight == DEFAULT_UNDEFINED) {
            mShadowHeight = (int) (DEFAULT_SHADOW_HEIGHT * density + 0.5f);
        }
        if (mParallaxOffset == DEFAULT_UNDEFINED) {
            mParallaxOffset = (int) (DEFAULT_PARALLAX_OFFSET * density);
        }
        // 如果阴影的高度为0则不绘制阴影
        if (mShadowHeight > 0) {
            if (mIsSlidingUp) {
                mShadowDrawable = getResources().getDrawable(R.drawable.above_shadow);
            } else {
                mShadowDrawable = getResources().getDrawable(R.drawable.below_shadow);
            }
        } else {
            mShadowDrawable = null;
        }

        setWillNotDraw(false);

        // 用来处理滑动的工具类
        mDragHelper = ViewDragHelper.create(this, 0.5f, scrollerInterpolator, new DragHelperCallback());
        mDragHelper.setMinVelocity(mMinFlingVelocity * density);

        mIsTouchEnabled = true;
    }

    /**
     * 当View中所有的子控件均被加载完成后调用该方法(用来初始化)
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mDragViewResId != DEFAULT_UNDEFINED) {
            setDragView(findViewById(mDragViewResId));
        }
        if (mScrollableViewResId != DEFAULT_UNDEFINED) {
            setScrollableView(findViewById(mScrollableViewResId));
        }
    }

    /**
     * 设置滑动面板的位置
     *
     * @param gravity 滑动面板的位置(有效值：{@link Gravity#TOP}或{@link Gravity#BOTTOM})
     */
    public void setGravity(int gravity) {
        if (gravity != Gravity.TOP && gravity != Gravity.BOTTOM) {
            throw new IllegalArgumentException("gravity must be set to either top or bottom");
        }
        mIsSlidingUp = gravity == Gravity.BOTTOM;
        if (!mFirstLayout) {
            requestLayout();
        }
    }

    /**
     * 设置蒙层颜色
     *
     * @param color 蒙层颜色
     */
    public void setCoveredFadeColor(int color) {
        mCoveredFadeColor = color;
        requestLayout();
    }

    /**
     * 返回蒙层颜色
     */
    public int getCoveredFadeColor() {
        return mCoveredFadeColor;
    }

    /**
     * 设置是否启用滑动功能
     *
     * @param enabled 是否启用滑动功能
     */
    public void setTouchEnabled(boolean enabled) {
        mIsTouchEnabled = enabled;
    }

    /**
     * 返回是否启用滑动功能
     */
    public boolean isTouchEnabled() {
        return mIsTouchEnabled && mSlideableView != null && mSlideState != PanelState.HIDDEN;
    }

    /**
     * 返回滑动面板的高度(单位：px)
     */
    public int getPanelHeight() {
        return mPanelHeight;
    }

    /**
     * 设置滑动面板的高度
     *
     * @param height 滑动面板的高度(单位：px)
     */
    public void setPanelHeight(int height) {
        if (getPanelHeight() == height) {
            return;
        }
        mPanelHeight = height;
        if (!mFirstLayout) {
            requestLayout();
        }
        if (getPanelState() == PanelState.COLLAPSED) {
            smoothToBottom();
            invalidate();
            return;
        }
    }

    /**
     * 返回阴影高度(单位：px)
     */
    public int getShadowHeight() {
        return mShadowHeight;
    }

    /**
     * 设置阴影高度
     *
     * @param height 阴影高度(单位：px)
     */
    public void setShadowHeight(int height) {
        mShadowHeight = height;
        if (!mFirstLayout) {
            invalidate();
        }
    }

    /**
     * 返回主视图的视差偏移量(单位：px)
     */
    public int getCurrentParallaxOffset() {
        // Clamp slide offset at zero for parallax computation;
        int offset = (int) (mParallaxOffset * Math.max(mSlideOffset, 0));
        return mIsSlidingUp ? -offset : offset;
    }

    /**
     * 设置主视图的视差偏移量
     *
     * @param offset 主视图的视差偏移量(单位：px)
     */
    public void setParallaxOffset(int offset) {
        mParallaxOffset = offset;
        if (!mFirstLayout) {
            requestLayout();
        }
    }

    /**
     * 返回最低快速滑动的阈值(默认为{@link #DEFAULT_MIN_FLING_VELOCITY})
     */
    public int getMinFlingVelocity() {
        return mMinFlingVelocity;
    }

    /**
     * 设置最低快速滑动的阈值(默认为{@link #DEFAULT_MIN_FLING_VELOCITY})
     *
     * @param velocity 最低快速滑动的阈值
     */
    public void setMinFlingVelocity(int velocity) {
        mMinFlingVelocity = velocity;
    }

    /**
     * 添加滑动面板滑动事件监听
     *
     * @param listener 滑动面板滑动事件监听
     */
    public void addPanelSlideListener(PanelSlideListener listener) {
        synchronized (mPanelSlideListeners) {
            mPanelSlideListeners.add(listener);
        }
    }

    /**
     * 移除滑动面板滑动事件监听
     *
     * @param listener 滑动面板滑动事件监听
     */
    public void removePanelSlideListener(PanelSlideListener listener) {
        synchronized (mPanelSlideListeners) {
            mPanelSlideListeners.remove(listener);
        }
    }

    /**
     * 主视图蒙层点击事件监听(滑动面板处于折叠或隐藏状态时不会触发改监听，如果没有设置则将点击事件传递到主视图)
     *
     * @param listener 蒙层点击事件监听
     */
    public void setFadeOnClickListener(OnClickListener listener) {
        mFadeOnClickListener = listener;
    }

    /**
     * 设置用来拖动的视图(如果未设置则整个滑动面板都可以响应拖动)
     *
     * @param dragView 用来拖动的视图
     */
    public void setDragView(View dragView) {
        if (mDragView != null) {
            mDragView.setOnClickListener(null);
        }
        mDragView = dragView;
        if (mDragView != null) {
            mDragView.setClickable(true);
            mDragView.setFocusable(false);
            mDragView.setFocusableInTouchMode(false);
            mDragView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isEnabled() || !isTouchEnabled()) return;
                    if (mSlideState != PanelState.EXPANDED && mSlideState != PanelState.ANCHORED) {
                        if (mAnchorPoint < 1.0f) {
                            setPanelState(PanelState.ANCHORED);
                        } else {
                            setPanelState(PanelState.EXPANDED);
                        }
                    } else {
                        setPanelState(PanelState.COLLAPSED);
                    }
                }
            });
        }
    }

    /**
     * 设置用来拖动的视图的资源ID(如果未设置则整个滑动面板都可以响应拖动)
     *
     * @param dragViewResId 用来拖动的视图的资源ID
     */
    public void setDragView(int dragViewResId) {
        mDragViewResId = dragViewResId;
        setDragView(findViewById(dragViewResId));
    }

    /**
     * 设置滑动布局中可滚动的View(如果设置则滚动将在面板和视图之间在必要时进行转换)
     *
     * @param scrollableView 可滚动的View
     */
    public void setScrollableView(View scrollableView) {
        mScrollableView = scrollableView;
    }

    /**
     * 设置确定当前滚动视图的滚动位置的辅助类
     *
     * @param helper 辅助类对象
     * @see ScrollableViewHelper
     */
    public void setScrollableViewHelper(ScrollableViewHelper helper) {
        mScrollableViewHelper = helper;
    }

    /**
     * 设置在滑动时面板可以停止的锚点(有效值范围[0,1])
     *
     * @param anchorPoint 在滑动时面板可以停止的锚点(有效值范围[0,1]，确定锚点从布局顶部开始的位置)
     */
    public void setAnchorPoint(float anchorPoint) {
        if (anchorPoint > 0 && anchorPoint <= 1) {
            mAnchorPoint = anchorPoint;
            mFirstLayout = true;
            requestLayout();
        }
    }

    /**
     * 返回在滑动时面板可以停止的锚点(有效值范围[0,1])
     */
    public float getAnchorPoint() {
        return mAnchorPoint;
    }

    /**
     * 设置面板是否覆盖内容
     *
     * @param overlayed 面板是否覆盖内容(默认为false)
     */
    public void setOverlayed(boolean overlayed) {
        mOverlayContent = overlayed;
    }

    /**
     * 返回面板是否覆盖内容(默认为false)
     */
    public boolean isOverlayed() {
        return mOverlayContent;
    }

    /**
     * 设置主视图是否被裁剪到滑动面板的顶部
     *
     * @param clip 主视图是否被裁剪到滑动面板的顶部
     */
    public void setClipPanel(boolean clip) {
        mClipPanel = clip;
    }

    /**
     * 返回主视图是否被裁剪到滑动面板的顶部
     */
    public boolean isClipPanel() {
        return mClipPanel;
    }

    protected void smoothToBottom() {
        smoothSlideTo(0, 0);
    }

    /**
     * 分发滑动面板的位置改变事件
     *
     * @param panel 滑动面板
     */
    void dispatchOnPanelSlide(View panel) {
        synchronized (mPanelSlideListeners) {
            for (PanelSlideListener listener : mPanelSlideListeners) {
                listener.onPanelSlide(panel, mSlideOffset);
            }
        }
    }

    /**
     * 分发滑动面板的状态改变事件
     *
     * @param panel         滑动面板
     * @param previousState 之前的状态
     * @param cunrentState  当前状态
     */
    void dispatchOnPanelStateChanged(View panel, PanelState previousState, PanelState cunrentState) {
        synchronized (mPanelSlideListeners) {
            for (PanelSlideListener listener : mPanelSlideListeners) {
                listener.onPanelStateChanged(panel, previousState, cunrentState);
            }
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    /**
     * 根据当前的view的位置判断是显示还是隐藏
     */
    void updateObscuredViewVisibility() {
        if (getChildCount() == 0) {
            return;
        }
        final int leftBound = getPaddingLeft();
        final int rightBound = getWidth() - getPaddingRight();
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - getPaddingBottom();
        final int left;
        final int right;
        final int top;
        final int bottom;
        if (mSlideableView != null && hasOpaqueBackground(mSlideableView)) {
            left = mSlideableView.getLeft();
            right = mSlideableView.getRight();
            top = mSlideableView.getTop();
            bottom = mSlideableView.getBottom();
        } else {
            left = right = top = bottom = 0;
        }
        View child = getChildAt(0);
        final int clampedChildLeft = Math.max(leftBound, child.getLeft());
        final int clampedChildTop = Math.max(topBound, child.getTop());
        final int clampedChildRight = Math.min(rightBound, child.getRight());
        final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
        final int visibility;
        if (clampedChildLeft >= left && clampedChildTop >= top &&
                clampedChildRight <= right && clampedChildBottom <= bottom) {
            visibility = INVISIBLE;
        } else {
            visibility = VISIBLE;
        }
        child.setVisibility(visibility);
    }

    /**
     * 设置所有子视图为可见状态
     */
    void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
        }
    }

    /**
     * 判断指定View背景是否透明
     *
     * @param view View对象
     */
    private static boolean hasOpaqueBackground(View view) {
        final Drawable bg = view.getBackground();
        return bg != null && bg.getOpacity() == PixelFormat.OPAQUE;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY && widthMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
        } else if (heightMode != MeasureSpec.EXACTLY && heightMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT");
        }

        final int childCount = getChildCount();

        if (childCount != 2) { // 如果子视图的数量大于2，则抛出异常
            throw new IllegalStateException("Sliding up panel layout must have exactly 2 children!");
        }

        mMainView = getChildAt(0);// 设置第一个子视图为主视图
        mSlideableView = getChildAt(1);// 设置第二个子视图为可滑动的视图
        if (mDragView == null) { // 如果可拖动的视图为空，则将可滑动的视图设置为可拖动的视图
            setDragView(mSlideableView);
        }

        // 如果滑动面板不可见，则将整个视图置于隐藏状态
        if (mSlideableView.getVisibility() != VISIBLE) {
            mSlideState = PanelState.HIDDEN;
        }

        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();
        int layoutWidth = widthSize - getPaddingLeft() - getPaddingRight();

        // First pass. Measure based on child LayoutParams width/height.
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            // We always measure the sliding panel in order to know it's height (needed for show panel)
            if (child.getVisibility() == GONE && i == 0) {
                continue;
            }

            int height = layoutHeight;
            int width = layoutWidth;
            if (child == mMainView) {
                if (!mOverlayContent && mSlideState != PanelState.HIDDEN) {
                    height -= mPanelHeight;
                }

                width -= lp.leftMargin + lp.rightMargin;
            } else if (child == mSlideableView) {
                // The slideable view should be aware of its top margin.
                // See https://github.com/umano/AndroidSlidingUpPanel/issues/412.
                height -= lp.topMargin;
            }

            // 子视图测量
            int childWidthSpec;
            if (lp.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
            } else if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
            } else {
                // Modify the height based on the weight.
                if (lp.weight > 0 && lp.weight < 1) {
                    height = (int) (height * lp.weight);
                } else if (lp.height != LayoutParams.MATCH_PARENT) {
                    height = lp.height;
                }
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            }
            //子视图measure调用
            child.measure(childWidthSpec, childHeightSpec);

            if (child == mSlideableView) {
                mSlideRange = mSlideableView.getMeasuredHeight() - mPanelHeight;
            }
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();

        final int childCount = getChildCount();

        // 根据当前mSlideState，初始化mSlideOffset值
        if (mFirstLayout) {
            switch (mSlideState) {
                case EXPANDED:
                    mSlideOffset = 1.0f;
                    break;
                case ANCHORED:
                    mSlideOffset = mAnchorPoint;
                    break;
                case HIDDEN:
                    int newTop = computePanelTopPosition(0.0f) + (mIsSlidingUp ? +mPanelHeight : -mPanelHeight);
                    mSlideOffset = computeSlideOffset(newTop);
                    break;
                default:
                    mSlideOffset = 0.f;
                    break;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            // Always layout the sliding view on the first layout
            if (child.getVisibility() == GONE && (i == 0 || mFirstLayout)) {
                continue;
            }

            final int childHeight = child.getMeasuredHeight();
            //计算childTop的值 ，这里mSlideOffset是可变因子
            int childTop = paddingTop;
            if (child == mSlideableView) {
                childTop = computePanelTopPosition(mSlideOffset);
            }

            if (!mIsSlidingUp) {
                if (child == mMainView && !mOverlayContent) {
                    childTop = computePanelTopPosition(mSlideOffset) + mSlideableView.getMeasuredHeight();
                }
            }
            final int childBottom = childTop + childHeight;
            final int childLeft = paddingLeft + lp.leftMargin;
            final int childRight = childLeft + child.getMeasuredWidth();
            //完成子视图的layout
            child.layout(childLeft, childTop, childRight, childBottom);
        }

        if (mFirstLayout) {
            updateObscuredViewVisibility();
        }
        applyParallaxForCurrentSlideOffset();

        mFirstLayout = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Recalculate sliding panes and their details
        // 重新计算滑动面板及其细节
        if (h != oldh) {
            mFirstLayout = true;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // If the scrollable view is handling touch, never intercept
        if (mIsScrollableViewHandlingTouch || !isTouchEnabled()) {
            mDragHelper.abort(); // 清空滑动状态
            return false;
        }

        final int action = MotionEventCompat.getActionMasked(ev);
        final float x = ev.getX();
        final float y = ev.getY();
        final float adx = Math.abs(x - mInitialMotionX);
        final float ady = Math.abs(y - mInitialMotionY);
        final int dragSlop = mDragHelper.getTouchSlop();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsUnableToDrag = false;
                mInitialMotionX = x;
                mInitialMotionY = y;
                if (!isViewUnder(mDragView, (int) x, (int) y)) {
                    mDragHelper.cancel(); // 清空滑动状态
                    mIsUnableToDrag = true;
                    return false;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (ady > dragSlop && adx > ady) {
                    mDragHelper.cancel(); // 清空滑动状态
                    mIsUnableToDrag = true;
                    return false;
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // If the dragView is still dragging when we get here, we need to call processTouchEvent
                // so that the view is settled
                // Added to make scrollable views work (tokudu)
                if (mDragHelper.isDragging()) {
                    mDragHelper.processTouchEvent(ev);
                    return true;
                }
                // Check if this was a click on the faded part of the screen, and fire off the listener if there is one.
                if (ady <= dragSlop
                        && adx <= dragSlop
                        && mSlideOffset > 0
                        && !isViewUnder(mSlideableView, (int) mInitialMotionX, (int) mInitialMotionY)
                        && mFadeOnClickListener != null) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK);
                    mFadeOnClickListener.onClick(this);
                    return true;
                }
                break;
        }
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled() || !isTouchEnabled()) {
            return super.onTouchEvent(ev);
        }
        try {
            // 具体的滑动计算处理
            mDragHelper.processTouchEvent(ev);
            return true;
        } catch (Exception ex) {
            // Ignore the pointer out of range exception
            return false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (!isEnabled() || !isTouchEnabled() || (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
            mDragHelper.abort();
            return super.dispatchTouchEvent(ev);
        }

        final float y = ev.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            mIsScrollableViewHandlingTouch = false;
            mPrevMotionY = y;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dy = y - mPrevMotionY;
            mPrevMotionY = y;

            // If the scroll view isn't under the touch, pass the
            // event along to the dragView.
            if (!isViewUnder(mScrollableView, (int) mInitialMotionX, (int) mInitialMotionY)) {
                return super.dispatchTouchEvent(ev);
            }

            // Which direction (up or down) is the drag moving?
            if (dy * (mIsSlidingUp ? 1 : -1) > 0) { // Collapsing
                // Is the child less than fully scrolled?
                // Then let the child handle it.
                if (mScrollableViewHelper.getScrollableViewScrollPosition(mScrollableView, mIsSlidingUp) > 0) {
                    mIsScrollableViewHandlingTouch = true;
                    return super.dispatchTouchEvent(ev);
                }

                // Was the child handling the touch previously?
                // Then we need to rejigger things so that the
                // drag panel gets a proper down event.
                if (mIsScrollableViewHandlingTouch) {
                    // Send an 'UP' event to the child.
                    MotionEvent up = MotionEvent.obtain(ev);
                    up.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(up);
                    up.recycle();

                    // Send a 'DOWN' event to the panel. (We'll cheat
                    // and hijack this one)
                    ev.setAction(MotionEvent.ACTION_DOWN);
                }

                mIsScrollableViewHandlingTouch = false;
                return this.onTouchEvent(ev);
            } else if (dy * (mIsSlidingUp ? 1 : -1) < 0) { // Expanding
                // Is the panel less than fully expanded?
                // Then we'll handle the drag here.
                if (mSlideOffset < 1.0f) {
                    mIsScrollableViewHandlingTouch = false;
                    return this.onTouchEvent(ev);
                }

                // Was the panel handling the touch previously?
                // Then we need to rejigger things so that the
                // child gets a proper down event.
                if (!mIsScrollableViewHandlingTouch && mDragHelper.isDragging()) {
                    mDragHelper.cancel();
                    ev.setAction(MotionEvent.ACTION_DOWN);
                }

                mIsScrollableViewHandlingTouch = true;
                return super.dispatchTouchEvent(ev);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            // If the scrollable view was handling the touch and we receive an up
            // we want to clear any previous dragging state so we don't intercept a touch stream accidentally
            if (mIsScrollableViewHandlingTouch) {
                mDragHelper.setDragState(ViewDragHelper.STATE_IDLE);
            }
        }

        // In all other cases, just let the default behavior take over.
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 判断当前Point是否落在指定视图上
     *
     * @param view View对象
     * @param x    X轴左边
     * @param y    Y轴左边
     */
    private boolean isViewUnder(View view, int x, int y) {
        if (view == null) return false;
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }

    /**
     * 根据滑动偏移量计算面板视图的顶部位置
     */
    private int computePanelTopPosition(float slideOffset) {
        int slidingViewHeight = mSlideableView != null ? mSlideableView.getMeasuredHeight() : 0;
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        // Compute the top of the panel if its collapsed
        return mIsSlidingUp
                ? getMeasuredHeight() - getPaddingBottom() - mPanelHeight - slidePixelOffset
                : getPaddingTop() - slidingViewHeight + mPanelHeight + slidePixelOffset;
    }

    /**
     * 根据面板视图的顶部位置计算滑动偏移量
     */
    private float computeSlideOffset(int topPosition) {
        // Compute the panel top position if the panel is collapsed (offset 0)
        final int topBoundCollapsed = computePanelTopPosition(0);

        // Determine the new slide offset based on the collapsed top position and the new required
        // top position
        return (mIsSlidingUp
                ? (float) (topBoundCollapsed - topPosition) / mSlideRange
                : (float) (topPosition - topBoundCollapsed) / mSlideRange);
    }

    /**
     * 返回滑动面板的当前状态
     */
    public PanelState getPanelState() {
        return mSlideState;
    }

    /**
     * 设置滑动面板的状态为指定状态(不能指定为{@link PanelState#DRAGGING})
     *
     * @param state 滑动面板的新状态
     */
    public void setPanelState(PanelState state) {
        if (state == null || state == PanelState.DRAGGING) {
            throw new IllegalArgumentException("Panel state cannot be null or DRAGGING.");
        }
        if (!isEnabled()
                || (!mFirstLayout && mSlideableView == null)
                || state == mSlideState
                || mSlideState == PanelState.DRAGGING) {
            return;
        }
        if (mFirstLayout) {
            setPanelStateInternal(state);
        } else {
            if (mSlideState == PanelState.HIDDEN) {
                mSlideableView.setVisibility(View.VISIBLE);
                requestLayout();
            }
            switch (state) {
                case ANCHORED:
                    smoothSlideTo(mAnchorPoint, 0);
                    break;
                case COLLAPSED:
                    smoothSlideTo(0, 0);
                    break;
                case EXPANDED:
                    smoothSlideTo(1.0f, 0);
                    break;
                case HIDDEN:
                    int newTop = computePanelTopPosition(0.0f) + (mIsSlidingUp ? +mPanelHeight : -mPanelHeight);
                    smoothSlideTo(computeSlideOffset(newTop), 0);
                    break;
            }
        }
    }

    /**
     * 设置滑动面板的状态(内部)
     *
     * @param newState 滑动面板的新状态
     */
    private void setPanelStateInternal(PanelState newState) {
        if (mSlideState == newState) { // 判断当前状态是否和新状态相同
            return;
        }
        PanelState oldState = mSlideState;
        mSlideState = newState;
        dispatchOnPanelStateChanged(this, oldState, newState);
    }

    /**
     * 根据当前滑动偏移来更新视差
     */
    private void applyParallaxForCurrentSlideOffset() {
        if (mParallaxOffset > 0) {
            // 开始计算mMainView的视差偏移量
            int mainViewOffset = getCurrentParallaxOffset();
            ViewCompat.setTranslationY(mMainView, mainViewOffset);
        }
    }

    /**
     * 触摸手势在drag下，处理mMainView的视差偏移和LayoutParams
     *
     * @param newTop
     */
    private void onPanelDragged(int newTop) {
        if (mSlideState != PanelState.DRAGGING) {
            mLastNotDraggingSlideState = mSlideState;
        }
        setPanelStateInternal(PanelState.DRAGGING);
        // 根据新的顶部位置重新计算滑动偏移量
        mSlideOffset = computeSlideOffset(newTop);
        applyParallaxForCurrentSlideOffset();
        // 分发滑动面板的位置改变事件
        dispatchOnPanelSlide(mSlideableView);
        // If the slide offset is negative, and overlay is not on, we need to increase the
        // height of the main content
        LayoutParams lp = (LayoutParams) mMainView.getLayoutParams();
        int defaultHeight = getHeight() - getPaddingBottom() - getPaddingTop() - mPanelHeight;

        if (mSlideOffset <= 0 && !mOverlayContent) {
            // 展开主视图
            lp.height = mIsSlidingUp ? (newTop - getPaddingBottom()) : (getHeight() - getPaddingBottom() - mSlideableView.getMeasuredHeight() - newTop);
            if (lp.height == defaultHeight) {
                lp.height = LayoutParams.MATCH_PARENT;
            }
            mMainView.requestLayout();
        } else if (lp.height != LayoutParams.MATCH_PARENT && !mOverlayContent) {
            lp.height = LayoutParams.MATCH_PARENT;
            mMainView.requestLayout();
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        // 必须需要save后，来clipRect
        final int save = canvas.save(Canvas.CLIP_SAVE_FLAG);
        boolean result;
        if (mSlideableView != null && mSlideableView != child) { // if main view
            // Clip against the slider; no sense drawing what will immediately be covered,
            // Unless the panel is set to overlay content
            canvas.getClipBounds(mTmpRect);
            if (!mOverlayContent) {
                if (mIsSlidingUp) {
                    mTmpRect.bottom = Math.min(mTmpRect.bottom, mSlideableView.getTop());
                } else {
                    mTmpRect.top = Math.max(mTmpRect.top, mSlideableView.getBottom());
                }
            }
            if (mClipPanel) {
                canvas.clipRect(mTmpRect); // 裁剪画布
            }

            result = super.drawChild(canvas, child, drawingTime);

            // 非完全收起情况下，需要绘制一个半透明的蒙层
            if (mCoveredFadeColor != 0 && mSlideOffset > 0) {
                final int baseAlpha = (mCoveredFadeColor & 0xff000000) >>> 24;//取alpha值
                final int imag = (int) (baseAlpha * mSlideOffset);//根据滑动的距离越大，蒙层透明度越低
                final int color = imag << 24 | (mCoveredFadeColor & 0xffffff);//取蒙层色值
                mCoveredFadePaint.setColor(color);
                canvas.drawRect(mTmpRect, mCoveredFadePaint); // 绘制蒙层
            }
        } else {
            result = super.drawChild(canvas, child, drawingTime);
        }

        canvas.restoreToCount(save);

        return result;
    }

    /**
     * 在其范围内平滑移动滑动面板到指定位置
     *
     * @param slideOffset 平滑移动到的位置
     * @param velocity    滑动的初始速度或0
     */
    boolean smoothSlideTo(float slideOffset, int velocity) {
        if (!isEnabled() || mSlideableView == null) { // 判断是否能滑动
            // Nothing to do.
            return false;
        }
        // 计算滑动到最终坐标的顶部位置
        int panelTop = computePanelTopPosition(slideOffset);
        // 开始准备滑动mSlideableView到指定位置
        if (mDragHelper.smoothSlideViewTo(mSlideableView, mSlideableView.getLeft(), panelTop)) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);// 刷新视图
            return true;
        }
        return false;
    }

    @Override
    public void computeScroll() {
        //在滑动中，若此时是非move事件触发的，DragHelper会把当前的mDragState设置为STATE_SETTLING。此时会进入此分支，来处理接下来的位移动画
        if (mDragHelper != null && mDragHelper.continueSettling(true)) {
            if (!isEnabled()) {
                mDragHelper.abort();
                return;
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);
        // 绘制阴影
        if (mShadowDrawable != null && mSlideableView != null) {
            final int right = mSlideableView.getRight();
            final int top;
            final int bottom;
            if (mIsSlidingUp) {
                top = mSlideableView.getTop() - mShadowHeight;
                bottom = mSlideableView.getTop();
            } else {
                top = mSlideableView.getBottom();
                bottom = mSlideableView.getBottom() + mShadowHeight;
            }
            final int left = mSlideableView.getLeft();
            mShadowDrawable.setBounds(left, top, right, bottom);
            mShadowDrawable.draw(c);
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v      View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dx     Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() &&
                        y + scrollY >= child.getTop() && y + scrollY < child.getBottom() &&
                        canScroll(child, true, dx, x + scrollX - child.getLeft(),
                                y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && ViewCompat.canScrollHorizontally(v, -dx);
    }


    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SUPER_STATE, super.onSaveInstanceState());
        bundle.putSerializable(SLIDING_STATE, mSlideState != PanelState.DRAGGING ? mSlideState : mLastNotDraggingSlideState);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mSlideState = (PanelState) bundle.getSerializable(SLIDING_STATE);
            mSlideState = mSlideState == null ? DEFAULT_SLIDE_STATE : mSlideState;
            state = bundle.getParcelable(SUPER_STATE);
        }
        super.onRestoreInstanceState(state);
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return !mIsUnableToDrag && child == mSlideableView;

        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper != null && mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                mSlideOffset = computeSlideOffset(mSlideableView.getTop());
                applyParallaxForCurrentSlideOffset();

                if (mSlideOffset == 1) {
                    updateObscuredViewVisibility();
                    setPanelStateInternal(PanelState.EXPANDED);
                } else if (mSlideOffset == 0) {
                    setPanelStateInternal(PanelState.COLLAPSED);
                } else if (mSlideOffset < 0) {
                    setPanelStateInternal(PanelState.HIDDEN);
                    mSlideableView.setVisibility(View.INVISIBLE);
                } else {
                    updateObscuredViewVisibility();
                    setPanelStateInternal(PanelState.ANCHORED);
                }
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            onPanelDragged(top);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int target = 0;

            // direction is always positive if we are sliding in the expanded direction
            float direction = mIsSlidingUp ? -yvel : yvel;

            if (direction > 0 && mSlideOffset <= mAnchorPoint) {
                // swipe up -> expand and stop at anchor point
                target = computePanelTopPosition(mAnchorPoint);
            } else if (direction > 0 && mSlideOffset > mAnchorPoint) {
                // swipe up past anchor -> expand
                target = computePanelTopPosition(1.0f);
            } else if (direction < 0 && mSlideOffset >= mAnchorPoint) {
                // swipe down -> collapse and stop at anchor point
                target = computePanelTopPosition(mAnchorPoint);
            } else if (direction < 0 && mSlideOffset < mAnchorPoint) {
                // swipe down past anchor -> collapse
                target = computePanelTopPosition(0.0f);
            } else if (mSlideOffset >= (1.f + mAnchorPoint) / 2) {
                // zero velocity, and far enough from anchor point => expand to the top
                target = computePanelTopPosition(1.0f);
            } else if (mSlideOffset >= mAnchorPoint / 2) {
                // zero velocity, and close enough to anchor point => go to anchor
                target = computePanelTopPosition(mAnchorPoint);
            } else {
                // settle at the bottom
                target = computePanelTopPosition(0.0f);
            }

            if (mDragHelper != null) {
                mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), target);
            }
            invalidate();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mSlideRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int collapsedTop = computePanelTopPosition(0.f);
            final int expandedTop = computePanelTopPosition(1.0f);
            if (mIsSlidingUp) {
                return Math.min(Math.max(top, expandedTop), collapsedTop);
            } else {
                return Math.min(Math.max(top, collapsedTop), expandedTop);
            }
        }
    }

    public static class LayoutParams extends MarginLayoutParams {

        private static final int[] ATTRS = new int[]{
                android.R.attr.layout_weight
        };

        public float weight = 0;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, float weight) {
            super(width, height);
            this.weight = weight;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            final TypedArray typedArray = context.obtainStyledAttributes(attrs, ATTRS);
            if (typedArray != null) {
                this.weight = typedArray.getFloat(0, 0);
                typedArray.recycle();
            }
        }
    }
}

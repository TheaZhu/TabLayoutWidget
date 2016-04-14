package com.thea.widget.tablayout;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Thea on 2015/12/17 0017.
 */
public class BottomTabLayout extends HorizontalScrollView {

    private static final int DEFAULT_HEIGHT_WITH_TEXT_ICON = 72; // dps
    private static final int DEFAULT_HEIGHT = 48; // dps
    private static final int TAB_MIN_WIDTH_MARGIN = 56; //dps
    private static final int ANIMATION_DURATION = 300;
    private static final int INVALID_WIDTH = -1;

    /**
     * Scrollable tabs display a subset of tabs at any given moment, and can contain longer tab
     * labels and a larger number of tabs. They are best used for browsing contexts in touch
     * interfaces when users don’t need to directly compare the tab labels.
     *
     * @see #setTabMode(int)
     * @see #getTabMode()
     */
    public static final int MODE_SCROLLABLE = 0;

    /**
     * Fixed tabs display all tabs concurrently and are best used with content that benefits from
     * quick pivots between tabs. The maximum number of tabs is limited by the view’s width.
     * Fixed tabs have equal width, based on the widest tab label.
     *
     * @see #setTabMode(int)
     * @see #getTabMode()
     */
    public static final int MODE_FIXED = 1;

    /**
     * Gravity used to fill the {@link BottomTabLayout} as much as possible. This option only takes effect
     * when used with {@link #MODE_FIXED}.
     *
     * @see #setTabGravity(int)
     * @see #getTabGravity()
     */
    public static final int GRAVITY_FILL = 0;

    /**
     * Gravity used to lay out the tabs in the center of the {@link BottomTabLayout}.
     *
     * @see #setTabGravity(int)
     * @see #getTabGravity()
     */
    public static final int GRAVITY_CENTER = 1;

    /**
     * Callback interface invoked when a tab's selection state changes.
     */
    public interface OnTabSelectedListener {

        /**
         * Called when a tab enters the selected state.
         *
         * @param tab The tab that was selected
         */
        void onTabSelected(TabView.Tab tab);

        /**
         * Called when a tab exits the selected state.
         *
         * @param tab The tab that was unselected
         */
        void onTabUnselected(TabView.Tab tab);

        /**
         * Called when a tab that is already selected is chosen again by the user. Some applications
         * may use this action to return to the top level of a category.
         *
         * @param tab The tab that was reselected.
         */
        void onTabReselected(TabView.Tab tab);
    }

    private final ArrayList<TabView.Tab> mTabs = new ArrayList<>();
    private TabView.Tab mSelectedTab;

    private final SlidingTabStrip mTabStrip;
    private boolean mShowSelectedTabAnimation = false;

    private int mTabPaddingStart;
    private int mTabPaddingTop;
    private int mTabPaddingEnd;
    private int mTabPaddingBottom;

    private int mTabTextAppearance;
    private ColorStateList mTabTextColors;
    private float mTabTextSize;
    private float mTabTextMultiLineSize;

    private final int mTabBackgroundResId;

    private int mTabMaxWidth = Integer.MAX_VALUE;
    private final int mRequestedTabMinWidth;
    private final int mRequestedTabMaxWidth;
    private final int mScrollableTabMinWidth;

    private int mContentInsetStart;

    private int mTabGravity;
    private int mMode;

    private OnTabSelectedListener mOnTabSelectedListener;
    private OnClickListener mTabClickListener;

    private ValueAnimator mScrollAnimator;
    private ValueAnimator mIndicatorAnimator;

    public BottomTabLayout(Context context) {
        this(context, null);
    }

    public BottomTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);

        // Add the TabStrip
        mTabStrip = new SlidingTabStrip(context);
        addView(mTabStrip, LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BottomTabLayout,
                defStyleAttr, R.style.Widget_BottomTabLayout);

        mShowSelectedTabAnimation = typedArray.getBoolean(
                R.styleable.BottomTabLayout_showSelectedTabAnimation, false);
        mTabStrip.setShowTabIndicator(typedArray.getBoolean(
                R.styleable.BottomTabLayout_showTabIndicator, false));
        mTabStrip.setTabIndicatorPosition(typedArray.getInt(
                R.styleable.BottomTabLayout_tabIndicatorPosition, 0));
        mTabStrip.setSelectedIndicatorHeight(
                typedArray.getDimensionPixelSize(R.styleable.BottomTabLayout_tabIndicatorHeight, 0));
        mTabStrip.setSelectedIndicatorColor(typedArray.getColor(R.styleable.BottomTabLayout_tabIndicatorColor, 0));

        mTabPaddingStart = mTabPaddingTop = mTabPaddingEnd = mTabPaddingBottom =
                typedArray.getDimensionPixelSize(R.styleable.BottomTabLayout_tabPadding, 0);
        mTabPaddingStart = typedArray.getDimensionPixelSize(
                R.styleable.BottomTabLayout_tabPaddingStart, mTabPaddingStart);
        mTabPaddingTop = typedArray.getDimensionPixelSize(
                R.styleable.BottomTabLayout_tabPaddingTop, mTabPaddingTop);
        mTabPaddingEnd = typedArray.getDimensionPixelSize(
                R.styleable.BottomTabLayout_tabPaddingEnd, mTabPaddingEnd);
        mTabPaddingBottom = typedArray.getDimensionPixelSize(
                R.styleable.BottomTabLayout_tabPaddingBottom, mTabPaddingBottom);

        mTabTextAppearance = typedArray.getResourceId(R.styleable.BottomTabLayout_tabTextAppearance,
                R.style.TextAppearance_Tab);

        // Text colors/sizes come from the text appearance first
        final TypedArray ta = context.obtainStyledAttributes(mTabTextAppearance,
                R.styleable.TextAppearance);
        try {
            mTabTextSize = ta.getDimensionPixelSize(R.styleable.TextAppearance_android_textSize, 0);
            mTabTextColors = ta.getColorStateList(R.styleable.TextAppearance_android_textColor);
        } finally {
            ta.recycle();
        }

        if (typedArray.hasValue(R.styleable.BottomTabLayout_tabTextColor))
            mTabTextColors = typedArray.getColorStateList(R.styleable.BottomTabLayout_tabTextColor);

        if (typedArray.hasValue(R.styleable.BottomTabLayout_tabSelectedTextColor)) {
            final int selected = typedArray.getColor(
                    R.styleable.BottomTabLayout_tabSelectedTextColor, 0);
            mTabTextColors = createColorStateList(mTabTextColors.getDefaultColor(), selected);
        }

        mRequestedTabMinWidth = typedArray.getDimensionPixelSize(
                R.styleable.BottomTabLayout_tabMinWidth, INVALID_WIDTH);
        mRequestedTabMaxWidth = typedArray.getDimensionPixelSize(
                R.styleable.BottomTabLayout_tabMaxWidth, INVALID_WIDTH);

        mTabBackgroundResId = typedArray.getResourceId(
                R.styleable.BottomTabLayout_tabBackground, 0);
        mContentInsetStart = typedArray.getDimensionPixelSize(
                R.styleable.BottomTabLayout_tabContentStart, 0);
        mMode = typedArray.getInt(R.styleable.BottomTabLayout_tabMode, MODE_FIXED);
        mTabGravity = typedArray.getInt(R.styleable.BottomTabLayout_tabGravity, GRAVITY_FILL);

        typedArray.recycle();

        final Resources res = getResources();
        mTabTextMultiLineSize = res.getDimensionPixelSize(R.dimen.tab_text_size_2line);
        mScrollableTabMinWidth = res.getDimensionPixelSize(R.dimen.tab_scrollable_min_width);

        applyModeAndGravity();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If we have a MeasureSpec which allows us to decide our height, try and use the default
        // height
        final int idealHeight = dpToPx(getDefaultHeight()) + getPaddingTop() + getPaddingBottom();
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.AT_MOST:
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        Math.min(idealHeight, MeasureSpec.getSize(heightMeasureSpec)),
                        MeasureSpec.EXACTLY);
                break;
            case MeasureSpec.UNSPECIFIED:
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(idealHeight, MeasureSpec.EXACTLY);
                break;
        }

        final int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            // If we don't have an unspecified width spec, use the given size to calculate
            // the max tab width
            mTabMaxWidth = mRequestedTabMaxWidth > 0
                    ? mRequestedTabMaxWidth
                    : specWidth - dpToPx(TAB_MIN_WIDTH_MARGIN);
        }

        // Now super measure itself using the (possibly) modified height spec
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (getChildCount() == 1) {
            // If we're in fixed mode then we need to make the tab strip is the same width as us
            // so we don't scroll
            final View child = getChildAt(0);
            boolean remeasure = false;

            switch (mMode) {
                case MODE_SCROLLABLE:
                    // We only need to resize the child if it's smaller than us. This is similar
                    // to fillViewport
                    remeasure = child.getMeasuredWidth() < getMeasuredWidth();
                    break;
                case MODE_FIXED:
                    // Resize the child so that it doesn't scroll
                    remeasure = child.getMeasuredWidth() != getMeasuredWidth();
                    break;
            }

            if (remeasure) {
                // Re-measure the child with a widthSpec set to be exactly our measure width
                int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop()
                        + getPaddingBottom(), child.getLayoutParams().height);
                int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        getMeasuredWidth(), MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list.
     * If this is the first tab to be added it will become the selected tab.
     *
     * @param tab Tab to add
     */
    public void addTab(@NonNull TabView.Tab tab) {
        addTab(tab, mTabs.isEmpty());
    }

    /**
     * Add a tab to this layout. The tab will be inserted at <code>position</code>.
     * If this is the first tab to be added it will become the selected tab.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     */
    public void addTab(@NonNull TabView.Tab tab, int position) {
        addTab(tab, position, mTabs.isEmpty());
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list.
     *
     * @param tab Tab to add
     * @param setSelected True if the added tab should become the selected tab.
     */
    public void addTab(@NonNull TabView.Tab tab, boolean setSelected) {
        addTab(tab, mTabs.size(), setSelected);
    }

    /**
     * Add a tab to this layout. The tab will be inserted at <code>position</code>.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     * @param setSelected True if the added tab should become the selected tab.
     */
    public void addTab(@NonNull TabView.Tab tab, int position, boolean setSelected) {
        mTabStrip.addTabView(tab, position, setSelected);
        configureTab(tab, position);
        if (setSelected)
            selectTab(tab);
    }

    /**
     * Remove a tab from the layout. If the removed tab was selected it will be deselected
     * and another tab will be selected if present.
     *
     * @param tab The tab to remove
     */
    public TabView.Tab removeTab(TabView.Tab tab) {
        return removeTabAt(tab.getPosition());
    }

    /**
     * Remove a tab from the layout. If the removed tab was selected it will be deselected
     * and another tab will be selected if present.
     *
     * @param position Position of the tab to remove
     */
    public TabView.Tab removeTabAt(int position) {
        final int selectedTabPosition = mSelectedTab != null ? mSelectedTab.getPosition() : 0;
        removeTabViewAt(position);

        TabView.Tab removedTab = mTabs.remove(position);
        if (removedTab != null) {
            removedTab.setPosition(TabView.Tab.INVALID_POSITION);
        }

        final int newTabCount = mTabs.size();
        for (int i = position; i < newTabCount; i++)
            mTabs.get(i).setPosition(i);

        if (selectedTabPosition == position)
            selectTab(mTabs.isEmpty() ? null : mTabs.get(Math.max(0, position - 1)));

        return removedTab;
    }

    /**
     * Remove all tabs from the action bar and deselect the current tab.
     */
    public void removeAllTabs() {
        // Remove all the views
        mTabStrip.removeAllViews();

        for (Iterator<TabView.Tab> i = mTabs.iterator(); i.hasNext(); ) {
            TabView.Tab tab = i.next();
            tab.setPosition(TabView.Tab.INVALID_POSITION);
            i.remove();
        }

        mSelectedTab = null;
    }

    /**
     * Create and return a new {@link TabView.Tab}. You need to manually add this using
     *
     * @return A new Tab
     */
    @NonNull
    public TabView.Tab newTab() {
        return new TabView.Tab(this.getContext());
    }

    public TabView.Tab getTabAt(int position) {
        return mTabs.get(position);
    }

    /**
     * Set the {@link OnTabSelectedListener} that will
     * handle switching to and from tabs.
     *
     * @param onTabSelectedListener Listener to handle tab selection events
     */
    public void setOnTabSelectedListener(OnTabSelectedListener onTabSelectedListener) {
        mOnTabSelectedListener = onTabSelectedListener;
    }

    public void showSelectedAnimation(TabView tabView, boolean selected) {
        if (tabView == null || tabView.getIconView() == null)
            return;
        if (mShowSelectedTabAnimation && selected) {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(tabView.getIconView(), "scaleX", 1f, 1.5f, 1),
                    ObjectAnimator.ofFloat(tabView.getIconView(), "scaleY", 1f, 1.5f, 1));
            animatorSet.setDuration(200);
            animatorSet.start();
        }
    }

    public void selectTab(TabView.Tab tab) {
        if (tab == null)
            return;
        if (mSelectedTab == tab) {
            if (mOnTabSelectedListener != null)
                mOnTabSelectedListener.onTabReselected(tab);
        }
        else {
            mTabStrip.setSelectedTabView(tab.getPosition());
            if (mSelectedTab != null && mOnTabSelectedListener != null)
                mOnTabSelectedListener.onTabUnselected(mSelectedTab);
            if (mOnTabSelectedListener != null)
                mOnTabSelectedListener.onTabSelected(tab);
            if ((mSelectedTab == null || mSelectedTab.getPosition() == TabView.Tab.INVALID_POSITION))
                // If we don't currently have a tab, just draw the indicator
                setScrollPosition(tab.getPosition(), 0f, true);
            else
                animateToTab(tab.getPosition());

            mSelectedTab = tab;
        }
    }

    public void setupWithViewPager(@NonNull ViewPager viewPager) {
        setUpWithTabsAndViewPager(null, viewPager);
    }

    /**
     * The one-stop shop for setting up this {@link BottomTabLayout} with a {@link ViewPager}.
     *
     * <p>This method will:
     * <ul>
     *     <li>Add a {@link ViewPager.OnPageChangeListener} that will forward events to
     *     this TabLayout.</li>
     *     <li>Populate the TabLayout's tabs from the ViewPager's {@link PagerAdapter}.</li>
     *     <li>Set our {@link OnTabSelectedListener} which will forward
     *     selected events to the ViewPager</li>
     * </ul>
     * </p>
     *
     * @see TabLayoutOnPageChangeListener
     * @see ViewPagerOnTabSelectedListener
     */
    public void setUpWithTabsAndViewPager(TabView.Tab[] tabs, @NonNull ViewPager viewPager) {
        final PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null) {
            throw new IllegalArgumentException("ViewPager does not have a PagerAdapter set");
        }

        removeAllTabs();
        if (tabs == null) {
            for (int i = 0, count = adapter.getCount(); i < count; i++)
                addTab(newTab().setText(adapter.getPageTitle(i)));
        }
        else {
            int n = tabs.length;
            for (int i = 0, count = adapter.getCount(); i < count; i++)
                addTab(tabs[i % n]);
        }

        // Now we'll add our page change listener to the ViewPager
        viewPager.addOnPageChangeListener(new TabLayoutOnPageChangeListener(this));

        // Now we'll add a tab selected listener to set ViewPager's current item
        setOnTabSelectedListener(new ViewPagerOnTabSelectedListener(viewPager));

        // Make sure we reflect the currently set ViewPager item
        if (adapter.getCount() > 0) {
            final int curItem = viewPager.getCurrentItem();
            if (getSelectedTabPosition() != curItem) {
                selectTab(getTabAt(curItem));
            }
        }
    }

    /**
     * Set the scroll position of the tabs. This is useful for when the tabs are being displayed as
     * part of a scrolling container such as {@link ViewPager}.
     * <p>
     * Calling this method does not update the selected tab, it is only used for drawing purposes.
     *
     * @param position current scroll position
     * @param positionOffset Value from [0, 1) indicating the offset from {@code position}.
     * @param updateSelectedText Whether to update the text's selected state.
     */
    public void setScrollPosition(int position, float positionOffset, boolean updateSelectedText) {
        if (mIndicatorAnimator != null && mIndicatorAnimator.isRunning()) {
            return;
        }
        if (position < 0 || position >= mTabStrip.getChildCount()) {
            return;
        }

        // Set the indicator position and update the scroll to match
        mTabStrip.setIndicatorPositionFromTabPosition(position, positionOffset);
        scrollTo(calculateScrollXForTab(position, positionOffset), 0);

        // Update the 'selected state' view as we scroll
        if (updateSelectedText)
            mTabStrip.setSelectedTabView(Math.round(position + positionOffset));
    }

    private void animateToTab(int newPosition) {
        if (newPosition == TabView.Tab.INVALID_POSITION) {
            return;
        }

        if (getWindowToken() == null || !ViewCompat.isLaidOut(this)
                || mTabStrip.childrenNeedLayout()) {
            // If we don't have a window token, or we haven't been laid out yet just draw the new
            // position now
            setScrollPosition(newPosition, 0f, true);
            return;
        }

        final int startScrollX = getScrollX();
        final int targetScrollX = calculateScrollXForTab(newPosition, 0);

        if (startScrollX != targetScrollX) {
            if (mScrollAnimator == null) {
                mScrollAnimator = new ValueAnimator();
                mScrollAnimator.setInterpolator(new FastOutSlowInInterpolator());
                mScrollAnimator.setDuration(ANIMATION_DURATION);
                mScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        scrollTo((Integer) animator.getAnimatedValue(), 0);
                    }
                });
            }

            mScrollAnimator.setIntValues(startScrollX, targetScrollX);
            mScrollAnimator.start();
        }

        // Now animate the indicator
        mTabStrip.animateIndicatorToPosition(newPosition, ANIMATION_DURATION);
    }

    public void removeTabViewAt(int position) {
        mTabStrip.removeViewAt(position);
        requestLayout();
    }

    public int getTabPaddingStart() {
        return mTabPaddingStart;
    }

    public int getTabPaddingTop() {
        return mTabPaddingTop;
    }

    public int getTabPaddingEnd() {
        return mTabPaddingEnd;
    }

    public int getTabPaddingBottom() {
        return mTabPaddingBottom;
    }

    public int getTabTextAppearance() {
        return mTabTextAppearance;
    }

    public ColorStateList getTabTextColors() {
        return mTabTextColors;
    }

    public float getTabTextSize() {
        return mTabTextSize;
    }

    public float getTabTextMultiLineSize() {
        return mTabTextMultiLineSize;
    }

    public int getTabBackgroundResId() {
        return mTabBackgroundResId;
    }

    public int getTabGravity() {
        return mTabGravity;
    }

    public int getTabMode() {
        return mMode;
    }

    public int getTabMaxWidth() {
        return mTabMaxWidth;
    }

    public int getTabMinWidth() {
        if (mRequestedTabMinWidth != INVALID_WIDTH) {
            // If we have been given a min width, use it
            return mRequestedTabMinWidth;
        }
        // Else, we'll use the default value
        return mMode == MODE_SCROLLABLE ? mScrollableTabMinWidth : 0;
    }

    public void setTabGravity(int mTabGravity) {
        this.mTabGravity = mTabGravity;
    }

    public void setTabMode(int mMode) {
        this.mMode = mMode;
    }

    /**
     * Returns the position of the current selected tab.
     *
     * @return selected tab position, or {@code -1} if there isn't a selected tab.
     */
    public int getSelectedTabPosition() {
        return mSelectedTab != null ? mSelectedTab.getPosition() : -1;
    }

    private void applyModeAndGravity() {
        int paddingStart = 0;
        if (mMode == MODE_SCROLLABLE) {
            // If we're scrollable, or fixed at start, inset using padding
            paddingStart = Math.max(0, mContentInsetStart - mTabPaddingStart);
        }
        ViewCompat.setPaddingRelative(mTabStrip, paddingStart, 0, 0, 0);

        switch (mMode) {
            case MODE_FIXED:
                mTabStrip.setGravity(Gravity.CENTER_HORIZONTAL);
                break;
            case MODE_SCROLLABLE:
                mTabStrip.setGravity(GravityCompat.START);
                break;
        }

        mTabStrip.updateTabViews(true);
    }

    private void configureTab(TabView.Tab tab, int position) {
        tab.setPosition(position);
        mTabs.add(position, tab);

        final int count = mTabs.size();
        for (int i = position + 1; i < count; i++)
            mTabs.get(i).setPosition(i);
    }

    private TabView createTabView(final TabView.Tab tab) {
        final TabView tabView = new TabView(getContext(), tab, this);
        tabView.setFocusable(true);
        tabView.setMinimumWidth(getTabMinWidth());

        if (mTabClickListener == null) {
            mTabClickListener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    TabView tabView = (TabView) view;
                    showSelectedAnimation(tabView, tabView.getTab() != mSelectedTab);
                    selectTab(tabView.getTab());
                }
            };
        }
        tabView.setOnClickListener(mTabClickListener);
        return tabView;
    }

    private int getDefaultHeight() {
        boolean hasIconAndText = false;
        for (int i = 0, count = mTabs.size(); i < count; i++) {
            TabView.Tab tab = mTabs.get(i);
            if (tab != null && tab.getIcon() != null && !TextUtils.isEmpty(tab.getText())) {
                hasIconAndText = true;
                break;
            }
        }
        return hasIconAndText ? DEFAULT_HEIGHT_WITH_TEXT_ICON : DEFAULT_HEIGHT;
    }

    private int calculateScrollXForTab(int position, float positionOffset) {
        if (mMode == MODE_SCROLLABLE) {
            final View selectedChild = mTabStrip.getChildAt(position);
            final View nextChild = position + 1 < mTabStrip.getChildCount()
                    ? mTabStrip.getChildAt(position + 1)
                    : null;
            final int selectedWidth = selectedChild != null ? selectedChild.getWidth() : 0;
            final int nextWidth = nextChild != null ? nextChild.getWidth() : 0;

            return selectedChild.getLeft()
                    + ((int) ((selectedWidth + nextWidth) * positionOffset * 0.5f))
                    + (selectedChild.getWidth() / 2)
                    - (getWidth() / 2);
        }
        return 0;
    }

    private static ColorStateList createColorStateList(int defaultColor, int selectedColor) {
        final int[][] states = new int[2][];
        final int[] colors = new int[2];
        int i = 0;

        states[i] = SELECTED_STATE_SET;
        colors[i] = selectedColor;
        i++;

        // Default enabled state
        states[i] = EMPTY_STATE_SET;
        colors[i] = defaultColor;

        return new ColorStateList(states, colors);
    }

    private int dpToPx(int dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }

    class SlidingTabStrip extends LinearLayout {
        private static final int MOTION_NON_ADJACENT_OFFSET = 24;
        private static final int FIXED_WRAP_GUTTER_MIN = 16; //dps

        private boolean mShowTabIndicator = true;
        private int mTabIndicatorPosition = 0;
        private int mSelectedIndicatorHeight;
        private int mIndicatorLeft = -1;
        private int mIndicatorRight = -1;
        private final Paint mSelectedIndicatorPaint = new Paint();

        private ValueAnimator mCurrentAnimator;

        private int mSelectedPosition = -1;
        private float mSelectionOffset;

        public SlidingTabStrip(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        public void setShowTabIndicator(boolean showTabIndicator) {
            mShowTabIndicator = showTabIndicator;
        }

        public void setTabIndicatorPosition(int tabIndicatorPosition) {
            mTabIndicatorPosition = tabIndicatorPosition;
        }

        public void setSelectedIndicatorHeight(int height) {
            if (mSelectedIndicatorHeight != height) {
                mSelectedIndicatorHeight = height;
                if (mShowTabIndicator)
                    ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        public void setSelectedIndicatorColor(int color) {
            if (mSelectedIndicatorPaint.getColor() != color) {
                mSelectedIndicatorPaint.setColor(color);
                if (mShowTabIndicator)
                    ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        public void addTabView(TabView.Tab tab, int position, boolean setSelected) {
            final TabView tabView = createTabView(tab);
            addView(tabView, position, createLayoutParamsForTabs());
            if (setSelected)
                tabView.setSelected(true);
        }

        public TabView getTabView(int position) {
            return (TabView) getChildAt(position);
        }

        public void setSelectedTabView(int position) {
            if (mSelectedPosition == position)
                return;
            if (mSelectedPosition != -1)
                getTabView(mSelectedPosition).setSelected(false);
            if (position != -1)
                getTabView(position).setSelected(true);
            mSelectedPosition = position;
        }

        @Override
        protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
                // HorizontalScrollView will first measure use with UNSPECIFIED, and then with
                // EXACTLY. Ignore the first call since anything we do will be overwritten anyway
                return;
            }

            if (mMode == MODE_FIXED && mTabGravity == GRAVITY_CENTER) {
                final int count = getChildCount();

                // First we'll find the widest tab
                int largestTabWidth = 0;
                for (int i = 0; i < count; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() == VISIBLE) {
                        largestTabWidth = Math.max(largestTabWidth, child.getMeasuredWidth());
                    }
                }

                if (largestTabWidth <= 0) {
                    // If we don't have a largest child yet, skip until the next measure pass
                    return;
                }

                final int gutter = dpToPx(FIXED_WRAP_GUTTER_MIN);
                boolean remeasure = false;

                if (largestTabWidth * count <= getMeasuredWidth() - gutter * 2) {
                    // If the tabs fit within our width minus gutters, we will set all tabs to have
                    // the same width
                    for (int i = 0; i < count; i++) {
                        final LayoutParams lp =
                                (LayoutParams) getChildAt(i).getLayoutParams();
                        if (lp.width != largestTabWidth || lp.weight != 0) {
                            lp.width = largestTabWidth;
                            lp.weight = 0;
                            remeasure = true;
                        }
                    }
                } else {
                    // If the tabs will wrap to be larger than the width minus gutters, we need
                    // to switch to GRAVITY_FILL
                    setTabGravity(GRAVITY_FILL);
                    updateTabViews(false);
                    remeasure = true;
                }

                if (remeasure) {
                    // Now re-measure after our changes
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);

            if (!mShowTabIndicator)
                return;

            if (mCurrentAnimator != null && mCurrentAnimator.isRunning()) {
                // If we're currently running an animation, lets cancel it and start a
                // new animation with the remaining duration
                mCurrentAnimator.cancel();
                final long duration = mCurrentAnimator.getDuration();
                animateIndicatorToPosition(mSelectedPosition,
                        Math.round((1f - mCurrentAnimator.getAnimatedFraction()) * duration));
            } else {
                // If we've been layed out, update the indicator position
                updateIndicatorPosition();
            }
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);

            // Thick colored underline below the current selection
            if (!mShowTabIndicator)
                return;
            if (mIndicatorLeft >= 0 && mIndicatorRight > mIndicatorLeft) {
                int top, bottom;
                if (mTabIndicatorPosition == 0) {
                    top = 0;
                    bottom = mSelectedIndicatorHeight;
                }
                else {
                    top = getHeight() - mSelectedIndicatorHeight;
                    bottom = getHeight();
                }

                canvas.drawRect(mIndicatorLeft, top, mIndicatorRight, bottom, mSelectedIndicatorPaint);
            }
        }

        public void updateTabViews(final boolean requestLayout) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.setMinimumWidth(getTabMinWidth());
                updateTabViewLayoutParams((LayoutParams) child.getLayoutParams());
                if (requestLayout)
                    child.requestLayout();
            }
        }

        public void updateTabViewLayoutParams(LayoutParams lp) {
            if (mMode == MODE_FIXED && mTabGravity == GRAVITY_FILL) {
                lp.width = 0;
                lp.weight = 1;
            } else {
                lp.width = LayoutParams.WRAP_CONTENT;
                lp.weight = 0;
            }
        }

        private LayoutParams createLayoutParamsForTabs() {
            final LayoutParams lp = new LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT);
            updateTabViewLayoutParams(lp);
            return lp;
        }

        boolean childrenNeedLayout() {
            for (int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                if (child.getWidth() <= 0) {
                    return true;
                }
            }
            return false;
        }

        private void updateIndicatorPosition() {
            final View selectedTab = getChildAt(mSelectedPosition);
            int left, right;

            if (selectedTab != null && selectedTab.getWidth() > 0) {
                left = selectedTab.getLeft();
                right = selectedTab.getRight();

                if (mSelectionOffset > 0f && mSelectedPosition < getChildCount() - 1) {
                    // Draw the selection partway between the tabs
                    View nextTitle = getChildAt(mSelectedPosition + 1);
                    left = (int) (mSelectionOffset * nextTitle.getLeft() +
                            (1.0f - mSelectionOffset) * left);
                    right = (int) (mSelectionOffset * nextTitle.getRight() +
                            (1.0f - mSelectionOffset) * right);
                }
            } else {
                left = right = -1;
            }

            setIndicatorPosition(left, right);
        }

        private void setIndicatorPosition(int left, int right) {
            if (left != mIndicatorLeft || right != mIndicatorRight) {
                // If the indicator's left/right has changed, invalidate
                mIndicatorLeft = left;
                mIndicatorRight = right;
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        void setIndicatorPositionFromTabPosition(int position, float positionOffset) {
            mSelectedPosition = position;
            mSelectionOffset = positionOffset;
            updateIndicatorPosition();
        }

        void animateIndicatorToPosition(final int position, int duration) {
            final boolean isRtl = ViewCompat.getLayoutDirection(this)
                    == ViewCompat.LAYOUT_DIRECTION_RTL;

            final View targetView = getChildAt(position);
            final int targetLeft = targetView.getLeft();
            final int targetRight = targetView.getRight();
            final int startLeft;
            final int startRight;

            if (Math.abs(position - mSelectedPosition) <= 1) {
                // If the views are adjacent, we'll animate from edge-to-edge
                startLeft = mIndicatorLeft;
                startRight = mIndicatorRight;
            } else {
                // Else, we'll just grow from the nearest edge
                final int offset = dpToPx(MOTION_NON_ADJACENT_OFFSET);
                if (position < mSelectedPosition) {
                    // We're going end-to-start
                    if (isRtl)
                        startLeft = startRight = targetLeft - offset;
                    else
                        startLeft = startRight = targetRight + offset;
                } else {
                    // We're going start-to-end
                    if (isRtl)
                        startLeft = startRight = targetRight + offset;
                    else
                        startLeft = startRight = targetLeft - offset;
                }
            }

            if (startLeft != targetLeft || startRight != targetRight) {
                final ValueAnimator animator = mIndicatorAnimator = new ValueAnimator();
                animator.setInterpolator(new FastOutSlowInInterpolator());
                animator.setDuration(duration);
                animator.setFloatValues(0, 1);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        final float fraction = animator.getAnimatedFraction();
                        setIndicatorPosition(lerp(startLeft, targetLeft, fraction),
                                lerp(startRight, targetRight, fraction));
                    }
                });
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        mSelectedPosition = position;
                        mSelectionOffset = 0f;
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                        mSelectedPosition = position;
                        mSelectionOffset = 0f;
                    }
                });
                animator.start();
                mCurrentAnimator = animator;
            }
        }

        float getIndicatorPosition() {
            return mSelectedPosition + mSelectionOffset;
        }

        private int lerp(int startValue, int endValue, float fraction) {
            return startValue + Math.round(fraction * (endValue - startValue));
        }
    }

    public static class TabLayoutOnPageChangeListener implements ViewPager.OnPageChangeListener {
        private final WeakReference<BottomTabLayout> mTabLayoutRef;
        private int mPreviousScrollState;
        private int mScrollState;

        public TabLayoutOnPageChangeListener(BottomTabLayout tabLayout) {
            mTabLayoutRef = new WeakReference<>(tabLayout);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            final BottomTabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null) {
                final boolean update = (mScrollState == ViewPager.SCROLL_STATE_DRAGGING)
                        || (mScrollState == ViewPager.SCROLL_STATE_SETTLING
                        && mPreviousScrollState == ViewPager.SCROLL_STATE_DRAGGING);
                tabLayout.setScrollPosition(position, positionOffset, update);
            }
        }

        @Override
        public void onPageSelected(int position) {
            final BottomTabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null && tabLayout.getSelectedTabPosition() != position)
                tabLayout.selectTab(tabLayout.getTabAt(position));
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mPreviousScrollState = mScrollState;
            mScrollState = state;
        }
    }

    /**
     * A {@link OnTabSelectedListener} class which contains the necessary calls back
     * to the provided {@link ViewPager} so that the tab position is kept in sync.
     */
    public static class ViewPagerOnTabSelectedListener implements OnTabSelectedListener {
        private final ViewPager mViewPager;

        public ViewPagerOnTabSelectedListener(ViewPager viewPager) {
            mViewPager = viewPager;
        }

        @Override
        public void onTabSelected(TabView.Tab tab) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabView.Tab tab) {
        }

        @Override
        public void onTabReselected(TabView.Tab tab) {
        }
    }
}

package com.thea.widget.tablayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.TintManager;
import android.text.Layout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Thea on 2015/12/17 0017.
 */
public class TabView extends LinearLayout {
    private final Tab mTab;
    private final BottomTabLayout mTabLayout;

    private ImageView mIconView;
    private TextView mTextView;

    private int mDefaultMaxLines = 2;

    public TabView(Context context, Tab tab, BottomTabLayout tabLayout) {
        super(context);
        mTab = tab;
        mTabLayout = tabLayout;

        init();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void init() {
        if (mTabLayout.getTabBackgroundResId() != 0)
            setBackground(TintManager.getDrawable(getContext(), mTabLayout.getTabBackgroundResId()));
        ViewCompat.setPaddingRelative(this, mTabLayout.getTabPaddingStart(), mTabLayout.getTabPaddingTop(),
                mTabLayout.getTabPaddingEnd(), mTabLayout.getTabPaddingBottom());
        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);
        initTextAndIcon();
    }

    private void initTextAndIcon() {
        final Drawable icon = mTab.getIcon();
        final CharSequence text = mTab.getText();

        final LayoutInflater inflater = LayoutInflater.from(getContext());
        if (icon != null && mIconView == null) {
            mIconView = (ImageView) inflater.inflate(R.layout.layout_tab_icon, this, false);
            mIconView.setImageDrawable(icon);
            addView(mIconView, 0);
        }
        if (!TextUtils.isEmpty(text) && mTextView == null) {
            mTextView = (TextView) inflater.inflate(R.layout.layout_tab_text, this, false);
//            mTextView.setTextAppearance(mTabLayout.getTabTextAppearance());
            mTextView.setTextColor(mTabLayout.getTabTextColors());
            mTextView.setTextSize(mTabLayout.getTabTextSize());
            mTextView.setText(text);
            addView(mTextView);
        }
    }

    public Tab getTab() {
        return mTab;
    }

    public ImageView getIconView() {
        return mIconView;
    }

    @Override
    public void setSelected(boolean selected) {
        final boolean changed = (isSelected() != selected);
        super.setSelected(selected);
        if (changed && selected) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);

            if (mTextView != null)
                mTextView.setSelected(true);
            if (mIconView != null)
                mIconView.setSelected(true);
        }
    }

    @Override
    public void onMeasure(final int origWidthMeasureSpec, final int origHeightMeasureSpec) {
        final int specWidthSize = MeasureSpec.getSize(origWidthMeasureSpec);
        final int specWidthMode = MeasureSpec.getMode(origWidthMeasureSpec);
        final int maxWidth = mTabLayout.getTabMaxWidth();

        final int widthMeasureSpec;

        if (maxWidth > 0 && (specWidthMode == MeasureSpec.UNSPECIFIED
                || specWidthSize > maxWidth)) {
            // If we have a max width and a given spec which is either unspecified or
            // larger than the max width, update the width spec using the same mode
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(mTabLayout.getTabMaxWidth(), specWidthMode);
        } else {
            // Else, use the original width spec
            widthMeasureSpec = origWidthMeasureSpec;
        }

        // Now lets measure
        super.onMeasure(widthMeasureSpec, origHeightMeasureSpec);

        // We need to switch the text size based on whether the text is spanning 2 lines or not
        if (mTextView != null) {
            final Resources res = getResources();
            float textSize = mTabLayout.getTabTextSize();
            int maxLines = mDefaultMaxLines;

            if (mIconView != null && mIconView.getVisibility() == VISIBLE) {
                // If the icon view is being displayed, we limit the text to 1 line
                maxLines = 1;
            } else if (mTextView.getLineCount() > 1) {
                // Otherwise when we have text which wraps we reduce the text size
                textSize = mTabLayout.getTabTextMultiLineSize();
            }

            final float curTextSize = mTextView.getTextSize();
            final int curLineCount = mTextView.getLineCount();
            final int curMaxLines = TextViewCompat.getMaxLines(mTextView);

            if (textSize != curTextSize || (curMaxLines >= 0 && maxLines != curMaxLines)) {
                // We've got a new text size and/or max lines...
                boolean updateTextView = true;

                if (mTabLayout.getTabMode() == BottomTabLayout.MODE_FIXED &&
                        textSize > curTextSize && curLineCount == 1) {
                    // If we're in fixed mode, going up in text size and currently have 1 line
                    // then it's very easy to get into an infinite recursion.
                    // To combat that we check to see if the change in text size
                    // will cause a line count change. If so, abort the size change.
                    final Layout layout = mTextView.getLayout();
                    if (layout == null
                            || approximateLineWidth(layout, 0, textSize) > layout.getWidth()) {
                        updateTextView = false;
                    }
                }

                if (updateTextView) {
                    mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                    mTextView.setMaxLines(maxLines);
                    super.onMeasure(widthMeasureSpec, origHeightMeasureSpec);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        // This view masquerades as an action bar tab.
        event.setClassName(TabView.class.getName());
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        // This view masquerades as an action bar tab.
        info.setClassName(TabView.class.getName());
    }

    /**
     * Approximates a given lines width with the new provided text size.
     */
    private float approximateLineWidth(Layout layout, int line, float textSize) {
        return layout.getLineWidth(line) * (textSize / layout.getPaint().getTextSize());
    }

    public static final class Tab {
        public static final int INVALID_POSITION = -1;

        private Context mContext;
        private Drawable mIcon;
        private CharSequence mText;

        private int mPosition = INVALID_POSITION;

        public Tab(Context context) {
            mContext = context;
        }

        public Tab(Context context, Drawable icon, CharSequence text) {
            this.mIcon = icon;
            this.mText = text;
            mContext = context;
        }

        public Tab(Context context, int iconResId, CharSequence text) {
            this(context, TintManager.getDrawable(context, iconResId), text);
        }

        public Tab(Context context, Drawable icon, int textResId) {
            this(context, icon, context.getResources().getText(textResId));
        }

        public Tab(Context context, int iconResId, int textResId) {
            this(context, TintManager.getDrawable(context, iconResId), textResId);
        }

        @Nullable
        public Drawable getIcon() {
            return mIcon;
        }

        @Nullable
        public CharSequence getText() {
            return mText;
        }

        public int getPosition() {
            return mPosition;
        }

        public void setPosition(int position) {
            mPosition = position;
        }

        public Tab setIcon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        public Tab setIcon(int resId) {
            mIcon = TintManager.getDrawable(mContext, resId);
            return this;
        }

        public Tab setText(CharSequence text) {
            mText = text;
            return this;
        }

        public Tab setText(int resId) {
            mText = mContext.getResources().getText(resId);
            return this;
        }
    }
}

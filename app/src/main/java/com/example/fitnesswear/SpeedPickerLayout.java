package com.example.fitnesswear;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.fitnesswear.R;

/**
 * A simple extension of the {@link android.widget.LinearLayout} to represent a single item in a
 * {@link android.support.wearable.view.WearableListView}.
 */
public class SpeedPickerLayout extends LinearLayout
        implements WearableListView.OnCenterProximityListener {

    private final float mFadedTextAlpha;
    private final int mFadedCircleColor;
    private final int mChosenCircleColor;
    private ImageView mCircle;
    private TextView mName;

    public SpeedPickerLayout(Context context) {
        this(context, null);
    }

    public SpeedPickerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpeedPickerLayout(Context context, AttributeSet attrs,
                             int defStyle) {
        super(context, attrs, defStyle);
        //mFadedTextAlpha = getResources().getInteger(R.integer.action_text_faded_alpha) / 100f;
        mFadedTextAlpha = 40/100f;
        mFadedCircleColor = getResources().getColor(R.color.grey);
        mChosenCircleColor = getResources().getColor(R.color.dark_blue);
    }

    // Get references to the icon and text in the item layout definiton
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCircle = findViewById(R.id.circle);
        mName = findViewById(R.id.name);
    }

    @Override
    public void onCenterPosition(boolean animate) {
        mName.setAlpha(1f);
        ((GradientDrawable) mCircle.getDrawable()).setColor(mChosenCircleColor);
    }

    @Override
    public void onNonCenterPosition(boolean animate) {
        ((GradientDrawable) mCircle.getDrawable()).setColor(mFadedCircleColor);
        mName.setAlpha(mFadedTextAlpha);

    }
}

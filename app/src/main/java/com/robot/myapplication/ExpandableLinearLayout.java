package com.robot.myapplication;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class ExpandableLinearLayout extends LinearLayout {

    public static final String KEY_SUPER_STATE = "super_state";
    public static final String KEY_EXPANDED = "expanded";

    private static final int DEFAULT_DURATION = 300;

    /**
     * 当不断改变子View的高度的时候，就会间接触发ExpandableLinearLayout对onMeasure的调用，从而改变wms的值。
     */
    private int wms;
    /**
     * 当不断改变子View的高度的时候，就会间接触发ExpandableLinearLayout对onMeasure的调用，从而改变hms的值。
     */
    private int hms;

    private List<View> expandableViews;

    private int duration = DEFAULT_DURATION;
    private boolean expanded = false;

    /**
     * 注意还有这个差值器，FastOutSlowInInterpolator，是v4包里面的。
     */
    private Interpolator interpolator = new FastOutSlowInInterpolator();
    private AnimatorSet animatorSet;

    private OnExpansionUpdateListener listener;

    public ExpandableLinearLayout(Context context) {
        super(context);
        init(null);
    }

    public ExpandableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ExpandableLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ExpandableLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ExpandableLayout);
            duration = a.getInt(R.styleable.ExpandableLayout_el_duration, DEFAULT_DURATION);
            expanded = a.getBoolean(R.styleable.ExpandableLayout_el_expanded, false);
            a.recycle();
        }

        expandableViews = new ArrayList<>();

        // We only support vertical layouts for now
        setOrientation(VERTICAL);
    }

    /**
     * 保存ExpandableLinearLayout的状态
     * 可以看到这个Bundle里面包含了父类需要保存的状态，
     *
     * （1）那么如何获取父类的状态？可以看到Parcelable superState = super.onSaveInstanceState();
     * （2）然后保存呢？保存父类的状态。bundle.putParcelable(KEY_SUPER_STATE, superState);
     *
     * @return
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        Bundle bundle = new Bundle();

        bundle.putBoolean(KEY_EXPANDED, expanded);
        bundle.putParcelable(KEY_SUPER_STATE, superState);

        return bundle;
    }

    /**
     * 恢复ExpandableLinearLayout的状态
     * @return
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        /**
         * 可以看到这里state直接转化为Bundle了。
         */
        Bundle bundle = (Bundle) state;
        expanded = bundle.getBoolean(KEY_EXPANDED);
        Parcelable superState = bundle.getParcelable(KEY_SUPER_STATE);

        for (View expandableView : expandableViews) {
            expandableView.setVisibility(expanded ? VISIBLE : GONE);
        }

        super.onRestoreInstanceState(superState);
    }

    /**
     * 可以看到重写了addView方法，目的也是为了将子View放到expandableViews中去。
     * 还应该注意到这里使用了自定义的LayoutParams，自定义LayoutParams记录了ExpandableLinearLayout的状态是展开还是收起。
     * @param child
     * @param index
     * @param params
     */
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        LayoutParams lp = (LayoutParams) params;
        if (lp.expandable) {
            expandableViews.add(child);
            child.setVisibility(expanded ? VISIBLE : GONE);
        }

        super.addView(child, index, params);
    }

    @Override
    public void removeView(View child) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.expandable) {
            expandableViews.remove(child);
        }

        super.removeView(child);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        wms = widthMeasureSpec;
        hms = heightMeasureSpec;
    }

    /**
     * 可以看到还可以重写这个方法来定制自己的LayoutParams
     * @param attrs
     * @return
     */
    @Override
    public LinearLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (animatorSet != null) {
            animatorSet.cancel();
        }
        super.onConfigurationChanged(newConfig);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void toggle() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    }

    /**
     * 展开
     */
    @SuppressLint("WrongCall")
    public void expand() {
        if (expanded) {
            return;
        }

        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }

        expanded = true;

        for (View expandableView : expandableViews) {
            LayoutParams lp = (LayoutParams) expandableView.getLayoutParams();

            // Calculate view's original height
            expandableView.setVisibility(View.VISIBLE);
            lp.weight = lp.originalWeight;
            lp.height = lp.originalHeight;
            super.onMeasure(wms, hms);
        }

        for (View expandableView : expandableViews) {
            animateHeight(expandableView, expandableView.getMeasuredHeight());
        }

        if (animatorSet != null) {
            animatorSet.start();
        }
    }

    /**
     * 收起
     */
    public void collapse() {
        if (!expanded) {
            return;
        }

        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }

        expanded = false;

        for (View expandableView : expandableViews) {
            animateHeight(expandableView, 0);
        }

        if (animatorSet != null) {
            animatorSet.start();
        }
    }

    public void setOnExpansionUpdateListener(OnExpansionUpdateListener listener) {
        this.listener = listener;
    }

    /**
     * 此函数会作用在每个View上面，比如有两个子View，子ViewA和子ViewB。
     * ViewA和ViewB会不断的动画，相当于一起动画。一起慢慢的展开或者收起。
     * @param view
     * @param targetHeight
     */
    private void animateHeight(final View view, final int targetHeight) {
        if (animatorSet == null) {
            animatorSet = new AnimatorSet();
            animatorSet.setInterpolator(interpolator);
            animatorSet.setDuration(duration);
        }

        final LayoutParams lp = (LayoutParams) view.getLayoutParams();
        lp.weight = 0;
        int height = view.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(height, targetHeight);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                view.getLayoutParams().height = (Integer) valueAnimator.getAnimatedValue();
                view.requestLayout();

                if (listener != null) {
                    float fraction = targetHeight == 0 ? 1 - valueAnimator.getAnimatedFraction() : valueAnimator.getAnimatedFraction();
                    listener.onExpansionUpdate(fraction);
                }
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (targetHeight == 0) {
                    view.setVisibility(GONE);
                } else {
                    lp.height = lp.originalHeight;
                    lp.weight = lp.originalWeight;
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        animatorSet.playTogether(animator);
    }

    /**
     * 继承自LinearLayout.LayoutParams，保存了ExpandableLinearLayout的状态是收起还是展开。
     */
    public static class LayoutParams extends LinearLayout.LayoutParams {
        private final boolean expandable;
        private final int originalHeight;
        private final float originalWeight;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout);
            expandable = a.getBoolean(R.styleable.ExpandableLayout_layout_expandable, false);
            originalHeight = this.height;
            originalWeight = this.weight;
            a.recycle();
        }
    }

    public interface OnExpansionUpdateListener {
        void onExpansionUpdate(float expansionFraction);
    }
}

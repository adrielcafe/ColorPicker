package org.xdty.preference;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.xdty.preference.colorpicker.ColorPickerDialog;
import org.xdty.preference.colorpicker.ColorPickerSwatch;
import org.xdty.preference.colorpicker.R;

/**
 * A preference showing a {@link ColorPickerDialog} to allow the user to select a color to save as
 * {@link Preference}.
 */
public class ColorPreference extends Preference implements ColorPickerSwatch
        .OnColorSelectedListener {

    private static final int DEFAULT_VALUE = Color.BLACK;

    private int mTitle = R.string.color_picker_default_title;
    private int mCurrentValue;
    private int[] mColors;
    private int mColumns;
    private boolean mMaterial;

    private ImageView mColorView;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable
                .ColorPreference, 0, 0);
        try {
            int id = a.getResourceId(R.styleable.ColorPreference_cp_colors, R.array.default_rainbow);
            if (id != 0) {
                mColors = getContext().getResources().getIntArray(id);
            }
            id = a.getResourceId(R.styleable.ColorPreference_cp_dialogTitle, 0);
            if (id != 0) {
                mTitle = a.getResourceId(R.styleable.ColorPreference_cp_dialogTitle,
                        R.string.color_picker_default_title);
            }
            mColumns = a.getInt(R.styleable.ColorPreference_cp_columns, 5);
            mMaterial = a.getBoolean(R.styleable.ColorPreference_cp_material, true);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
	    if(mColorView == null) {
		    mColorView = new ImageView(getContext());
		    int size = (int) dpToPx(32);
		    mColorView.setLayoutParams(new ViewGroup.LayoutParams(size, size));
		    updateShownColor();
		    ViewGroup w = (ViewGroup) holder.itemView.findViewById(android.R.id.widget_frame);
		    w.setVisibility(View.VISIBLE);
		    w.addView(mColorView);
		    if (mMaterial) {
			    TextView textTitle = (TextView) holder.itemView.findViewById(android.R.id.title);
			    TextView textSummary = (TextView) holder.itemView.findViewById(android.R.id.summary);

			    textTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			    textSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			    textTitle.setTextColor(getColor(android.R.attr.textColorPrimary));
			    textSummary.setTextColor(getColor(android.R.attr.textColorSecondary));

			    View parent = (View) textSummary.getParent().getParent();
			    parent.setPadding((int) dpToPx(16), 0, (int) dpToPx(16), 0);
		    }
	    }
    }

    @Override
    protected void onClick() {
        int[] colors = mColors.length != 0 ? mColors : new int[]{
                Color.BLACK, Color.WHITE, Color
                .RED, Color.GREEN, Color.BLUE
        };
        ColorPickerDialog d = ColorPickerDialog.newInstance(mTitle,
                colors, mCurrentValue, mColumns,
                ColorPickerDialog.SIZE_SMALL);
        d.setOnColorSelectedListener(this);
        try {
            d.show(getActivity().getFragmentManager(), null);
        } catch (Exception e){ }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue = this.getPersistedInt(DEFAULT_VALUE);
        } else {
            // Set default state from the XML attribute
            mCurrentValue = (Integer) defaultValue;
            persistInt(mCurrentValue);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent,
            // use superclass state
            return superState;
        }

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current
        // setting value
        myState.current = mCurrentValue;
        myState.colors = mColors;
        myState.columns = mColumns;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        // Update own values
        mCurrentValue = myState.current;
        mColors = myState.colors;
        mColumns = myState.columns;

        // Update shown color
        updateShownColor();

        // Set this Preference's widget to reflect the restored state
        //mNumberPicker.setValue(myState.value);
    }

    @Override
    public void onColorSelected(int color) {
        persistInt(color);
        mCurrentValue = color;
        updateShownColor();
    }

    private void updateShownColor() {
	    mColorView.setImageDrawable(new ColorCircleDrawable(mCurrentValue));
        mColorView.invalidate();
    }

    /**
     * Convert a dp size to pixel.
     * Useful for specifying view sizes in code.
     *
     * @param dp The size in density-independent pixels.
     * @return {@code px} - The size in generic pixels (density-dependent).
     */
    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }

    private int getColor(int attrId) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(attrId, typedValue, true);
        TypedArray arr = getContext().obtainStyledAttributes(typedValue.data, new int[]{attrId});
        int color = arr.getColor(0, -1);
        arr.recycle();
        return color;
    }

    private static class SavedState extends BaseSavedState {
        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        // Member that holds the preference's values
        int current;
        int[] colors;
        int columns;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's values
            current = source.readInt();
            source.readIntArray(colors);
            columns = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's values
            dest.writeInt(current);
            dest.writeIntArray(colors);
            dest.writeInt(columns);
        }
    }

	private class ColorCircleDrawable extends Drawable {
		private final Paint mPaint;
		private int mRadius = 0;

		public ColorCircleDrawable(final @ColorInt int color) {
			mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mPaint.setColor(color);
		}

		public void setColor(@ColorInt int color) {
			mPaint.setColor(color);
			invalidateSelf();
		}

		@Override
		public void draw(final Canvas canvas) {
			final Rect bounds = getBounds();
			canvas.drawCircle(bounds.centerX(), bounds.centerY(), mRadius, mPaint);
		}

		@Override
		protected void onBoundsChange(final Rect bounds) {
			super.onBoundsChange(bounds);
			mRadius = Math.min(bounds.width(), bounds.height()) / 2;
		}

		@Override
		public void setAlpha(final int alpha) {
			mPaint.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(final ColorFilter cf) {
			mPaint.setColorFilter(cf);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}
	}

    public Activity getActivity() {
        if (getContext() instanceof ContextThemeWrapper){
            if (((ContextThemeWrapper) getContext()).getBaseContext() instanceof Activity) {
                return (Activity) ((ContextThemeWrapper) getContext()).getBaseContext();
            }
        } else if (getContext() instanceof Activity) {
            return (Activity) getContext();
        }
        return null;
    }
}
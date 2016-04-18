package com.chiralcode.colorpicker;


import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import static android.view.View.generateViewId;

public class ColorPickerPreference extends DialogPreference {

    public static final int DEFAULT_COLOR = Color.WHITE;

    private int selectedColor;
    private ColorPicker colorPickerView;

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateDialogView() {

        RelativeLayout relativeLayout = new RelativeLayout(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        colorPickerView = new ColorPicker(getContext());
        colorPickerView.setId(generateViewId());

        relativeLayout.addView(colorPickerView, layoutParams);

        return relativeLayout;

    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        colorPickerView.setColor(selectedColor);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(null); // remove dialog title to get more space for color picker
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && shouldPersist()) {
            if (callChangeListener(colorPickerView.getColor())) {
                selectedColor = colorPickerView.getColor();
                setSummary(Integer.toHexString(selectedColor));
                persistInt(selectedColor);
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		super.onSetInitialValue(restoreValue, defaultValue);

		if (restoreValue) // There is a value already in persistent storage
			selectedColor = getPersistedInt(DEFAULT_COLOR);
		else if (defaultValue != null) {// There is a default value
			selectedColor = (Integer) defaultValue;
			if (shouldPersist())
				persistInt(selectedColor);
		} else
			// nothing there - make up something!
			selectedColor = DEFAULT_COLOR;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_COLOR);
    }
	@Override
	public CharSequence getSummary() {
		return Integer.toHexString(selectedColor);
	}

}

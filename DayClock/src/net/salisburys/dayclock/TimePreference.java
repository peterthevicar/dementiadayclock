package net.salisburys.dayclock;

import java.util.Date;
import java.util.TimeZone;

import net.salisburys.dayclock.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference {
	private Time time;
	private TimePicker picker = null;

	public TimePreference(Context ctxt) {
		this(ctxt, null);
	}

	public TimePreference(Context ctxt, AttributeSet attrs) {
		this(ctxt, attrs, android.R.attr.dialogPreferenceStyle);
	}

	public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
		super(ctxt, attrs, defStyle);

		setPositiveButtonText(ctxt.getString(R.string.label_OK));
		setNegativeButtonText(ctxt.getString(R.string.label_cancel));
		// Must use UTC times for storage to allow use across time zones
		time = new Time("UTC");
	}

	@Override
	protected View onCreateDialogView() {
		super.onCreateDialogView();
		picker = new TimePicker(getContext());
		picker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
		return (picker);
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		picker.setCurrentHour(time.hour);
		picker.setCurrentMinute(time.minute);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			// Recalculate millis in case day field in time has got set somehow
			Long millis = (long) 60000
					* (picker.getCurrentMinute() + (60 * picker
							.getCurrentHour()));
			if (callChangeListener("" + millis)) {
				persistString("" + millis);
				time.set(millis);
				notifyChanged();
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return (a.getString(index));
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		super.onSetInitialValue(restoreValue, defaultValue);

		if (restoreValue) // There is a value already in persistent storage
			time.set(Long.parseLong(getPersistedString("0")));
		else if (defaultValue != null) {// There is a default value
			time.set(Long.parseLong((String) defaultValue));
			if (shouldPersist())
				persistString((String) defaultValue);
		} else
			// nothing there - make up something!
			time.set(0);
	}

	@Override
	public CharSequence getSummary() {
		if (time == null) {
			return null;
		}
		java.text.DateFormat df = DateFormat.getTimeFormat(getContext());
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		return df.format(new Date(time.toMillis(true)));
	}
}

package net.salisburys.dayclock;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public final class TextPrefSummary extends EditTextPreference {
	public TextPrefSummary(final Context ctx, final AttributeSet attrs) {
		super(ctx, attrs);
	}

	public TextPrefSummary(final Context ctx) {
		super(ctx);
	}

	@Override
	public CharSequence getSummary() {
		return getText();
	}
}

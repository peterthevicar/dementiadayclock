package net.salisburys.dayclock;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Created by peter on 22/04/15.
 */
public final class ListPrefSummary extends ListPreference {
    public ListPrefSummary(final Context ctx, final AttributeSet attrs) {
        super(ctx, attrs);
    }

    public ListPrefSummary(final Context ctx) {
        super(ctx);
    }

    @Override
    public CharSequence getSummary() {
        return getEntry();
    }

}

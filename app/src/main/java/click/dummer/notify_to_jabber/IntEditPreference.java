package click.dummer.notify_to_jabber;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class IntEditPreference extends EditTextPreference {

    public IntEditPreference(Context context) {
        super(context);
    }

    public IntEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IntEditPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return String.valueOf(getPersistedInt(0));
    }

    @Override
    protected boolean persistString(String value) {
        if (value.equals("")) value="0";
        int val = Integer.valueOf(value);
        if (val < 0) val = 0;
        return persistInt(val);
    }
}

package net.salisburys.dayclock;

import java.util.Calendar;
import java.util.Set;

import android.util.Log;
import android.util.TypedValue;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	SharedPreferences sharedPrefs;
	SettingsFragment settingsFrag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//getActionBar().setDisplayHomeAsUpEnabled(true);
		PreferenceManager.setDefaultValues(this, R.xml.preferences_fragment,
				false);
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		TickHandler tickHandler = new TickHandler();
		tickHandler.scheduleTick(0);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new MainDisplayFragment()).commit();
		}
	}

	/**
	 * ******* Main display
	 * 
	 * @author peter
	 * 
	 */
	public static class MainDisplayFragment extends Fragment {
		public MainDisplayFragment() {
		}

		private Activity current_act;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}

		@Override
		public void onAttach(Activity attach_act) {
			super.onAttach(attach_act);
			current_act = attach_act;
		}

		@Override
		public void onResume() {
			super.onResume();
			((MainActivity) current_act).refreshDisplay();
		}

	}

	private class TickHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				if (refreshDisplay()) // successfully refreshed so wait 3
					// seconds
					scheduleTick(3000);
				else
					// failed to refresh so try again soon
					scheduleTick(200);

				break;
			default:
				showToast("Error");
				break;
			}
		}

		public void scheduleTick(long interval) {
			Message m = this.obtainMessage(1);
			this.sendMessageDelayed(m, interval);
		}
	}

	private Long getLong(String key, String defValue) {
		return Long.parseLong(sharedPrefs.getString(key, defValue));
	}

	private Boolean testDay(String key) {
		// first find today's day-of-week number
		Calendar now = Calendar.getInstance();
		String nowDOW = "" + now.get(Calendar.DAY_OF_WEEK);
		Set<String> daySet = sharedPrefs.getStringSet(key, null);
		return daySet.contains(nowDOW);
	}

	private boolean refreshDisplay() {
		// Wait for main fragment to be live
		if (findViewById(R.id.prologue) == null) {
			return false;
		}
		try {
			// Get general preference values

			String sPrologue = sharedPrefs.getString("prologue", "");
			Integer iPrologueSize = Integer.parseInt(sharedPrefs.getString(
					"prologueSize", ""));
			Integer iDOWSize = Integer.parseInt(sharedPrefs.getString(
					"dateDOWSize", ""));
			Integer iTODSize = Integer.parseInt(sharedPrefs.getString(
					"dateTODSize", ""));

			// Now decide which period is current
			//Calendar now = Calendar.getInstance();
			//Long nowMs = 1000 * (long) (now.get(Calendar.SECOND) + 60 * (now
			//		.get(Calendar.MINUTE) + 60 * now.get(Calendar.HOUR_OF_DAY)));
			Time now = new Time(); now.setToNow();
			Long nowMs = 1000 * (long) (now.second + 60 * (now.minute + 60 * now.hour));
			//Can't use toMillis because it's time zone dependent
			//Long nowMs = now.toMillis(true);
			String datePeriod = "";

			// Check each period to find the best fit for the current time
			// I use the last active period with the latest start time
			Long bestMs = (long) -1; // Something must be better!
			String bestPrefN = "";
			for (Integer per = 1; per <= 8; per++) {
				String prefN = "pref_period" + per;
				long startMs = Long.parseLong(sharedPrefs.getString(prefN
						+ "_start", "0"));
				// Log.e("N,S,B", "" + per + ":" + nowMs + "," + startMs + "," +
				// bestMs);
				// First check we're past the start time (and always use period
				// with
				// best (greatest) start time)
				if (nowMs >= startMs && startMs >= bestMs) {
					// Now check we're not past the end time if specified
					if (sharedPrefs.getBoolean(prefN + "_tillNext", true)
							|| getLong(prefN + "_end", "0") >= nowMs) {
						// Now check it's valid for today
						if ((sharedPrefs.getBoolean(prefN + "_allDays", true) || testDay(prefN
								+ "_days"))
								// Ignore periods with empty titles
								&& sharedPrefs.getString(prefN + "_text", "")
										.length() > 0) {
							// Yay! We've found a better period
							bestMs = startMs;
							bestPrefN = prefN;
						}
					}
				}
			}

			// Now we know the period we can fill in the screen
			// First get the correct values out
			datePeriod = sharedPrefs.getString(
					bestPrefN.concat("_text"), "");
			Boolean contrastColour = sharedPrefs.getBoolean(bestPrefN + "_contrast", true);
			String textColour = (contrastColour? "textColourCont": "textColour");
			Integer iTextColour = sharedPrefs.getInt(textColour, 0xff000000);
			String bgColour = (contrastColour? "bgColourCont": "bgColour");
			Integer iBgColour = sharedPrefs.getInt(bgColour, 0xff000000);
			//showToast(""+iTextColour);
			// Set background
			RelativeLayout displayView = (RelativeLayout) findViewById(R.id.displayView);
			displayView.setBackgroundColor(iBgColour);
			
			// Fill in the main elements of the display
			TextView prologueText = (TextView) findViewById(R.id.prologue);
			prologueText.setText(sPrologue);
			prologueText
					.setTextSize(TypedValue.COMPLEX_UNIT_DIP, iPrologueSize);
			prologueText.setTextColor(iTextColour);
			TextView dateDOW = (TextView) findViewById(R.id.dateDOW);
			String s_dateDOW = (String) DateFormat.format("EEEE",
					System.currentTimeMillis());
			dateDOW.setText(s_dateDOW);
			dateDOW.setTextSize(TypedValue.COMPLEX_UNIT_DIP, iDOWSize);
			dateDOW.setTextColor(iTextColour);

			TextView tvDateTOD = (TextView) findViewById(R.id.dateTOD);
			tvDateTOD.setText(datePeriod);
			tvDateTOD.setTextSize(TypedValue.COMPLEX_UNIT_DIP, iTODSize);
			tvDateTOD.setTextColor(iTextColour);
			return true;
		} catch (Exception e) {
			showToast("Error in settings:" + e.getMessage());
			return false;
		}
	}

	/**
	 * ************ Options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
		case R.id.action_preferences:
			// Add preferences fragment if not there already
			if (getFragmentManager().getBackStackEntryCount() < 1) {
				FragmentTransaction trans = getFragmentManager()
						.beginTransaction();
				settingsFrag = new SettingsFragment();
				trans.replace(R.id.container, settingsFrag);
				trans.addToBackStack(null);
				trans.commit();
			}
			return true;
		case android.R.id.home:
			// Implement UP
			if (getFragmentManager().getBackStackEntryCount() >= 1) {
				FragmentTransaction trans = getFragmentManager()
						.beginTransaction();
				getFragmentManager().popBackStack();
				trans.commit();
			}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * ******* Preferences
	 */
	public static class SettingsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences_fragment);
		}

		@Override
		public void onStart() {
			super.onStart();
			setPeriodSummaries(this);
		}
	}

	public static void setPeriodSummaries(SettingsFragment sf) {
		// Set the period preferences summaries
		if (sf == null)
			return;
		for (int per = 1; per <= 8; per++) {
			String prefN = "pref_period" + per;
			PreferenceScreen prefScr = (PreferenceScreen) sf
					.findPreference(prefN);
			if (prefScr != null) {
				Preference prefText = prefScr.getPreference(0);
				if (prefText.getSummary().length() > 0) {
					Preference prefStart = prefScr.getPreference(2);
					prefScr.setSummary(prefText.getSummary() + " "
							+ prefStart.getSummary());
				} else
					prefScr.setSummary(R.string.summary_disabled);
			}
			// Force the parent screen summaries to update
			prefScr = (PreferenceScreen) sf.findPreference("prefs_root");
			if (prefScr != null)
				((BaseAdapter) prefScr.getRootAdapter()).notifyDataSetChanged();
		}
	}

	OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key) {
			//Log.e("L", "Change");
			setPeriodSummaries(settingsFrag);
		}
	};

	@Override
	public void onResume() {
		super.onResume();

		sharedPrefs.registerOnSharedPreferenceChangeListener(listener);

		refreshDisplay();
	}

	@Override
	protected void onPause() {
		super.onPause();
		sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener);
	}

	/*
	 * *********** Utility
	 */
	public void showToast(CharSequence text) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
}

package net.salisburys.dayclock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
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
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPrefs;
    private SettingsFragment settingsFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //   ActionBar actionBar = getSupportActionBar();
        //   actionBar.setDisplayHomeAsUpEnabled(true);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_fragment,
                true);
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
            current_act = getActivity();
            return rootView;
        }

//		@Override
//		public void onAttach(Activity attach_act) {
//			super.onAttach(attach_act);
//			current_act = attach_act;
//		}

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
                        scheduleTick(500);

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
        return daySet != null && daySet.contains(nowDOW);
    }

    private boolean refreshDisplay() {
        // Wait for main fragment to be live
        if (findViewById(R.id.prologue) == null) {
            return false;
        }
        try {
            // Make sure 'keep on' flag is set correctly
            if (sharedPrefs.getBoolean("keepScreenOn", true))
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            // Get general preference values
            String sPrologue = sharedPrefs.getString("prologue", "");
            Integer iPrologueSize = Integer.parseInt(sharedPrefs.getString(
                    "prologueSize", "20"));
            Integer iDOWSize = Integer.parseInt(sharedPrefs.getString(
                    "dateDOWSize", "20"));
            Integer iPERSize = Integer.parseInt(sharedPrefs.getString(
                    "datePERSize", "20"));
            Integer iTIMSize = Integer.parseInt(sharedPrefs.getString(
                    "dateTIMSize", "20"));
            String sBottom = sharedPrefs.getString("bottomText", "");
            Integer iBottomSize = Integer.parseInt(sharedPrefs.getString(
                    "bottomSize", "10"));


            // Now decide which period is current
            //Calendar now = Calendar.getInstance();
            //Long nowMs = 1000 * (long) (now.get(Calendar.SECOND) + 60 * (now
            //		.get(Calendar.MINUTE) + 60 * now.get(Calendar.HOUR_OF_DAY)));
            Time now = new Time();
            now.setToNow();
            Long nowMs = 1000 * (long) (now.second + 60 * (now.minute + 60 * now.hour));
            //Can't use toMillis because it's time zone dependent
            //Long nowMs = now.toMillis(true);
            String datePeriod;

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
            String prefColours = sharedPrefs.getString(bestPrefN + "_colour", "1");
            Integer iTextColour = sharedPrefs.getInt("textColour" + prefColours, 0xff000000);
            Integer iBgColour = sharedPrefs.getInt("bgColour" + prefColours, 0xffffffff);
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

            TextView tvDateDOW = (TextView) findViewById(R.id.dateDOW);
            Integer iDowOffset = Integer.parseInt(sharedPrefs.getString(
                    "dateTIMEDowOffset", "0"));
            String s_dateDOW = (String) DateFormat.format("EEEE",
                    System.currentTimeMillis() - (iDowOffset * 3600 * 1000));
            tvDateDOW.setText(s_dateDOW);
            tvDateDOW.setTextSize(TypedValue.COMPLEX_UNIT_DIP, iDOWSize);
            tvDateDOW.setTextColor(iTextColour);

            TextView tvDatePER = (TextView) findViewById(R.id.datePER);
            tvDatePER.setText(datePeriod);
            tvDatePER.setTextSize(TypedValue.COMPLEX_UNIT_DIP, iPERSize);
            tvDatePER.setTextColor(iTextColour);

            TextView tvDateTIM = (TextView) findViewById(R.id.dateTIM);
            String sTIMFormat = sharedPrefs.getString("dateTIMFormat", "h:mm a");
            if (sTIMFormat.equals("CUSTOM"))
                sTIMFormat = sharedPrefs.getString("dateTIMFormatCust", "h:mm a");
            String sDateTIM = (String) DateFormat.format(sTIMFormat,
                    System.currentTimeMillis());
            tvDateTIM.setText(sDateTIM);
            tvDateTIM.setTextSize(TypedValue.COMPLEX_UNIT_DIP, iTIMSize);
            tvDateTIM.setTextColor(iTextColour);

            TextView bottomText = (TextView) findViewById(R.id.bottomText);
            bottomText.setText(sBottom);
            bottomText
                    .setTextSize(TypedValue.COMPLEX_UNIT_DIP, iBottomSize);
            bottomText.setTextColor(iTextColour);

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

    private boolean stackInUse = false;

    private boolean popStack() {
        // pop the top fragment off the stack or return true
        if (stackInUse) {
            FragmentTransaction trans = getFragmentManager()
                    .beginTransaction();
            getFragmentManager().popBackStack();
            trans.commit();
            stackInUse = false;
            return false;
        } else return true;
    }

    @Override
    public void onBackPressed() {
        // This is to avoid the app closing when back is pressed in settings fragment
        if (popStack()) super.onBackPressed();
    }

    private void displayDialog(int title, String message) {
        Context myViewContext = findViewById(R.id.displayView).getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(myViewContext);
        builder.setTitle(title);
        builder.setMessage(message)
                .setCancelable(true)
                .setNegativeButton(R.string.label_OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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
                    stackInUse = true;
                }
                return true;
            case R.id.action_about:
                String myVersionName = "0";
                try {
                    myVersionName = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                displayDialog(R.string.about_dialog_title, getString(R.string.about_dialog_version) + " " + myVersionName + getString(R.string.about_dialog_text));
                return true;
            case R.id.action_thanks:
                displayDialog(R.string.thanks_dialog_title, getString(R.string.thanks_dialog_text));
                return true;
            case R.id.action_help:
                displayDialog(R.string.help_dialog_title, getString(R.string.help_dialog_text));
                return true;
            case R.id.action_load_save:
                if (!stackInUse) {
                    LoadSaveFragment loadSaveFragment = new LoadSaveFragment();
                    FragmentTransaction trans = getFragmentManager().beginTransaction();
                    trans.add(R.id.container, loadSaveFragment);
                    trans.addToBackStack(null);
                    trans.commit();
                    stackInUse = true;
                }
                return true;
            case android.R.id.home:
                // Implement UP
                popStack();
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

    private static void setPeriodSummaries(SettingsFragment sf) {
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

    private final OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
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
    * ******* Load/Save fragment
    * Started from http://stackoverflow.com/questions/10864462/how-can-i-backup-sharedpreferences-to-sd-card
    */
    final String dirName = Environment.getExternalStorageDirectory().getPath() + "/" +"DayClock"+"/"; // For saving and loading settings
    final String fullName = dirName + "DayClockPrefs.dat";

    public void lsButSave(View view) {
        boolean success = false;
        if (verifyStoragePermissions(this, dirName)) {
            //TODO: are you sure?
            ObjectOutputStream output = null;
            try {
                output = new ObjectOutputStream(new FileOutputStream(fullName));
                output.writeObject(sharedPrefs.getAll());
                success = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (output != null) {
                        output.flush();
                        output.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (success)
            displayDialog(R.string.save_dialog_title, getString(R.string.save_dialog_text) +"\n"+ fullName);
        else
            displayDialog(R.string.save_dialog_title, getString(R.string.save_dialog_fail) +"\n"+ fullName);
    }

    public void lsButLoad(View view) {
        boolean success=false;
        if (verifyStoragePermissions(this, dirName)) {
            ObjectInputStream input = null;
            try {
                input = new ObjectInputStream(new FileInputStream(fullName));
                SharedPreferences.Editor prefEdit = sharedPrefs.edit();
                prefEdit.clear();
                Map<String, ?> entries = (Map<String, ?>) input.readObject();
                for (Map.Entry<String, ?> entry : entries.entrySet()) {
                    Object v = entry.getValue();
                    String key = entry.getKey();

                    if (v instanceof Boolean)
                        prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                    else if (v instanceof Float)
                        prefEdit.putFloat(key, ((Float) v).floatValue());
                    else if (v instanceof Integer)
                        prefEdit.putInt(key, ((Integer) v).intValue());
                    else if (v instanceof Long)
                        prefEdit.putLong(key, ((Long) v).longValue());
                    else if (v instanceof String)
                        prefEdit.putString(key, ((String) v));
                }
                prefEdit.commit();
                success = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (success)
            displayDialog(R.string.load_dialog_title, getString(R.string.load_dialog_text) +"\n"+ fullName);
        else
            displayDialog(R.string.load_dialog_title, getString(R.string.load_dialog_fail) +"\n"+ fullName);
    }

    public void lsButClear(View view) {
        // Clear all the saved settings
        sharedPrefs.edit().clear().apply();
        // Need to reload default values for prefs
        PreferenceManager.setDefaultValues(this, R.xml.preferences_fragment, true);
    }

    public void lsButDone(View view) {
        // Remove the fragment
        popStack();
    }
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to grant permissions
     */
    public static boolean verifyStoragePermissions(Activity activity, String dirName) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean success = false;

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Have the right permissions so make sure directory exists
            File saveDir = new File(dirName);
            if (!saveDir.isDirectory()) {
                saveDir.mkdirs();
            }
            success = (saveDir.isDirectory());
        }
        return success;
    }

    public static class LoadSaveFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.load_save_fragment, container, false);
        }

    }

    /*
     * *********** Utility
     */
    private void showToast(CharSequence text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}

# Day Clock

Day Clock is an Android orientation clock for people with dementia. It shows the weekday and a period such as Morning or Afternoon. You can also show the date and time, change the size of each line, and choose different colours for different times of day.

[Install Day Clock from Google Play](https://play.google.com/store/apps/details?id=net.salisburys.dayclock). Open **Help** in the app for more details.

## Set up the display

In **Settings → General**, choose a preset under **Time display format** or select **Custom** and enter a format in the custom field. Use `MMMM d, yyyy` for a full date with the year, or `MMMM d, yyyy · h:mm a` to include the time. Set the first or last line’s text size to `0` to hide it. Adjust the sizes on the device where the clock will run so the full date fits.

**Keep screen on always** keeps the display on while Day Clock is visible. To open the clock automatically after a restart, configure startup separately on the device.

## Copy settings to another device

1. On the first device, open **Save/Restore settings** and tap **Save settings**. The app shows where it saved the file.
2. Copy `DayClock/DayClockPrefs.dat` from shared storage to the `DayClock` folder in shared storage on the other device. Create that folder if needed.
3. On the other device, open **Save/Restore settings** and tap **Restore Settings**. This replaces the current Day Clock settings. Recheck any periods that run only on selected weekdays.

Repeat these steps to copy later changes.

## Build from source

Clone the repository and open it in Android Studio.

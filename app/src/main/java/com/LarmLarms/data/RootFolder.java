package com.larmlarms.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.larmlarms.BuildConfig;
import com.larmlarms.Constants;
import com.larmlarms.ringing.RingingService;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages everything about the root folder, including registering alarms to ring with the system
 * and saving changes to disk.
 */
public class RootFolder extends AlarmGroup {
    /**
     * Tag of the class for logging purposes.
     */
    private static final String TAG = "RootFolder";

    /**
     * The name of the file that stores the alarms. Found within private storage for the app.
     */
    private static final String ALARM_STORE_FILE_NAME = "alarms.txt";

    /**
     * Executor service for saving data asynchronously. Only really needs one thread since it's
     * writing to file.
     */
    private final ExecutorService execService;

    /**
     * The current alarm to ring next.
     */
    @Nullable
    private Alarm currNextAlarm;

    /**
     * Current context (required to save to disk).
     */
    private final Context context;

    /**
     * Initializes a new root folder with a name and contents.
     *
     * @param name the new name of the folder
     */
    public RootFolder(@Nullable String name, @NotNull Context c) {
        super(name, RootFolder.getAlarmsFromDisk(c));
        execService = Executors.newSingleThreadExecutor();
        context = c;

        ItemInfo info = findNextRingingAlarm();
        currNextAlarm = registerAlarm(context, info);
    }

    // shouldn't need a copy constructor...

    /* *************************************  Folder Overrides  ********************************* */

    /**
     * Sets the items within the folder. If the new list is invalid (the list or any items
     * within it are null), will not do anything.
     * @param items a new list of items to use, can be null
     */
    @Override
    public synchronized void setItems(@Nullable List<Item> items) {
        super.setItems(items);
        save();
    }

    /* *********************************  Root-Specific Methods  ******************************** */

    @Nullable @Contract(pure = true)
    public final Alarm getCurrNextAlarm() {
        if (currNextAlarm == null) return null;
        return new Alarm(currNextAlarm);
    }

    /**
     * Saves everything to disk and sets the alarms to ring.
     */
    private void save() {
        execService.execute(() -> {
            writeAlarmsToDisk(context, this);
            ItemInfo info = findNextRingingAlarm();
            if (info.item != currNextAlarm) currNextAlarm = registerAlarm(context, info);
        });
    }

    /**
     * Convenience method for findNextRingingAlarm(). Searches for the next alarm that will ring
     * within the folder. Returns the listable and absolute index of the listable (within the
     * current dataset) within a ListableInfo struct.
     * @return a ListableInfo with alarm and absolute index (real index) filled correctly, alarm can
     * be null if there is no active alarm within the data given
     */
    @NotNull
    private ItemInfo findNextRingingAlarm() { return findNextRingingAlarm(items); }

    /**
     * Sets the next alarm to ring. Does not create a new pending intent, rather updates the current
     * one. Tells AlarmManager to wake up and call AlarmRingingService. Sends MSG_NEXT_ALARM if
     * necessary.
     * @param context the current context
     * @param alarmInfo the alarm to register as the next alarm
     */
    private synchronized static Alarm registerAlarm(@NotNull Context context, @NotNull ItemInfo alarmInfo) {
        Intent intent = new Intent(context, RingingService.class);
        if (alarmInfo.item != null) intent.putExtra(Constants.EXTRA_ITEM_INFO, alarmInfo);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent;

        // flags for the pending intent
        int PIFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            PIFlags = PIFlags | PendingIntent.FLAG_MUTABLE;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(context, 0, intent, PIFlags);
        }
        else {
            pendingIntent = PendingIntent.getService(context, 0, intent, PIFlags);
        }

        if (manager == null || pendingIntent == null) {
            if (BuildConfig.DEBUG) Log.i(TAG, "Couldn't reach alarm manager or the service to get" +
                    "the pending intent.");
            return (Alarm) alarmInfo.item;
        }

        if (alarmInfo.item == null) {
            if (BuildConfig.DEBUG) Log.i(TAG, "No next listable to register to ring.");
            manager.cancel(pendingIntent);
        }
        else {
            if (BuildConfig.DEBUG) Log.i(TAG, "Sent an intent to AlarmManager.");
            manager.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(((Alarm) alarmInfo.item).getAlarmTimeMillis(),
                            pendingIntent),
                    pendingIntent);
        }
        return (Alarm) alarmInfo.item;
    }

    /**
     * Searches for the next Alarm that will ring. Returns the listable and absolute index of the
     * listable (within the current dataset) within a ListableInfo struct.
     * @param data the dataset to look through, cannot be null
     * @return a ListableInfo with alarm and path filled correctly, alarm can be null if there is no
     * active alarm within the data given
     */
    @NotNull
    private synchronized static ItemInfo findNextRingingAlarm(@NotNull List<Item> data) {
        Alarm next = null;

        for (Item curr : data) {
            if (!curr.isActive()) continue;

            if (curr instanceof Alarm) {
                ((Alarm) curr).updateRingTime();

                // check whether it could be the next listable
                if (next == null || ((Alarm) curr).getAlarmTimeMillis() < next.getAlarmTimeMillis()) {
                    next = (Alarm) curr;
                }
            }
            else {
                ItemInfo poss = findNextRingingAlarm(((AlarmGroup) curr).getItems());
                // there is no candidate in this folder
                if (poss.item == null) continue;

                // we had no candidate before or this candidate is better
                if (next == null || ((Alarm) poss.item).getAlarmTimeMillis() < next.getAlarmTimeMillis()) {
                    next = (Alarm) poss.item;
                }
            }
        }

        return next == null ? new ItemInfo() : next.getInfo();
    }

    /**
     * Initializes alarm data from file.
     * @param context The context to get file streams from. This value may not be null.
     * @return A populated ArrayList of Listables or an empty one in the case of an error
     */
    @NotNull
    public synchronized static List<Item> getAlarmsFromDisk(@NotNull Context context) {
        ArrayList<Item> data = new ArrayList<>();

        try {
            FileInputStream is = context.openFileInput(ALARM_STORE_FILE_NAME);
            FileLock fileLock = is.getChannel().lock(0, Long.MAX_VALUE, true);
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader bReader = new BufferedReader(isr);

            String currLine = bReader.readLine();
            StringBuilder currFolder = null;
            Item currItem;

            while (currLine != null) {
                // end of the current folder
                if (!currLine.startsWith("\t") && currFolder != null) {
                    currItem = AlarmGroup.fromStoreString(context, currFolder.toString());
                    if (currItem != null) { data.add(currItem); }
                    currFolder = null;
                }

                // a top-level alarm
                if (currLine.startsWith("a")) {
                    currItem = Alarm.fromStoreString(context, currLine);
                    if (currItem != null) { data.add(currItem); }
                }
                // start of a top-level folder
                else if (currLine.startsWith("f")) {
                    currFolder = new StringBuilder(currLine);
                }
                // part of a top-level folder
                else if (currLine.startsWith("\t") && currFolder != null) {
                    currFolder.append('\n').append(currLine);
                }
                else {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Invalid line in alarms.txt: " + currLine);
                    return new ArrayList<>();
                }
                currLine = bReader.readLine();
            }
            // ends the folder in case there wasn't another alarm after it to close within the loop
            if (currFolder != null) {
                currItem = AlarmGroup.fromStoreString(context, currFolder.toString());
                if (currItem != null) { data.add(currItem); }
            }

            is.close();
            fileLock.release();
        }
        catch (IOException e) {
            if (BuildConfig.DEBUG) Log.e(TAG, e.getMessage());
            return new ArrayList<>();
        }

        if (BuildConfig.DEBUG) Log.i(TAG, "Alarm list retrieved successfully.");
        return data;
    }

    /**
     * Writes all alarms in myAdapter to app-specific file storage, with file name ALARM_STORE_FILE_NAME
     * @param context The context to get file streams from. This value may not be null.
     * @param data The data to write, doesn't include the AlarmGroup itself (uses getListables() to
     *             retrieve Listables to write). This value may not be null.
     */
    private synchronized static void writeAlarmsToDisk(@NotNull Context context, @NotNull AlarmGroup data) {
        try {
            File alarmFile = new File(context.getFilesDir(), ALARM_STORE_FILE_NAME);
            //noinspection ResultOfMethodCallIgnored
            alarmFile.createNewFile();

            FileOutputStream os = context.openFileOutput(ALARM_STORE_FILE_NAME, Context.MODE_PRIVATE);
            FileLock fileLock = os.getChannel().lock();

            StringBuilder builder = new StringBuilder();
            for (Item l : data.getItems()) {
                builder.append(l.toStoreString()).append('\n');
            }
            // delete the last '\n'
            if (builder.length() != 0) builder.deleteCharAt(builder.length() - 1);

            os.write(builder.toString().getBytes());
            os.close();
            fileLock.release();
        }
        catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, e.getMessage());
        }
    }
}

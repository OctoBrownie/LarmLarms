package com.larmlarms;

/**
 * Contains a bunch of constants used throughout the app, mostly Intent-related.
 */
public final class Constants {
    /**
     * An extra used for carrying a string path.
     */
    public final static String EXTRA_PATH = "com.apps.larmlarms.extra.PATH";

    /**
     * An extra used for carrying an item (of any type) as an edit string.
     */
    public final static String EXTRA_ITEM = "com.apps.larmlarms.extra.ITEM";

    // *********************************  Editing Intent Actions  *******************************
    /**
     * Intent action for creating a new alarm. Requires nothing else. Used with the editors.
     */
    public final static String ACTION_CREATE_ALARM = "com.apps.larmlarms.action.CREATE_ALARM";
    /**
     * Intent action for editing an existing Alarm. Requires that EXTRA_ITEM contain an alarm and
     * EXTRA_PATH contains the path. Used with the editors.
     */
    public final static String ACTION_EDIT_ALARM = "com.apps.larmlarms.action.EDIT_ALARM";
    /**
     * Intent action to create a new folder. Requires nothing else. Used with the editors.
     */
    public final static String ACTION_CREATE_FOLDER = "com.apps.larmlarms.action.CREATE_FOLDER";
    /**
     * Request code to edit an existing AlarmGroup. Requires that EXTRA_ITEM_INFO contain a
     * ItemInfo with item and path filled out. Used with the editors.
     */
    public final static String ACTION_EDIT_FOLDER = "com.apps.larmlarms.action.EDIT_FOLDER";

    // *****************************  Alarm Ringing Intent Actions  *****************************

    /**
     * Action used in intents, used for snoozing alarms. Does not assume anything about the contents
     * of the intent.
     */
    public static final String ACTION_SNOOZE = "com.apps.larmlarms.action.ACTION_SNOOZE";
    /**
     * Action used in intents, used for dismissing alarms. Does not assume anything about the
     * contents of the intent.
     */
    public static final String ACTION_DISMISS = "com.apps.larmlarms.action.ACTION_DISMISS";
}


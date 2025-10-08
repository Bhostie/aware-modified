package com.aware.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.aware.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * ContentProvider database helper<br/>
 * This class is responsible to make sure we have the most up-to-date database structures from plugins and sensors
 *
 * @author denzil
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private final boolean DEBUG = true;

    private String TAG = "AwareDBHelper";

    private String databaseName;
    private String[] databaseTables;
    private String[] tableFields;
    private int newVersion;
    private CursorFactory cursorFactory;
    private SQLiteDatabase database;
    private Context mContext;

    private HashMap<String, String> renamed_columns = new HashMap<>();

    public DatabaseHelper(Context context, String database_name, CursorFactory cursor_factory, int database_version, String[] database_tables, String[] table_fields) {
        super(context, database_name, cursor_factory, database_version);
        mContext = context;
        databaseName = database_name;
        databaseTables = database_tables;
        tableFields = table_fields;
        newVersion = database_version;
        cursorFactory = cursor_factory;
    }

    public void setRenamedColumns(HashMap<String, String> renamed) {
        renamed_columns = renamed;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (DEBUG) Log.w(TAG, "Creating database: " + db.getPath());
        for (int i = 0; i < databaseTables.length; i++) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + databaseTables[i] + " (" + tableFields[i] + ");");
            db.execSQL("CREATE INDEX IF NOT EXISTS time_device ON " + databaseTables[i] + " (timestamp, device_id);");
        }
        db.setVersion(newVersion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (DEBUG) Log.w(TAG, "Upgrading database: " + db.getPath());

        for (int i = 0; i < databaseTables.length; i++) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + databaseTables[i] + " (" + tableFields[i] + ");");

            //Modify existing tables if there are changes, while retaining old data. This also works for brand new tables, where nothing is changed.
            List<String> columns = getColumns(db, databaseTables[i]);

            db.execSQL("ALTER TABLE " + databaseTables[i] + " RENAME TO temp_" + databaseTables[i] + ";");

            db.execSQL("CREATE TABLE " + databaseTables[i] + " (" + tableFields[i] + ");");
            db.execSQL("CREATE INDEX IF NOT EXISTS time_device ON " + databaseTables[i] + " (timestamp, device_id);");

            columns.retainAll(getColumns(db, databaseTables[i]));

            String cols = TextUtils.join(",", columns);
            String new_cols = cols;

            if (renamed_columns.size() > 0) {
                for (String key : renamed_columns.keySet()) {
                    if (DEBUG) Log.d(TAG, "Renaming: " + key + " -> " + renamed_columns.get(key));
                    new_cols = new_cols.replace(key, renamed_columns.get(key));
                }
            }

            //restore old data back
            if (DEBUG)
                Log.d(TAG, String.format("INSERT INTO %s (%s) SELECT %s from temp_%s;", databaseTables[i], new_cols, cols, databaseTables[i]));

            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from temp_%s;", databaseTables[i], new_cols, cols, databaseTables[i]));
            db.execSQL("DROP TABLE temp_" + databaseTables[i] + ";");
        }
        db.setVersion(newVersion);
    }

    /**
     * Creates a String of a JSONArray representation of a database cursor result
     *
     * @param cursor
     * @return String
     */
    public static String cursorToString(Cursor cursor) {
        JSONArray jsonArray = new JSONArray();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int nColumns = cursor.getColumnCount();
                JSONObject row = new JSONObject();
                for (int i = 0; i < nColumns; i++) {
                    String colName = cursor.getColumnName(i);
                    if (colName != null) {
                        try {
                            switch (cursor.getType(i)) {
                                case Cursor.FIELD_TYPE_BLOB:
                                    row.put(colName, cursor.getBlob(i).toString());
                                    break;
                                case Cursor.FIELD_TYPE_FLOAT:
                                    row.put(colName, cursor.getDouble(i));
                                    break;
                                case Cursor.FIELD_TYPE_INTEGER:
                                    row.put(colName, cursor.getLong(i));
                                    break;
                                case Cursor.FIELD_TYPE_NULL:
                                    row.put(colName, null);
                                    break;
                                case Cursor.FIELD_TYPE_STRING:
                                    row.put(colName, cursor.getString(i));
                                    break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                jsonArray.put(row);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();

        return jsonArray.toString();
    }

    private static List<String> getColumns(SQLiteDatabase db, String tableName) {
        List<String> columns = null;
        Cursor database_meta = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 1", null);
        if (database_meta != null) {
            columns = new ArrayList<>(Arrays.asList(database_meta.getColumnNames()));
        }
        if (database_meta != null && !database_meta.isClosed()) database_meta.close();

        return columns;
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        try {
            if (database != null) {
                if (!database.isOpen()) {
                    database = null;
                } else if (!database.isReadOnly()) {
                    return database;
                }
            }

            database = getDatabaseFile();
            if (database == null) {
                // FIX: Log the error and throw exception instead of returning null
                Log.e(TAG, "Failed to get database file for: " + databaseName);
                throw new SQLiteException("Unable to open database: " + databaseName);
            }

            int current_version = database.getVersion();
            if (current_version != newVersion) {
                database.beginTransaction();
                try {
                    if (current_version == 0) {
                        onCreate(database);
                    } else {
                        onUpgrade(database, current_version, newVersion);
                    }
                    database.setTransactionSuccessful();
                } finally {
                    database.endTransaction();
                }
            }
            return database;
        } catch (Exception e) {
            // FIX: Log the error properly and rethrow
            Log.e(TAG, "Error getting writable database: " + databaseName, e);
            if (e instanceof SQLiteException) {
                throw (SQLiteException) e;
            }
            throw new SQLiteException("Failed to get writable database", e);
        }
    }

    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        try {
            if (database != null) {
                if (!database.isOpen()) {
                    database = null;
                }
            }
            database = getDatabaseFile();
            if (database == null) {
                // FIX: Log the error and throw exception instead of returning null
                Log.e(TAG, "Failed to get database file for: " + databaseName);
                throw new SQLiteException("Unable to open database: " + databaseName);
            }
            return database;
        } catch (Exception e) {
            // FIX: Log the error properly and rethrow
            Log.e(TAG, "Error getting readable database: " + databaseName, e);
            if (e instanceof SQLiteException) {
                throw (SQLiteException) e;
            }
            throw new SQLiteException("Failed to get readable database", e);
        }
    }

    /**
     * Returns the SQLiteDatabase
     *
     * @return
     */
    private synchronized SQLiteDatabase getDatabaseFile() {
        try {
            File aware_folder = null;

            // Check if we have permission to write to external storage
            boolean hasExternalStoragePermission = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hasExternalStoragePermission = ContextCompat.checkSelfPermission(mContext,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
            } else {
                hasExternalStoragePermission = true; // Pre-M devices don't need runtime permissions
            }

            // Try to use external storage if we have permission, otherwise use internal storage
            if (hasExternalStoragePermission) {
                try {
                    // TESTING
                    // My purpose is to create the log files in Documents dir, so it will be accessible
                    aware_folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AWARE");

                    // Test if we can actually create the directory
                    if (!aware_folder.exists()) {
                        boolean created = aware_folder.mkdirs();
                        if (!created && !aware_folder.exists()) {
                            Log.w(TAG, "Could not create external storage directory, falling back to internal storage");
                            throw new Exception("External storage not accessible");
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "External storage not accessible, using internal storage: " + e.getMessage());
                    hasExternalStoragePermission = false;
                }
            }

            if (!hasExternalStoragePermission) {
                // Fallback to internal storage
                aware_folder = new File(mContext.getFilesDir(), "AWARE");
                Log.i(TAG, "Using internal storage for database: " + aware_folder.getAbsolutePath());
            }

           /* if (mContext.getResources().getBoolean(R.bool.internalstorage)) {
                // Internal storage.  This is not acceassible to any other apps and is removed once
                // app is uninstalled.  Plugins can't use it.  Hard-coded to off, only change if
                // you know what you are doing.  Beware!
                aware_folder = mContext.getFilesDir();
            } else if (!mContext.getResources().getBoolean(R.bool.standalone)) {
                // sdcard/AWARE/ (shareable, does not delete when uninstalling)
                aware_folder = new File(Environment.getExternalStoragePublicDirectory("AWARE").toString());
            } else {
                if (isEmulator()) {
                    aware_folder = mContext.getFilesDir();
                } else {
                    // sdcard/Android/<app_package_name>/AWARE/ (not shareable, deletes when uninstalling package)
                    aware_folder = new File(ContextCompat.getExternalFilesDirs(mContext, null)[0] + "/AWARE");
                }
            } */

            if (!aware_folder.exists()) {
                boolean created = aware_folder.mkdirs();
                if (!created && !aware_folder.exists()) {
                    // FIX: Log error if folder creation fails
                    Log.e(TAG, "Failed to create database directory: " + aware_folder.getAbsolutePath());
                    throw new SQLiteException("Cannot create database directory: " + aware_folder.getAbsolutePath());
                }
            }

            File dbFile = new File(aware_folder, this.databaseName);
            if (DEBUG) Log.d(TAG, "Opening database: " + dbFile.getAbsolutePath());

            database = SQLiteDatabase.openOrCreateDatabase(dbFile.getPath(), this.cursorFactory);

            // FIX: Enable WAL mode for better concurrency and performance
            // WAL allows concurrent reads while writing, significantly improving performance
            if (database != null && database.isOpen()) {
                try {
                    // enableWriteAheadLogging() handles WAL mode internally
                    boolean walEnabled = database.enableWriteAheadLogging();
                    if (DEBUG) Log.d(TAG, "WAL mode enabled: " + walEnabled + " for " + databaseName);

                    // Set synchronous mode to NORMAL for better performance
                    // Use rawQuery instead of execSQL for PRAGMA statements
                    Cursor cursor = database.rawQuery("PRAGMA synchronous=NORMAL", null);
                    if (cursor != null) cursor.close();
                } catch (Exception e) {
                    // WAL mode might not be supported on all devices, log but don't fail
                    if (DEBUG) Log.w(TAG, "Could not enable WAL mode for " + databaseName + ": " + e.getMessage());
                }
            }

            return database;
        } catch (SQLiteException e) {
            // FIX: Log and rethrow instead of returning null
            Log.e(TAG, "SQLiteException opening database: " + databaseName, e);
            throw e;
        } catch (Exception e) {
            // FIX: Log and throw wrapped exception instead of returning null
            Log.e(TAG, "Exception opening database: " + databaseName, e);
            throw new SQLiteException("Failed to open database: " + databaseName, e);
        }
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}

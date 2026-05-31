package com.aware.utils;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.aware.Aware;
import com.aware.R;

/**
 * Utility class to handle OEM-specific background execution restrictions.
 * Many manufacturers (Xiaomi, Huawei, OnePlus, Samsung, etc.) aggressively
 * kill background services beyond what stock Android does.
 * 
 * Reference: https://dontkillmyapp.com/
 *
 * @author AWARE Team
 */
public class OEMProtection {

    private static final String TAG = "AWARE::OEMProtection";

    /**
     * Known OEM-specific intent targets for autostart/battery settings.
     * Each entry is: { package, activity class }.
     */
    private static final String[][] OEM_INTENTS = {
            // Xiaomi MIUI
            {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
            // Xiaomi MIUI Battery Saver
            {"com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"},
            // Huawei / Honor
            {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
            // Huawei (older)
            {"com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"},
            // Oppo
            {"com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"},
            // Oppo (alternative)
            {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"},
            // Vivo
            {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
            // OnePlus
            {"com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"},
            // Samsung
            {"com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"},
            // Samsung Device Care
            {"com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"},
            // Asus
            {"com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"},
            // Letv / LeEco
            {"com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"},
            // Meizu
            {"com.meizu.safe", "com.meizu.safe.permission.PermissionMainActivity"},
    };

    /**
     * Checks if the current device is from a manufacturer known to aggressively
     * kill background services.
     *
     * @return true if the manufacturer is known to restrict background execution
     */
    public static boolean isAggressiveOEM() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.equals("xiaomi")
                || manufacturer.equals("redmi")
                || manufacturer.equals("huawei")
                || manufacturer.equals("honor")
                || manufacturer.equals("oppo")
                || manufacturer.equals("vivo")
                || manufacturer.equals("oneplus")
                || manufacturer.equals("meizu")
                || manufacturer.equals("asus")
                || manufacturer.equals("letv")
                || manufacturer.equals("leeco")
                || manufacturer.equals("samsung");
    }

    /**
     * Returns a human-readable manufacturer name for display in dialogs.
     *
     * @return manufacturer display name
     */
    public static String getManufacturerName() {
        return Build.MANUFACTURER.substring(0, 1).toUpperCase() + Build.MANUFACTURER.substring(1);
    }

    /**
     * Attempts to launch the OEM-specific autostart or battery optimization settings.
     * Tries all known intents for the current manufacturer.
     *
     * @param context application context
     * @return true if an OEM settings activity was successfully launched
     */
    public static boolean launchOEMSettings(Context context) {
        for (String[] intentDef : OEM_INTENTS) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(intentDef[0], intentDef[1]));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (isIntentAvailable(context, intent)) {
                try {
                    context.startActivity(intent);
                    if (Aware.DEBUG) Log.d(TAG, "Launched OEM settings: " + intentDef[1]);
                    return true;
                } catch (Exception e) {
                    if (Aware.DEBUG) Log.w(TAG, "Failed to launch: " + intentDef[1], e);
                }
            }
        }
        if (Aware.DEBUG) Log.d(TAG, "No OEM-specific settings found for: " + Build.MANUFACTURER);
        return false;
    }

    /**
     * Checks if an Intent can be resolved to an activity on this device.
     *
     * @param context application context
     * @param intent  the intent to check
     * @return true if the intent can be resolved
     */
    private static boolean isIntentAvailable(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        return intent.resolveActivity(pm) != null;
    }

    /**
     * Returns OEM-specific instructions for the user.
     *
     * @return instruction string
     */
    public static String getOEMInstructions() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        if (manufacturer.equals("xiaomi") || manufacturer.equals("redmi")) {
            return "For Xiaomi/MIUI devices:\n\n"
                    + "1. Go to Settings > Apps > Manage Apps > AWARE > Autostart and enable it\n"
                    + "2. Go to Settings > Battery & Performance > App Battery Saver > AWARE > set to 'No restriction'\n"
                    + "3. In Recent Apps, swipe down on AWARE to lock it (padlock icon)\n"
                    + "4. (Optional) Developer Options > MIUI Optimizations > disable";
        } else if (manufacturer.equals("huawei") || manufacturer.equals("honor")) {
            return "For Huawei/Honor devices:\n\n"
                    + "1. Go to Settings > Battery > App Launch > AWARE > set to 'Manage manually' and enable all toggles\n"
                    + "2. Go to Settings > Apps > AWARE > Battery > enable 'Allow background activity'\n"
                    + "3. In Recent Apps, swipe down on AWARE to lock it";
        } else if (manufacturer.equals("oppo")) {
            return "For OPPO/ColorOS devices:\n\n"
                    + "1. Go to Settings > Battery > More Settings > Optimize Battery Use > AWARE > disable\n"
                    + "2. Go to Settings > App Management > AWARE > enable 'Allow Auto-start'\n"
                    + "3. In Recent Apps, lock AWARE by dragging down or tapping the lock icon";
        } else if (manufacturer.equals("vivo")) {
            return "For Vivo devices:\n\n"
                    + "1. Go to Settings > Battery > Background Power Consumption Management > AWARE > disable\n"
                    + "2. Go to Settings > More Settings > Applications > Autostart > enable AWARE";
        } else if (manufacturer.equals("oneplus")) {
            return "For OnePlus devices:\n\n"
                    + "1. Go to Settings > Battery > Battery Optimization > AWARE > Don't Optimize\n"
                    + "2. Go to Settings > Apps > AWARE > Advanced > Battery optimization > Not Optimized\n"
                    + "3. In Recent Apps, lock AWARE";
        } else if (manufacturer.equals("samsung")) {
            return "For Samsung devices:\n\n"
                    + "1. Go to Settings > Device Care > Battery > App Power Management\n"
                    + "2. Add AWARE to 'Never sleeping apps'\n"
                    + "3. Disable 'Adaptive Battery' if data collection is interrupted";
        } else if (manufacturer.equals("meizu")) {
            return "For Meizu devices:\n\n"
                    + "1. Go to Settings > Battery > AWARE > disable 'Power Saving Mode'\n"
                    + "2. Go to Security > Permissions > Autostart > enable AWARE";
        } else if (manufacturer.equals("asus")) {
            return "For ASUS devices:\n\n"
                    + "1. Go to Settings > Power Management > Auto-start Manager > enable AWARE\n"
                    + "2. Remove AWARE from any battery-saving lists";
        }
        return "To ensure AWARE runs reliably:\n\n"
                + "1. Go to Settings > Battery > Battery Optimization > AWARE > Don't Optimize\n"
                + "2. Ensure AWARE is not in any power-saving restricted list";
    }

    /**
     * Returns a unique preference key for tracking if the OEM protection dialog has been shown.
     * Uses manufacturer name so the dialog only needs to be shown once per device type.
     *
     * @return preference key string
     */
    public static String getOEMDialogShownPrefKey() {
        return "oem_protection_dialog_shown_" + Build.MANUFACTURER.toLowerCase();
    }
}

package com.assoft.peekster.activity.shareable.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import java.io.File
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** [PreferenceStorage] for app and user preferences. */
interface PreferenceStorage {
    var behaviorReceive: Boolean
    var behaviorOverwrite: Boolean
    var deviceName: String?
    var deviceUUID: String?
    var transferDirectory: String?
    var transferNotification: Boolean
}
/**
 * Provide a convenient location for accessing common settings and their defaults.
 */
class Settings(context: Context) : PreferenceStorage{

    /** Reference to [SharedPreferences] instance. */
    private val sharedPreferences: Lazy<SharedPreferences> = lazy {
        // Lazy to prevent IO access to main thread.
        context.applicationContext.getSharedPreferences(
            PREFS_NAME, MODE_PRIVATE
        )
    }

    override var behaviorReceive by BooleanPreference(
        sharedPreferences,
        PREFS_BEHAVIOR_RECEIVE,
        true
    )

    override var behaviorOverwrite by BooleanPreference(
        sharedPreferences,
        PREFS_BEHAVIOR_OVERWRITE,
        true
    )

    override var deviceName by StringPreference(
        sharedPreferences,
        PREF_DEVICE_NAME,
        Build.MODEL
    )

    override var deviceUUID by StringPreference(
        sharedPreferences,
        PREF_DEVICE_UUID,
        String.format("{%s}", UUID.randomUUID().toString())
    )

    override var transferDirectory by StringPreference(
        sharedPreferences,
        PREF_TRANSFER_DIRECTORY,
        File(File(Environment.getExternalStorageDirectory(), "Download"), "Peekster").absolutePath
    )

    override var transferNotification by BooleanPreference(
        sharedPreferences,
        PREF_TRANSFER_NOTIFICATION,
        true
    )

    companion object {
        const val PREFS_NAME = "peekster_shared_prefs"
        const val PREFS_BEHAVIOR_RECEIVE = "prefs_behavior_receive" // Listen for incoming transfers
        const val PREFS_BEHAVIOR_OVERWRITE = "prefs_behavior_overwrite" // Overwrite files with identical names
        const val PREF_DEVICE_NAME = "pref_device_name" // Device name broadcast via mDNS
        const val PREF_DEVICE_UUID= "pref_device_uuid" // Unique identifier for the device
        const val PREF_TRANSFER_DIRECTORY = "pref_transfer_directory" // Directory for storing received files
        const val PREF_TRANSFER_NOTIFICATION = "pref_transfer_notification" // Default sounds, vibrate, etc. for transfers
    }
}

/** [BooleanPreference] for setting and getting boolean value */
class BooleanPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: Boolean
) : ReadWriteProperty<Any, Boolean> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return preferences.value.getBoolean(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        preferences.value.edit { putBoolean(name, value) }
    }
}

/** [StringPreference] for setting and getting boolean value */
class StringPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: String?
) : ReadWriteProperty<Any, String?> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): String? {
        return preferences.value.getString(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) {
        preferences.value.edit { putString(name, value) }
    }
}

/** [IntPreference] for setting and getting boolean value */
class IntPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: Int
) : ReadWriteProperty<Any, Int> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return preferences.value.getInt(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        preferences.value.edit { putInt(name, value) }
    }
}

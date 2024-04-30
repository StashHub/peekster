/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.assoft.peekster.domain.prefs

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** [PreferenceStorage] for app and user preferences. */
interface PreferenceStorage {

    /** Variable used to keep track of whether the parental contol is active */
    var parentalControl: Boolean

    var introductionShown: Boolean

    var activeServer: Boolean

    /** Variable used to keep track of the device name */
    var deviceName: String?

    var transferNotification: Boolean

    var sortOrder: Int

    var sortBy: Int

    /** Variable used to keep track of the selected filters */
    var selectedFilters: String?
}

/**
 * [PreferenceStorage] implementation backed by [android.content.SharedPreferences].
 */
class SharedPreferenceStorage(context: Context) :
    PreferenceStorage {

    /** Reference to [SharedPreferences] instance. */
    private val prefs: Lazy<SharedPreferences> = lazy {
        // Lazy to prevent IO access to main thread.
        context.applicationContext.getSharedPreferences(
            PREFS_NAME, MODE_PRIVATE
        )
    }

    /** Returns `true` if on-boarding process is completed, else `false`. */
    override var parentalControl by BooleanPreference(
        prefs,
        PREF_PARENT_CONTROL,
        false
    )
    override var introductionShown by BooleanPreference(
        prefs,
        PREF_INTRODUCTION_SHOWN,
        false
    )

    override var activeServer by BooleanPreference(
        prefs,
        PREF_ACTIVE_SERVER,
        false
    )

    /** Returns the current selected filter, else null */
    override var selectedFilters by StringPreference(
        prefs,
        PREF_SELECTED_FILTERS,
        null
    )

    override var deviceName by StringPreference(
        prefs,
        PREF_DEVICE_NAME,
        null
    )

    override var transferNotification by BooleanPreference(
        prefs,
        PREF_TRANSFER_NOTIFICATION,
        true
    )

    override var sortOrder by IntPreference(
        prefs,
        PREF_SORT_ORDER,
        100
    )

    override var sortBy by IntPreference(
        prefs,
        PREF_SORT_BY,
        100
    )

    companion object {
        const val PREFS_NAME = "peekster_shared_prefs"
        const val PREF_PARENT_CONTROL = "pref_parental_control"
        const val PREF_INTRODUCTION_SHOWN = "pref_introduction_shown"
        const val PREF_ACTIVE_SERVER = "pref_active_server"
        const val PREF_DEVICE_NAME = "pref_device_name"
        const val PREF_SELECTED_FILTERS = "pref_selected_filters"
        const val PREF_TRANSFER_NOTIFICATION = "pref_transfer_notification"
        const val PREF_SORT_ORDER = "pref_sort_order"
        const val PREF_SORT_BY = "pref_sort_by"
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
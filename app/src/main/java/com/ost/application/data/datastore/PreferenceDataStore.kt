package com.ost.application.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext


val Context.appPreferences: DataStore<Preferences> by preferencesDataStore(name = "sample_app_prefs")


/**
 * Implementation of [PreferenceDataStore] that uses the more efficient
 * proto-based [DataStorePreference][androidx.datastore.core.DataStore]
 * for use with [PreferenceFragment][androidx.preference.PreferenceFragment]
 * replacing the legacy xml-based preference.
 */
class PreferenceDataStoreImpl private constructor(
    val dataStore: DataStore<Preferences>
): PreferenceDataStore(), CoroutineScope {

    companion object{
        @Volatile
        private var INSTANCE: PreferenceDataStoreImpl? = null

        /**
         * Returns the singleton instance of [PreferenceDataStoreImpl].
         */
        fun getInstance(context: Context): PreferenceDataStoreImpl = INSTANCE
            ?: synchronized(this) { PreferenceDataStoreImpl(context.applicationContext.appPreferences).also { INSTANCE = it } }
    }

    fun interface OnPreferencesChangeListener {
        fun onPreferenceChanged(key: String, newValue: Any?)
    }

    private val listeners = mutableSetOf<OnPreferencesChangeListener>()

    /**
     * Registers a callback to be invoked when a preference is changed.
     * Ensure to call [removeOnPreferencesChangeListener] to un-register the listener.
     * @see OnPreferencesChangeListener
     * @see removeOnPreferencesChangeListener
     */
    fun addOnPreferencesChangeListener(listener: OnPreferencesChangeListener) {
        listeners.add(listener)
    }

    /**
     * Un-registers a previously registered callback.
     * @see OnPreferencesChangeListener
     * @see addOnPreferencesChangeListener
     */
    fun removeOnPreferencesChangeListener(listener: OnPreferencesChangeListener) {
        listeners.remove(listener)
    }

    override fun putString(key: String, value: String?) {
        launch {
            dataStore.edit { it[stringPreferencesKey(key)] = value ?: "" }
            listeners.forEach { it.onPreferenceChanged(key, value) }
        }
    }

    override fun getString(key: String, defValue: String?): String {
        return runBlocking {
            dataStore.data.map { it[stringPreferencesKey(key)] ?: defValue ?: "" }.first()
        }
    }

    override fun putInt(key: String, value: Int) {
        launch{
            dataStore.edit {  it[intPreferencesKey(key)] = value }
            listeners.forEach { it.onPreferenceChanged(key, value) }
        }
    }

    override fun getInt(key: String, defValue: Int): Int {
        return runBlocking { dataStore.data.map { it[intPreferencesKey(key)] ?: defValue }.first() }
    }

    override fun putBoolean(key: String, value: Boolean) {
        launch{
            dataStore.edit {  it[booleanPreferencesKey(key)] = value }
            listeners.forEach { it.onPreferenceChanged(key, value) }
        }
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return runBlocking {
            dataStore.data.map { it[booleanPreferencesKey(key)] ?: defValue }.first()
        }
    }

    override fun putFloat(key: String, value: Float) {
        launch{
            dataStore.edit {  it[floatPreferencesKey(key)] = value }
            listeners.forEach { it.onPreferenceChanged(key, value) }
        }
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return runBlocking {
            dataStore.data.map { it[floatPreferencesKey(key)] ?: defValue }.first()
        }
    }

    override fun getLong(key: String, defValue: Long): Long {
        return runBlocking {
            dataStore.data.map { it[longPreferencesKey(key)] ?: defValue }.first()
        }
    }

    override fun putLong(key: String, value: Long) {
        launch{
            dataStore.edit {  it[longPreferencesKey(key)] = value }
            listeners.forEach { it.onPreferenceChanged(key, value) }
        }
    }

    override fun getStringSet(key: String, defValues: MutableSet<String>?): Set<String> {
        return runBlocking {
            dataStore.data.map {
                it[stringSetPreferencesKey(key)] ?: defValues ?: emptySet()
            }.first()
        }
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        launch{
            dataStore.edit {  it[stringSetPreferencesKey(key)] = values ?: emptySet() }
            listeners.forEach { it.onPreferenceChanged(key, values) }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main
}
package com.tpoAppInteractivas.olacheck.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

@Singleton
class UserDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_UID = stringPreferencesKey("uid")
        private val KEY_NAME = stringPreferencesKey("display_name")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_PHOTO = stringPreferencesKey("photo_url")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_UID] != null }

    suspend fun saveUser(uid: String, displayName: String, email: String, photoUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_UID] = uid
            prefs[KEY_NAME] = displayName
            prefs[KEY_EMAIL] = email
            prefs[KEY_PHOTO] = photoUrl
        }
    }

    suspend fun clearUser() {
        context.dataStore.edit { it.clear() }
    }
}
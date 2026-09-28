package com.velocity.editor.data

import android.content.Context
import com.velocity.editor.domain.Addon
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("velocity_settings", Context.MODE_PRIVATE)

    private val _addons = MutableStateFlow(Addon.values().associateWith { prefs.getBoolean(it.key, true) })
    val addons: StateFlow<Map<Addon, Boolean>> = _addons.asStateFlow()

    fun setAddon(addon: Addon, enabled: Boolean) {
        prefs.edit().putBoolean(addon.key, enabled).apply()
        _addons.update { it + (addon to enabled) }
    }
}

package com.spyshield.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spyshield.app.data.AppPrivacyInfo
import com.spyshield.app.data.AppScanner
import com.spyshield.app.data.PermissionChange
import com.spyshield.app.data.PermissionSnapshot
import com.spyshield.app.data.Store
import com.spyshield.app.monitor.SensorMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _apps = MutableStateFlow<List<AppPrivacyInfo>>(emptyList())
    val apps = _apps.asStateFlow()

    private val _permissionChanges = MutableStateFlow<List<PermissionChange>>(emptyList())
    val permissionChanges = _permissionChanges.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    val events = Store.events
    val blocked = Store.blocked
    val live = Store.live
    val protection = Store.protectionOn

    fun refresh() {
        if (_scanning.value) return
        _scanning.value = true
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) {
                runCatching { AppScanner.scan(getApplication(), Store.events.value) }.getOrDefault(_apps.value)
            }
            val scanned = _apps.value
            val changes = withContext(Dispatchers.IO) {
                runCatching { PermissionSnapshot.diff(getApplication(), scanned) }.getOrDefault(emptyList())
            }
            // Drop pending warnings for access that has since been taken away again.
            val revoked = changes.filter { !it.gained }.map { it.packageName to it.sensor }.toSet()
            _permissionChanges.value = (_permissionChanges.value + changes.filter { it.gained })
                .filter { (it.packageName to it.sensor) !in revoked }
                .distinctBy { it.packageName to it.sensor }
            _scanning.value = false
        }
    }

    fun setProtection(on: Boolean) {
        Store.setProtection(on)
        if (on) SensorMonitorService.start(getApplication()) else SensorMonitorService.stop(getApplication())
        com.spyshield.app.monitor.ProtectionTileService.refresh(getApplication())
        com.spyshield.app.monitor.StatusWidgetProvider.refresh(getApplication())
    }

    fun setBlocked(pkg: String, blocked: Boolean) = Store.setBlocked(pkg, blocked)

    fun dismissPermissionChanges() {
        _permissionChanges.value = emptyList()
    }

    fun clearHistory() {
        Store.clearHistory()
        refresh()
    }
}

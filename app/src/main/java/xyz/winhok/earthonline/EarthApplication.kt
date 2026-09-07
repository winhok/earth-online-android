package xyz.winhok.earthonline

import android.app.Application
import kotlinx.coroutines.*
import xyz.winhok.earthonline.data.*
import xyz.winhok.earthonline.reminder.Reminders

class EarthApplication : Application() {
    val database by lazy { EarthDatabase.open(this) }
    val repository by lazy { WorldRepository(database) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try { Reminders.configure(this@EarthApplication, repository.snapshot().player.remindersEnabled) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Main UI exposes database failures; no private data is logged. */ }
        }
    }
}

package xyz.winhok.earthonline.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import xyz.winhok.earthonline.EarthApplication
import xyz.winhok.earthonline.MainActivity
import xyz.winhok.earthonline.R
import xyz.winhok.earthonline.core.*

object Reminders {
    const val CHANNEL = "daily_adventure"
    const val NOTIFICATION_ID = 7001
    private const val WORK_NAME = "earth-daily-reminder"
    fun allowed(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun configure(
        context: Context,
        enabled: Boolean,
        systemId: NarrativeSystemId = NarrativeSystemId.EARTH_NATIVE,
    ) {
        val manager = requireNotNull(context.getSystemService(NotificationManager::class.java)) {
            "NotificationManager unavailable"
        }
        val channelName = narrativeText(systemId, NotificationSemantic.REMINDER_CHANNEL)
        val channel = manager.getNotificationChannel(CHANNEL)?.apply { name = channelName }
            ?: NotificationChannel(CHANNEL, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK_NAME)
            manager.cancel(NOTIFICATION_ID)
            return
        }
        // Date/hour gating avoids 24-hour drift. WorkManager is intentionally NOT an exact alarm.
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build())
    }
}

class ReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    @SuppressLint("MissingPermission") // Runtime guard before claim and again immediately before notify.
    override suspend fun doWork(): Result {
        if (!Reminders.allowed(applicationContext)) return Result.success()
        val repo = (applicationContext as EarthApplication).repository
        var claimedDay: Long? = null
        return try {
            val world = repo.snapshot()
            val registry = NarrativeRegistry.builtIns()
            val systemId = registry.resolveSystemId(repo.requestedNarrativeSystemId())
            val player = world.player
            if (!player.onboarded || !player.remindersEnabled) return Result.success()
            val now = Instant.now().atZone(ZoneId.of(player.zoneId))
            val day = now.toLocalDate().toEpochDay()
            if (now.hour < player.reminderHour || player.lastReminderDay == day) return Result.success()
            val done = world.completions.filter { it.revokedAt == null }.map { it.id }.toHashSet()
            val count = world.quests.count { QuestRules.available(it, day) && QuestRules.completionId(it, day) !in done }
            if (count == 0 || !repo.claimReminder(day)) return Result.success()
            claimedDay = day
            if (!Reminders.allowed(applicationContext)) {
                repo.releaseReminder(day)
                return Result.success()
            }
            val intent = Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(applicationContext, Reminders.CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(narrativeText(systemId, NotificationSemantic.REMINDER_TITLE))
                .setContentText(narrativeText(systemId, NotificationSemantic.REMINDER_AVAILABLE, semanticArguments {
                    put(SemanticParameters.COUNT, CountValue(count))
                }))
                .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
            NotificationManagerCompat.from(applicationContext).notify(Reminders.NOTIFICATION_ID, notification)
            Result.success()
        } catch (cancelled: CancellationException) {
            // At-most-once claim can intentionally miss a day after process death; not an exact delivery guarantee.
            throw cancelled
        } catch (_: Exception) {
            claimedDay?.let { day ->
                try { repo.releaseReminder(day) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Retry without logging user contents. */ }
            }
            Result.retry()
        }
    }
}

private fun narrativeText(
    systemId: NarrativeSystemId,
    key: SemanticKey,
    arguments: SemanticArguments = SemanticArguments.EMPTY,
): String = NarrativeRegistry.builtIns().interpret(
    systemId,
    SemanticRequest(key, arguments),
    NarrativeLocale.ZH_CN,
    NarrativeScheme.DARK,
).text

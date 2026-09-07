package xyz.winhok.earthonline.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import xyz.winhok.earthonline.EarthApplication
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.data.BackupCodec
import xyz.winhok.earthonline.reminder.Reminders

data class EarthUiState(
    val world: World = World(), val now: Long = System.currentTimeMillis(),
    val loading: Boolean = true, val loadError: Boolean = false, val busy: Boolean = false,
) { val day: Long get() = world.dayAt(now) }
data class UiMessage(val text: String, val undoId: String? = null)
private data class LoadedWorld(val world: World = World(), val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class EarthViewModel(private val app: EarthApplication) : ViewModel() {
    private val repo = app.repository
    private val retry = MutableStateFlow(0)
    private val working = MutableStateFlow(false)
    private val channel = Channel<UiMessage>(Channel.BUFFERED)
    val messages = channel.receiveAsFlow()
    private val restoreDraft = MutableStateFlow<World?>(null)
    val pendingRestore = restoreDraft.asStateFlow()
    private val world = retry.flatMapLatest {
        repo.observe().map { LoadedWorld(it) }.catch { error ->
            if (error is CancellationException) throw error
            emit(LoadedWorld(failed = true))
        }
    }
    private val ticker = flow { while (true) { emit(System.currentTimeMillis()); delay(15_000) } }
    val state = combine(world, ticker, working) { loaded, now, busy ->
        EarthUiState(loaded.world, now, loading = false, loadError = loaded.failed, busy = busy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EarthUiState())

    fun retryLoad() { retry.value++ }
    fun notify(text: String) { viewModelScope.launch { channel.send(UiMessage(text)) } }
    private fun change(action: suspend () -> Unit) {
        viewModelScope.launch {
            if (working.value) return@launch
            working.value = true
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                channel.send(UiMessage(when (error) {
                    is RuleViolation -> error.reason.message()
                    is IOException -> "文件读写失败。请检查存储空间和文件访问权限。"
                    else -> "操作遇到错误，请检查当前状态后重试。导入文件必须完整且版本兼容。"
                }))
            } finally { working.value = false }
        }
    }
    fun join(name: String, server: String) = change { repo.join(name, server) }
    fun saveQuest(draft: QuestDraft, success: () -> Unit) = change { repo.saveQuest(draft); success() }
    fun complete(id: String) = change {
        val result = repo.complete(id)
        if (result.changed) channel.send(UiMessage("任务完成 · +${result.completion.xp} XP", result.completion.id))
    }
    fun undo(id: String) = change { if (repo.undo(id)) channel.send(UiMessage("已撤销，经验也已恢复至完成前。")) }
    fun postpone(id: String) = change { repo.postpone(id); channel.send(UiMessage("已暂缓一天。原截止日期保留，不扣经验。")) }
    fun setQuestState(id: String, status: QuestState) = change { repo.setQuestState(id, status) }
    fun saveGoal(id: String?, title: String, description: String, success: () -> Unit) = change {
        repo.saveGoal(id, title, description); success()
    }
    fun archiveGoal(id: String) = change { repo.archiveGoal(id) }
    fun addNote(text: String, success: () -> Unit) = change { repo.addNote(text); success() }
    private suspend fun configureReminder(enabled: Boolean) {
        try { Reminders.configure(app, enabled) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            channel.send(UiMessage("存档已保存，但提醒调度失败。请重新打开应用后检查提醒设置。"))
        }
    }
    fun savePlayer(name: String, server: String, theme: ThemeMode, reminders: Boolean, hour: Int, success: () -> Unit) = change {
        repo.updatePlayer(name, server, theme, reminders, hour)
        configureReminder(reminders)
        success()
    }
    fun export(uri: Uri) = change {
        withContext(Dispatchers.IO) {
            val text = BackupCodec.encode(repo.snapshot(), System.currentTimeMillis())
            val output = app.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
            output.use { it.write(text.toByteArray(Charsets.UTF_8)); it.flush() }
        }
        channel.send(UiMessage("存档已导出。此文件为明文，请妥善保存。"))
    }
    fun prepareRestore(uri: Uri) = change {
        restoreDraft.value = withContext(Dispatchers.IO) {
            val stream = app.contentResolver.openInputStream(uri) ?: throw IOException("No input stream")
            val bytes = stream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buffer)
                    if (n == -1) break
                    require(out.size() + n <= BackupCodec.MAX_BYTES) { "Backup exceeds size limit" }
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            }
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            BackupCodec.decode(text)
        }
    }
    fun dismissRestore() { if (!working.value) restoreDraft.value = null }
    fun confirmRestore(success: () -> Unit) = change {
        val value = restoreDraft.value ?: return@change
        repo.restore(value)
        restoreDraft.value = null
        configureReminder(false)
        success()
        channel.send(UiMessage("存档已恢复，提醒已关闭。需要时请重新开启。"))
    }
    fun reset(success: () -> Unit) = change {
        repo.reset(); configureReminder(false); restoreDraft.value = null; success()
    }
    class Factory(private val app: EarthApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(EarthViewModel::class.java))
            return EarthViewModel(app) as T
        }
    }
}

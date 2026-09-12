package xyz.winhok.earthonline

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import xyz.winhok.earthonline.core.*
import xyz.winhok.earthonline.data.*

/** Synthetic, validator-checked import files for the exact signed APK's black-box tests. */
@RunWith(AndroidJUnit4::class)
class ReleaseFixtureTest {
    @Test fun exportValidatedSyntheticTimelinesForSignedApkJourneys() {
        val app=ApplicationProvider.getApplicationContext<EarthApplication>()
        val dir=File(app.getExternalFilesDir(null),"acceptance").apply { mkdirs() }
        val now=System.currentTimeMillis()
        fun day(t:Long)=Instant.ofEpochMilli(t).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
        fun base(t:Long)=DeadlineState(World(player=Player("ReleaseTester","Verification","UTC",t,true)))
        fun save(s:DeadlineState,id:String,t:Long,due:Long?):DeadlineState {
            val c=DeadlineCommand.Save(QuestDraft(title=id,dueDay=due),id)
            return DeadlineEngine.execute(s,c,t,DeadlineEngine.preview(s,c,t)).state
        }
        fun write(name:String,s:DeadlineState) {
            val snapshot=BackupSnapshot(s.world).withDeadline(s)
            val encoded=BackupCodec.encode(snapshot,now)
            assertEquals(snapshot,BackupCodec.decodeSnapshot(encoded))
            File(dir,name).writeText(encoded)
        }
        write("v15-active.json",save(base(now),"SignedPromise",now,day(now)+2))
        val past=now-2*86_400_000L
        var overdue=base(past)
        repeat(3) { overdue=save(overdue,"ExpiredPromise$it",past,day(past)) }
        overdue=save(overdue,"OrdinaryRecovery",past,null)
        write("v15-overdue.json",overdue)
    }
}

package projektor.schedule

import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import projektor.DatabaseRepositoryTestCase
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotNull
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class SchedulerTest : DatabaseRepositoryTestCase() {
    @Test
    fun `should run scheduled job`() {
        var schedulerCalled = false

        val scheduledJob =
            Runnable {
                schedulerCalled = true
            }

        val schedulerLock = SchedulerLock(dataSource)
        val scheduler = Scheduler(schedulerLock)
        val jobName = "my_scheduled_job"

        val scheduleDuration =
            measureTime {
                scheduler.scheduleJob(ScheduledJob(jobName, scheduledJob, ScheduleDelay(4, TimeUnit.SECONDS)))

                await until { schedulerCalled }
            }

        expectThat(scheduleDuration.inWholeSeconds).isGreaterThanOrEqualTo(4)

        expectThat(scheduler.findScheduledJob(jobName)).isNotNull().and {
            get { name }.isEqualTo(jobName)
            get { scheduleDelay }.isEqualTo(ScheduleDelay(4, TimeUnit.SECONDS))
        }
    }
}

/*
Privacy Friendly To-Do List
Copyright (C) 2024  Christian Adams

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.secuso.privacyfriendlytodolist.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Context.JOB_SCHEDULER_SERVICE
import android.os.PersistableBundle
import android.util.Log
import org.secuso.privacyfriendlytodolist.util.AlarmMgr
import org.secuso.privacyfriendlytodolist.util.LogTag
import org.secuso.privacyfriendlytodolist.util.NotificationMgr

object JobManager {
    private val TAG = LogTag.create(this::class.java)

    /**
     * WorkManager uses ID range 0 .. Integer#MAX_VALUE. The JobInfo ID range must not overlap with
     * this range. See androidx.work.Configuration.Builder#setJobSchedulerJobIdRange() for details.
     */
    private const val JOB_SCHEDULER_JOB_ID_RANGE_BEGIN = -1
    private const val JOB_SCHEDULER_JOB_ID_RANGE_END = -10000
    private var jobIdBuilder = JOB_SCHEDULER_JOB_ID_RANGE_BEGIN

    fun processAutoStart(context: Context): Int {
        return scheduleJob(context, ReloadAlarmsJob::class.java)
    }

    fun processAlarmPermissionStateChanged(context: Context): Int {
        return scheduleJob(context, ReloadAlarmsJob::class.java)
    }

    fun processAlarm(context: Context, alarmId: Int): Int {
        val extras = PersistableBundle()
        extras.putInt(AlarmMgr.KEY_ALARM_ID, alarmId)
        return scheduleJob(context, AlarmJob::class.java, extras)
    }

    fun processNotificationAction(context: Context, action: String, taskId: Int): Int {
        val extras = PersistableBundle()
        extras.putInt(action, 0)
        extras.putInt(NotificationMgr.EXTRA_NOTIFICATION_TASK_ID, taskId)
        return scheduleJob(context, NotificationJob::class.java, extras)
    }

    private fun scheduleJob(context: Context, jobClass: Class<*>, extras: PersistableBundle? = null): Int {
        val componentName = ComponentName(context, jobClass)
        var jobId = getNextJobId()
        val builder = JobInfo.Builder(jobId, componentName)
        if (null != extras) {
            builder.setExtras(extras)
        }
        val jobInfo = builder.build()
        val jobScheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        val result = jobScheduler.schedule(jobInfo)
        if (result != JobScheduler.RESULT_SUCCESS) {
            jobId = 0
            Log.e(TAG, "JobScheduler failed to schedule job for reminder service. Result: $result")
        }
        return jobId
    }

    private fun getNextJobId(): Int {
        val jobId = jobIdBuilder--
        if (jobIdBuilder < JOB_SCHEDULER_JOB_ID_RANGE_END) {
            jobIdBuilder = JOB_SCHEDULER_JOB_ID_RANGE_BEGIN
        }
        return jobId
    }
}
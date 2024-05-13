/*
 This file is part of Privacy Friendly To-Do List.

 Privacy Friendly To-Do List is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly To-Do List is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly To-Do List. If not, see <http://www.gnu.org/licenses/>.
 */
package org.secuso.privacyfriendlytodolist.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.receiver.AlarmReceiver
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object AlarmMgr {
    const val KEY_ALARM_ID = "KEY_ALARM_ID"
    private val TAG = LogTag.create(this::class.java)
    private var manager: AlarmManager? = null

    private fun getManager(context: Context): AlarmManager {
        if (manager == null) {
            manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }
        return manager!!
    }

    /**
     * Sets an alarm for the given task if it is not done and has a reminder time.
     *
     * Timestamp of alarm is determined as follows:
     * - If task has reminder time:
     *      - If it is later than current time it gets used
     *      - Otherwise, if setAlarmEvenItsInPast is true, current time gets used
     *      - Otherwise no alarm gets set
     *
     * @return If an alarm gets set the alarm ID gets returned (task ID gets used as alarm ID).
     * If no alarm gets set null gets returned.
     */
    fun setAlarmForTask(context: Context, todoTask: TodoTask, setAlarmEvenIfItIsInPast: Boolean): Int? {
        // Use task's database ID as unique alarm ID.
        val alarmId = todoTask.getId()
        cancelAlarmForTask(context, alarmId)

        var reminderTime = todoTask.getReminderTime()
        if (reminderTime == -1L) {
            Log.i(TAG, "No alarm set because $todoTask has no reminder time.")
            return null
        }

        if (todoTask.isDone() && !todoTask.isRecurring()) {
            Log.i(TAG, "No alarm set because $todoTask is done and not recurring.")
            return null
        }

        val now = Helper.getCurrentTimestamp()
        if (todoTask.isRecurring()) {
            // Get the upcoming due date of the recurring task. The initial reminder time is not the
            // right date for the alarm.
            reminderTime = Helper.getNextRecurringDate(reminderTime, todoTask.getRecurrencePattern(), now)
        }

        val alarmTime: Long
        val logDuration: String
        val logDetail: String
        if (reminderTime > now) {
            alarmTime = reminderTime
            val duration = (reminderTime - now).toDuration(DurationUnit.SECONDS)
            logDuration = "in $duration"
            logDetail = "reminder time"
        } else if (setAlarmEvenIfItIsInPast) {
            alarmTime = now
            logDuration = "now"
            logDetail = "reminder time is in the past, using 'now'"
        } else {
            Log.i(TAG, "No alarm set because reminder time of $todoTask is in the past.")
            return null
        }

        val timestamp = setAlarm(context, alarmId, alarmTime)
        Log.i(TAG, "Alarm set for $todoTask at $timestamp which is $logDuration ($logDetail).")
        return alarmId
    }

    fun setAlarmForTask(context: Context, todoTaskId: Int, alarmTime: Long): Int {
        // Use task's database ID as unique alarm ID.
        cancelAlarmForTask(context, todoTaskId)

        val timestamp = setAlarm(context, todoTaskId, alarmTime)
        Log.i(TAG, "Alarm set for task $todoTaskId at $timestamp.")
        return todoTaskId
    }

    private fun setAlarm(context: Context, alarmId: Int, alarmTime: Long): String {
        val calendar = Calendar.getInstance()
        calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(alarmTime))
        // Use task's database ID as unique alarm ID.
        val pendingIntent = getPendingAlarmIntent(context, alarmId, true)!!
        getManager(context)[AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis()] = pendingIntent
        return Helper.createDateTimeString(calendar)
    }

    private fun cancelAlarmForTask(context: Context, alarmId: Int): Boolean {
        val pendingIntent = getPendingAlarmIntent(context, alarmId, false)
        var alarmWasSet = false
        if (pendingIntent != null) {
            alarmWasSet = true
            getManager(context).cancel(pendingIntent)
            Log.i(TAG, "Alarm $alarmId cancelled.")
        }
        return alarmWasSet
    }

    private fun getPendingAlarmIntent(context: Context, alarmId: Int, createIfNotExist: Boolean): PendingIntent? {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        if (!createIfNotExist) {
            flags = flags or PendingIntent.FLAG_NO_CREATE
        }
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.setAction(AlarmReceiver.ACTION)
        intent.putExtra(KEY_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(context, alarmId, intent, flags)
    }
}

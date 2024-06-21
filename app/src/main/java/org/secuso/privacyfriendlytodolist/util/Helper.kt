package org.secuso.privacyfriendlytodolist.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.TodoTask.DeadlineColors
import org.secuso.privacyfriendlytodolist.model.TodoTask.RecurrencePattern
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Helper {
    private val TAG = LogTag.create(this::class.java)

    fun createLocalizedDateString(time: Long): String {
        val dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.DEFAULT, Locale.getDefault())
        val date = Date(TimeUnit.SECONDS.toMillis(time))
        return dateFormat.format(date)
    }

    fun createLocalizedDateTimeString(time: Long): String {
        val dateTimeFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.DEFAULT, SimpleDateFormat.DEFAULT, Locale.getDefault())
        val dateTime = Date(TimeUnit.SECONDS.toMillis(time))
        return dateTimeFormat.format(dateTime)
    }

    fun createCanonicalDateTimeString(time: Long): String {
        val canonicalDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
        val dateTime = Date(TimeUnit.SECONDS.toMillis(time))
        return canonicalDateTimeFormat.format(dateTime)
    }

    /**
     * @return The number of seconds since midnight, January 1, 1970 UTC.
     */
    fun getCurrentTimestamp(): Long {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
    }

    fun getNextRecurringDate(recurringDate: Long, recurrencePattern: RecurrencePattern, now: Long): Long {
        var result = recurringDate
        if (recurringDate != -1L && recurrencePattern != RecurrencePattern.NONE) {
            val recurringDateCal = Calendar.getInstance()
            recurringDateCal.setTimeInMillis(TimeUnit.SECONDS.toMillis(recurringDate))
            val nowCal = Calendar.getInstance()
            nowCal.setTimeInMillis(TimeUnit.SECONDS.toMillis(now))
            getNextRecurringDate(recurringDateCal, recurrencePattern, nowCal)
            result = TimeUnit.MILLISECONDS.toSeconds(recurringDateCal.timeInMillis)
        }
        return result
    }

    fun getNextRecurringDate(recurringDate: Calendar, recurrencePattern: RecurrencePattern, now: Calendar) {
        if (recurrencePattern != RecurrencePattern.NONE) {
            // TODO When API 26 can be used, use ChronoUnit for a better implementation of this method.
            // Jump to previous year to have less iterations.
            val previousYear = now[Calendar.YEAR] - 1
            if (recurringDate[Calendar.YEAR] < previousYear) {
                recurringDate[Calendar.YEAR] = previousYear
            }
            while (recurringDate < now) {
                addInterval(recurringDate, recurrencePattern)
            }
        }
    }

    fun addInterval(date: Calendar, recurrencePattern: RecurrencePattern, amount: Int = 1) {
        when (recurrencePattern) {
            RecurrencePattern.NONE -> Log.e(TAG, "Unable to add interval because no recurrence pattern set.")
            RecurrencePattern.DAILY -> date.add(Calendar.DAY_OF_YEAR, amount)
            RecurrencePattern.WEEKLY -> date.add(Calendar.WEEK_OF_YEAR, amount)
            RecurrencePattern.MONTHLY -> date.add(Calendar.MONTH, amount)
            RecurrencePattern.YEARLY -> date.add(Calendar.YEAR, amount)
        }
    }

    fun computeRepetitions(firstDate: Long, followingDate: Long, recurrencePattern: RecurrencePattern): Long {
        if (recurrencePattern == RecurrencePattern.NONE) {
            return 0
        }
        if (recurrencePattern == RecurrencePattern.DAILY) {
            return TimeUnit.DAYS.convert(followingDate - firstDate, TimeUnit.SECONDS)
        }

        val first = Calendar.getInstance()
        first.setTimeInMillis(TimeUnit.SECONDS.toMillis(firstDate))
        val following = Calendar.getInstance()
        following.setTimeInMillis(TimeUnit.SECONDS.toMillis(followingDate))
        val result: Int
        when (recurrencePattern) {
            RecurrencePattern.WEEKLY -> {
                val unitsFirst = 52 * first[Calendar.YEAR] + first[Calendar.WEEK_OF_YEAR]
                val unitsFollowing = 52 * following[Calendar.YEAR] + following[Calendar.WEEK_OF_YEAR]
                result = unitsFollowing - unitsFirst
            }
            RecurrencePattern.MONTHLY -> {
                val unitsFirst = 12 * first[Calendar.YEAR] + first[Calendar.MONTH]
                val unitsFollowing = 12 * following[Calendar.YEAR] + following[Calendar.MONTH]
                result = unitsFollowing - unitsFirst
            }
            RecurrencePattern.YEARLY -> {
                result = following[Calendar.YEAR] - first[Calendar.YEAR]
            }
            else -> throw InternalError("Unhandled recurrence pattern: $recurrencePattern")
        }
        return result.toLong()
    }

    fun getDeadlineColor(context: Context, color: DeadlineColors?): Int {
        return when (color) {
            DeadlineColors.RED -> ContextCompat.getColor(context, R.color.deadline_red)
            DeadlineColors.BLUE -> ContextCompat.getColor(context, R.color.deadline_blue)
            DeadlineColors.ORANGE -> ContextCompat.getColor(context, R.color.deadline_orange)
            else -> throw IllegalArgumentException("Unknown deadline color '$color'.")
        }
    }

    fun recurrencePatternToString(context: Context, recurrencePattern: RecurrencePattern?): String {
        return when (recurrencePattern) {
            RecurrencePattern.NONE -> context.resources.getString(R.string.none)
            RecurrencePattern.DAILY -> context.resources.getString(R.string.daily)
            RecurrencePattern.WEEKLY -> context.resources.getString(R.string.weekly)
            RecurrencePattern.MONTHLY -> context.resources.getString(R.string.monthly)
            RecurrencePattern.YEARLY -> context.resources.getString(R.string.yearly)
            else -> "Unknown recurrence pattern '$recurrencePattern'"
        }
    }

    fun priorityToString(context: Context, priority: TodoTask.Priority?): String {
        return when (priority) {
            TodoTask.Priority.HIGH -> context.resources.getString(R.string.high_priority)
            TodoTask.Priority.MEDIUM -> context.resources.getString(R.string.medium_priority)
            TodoTask.Priority.LOW -> context.resources.getString(R.string.low_priority)
            else -> "Unknown priority '$priority'"
        }
    }

    fun snoozeDurationToString(context: Context, snoozeDuration: Long, shortVersion: Boolean = false): String {
        val snoozeDurationValues = context.resources.getStringArray(R.array.snooze_duration_values)
        for (index in snoozeDurationValues.indices) {
            if (snoozeDurationValues[index].toLong() == snoozeDuration) {
                val valuesHuman = context.resources.getStringArray(
                    if (shortVersion) R.array.snooze_duration_values_human_short else R.array.snooze_duration_values_human)
                if (index < valuesHuman.size) {
                    return valuesHuman[index]
                }
                break
            }
        }
        return "Unknown snooze duration '$snoozeDuration'"
    }

    fun getMenuHeader(context: Context, title: String?): TextView {
        val blueBackground = TextView(context)
        blueBackground.setLayoutParams(LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))
        blueBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
        blueBackground.text = title
        blueBackground.setTextColor(ContextCompat.getColor(context, R.color.black))
        blueBackground.setPadding(65, 65, 65, 65)
        blueBackground.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        blueBackground.setTypeface(null, Typeface.BOLD)
        return blueBackground
    }

    fun isPackageAvailable(packageManager: PackageManager, packageName: String): Boolean {
        // TODO Try to find a way to get the information without functional use of exception.
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

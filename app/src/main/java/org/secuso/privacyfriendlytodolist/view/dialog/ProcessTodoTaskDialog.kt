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
package org.secuso.privacyfriendlytodolist.view.dialog

import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.Model
import org.secuso.privacyfriendlytodolist.model.TodoList
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.TodoTask.RecurrencePattern
import org.secuso.privacyfriendlytodolist.util.Helper.createDateString
import org.secuso.privacyfriendlytodolist.util.Helper.createDateTimeString
import org.secuso.privacyfriendlytodolist.util.Helper.getCurrentTimestamp
import org.secuso.privacyfriendlytodolist.util.Helper.priorityToString
import org.secuso.privacyfriendlytodolist.util.Helper.recurrencePatternToString
import org.secuso.privacyfriendlytodolist.util.LogTag
import org.secuso.privacyfriendlytodolist.util.PreferenceMgr

/**
 * This class creates a dialog that lets the user create/edit a task.
 *
 * Created by Sebastian Lutz on 12.03.2018.
 */
@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class ProcessTodoTaskDialog(context: FragmentActivity,
                            private val todoLists: List<TodoList>,
                            private var selectedTodoList: TodoList? = null,
                            taskToEdit: TodoTask? = null):
        FullScreenDialog<ResultCallback<TodoTask>>(context, R.layout.add_task_dialog) {
    private val editExistingTask: Boolean
    private var todoTask: TodoTask
    private var deadline: Long
    private var recurrencePattern: RecurrencePattern
    private var reminderTime: Long
    private var taskProgress: Int
    private var taskPriority: TodoTask.Priority

    // GUI elements
    private lateinit var taskName: EditText
    private lateinit var taskDescription: EditText
    private lateinit var deadlineTextView: TextView
    private lateinit var recurrencePatternTextView: TextView
    private lateinit var reminderTextView: TextView
    private lateinit var progressSelector: SeekBar
    private lateinit var progressPercent: TextView
    private lateinit var prioritySelector: TextView
    private lateinit var listSelector: TextView

    init {
        if (null != taskToEdit) {
            editExistingTask = true
            todoTask = taskToEdit
        } else {
            editExistingTask = false
            todoTask = Model.createNewTodoTask()
        }
        deadline = todoTask.getDeadline()
        recurrencePattern = todoTask.getRecurrencePattern()
        reminderTime = todoTask.getReminderTime()
        taskProgress = todoTask.getProgress(false)
        taskPriority = todoTask.getPriority()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initGui()
        if (editExistingTask) {
            taskName.setText(todoTask.getName())
            taskDescription.setText(todoTask.getDescription())
            deadlineTextView.text = if (todoTask.getDeadline() <= 0)
                context.getString(R.string.deadline) else createDateString(deadline)
            updateRecurrencePatternText()
            reminderTextView.text = if (todoTask.getReminderTime() <= 0)
                context.getString(R.string.reminder) else createDateTimeString(reminderTime)
            progressSelector.progress = todoTask.getProgress(false)
            prioritySelector.text = priorityToString(context, todoTask.getPriority())
        }
    }

    private fun initGui() {
        taskName = findViewById(R.id.et_task_name)
        taskDescription = findViewById(R.id.et_task_description)

        if (!editExistingTask) {
            // Request focus for first input field.
            taskName.requestFocus()
            // Show soft-keyboard
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }

        // initialize textview that displays the selected priority
        prioritySelector = findViewById(R.id.tv_task_priority)
        prioritySelector.setOnClickListener {
            registerForContextMenu(prioritySelector)
            openContextMenu(prioritySelector)
        }
        prioritySelector.setOnCreateContextMenuListener(this)
        taskPriority = TodoTask.Priority.DEFAULT_VALUE
        prioritySelector.text = priorityToString(context, taskPriority)

        //initialize titles of the dialog
        val dialogTitle = findViewById<TextView>(R.id.dialog_title)
        if (editExistingTask) {
            dialogTitle.text = context.resources.getString(R.string.edit_todo_task)
        }

        // Initialize textview that displays selected list
        listSelector = findViewById(R.id.tv_task_list_choose)
        listSelector.setOnClickListener { v: View? ->
            registerForContextMenu(listSelector)
            openContextMenu(listSelector)
        }
        listSelector.setOnCreateContextMenuListener(this)
        updateListSelector()

        progressPercent = findViewById(R.id.tv_task_progress)

        // initialize seekbar that allows to select the progress
        progressSelector = findViewById(R.id.sb_task_progress)
        if (hasAutoProgress()) {
            findViewById<TextView>(R.id.tv_task_progress_str).visibility = View.GONE
            progressSelector.visibility = View.GONE
            progressPercent.visibility = View.GONE
        } else {
            progressSelector.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    taskProgress = progress
                    val text = "$progress %"
                    progressPercent.text = text
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }
        
        // initialize buttons
        val okayButton: Button = findViewById(R.id.bt_process_task_ok)
        okayButton.setOnClickListener { v: View? ->
            val name: String = taskName.getText().toString()
            val description: String = taskDescription.getText().toString()
            if (name.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.todo_name_must_not_be_empty),
                    Toast.LENGTH_SHORT).show()
            } else if (recurrencePattern != RecurrencePattern.NONE && deadline == -1L) {
                Toast.makeText(context, context.getString(R.string.set_deadline_if_recurring),
                    Toast.LENGTH_SHORT).show()
            } else {
                todoTask.setName(name)
                todoTask.setDescription(description)
                todoTask.setDeadline(deadline)
                todoTask.setRecurrencePattern(recurrencePattern)
                todoTask.setReminderTime(reminderTime)
                todoTask.setProgress(taskProgress)
                todoTask.setPriority(taskPriority)
                todoTask.setListId(selectedTodoList?.getId())
                todoTask.setChanged()
                getDialogCallback().onFinish(todoTask)
                dismiss()
            }
        }
        val cancelButton: Button = findViewById(R.id.bt_process_task_cancel)
        cancelButton.setOnClickListener { dismiss() }

        // initialize text-views to get deadline and reminder time
        deadlineTextView = findViewById(R.id.tv_todo_list_deadline)
        deadlineTextView.setTextColor(okayButton.currentTextColor)
        deadlineTextView.setOnClickListener {
            val deadlineDialog = DeadlineDialog(context, deadline)
            deadlineDialog.setDialogCallback(object : DeadlineCallback {
                override fun setDeadline(selectedDeadline: Long) {
                    deadline = selectedDeadline
                    deadlineTextView.text = createDateString(selectedDeadline)
                }

                override fun removeDeadline() {
                    deadline = -1
                    deadlineTextView.text = context.resources.getString(R.string.deadline)
                }
            })
            deadlineDialog.show()
        }

        recurrencePatternTextView = findViewById(R.id.tv_task_recurrence_pattern)
        recurrencePatternTextView.setTextColor(okayButton.currentTextColor)
        recurrencePatternTextView.setOnClickListener {
            registerForContextMenu(recurrencePatternTextView)
            openContextMenu(recurrencePatternTextView)
        }
        recurrencePatternTextView.setOnCreateContextMenuListener(this)

        reminderTextView = findViewById(R.id.tv_todo_list_reminder)
        reminderTextView.setTextColor(okayButton.currentTextColor)
        reminderTextView.setOnClickListener {
            val reminderDialog = ReminderDialog(context, reminderTime, deadline)
            reminderDialog.setDialogCallback(object : ReminderCallback {
                override fun setReminderTime(selectedReminderTime: Long) {
                    var resIdErrorMsg = 0
                    if (recurrencePattern == RecurrencePattern.NONE) {
                        /* if (deadline == -1L) {
                            resIdErrorMsg = R.string.set_deadline_before_reminder
                        } else */
                        if (deadline != -1L && deadline < selectedReminderTime) {
                            resIdErrorMsg = R.string.deadline_smaller_reminder
                        } else if (selectedReminderTime < getCurrentTimestamp()) {
                            resIdErrorMsg = R.string.reminder_smaller_now
                        }
                    }
                    if (resIdErrorMsg == 0) {
                        reminderTime = selectedReminderTime
                        reminderTextView.text = createDateTimeString(reminderTime)
                    } else {
                        Toast.makeText(context, context.getString(resIdErrorMsg), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun removeReminderTime() {
                    reminderTime = -1L
                    val reminderTextView: TextView = findViewById(R.id.tv_todo_list_reminder)
                    reminderTextView.text = context.resources.getString(R.string.reminder)
                }
            })
            reminderDialog.show()
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        when (v.id) {
            R.id.tv_task_recurrence_pattern -> {
                menu.setHeaderTitle(R.string.select_recurrence_pattern)
                for (pattern in RecurrencePattern.entries) {
                    menu.add(v.id, pattern.ordinal, Menu.NONE, recurrencePatternToString(context, pattern))
                }
            }

            R.id.tv_task_priority -> {
                menu.setHeaderTitle(R.string.select_priority)
                for (priority in TodoTask.Priority.entries) {
                    menu.add(v.id, priority.ordinal, Menu.NONE, priorityToString(context, priority))
                }
            }

            R.id.tv_task_list_choose -> {
                menu.setHeaderTitle(R.string.select_list)
                menu.add(v.id, -1, Menu.NONE, R.string.select_no_list)
                var i = 0
                while (i < todoLists.size) {
                    val todoList = todoLists[i]
                    menu.add(v.id, i, Menu.NONE, todoList.getName())
                    ++i
                }
            }
        }
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        when (item.groupId) {
            R.id.tv_task_recurrence_pattern -> {
                recurrencePattern = RecurrencePattern.fromOrdinal(item.itemId)!!
                updateRecurrencePatternText()
            }

            R.id.tv_task_priority -> {
                taskPriority = TodoTask.Priority.fromOrdinal(item.itemId)!!
                prioritySelector.text = priorityToString(context, taskPriority)
            }

            R.id.tv_task_list_choose -> {
                val index = item.itemId
                selectedTodoList = if (index >= 0 && index < todoLists.size) todoLists[index] else null
                updateListSelector()
            }

            else -> {
                Log.e(TAG, "Unhandled menu item group ID ${item.groupId}.")
            }
        }

        return super.onMenuItemSelected(featureId, item)
    }

    private fun updateListSelector() {
        listSelector.text = if (null != selectedTodoList) {
            selectedTodoList!!.getName()
        } else {
            context.getString(R.string.click_to_choose)
        }
    }

    private fun updateRecurrencePatternText() {
        recurrencePatternTextView.text = if (recurrencePattern == RecurrencePattern.NONE) {
            context.getString(R.string.recurrence_pattern)
        } else {
            recurrencePatternToString(context, recurrencePattern)
        }
    }

    private fun hasAutoProgress(): Boolean {
        //automatic-progress enabled?
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PreferenceMgr.P_IS_AUTO_PROGRESS.name, false)
    }

    companion object {
        private val TAG = LogTag.create(this::class.java.declaringClass)
    }
}

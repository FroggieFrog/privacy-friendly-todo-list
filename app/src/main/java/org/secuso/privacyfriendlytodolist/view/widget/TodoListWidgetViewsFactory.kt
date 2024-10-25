/*
Privacy Friendly To-Do List
Copyright (C) 2024  SECUSO (www.secuso.org)

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
package org.secuso.privacyfriendlytodolist.view.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.ModelServices
import org.secuso.privacyfriendlytodolist.model.ModelServices.DeliveryOption
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.Tuple
import org.secuso.privacyfriendlytodolist.util.LogTag
import org.secuso.privacyfriendlytodolist.viewmodel.CustomViewModel


/**
 * Created by Sebastian Lutz on 15.02.2018.
 *
 * This class sets to-do tasks to show up in the widget
 *
 */
class TodoListWidgetViewsFactory(private val context: Context, private val appWidgetId: Int) : RemoteViewsFactory {
    private var viewModel: CustomViewModel? = null
    private var model: ModelServices? = null
    private val items = ArrayList<Tuple<Int, RemoteViews>>()
    private lateinit var defaultTitle: String
    private var currentTitle: String = ""

    override fun onCreate() {
        viewModel = CustomViewModel(context)
        model = viewModel!!.model
        defaultTitle = context.getString(R.string.app_name)
    }

    override fun onDestroy() {
        model = null
        viewModel!!.destroy()
        viewModel = null
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun onDataSetChanged() {
        val pref = TodoListWidgetConfigureActivity.loadWidgetPreferences(context, appWidgetId)
        val job: Job
        var newTitle: String? = null
        var changedTodoTasks: List<TodoTask>? = null
        if (null != pref.todoListId) {
            job = model!!.getToDoListById(pref.todoListId!!, DeliveryOption.DIRECT) { todoList ->
                if (null != todoList) {
                    newTitle = todoList.getName()
                    changedTodoTasks = todoList.getTasks()
                }
            }
        } else {
            job = model!!.getAllToDoTasks(DeliveryOption.DIRECT) { todoTasks ->
                newTitle = defaultTitle
                changedTodoTasks = todoTasks
            }
        }
        runBlocking {
            job.join()
        }
        if (null == changedTodoTasks) {
            Log.e(TAG, "Widget $appWidgetId: Failed to get changed tasks.")
            return
        }

        if (null != newTitle && currentTitle != newTitle) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val bundle: Bundle = appWidgetManager.getAppWidgetOptions(appWidgetId)
            bundle.putString(TodoListWidget.OPTION_WIDGET_TITLE, newTitle)
            appWidgetManager.updateAppWidgetOptions(appWidgetId, bundle)
            currentTitle = newTitle!!
        }

        items.clear()
        val fillInIntent = Intent()
        fillInIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        fillInIntent.putExtra(TodoListWidget.EXTRA_WIDGET_LIST_ID, pref.todoListId.toString())
        for (todoTask in changedTodoTasks!!) {
            val item = createItem(todoTask, fillInIntent)
            val tuple = Tuple(todoTask.getId(), item)
            items.add(tuple)
        }
        Log.d(TAG, "Widget $appWidgetId: Updated data. Items: ${items.count()}, list ID: ${pref.todoListId}, title: '$currentTitle'.")
    }

    @SuppressLint("ResourceType")
    private fun createItem(todoTask: TodoTask, fillInIntent: Intent): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.widget_tasks)
        if (todoTask.isDone()) {
            view.setViewVisibility(R.id.widget_done, View.VISIBLE)
            view.setViewVisibility(R.id.widget_undone, View.INVISIBLE)
        } else {
            view.setViewVisibility(R.id.widget_done, View.INVISIBLE)
            view.setViewVisibility(R.id.widget_undone, View.VISIBLE)
        }
        view.setTextViewText(R.id.tv_widget_task_name, todoTask.getName())
        view.setEmptyView(R.id.tv_empty_widget, R.string.empty_todo_list)
        view.setOnClickFillInIntent(R.id.tv_widget_task_name, fillInIntent)
        view.setOnClickFillInIntent(R.id.widget_undone, fillInIntent)
        view.setOnClickFillInIntent(R.id.widget_done, fillInIntent)
        return view
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewAt(position: Int): RemoteViews? {
        return if (position >= 0 && position < items.size) items[position].right else null
    }

    override fun getItemId(position: Int): Long {
        return if (position >= 0 && position < items.size) items[position].left.toLong() else 0
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    companion object {
        private val TAG = LogTag.create(this::class.java.declaringClass)
    }
}

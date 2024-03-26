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
package org.secuso.privacyfriendlytodolist.model

import android.content.Context
import android.os.Handler
import kotlinx.coroutines.CoroutineScope
import org.secuso.privacyfriendlytodolist.model.impl.ModelServicesImpl
import org.secuso.privacyfriendlytodolist.model.impl.TodoListImpl
import org.secuso.privacyfriendlytodolist.model.impl.TodoSubtaskImpl
import org.secuso.privacyfriendlytodolist.model.impl.TodoTaskImpl

/**
 * Created by Christian Adams on 25.02.2024.
 *
 * This class creates instances of the data model of the To-Do List App.
 */
object Model {
    /**
     * @param context Gets used to access the database.
     * @param coroutineScope Gets used to call co-routines.
     * @param resultHandler Gets used to dispatch result of co-routines to event queue of receiver thread.
     */
    @JvmStatic
    fun createServices(context: Context, coroutineScope: CoroutineScope, resultHandler: Handler): ModelServices {
        return ModelServicesImpl(context, coroutineScope, resultHandler)
    }

    @JvmStatic
    fun createNewTodoList(): TodoList {
        return TodoListImpl()
    }

    @JvmStatic
    fun createNewTodoTask(): TodoTask {
        return TodoTaskImpl()
    }

    @JvmStatic
    fun createNewTodoSubtask(): TodoSubtask {
        return TodoSubtaskImpl()
    }
}

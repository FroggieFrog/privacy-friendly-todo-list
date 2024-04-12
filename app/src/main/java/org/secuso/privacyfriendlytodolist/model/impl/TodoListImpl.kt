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
package org.secuso.privacyfriendlytodolist.model.impl

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable.Creator
import org.secuso.privacyfriendlytodolist.model.TodoList
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.TodoTask.DeadlineColors
import org.secuso.privacyfriendlytodolist.model.database.entities.TodoListData
import java.util.Locale


/**
 * Created by Sebastian Lutz on 12.03.2018.
 *
 * Class to set up a To-Do List and its parameters.
 */
class TodoListImpl : BaseTodoImpl, TodoList {
    /** Container for data that gets stored in the database.  */
    val data: TodoListData

    private var tasks: MutableList<TodoTask> = ArrayList()
    private var isDummyList = false

    constructor() {
        data = TodoListData()
        isDummyList = true
        // New item needs to be stored in database.
        requiredDBAction = RequiredDBAction.INSERT
    }

    constructor(data: TodoListData) {
        this.data = data
        isDummyList = false
    }

    constructor(parcel: Parcel) {
        data = TodoListData()
        data.id = parcel.readInt()
        isDummyList = parcel.readByte().toInt() != 0
        data.name = parcel.readString()!!
        parcel.readList(tasks, TodoTask::class.java.getClassLoader())
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(data.id)
        dest.writeByte((if (isDummyList) 1 else 0).toByte())
        dest.writeString(data.name)
        dest.writeList(tasks)
        // Parcel-interface is used for data backup.
        // This use case does not require that 'dbState' gets stored in the parcel.
    }

    override fun setId(id: Int) {
        data.id = id
        isDummyList = false
    }

    override fun getId(): Int {
        return data.id
    }

    override fun isDummyList(): Boolean {
        return isDummyList
    }

    override fun setName(name: String) {
        data.name = name
    }

    override fun getName(): String {
        return data.name
    }

    override fun getSize(): Int {
        return tasks.size
    }

    override fun setTasks(tasks: MutableList<TodoTask>) {
        this.tasks = tasks
    }

    override fun getTasks(): MutableList<TodoTask> {
        return tasks
    }

    override fun getColor(): Int {
        return Color.BLACK
    }

    override fun getDoneTodos(): Int {
        var counter = 0
        for (task in tasks) {
            if (task.isDone()) {
                ++counter
            }
        }
        return counter
    }

    override fun getNextDeadline(): Long {
        var minDeadLine: Long = -1
        for (i in tasks.indices) {
            val currentTask = tasks[i]
            if (!currentTask.isDone()) {
                if (minDeadLine == -1L && currentTask.getDeadline() > 0) minDeadLine =
                    currentTask.getDeadline() else {
                    val possNewDeadline = currentTask.getDeadline()
                    if (possNewDeadline in 1..<minDeadLine) {
                        minDeadLine = possNewDeadline
                    }
                }
            }
        }


        /*

        long minDeadLine = -1;
        if(tasks.size() > 0 ) {

            minDeadLine = tasks.get(0).getDeadline();
            for(int i=1; i<tasks.size(); i++) {
                long possNewDeadline = tasks.get(i).getDeadline();
                if (possNewDeadline > 0 && possNewDeadline < minDeadLine)
                    minDeadLine = possNewDeadline;
            }

        } */return minDeadLine
    }

    override fun getDeadlineColor(defaultReminderTime: Long): DeadlineColors {
        var result = DeadlineColors.BLUE
        for (currentTask in tasks) {
            when (currentTask.getDeadlineColor(defaultReminderTime)) {
                DeadlineColors.RED -> return DeadlineColors.RED
                DeadlineColors.ORANGE -> result = DeadlineColors.ORANGE
                else -> {}
            }
        }
        return result
    }


    override fun checkQueryMatch(query: String?, recursive: Boolean): Boolean {
        // no query? always match!
        if (query.isNullOrEmpty()) {
            return true
        }
        val queryLowerCase = query.lowercase(Locale.getDefault())
        if (data.name.lowercase(Locale.getDefault()).contains(queryLowerCase)) {
            return true
        }
        if (recursive) {
            for (task in tasks) {
                if (task.checkQueryMatch(queryLowerCase, true)) {
                    return true
                }
            }
        }
        return false
    }

    override fun checkQueryMatch(query: String?): Boolean {
        return checkQueryMatch(query, true)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "'${getName()}' (id ${getId()})"
    }

    companion object CREATOR : Creator<TodoListImpl> {
        override fun createFromParcel(parcel: Parcel): TodoListImpl {
            return TodoListImpl(parcel)
        }

        override fun newArray(size: Int): Array<TodoListImpl?> {
            return arrayOfNulls(size)
        }
    }
}

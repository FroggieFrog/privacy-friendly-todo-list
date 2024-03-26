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

package org.secuso.privacyfriendlytodolist.model.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import org.secuso.privacyfriendlytodolist.model.database.entities.TodoTaskData

@Dao
interface TodoTaskDao {
    @Insert
    suspend fun insert(todoTaskData: TodoTaskData): Long
    @Update
    suspend fun update(todoTaskData: TodoTaskData): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(todoTaskData: TodoTaskData)
    @Delete
    suspend fun delete(todoTaskData: TodoTaskData): Int
    @Query("DELETE FROM todoTasks")
    suspend fun deleteAll(): Int
    @Query("SELECT COUNT(id) FROM todoTasks WHERE isInRecycleBin = 0")
    suspend fun getCountNotInRecycleBin(): Int
    @Query("SELECT * FROM todoTasks WHERE id = :todoTaskId LIMIT 1")
    suspend fun getById(todoTaskId: Int): TodoTaskData?
    @Query("SELECT * FROM todoTasks WHERE isDone = 0 AND reminderTime > 0 AND isInRecycleBin = 0 AND reminderTime - :today > 0 ORDER BY ABS(reminderTime - :today) LIMIT 1")
    suspend fun getNextDueTask(today: Long): TodoTaskData?
    @Query("SELECT * FROM todoTasks WHERE isDone = 0 AND isInRecycleBin = 0 AND reminderTime > 0 AND reminderTime <= :today AND id NOT IN (:lockedIds)")
    suspend fun getAllToRemind(today: Long, lockedIds: Set<Int>?): Array<TodoTaskData>
    @Query("SELECT * FROM todoTasks WHERE isInRecycleBin = 0")
    suspend fun getAllNotInRecycleBin(): Array<TodoTaskData>
    @Query("SELECT * FROM todoTasks WHERE isInRecycleBin <> 0")
    suspend fun getAllInRecycleBin(): Array<TodoTaskData>
    @Query("SELECT * FROM todoTasks WHERE listId = :listId AND isInRecycleBin = 0")
    suspend fun getAllOfListNotInRecycleBin(listId: Int): Array<TodoTaskData>
    @Query("UPDATE todoTasks SET name = :name, progress = :progress, isDone = :isDone WHERE id = :id")
    suspend fun updateValuesFromPomodoro(id: Int, name: String, progress: Int, isDone: Boolean): Int
}
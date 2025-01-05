/*
Privacy Friendly To-Do List
Copyright (C) 2018-2025  Sebastian Lutz

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
package org.secuso.privacyfriendlytodolist.view.dialog

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.Model
import org.secuso.privacyfriendlytodolist.model.TodoList

/**
 * Created by Sebastian Lutz on 12.03.2018.
 *
 * Defines the dialog that lets the user create a list
 */
class ProcessTodoListDialog(context: Context) :
    FullScreenDialog<ResultCallback<TodoList>>(context, R.layout.list_dialog) {
    private lateinit var todoList: TodoList
    private var editExistingList = false

    constructor(context: Context, todoList: TodoList) : this(context) {
        this.todoList = todoList
        editExistingList = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val etName = findViewById<EditText>(R.id.et_todo_list_name)
        val buttonOkay = findViewById<Button>(R.id.bt_todo_list_ok)
        val buttonCancel = findViewById<Button>(R.id.bt_todo_list_cancel)

        // Request focus for first input field.
        etName.requestFocus()
        // Show soft-keyboard
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        if (editExistingList) {
            findViewById<Toolbar>(R.id.list_dialog_title).setTitle(R.string.edit_todo_list)
            etName.setText(todoList.getName())
            etName.selectAll()
        } else {
            todoList = Model.createNewTodoList()
        }

        buttonOkay.setOnClickListener {
            // prepare list data
            val listName = etName.getText().toString()
            if (listName.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.list_name_must_not_be_empty),
                    Toast.LENGTH_SHORT).show()
            } else {
                // check if real changes were made
                if (todoList.getName() != listName) {
                    todoList.setName(listName)
                    getDialogCallback().onFinish(todoList)
                }
                dismiss()
            }
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }
    }
}

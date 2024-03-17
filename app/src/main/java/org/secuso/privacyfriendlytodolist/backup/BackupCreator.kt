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

package org.secuso.privacyfriendlytodolist.backup

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.preference.PreferenceManager
import android.util.JsonWriter
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.secuso.privacyfriendlybackup.api.backup.DatabaseUtil
import org.secuso.privacyfriendlybackup.api.backup.DatabaseUtil.writeDatabase
import org.secuso.privacyfriendlybackup.api.backup.PreferenceUtil.writePreferences
import org.secuso.privacyfriendlybackup.api.pfa.IBackupCreator
import org.secuso.privacyfriendlytodolist.model.database.TodoListDatabase
import org.secuso.privacyfriendlytodolist.util.PinUtil.hasPin
import org.secuso.privacyfriendlytodolist.view.PinActivity
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset

class BackupCreator : IBackupCreator {
    override fun writeBackup(context: Context, outputStream: OutputStream) : Boolean {
        return runBlocking {

            val pinCheck = async<Boolean> {
                // check if a pin is set and validate it first
                if(hasPin(context)) {
                    // wait for pin
                    PinActivity.result = null
                    context.startActivity(Intent(context, PinActivity::class.java).apply {
                        flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP or FLAG_ACTIVITY_CLEAR_TASK
                    })

                    while(PinActivity.result == null) {
                        delay(200)
                    }

                    return@async PinActivity.result!!
                } else {
                    return@async true
                }
            }

            if(pinCheck.await()) {
                return@runBlocking writeBackupInternal(context, outputStream)
            } else {
                return@runBlocking false
            }
        }
    }

    private fun writeBackupInternal(context: Context, outputStream: OutputStream) : Boolean {
        val outputStreamWriter = OutputStreamWriter(outputStream, Charset.forName("UTF-8"))
        val writer = JsonWriter(outputStreamWriter)
        writer.setIndent("")

        try {
            writer.beginObject()
            val dataBase = DatabaseUtil.getSupportSQLiteOpenHelper(context, TodoListDatabase.DATABASE_NAME).readableDatabase
            writer.name("database")
            writeDatabase(writer, dataBase)
            dataBase.close()

            writer.name("preferences")
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            writePreferences(writer, pref, arrayOf("pref_pin"))

            writer.endObject()
            writer.close()
            return true
        } catch (e: Exception) {
            Log.e("PFA BackupCreator", "Error occurred", e)
            e.printStackTrace()
            return false
        }
    }
}
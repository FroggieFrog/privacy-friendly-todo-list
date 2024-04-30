package org.secuso.privacyfriendlytodolist.view.dialog

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.util.PreferenceMgr
import org.secuso.privacyfriendlytodolist.view.dialog.PinDialog.PinCallback

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class PinDialog(context: Context, private val allowReset: Boolean) :
    FullScreenDialog<PinCallback>(context, R.layout.pin_dialog) {
    interface PinCallback {
        fun accepted()
        fun declined()
        fun resetApp()
    }

    private var wrongCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textEditPin: EditText = findViewById(R.id.et_pin_pin)
        val buttonOkay: Button = findViewById(R.id.bt_pin_ok)

        // Request focus for first input field.
        textEditPin.requestFocus()
        // Show soft-keyboard
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        buttonOkay.setOnClickListener {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val pinExpected = prefs.getString(PreferenceMgr.P_PIN.name, "")
            if (pinExpected == textEditPin.getText().toString()) {
                getDialogCallback().accepted()
                setOnDismissListener(null)
                dismiss()
            } else {
                wrongCounter++
                Toast.makeText(context, context.resources.getString(R.string.wrong_pin),
                    Toast.LENGTH_SHORT).show()
                textEditPin.setText("")
                textEditPin.isActivated = true
                if (wrongCounter >= 3 && allowReset) {
                    val buttonResetApp: Button = findViewById(R.id.bt_reset_application)
                    buttonResetApp.visibility = View.VISIBLE
                }
            }
        }
        val buttonResetApp: Button = findViewById(R.id.bt_reset_application)
        buttonResetApp.setOnClickListener {
            val resetDialogListener = DialogInterface.OnClickListener { dialog, which ->
                if (which == BUTTON_POSITIVE) {
                    getDialogCallback().resetApp()
                }
            }
            val builder = AlertDialog.Builder(context)
            val resources = context.resources
            builder.setMessage(resources.getString(R.string.reset_application_msg))
            builder.setPositiveButton(resources.getString(R.string.yes), resetDialogListener)
            builder.setNegativeButton(resources.getString(R.string.no), resetDialogListener)
            builder.show()
        }
        val buttonNoDeadline: Button = findViewById(R.id.bt_pin_cancel)
        buttonNoDeadline.setOnClickListener {
            getDialogCallback().declined()
            dismiss()
        }
        setOnDismissListener { getDialogCallback().declined() }
        textEditPin.isActivated = true
    }
}

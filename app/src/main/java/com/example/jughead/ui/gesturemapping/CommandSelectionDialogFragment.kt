package com.example.jughead.ui.gesturemapping

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class CommandSelectionDialogFragment(
    private val gestureName: String,
    private val onCommandSelected: (String) -> Unit
) : DialogFragment() {

    private val commandList = arrayOf("Change Color", "Adjust Brightness", "Activate Strobe", "Toggle Mode")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Assign Command to $gestureName")
            .setItems(commandList) { _, which ->
                val selectedCommand = commandList[which]
                onCommandSelected(selectedCommand)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}

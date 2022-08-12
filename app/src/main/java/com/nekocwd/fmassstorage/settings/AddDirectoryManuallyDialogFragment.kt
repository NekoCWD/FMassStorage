package com.nekocwd.fmassstorage.settings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import com.nekocwd.fmassstorage.R
import com.nekocwd.fmassstorage.Utils
import com.nekocwd.fmassstorage.databinding.DialogAddNewDirectoryBinding

class AddDirectoryManuallyDialogFragment(val addFun:(Utils.Directory) -> Unit): DialogFragment() {
    private lateinit var binding: DialogAddNewDirectoryBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val builder = AlertDialog.Builder(requireActivity())
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_add_new_directory, null)
        builder.setView(view)
        binding = DialogAddNewDirectoryBinding.bind(view)
        builder.setPositiveButton(R.string.add
        ) { _: DialogInterface, _: Int ->
            val path = binding.path.text.toString()
            val label = binding.label.text.toString()
            if(Utils.Directory.checkDirectory(path)){
                addFun(
                    Utils.Directory(
                        binding.path.text.toString(),
                        binding.label.text.toString(),
                        true,
                        null
                    )
                )
                dialog?.hide()
            }
            else {
                Toast.makeText(requireContext(), R.string.directory_not_exist, Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(R.string.cancel) { _, _ ->
            dialog?.cancel()
        }
        return builder.create()
    }
}
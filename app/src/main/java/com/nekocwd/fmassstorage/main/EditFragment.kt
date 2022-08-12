package com.nekocwd.fmassstorage.main

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.nekocwd.fmassstorage.R
import com.nekocwd.fmassstorage.Utils
import com.nekocwd.fmassstorage.databinding.FragmentCreateNewImageBinding

class EditFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: FragmentCreateNewImageBinding
    private lateinit var directories: ArrayList<Utils.Directory>
    private lateinit var directoryNames: ArrayList<String>
    private var selectedDir: Utils.Directory? = null
    private var newImageUri: Uri? = null

    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK) {
            Log.i("Data", it.data?.data.toString())
            val intentFlag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            newImageUri = it.data?.data
            newImageUri?.let{ uri ->
                requireContext().grantUriPermission(requireContext().packageName, uri, intentFlag)
                requireContext().contentResolver.takePersistableUriPermission(uri, intentFlag)
                binding.image.setImageDrawable(Utils.getDrawableFromUri(uri, requireContext()))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentCreateNewImageBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.dirChooser.onItemSelectedListener = this

        directories = Utils.Directory.getAll(requireContext())
        directoryNames = arrayListOf<String>()

        var directory: Utils.Directory? = null
        for (dir in directories){
            directoryNames.add(dir.label)
        }
        val dirAdapter = ArrayAdapter(requireContext(), R.layout.item_string_spinner, directoryNames)
        binding.dirChooser.adapter = dirAdapter

        for(dir in directories){
            if(dir.dirId == arguments?.getInt("dirId")){
                directory = dir
                binding.dirChooser.setSelection(directoryNames.indexOf(dir.label))
            }
        }
        Log.i("Directory", directory.toString())

        var image: Utils.Image? = null
        directory?.let {
            for(img in Utils.Image.getAllFromDirectory(it, requireContext())){
                if(img.path == arguments?.getString("imgPath")){
                    image = img
                }
            }
        }
        binding.label.setText(image?.label)
        binding.image.setImageDrawable(image?.image)

        Log.i("Image", image.toString())
        binding.addButton.setOnClickListener{
            selectedDir?.let {
                if(binding.label.text.toString().isBlank()){
                    Toast.makeText(requireContext(), getString(R.string.please_define_directory_name), Toast.LENGTH_SHORT).show()
                }
                else {
                    image?.apply {
                        label = binding.label.text.toString()
                        commit(requireContext())
                        newImageUri?.let { setIcon(requireContext(), it) }
                        if(selectedDir != directory){
                            move(it)
                        }
                    }
                    findNavController().navigateUp()
                }
            }
            if(selectedDir == null){
                Toast.makeText(requireContext(), getString(R.string.directory_wasnt_selected), Toast.LENGTH_SHORT).show()
            }
        }
        binding.select.setOnClickListener{
            getResult.launch(Utils.selectImage())
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        selectedDir = directories[pos]
        Log.i(selectedDir!!.label, "selected")
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }
}
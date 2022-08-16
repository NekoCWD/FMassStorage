package com.nekocwd.fmassstorage.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.nekocwd.fmassstorage.R
import com.nekocwd.fmassstorage.Utils
import com.nekocwd.fmassstorage.databinding.FragmentDirectorySettingBinding

class DirectorySettingFragment : Fragment() {

    private lateinit var binding: FragmentDirectorySettingBinding
    private val adapter = Utils.DirectoryListAdapter({
        it.remove(requireContext())
    },{
        it.commit(requireContext())
    })

    @SuppressLint("NotifyDataSetChanged")
    private val getResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == Activity.RESULT_OK){
            val value = it.data!!.data
            val dirResult = DocumentFile.fromTreeUri(requireContext(), value!!)!!
            Utils.Directory.fromDocumentFile(dirResult, requireContext())
            adapter.dataset.clear()
            adapter.dataset.addAll(Utils.Directory.getAll(requireContext()))
            adapter.notifyDataSetChanged()
            binding.noDirectories.visibility = if(adapter.dataset.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, sis: Bundle?): View {
        binding = FragmentDirectorySettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.dataset.addAll(Utils.Directory.getAll(requireContext()))
        binding.noDirectories.visibility = if(adapter.dataset.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()

        binding.addFab.setOnClickListener {
            val isOpened = binding.addManuallyFab.visibility == View.VISIBLE

            val rotateFabClose = AnimationUtils.loadAnimation(requireContext(), R.anim.fab_add_close_anim)
            val rotateFabOpen = AnimationUtils.loadAnimation(requireContext(), R.anim.fab_add_open_anim)
            val showFab = AnimationUtils.loadAnimation(requireContext(), R.anim.fabs_open_anim)
            val hideFab = AnimationUtils.loadAnimation(requireContext(), R.anim.fabs_close_anim)
            val showFabLabel = AnimationUtils.loadAnimation(requireContext(), R.anim.label_fabs_open_anim)
            val hideFabLabel = AnimationUtils.loadAnimation(requireContext(), R.anim.label_fabs_close_anim)

            val visibility = if(isOpened) View.GONE else View.VISIBLE
            val animationFab = if(isOpened) hideFab else showFab
            val animationFabLabel = if(isOpened) hideFabLabel else showFabLabel

            binding.addFab.startAnimation(if(isOpened) rotateFabClose else rotateFabOpen)
            binding.addFab.rotation = if(isOpened) 0f else 45f

            binding.addManuallyLabel.startAnimation(animationFabLabel)
            binding.addManuallyLabel.visibility = visibility
            binding.addManuallyFab.startAnimation(animationFab)
            binding.addManuallyFab.visibility = visibility

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.addDirectoryFromDirectoryPickerLabel.startAnimation(animationFabLabel)
                binding.addDirectoryFromDirectoryPickerLabel.visibility = visibility
                binding.addDirectoryFromDirectoryPickerFab.startAnimation(animationFab)
                binding.addDirectoryFromDirectoryPickerFab.visibility = visibility
            }
        }
        binding.addManuallyFab.setOnClickListener {
            val alertDialog = AddDirectoryManuallyDialogFragment {
                adapter.dataset.add(it)
                it.commit(requireContext())
                adapter.notifyDataSetChanged()
                binding.noDirectories.visibility = if(adapter.dataset.isEmpty()) View.VISIBLE else View.GONE
            }
            alertDialog.show(childFragmentManager, "a")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.addDirectoryFromDirectoryPickerFab.setOnClickListener {
                getResult.launch(Utils.Directory.choseDirectory())
            }
        }
    }
}
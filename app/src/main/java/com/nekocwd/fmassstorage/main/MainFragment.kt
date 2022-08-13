package com.nekocwd.fmassstorage.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.UiAutomation
import android.os.Bundle
import android.transition.Visibility
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.nekocwd.fmassstorage.R
import com.nekocwd.fmassstorage.Utils
import com.nekocwd.fmassstorage.databinding.FragmentMainBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    @SuppressLint("SetTextI18n")
    private val imageListAdapter = Utils.ImageListAdapter({ img->
        val lun = Utils.findLunFile()
        if(lun == null){
            Toast.makeText(requireContext(), R.string.cannot_find_lun_file, Toast.LENGTH_SHORT).show()
        }
        lun?.let {
            if(img.host(requireContext())){
                img.image.let {binding.imageView.setImageDrawable(it)}
                binding.imageLabel.text = img.label
                binding.imagePath.text = "${img.directory.label}/${img.path}"
                binding.statusContainer.visibility = View.VISIBLE
            } else {
                Toast.makeText(requireContext(), R.string.fail_device_in_use, Toast.LENGTH_SHORT).show()
            }
        }
    },
    {img ->
        val args = Bundle().apply {
            putInt("dirId", img.directory.dirId!!)
            putString("imgPath", img.path)
        }
        findNavController().navigate(R.id.action_main2createnew, args)
    })


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, sis: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageList.adapter = imageListAdapter
        val directories = Utils.Directory.getAll(requireContext())
        imageListAdapter.dataset.clear()
        for(directory in directories){
            val images = Utils.Image.getAllFromDirectory(directory, requireContext())
            imageListAdapter.dataset.addAll(images)
        }
        imageListAdapter.notifyDataSetChanged()

        binding.addFab.setOnClickListener {
            val isOpened = binding.createNewFab.visibility == View.VISIBLE

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

            binding.createNewLabel.startAnimation(animationFabLabel)
            binding.createNewLabel.visibility = visibility
            binding.createNewFab.startAnimation(animationFab)
            binding.createNewFab.visibility = visibility

            binding.downloadImageLabel.startAnimation(animationFabLabel)
            binding.downloadImageLabel.visibility = visibility
            binding.downloadImageFab.startAnimation(animationFab)
            binding.downloadImageFab.visibility = visibility
        }
        binding.createNewFab.setOnClickListener {
            findNavController().navigate(R.id.action_main2createnew)
        }
        binding.eject.setOnClickListener {
            Utils.eject()
            binding.statusContainer.visibility = View.GONE
        }
        Utils.Image.findNowHosting(requireContext())?.let {
            binding.statusContainer.visibility = View.VISIBLE
            binding.imageView.setImageDrawable(it.image)
            binding.imageLabel.text = it.label
            binding.imagePath.text = "${it.directory.label}/${it.path}"
        }
    }

}
package com.nekocwd.fmassstorage.rootcheck

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.nekocwd.fmassstorage.R
import com.nekocwd.fmassstorage.Utils
import com.nekocwd.fmassstorage.databinding.FragmentRootCheckBinding
import com.nekocwd.fmassstorage.main.MainActivity

/**
 * A [Fragment] subclass for check for root permissions
 */
class RootCheckFragment : Fragment() {

    private lateinit var binding: FragmentRootCheckBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, sis: Bundle?): View {
        preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        binding = FragmentRootCheckBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isRooted = preferences.getBoolean(Utils.isRootedPrefs, false)
        if(isRooted && Utils.checkForRoot()){
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
        else{
            preferences.edit().putBoolean(Utils.isRootedPrefs, false).apply()
            findNavController().navigate(R.id.action_root_checking2request)
        }
    }
}
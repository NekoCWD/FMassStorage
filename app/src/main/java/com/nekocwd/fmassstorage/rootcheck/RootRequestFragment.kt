package com.nekocwd.fmassstorage.rootcheck

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.nekocwd.fmassstorage.R
import com.nekocwd.fmassstorage.Utils
import com.nekocwd.fmassstorage.databinding.FragmentRootRequestBinding

/**
 * A [Fragment] subclass for request root
 */
class RootRequestFragment : Fragment() {

    private lateinit var binding: FragmentRootRequestBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, sis: Bundle?): View {
        preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        binding = FragmentRootRequestBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.requestButton.setOnClickListener {
            preferences.edit().putBoolean(Utils.isRootedPrefs, Utils.checkForRoot()).apply()
            findNavController().navigate(R.id.action_root_request2checking)
        }
    }
}
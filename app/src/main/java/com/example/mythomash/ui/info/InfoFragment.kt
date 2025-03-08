package com.example.mythomash.ui.info

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.mythomash.databinding.FragmentInfoMainBinding
import org.json.JSONObject

class InfoFragment : Fragment() {

    private lateinit var infoViewModel: InfoViewModel
    private lateinit var binding: FragmentInfoMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment using ViewBinding
        binding = FragmentInfoMainBinding.inflate(inflater, container, false)

        // Initialize ViewModel using ViewModelFactory with context and an empty initialData map
        infoViewModel = ViewModelProvider(
            this,
            InfoViewModelFactory(requireContext(), emptyMap()) // Pass an empty map as initialData
        ).get(InfoViewModel::class.java)

        // Observe LiveData for changes
        infoViewModel.responseLiveData.observe(viewLifecycleOwner, Observer { response ->
            // Parse the response and create fragments
            val dataMap = parseResponse(response)
            createInfoFragments(dataMap)
        })

        infoViewModel.errorLiveData.observe(viewLifecycleOwner, Observer { error ->
            // Handle error
            binding.fragmentContainer.removeAllViews()
            val errorFragment = InfoItemFragment.newInstance("Error", error)
            addFragment(errorFragment)
        })

        // Start fetching data from home server (connection by VPN)
        infoViewModel.fetchData("http://192.168.1.66:3000/")

        return binding.root
    }

    private fun cleanResponse(response: String): String {
        // Remove unwanted characters like '[', ']', and '_'
        var cleanedResponse = response.replace("[", "")
            .replace("]", "")
            .replace("_", "")

        // Ensure proper separation of key-value pairs
        cleanedResponse = cleanedResponse.replace("\n", "")
            .replace("\t", "")
            .replace("\r", "")

        return cleanedResponse
    }

    private fun parseResponse(response: String): Map<String, String> {
        val dataMap = mutableMapOf<String, String>()

        try {
            // Parse the JSON string into a JSONObject
            val jsonObject = JSONObject(response)

            // Iterate through the keys and values
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.getString(key)
                dataMap[key] = value
            }
        } catch (e: Exception) {
            // Handle JSON parsing errors
            Log.e("InfoFragment", "Error parsing JSON: ${e.message}")
        }

        return dataMap
    }
    fun addSpaceBeforeCapitals(input: String): String {
        return input.replace(Regex("(?<!^)([A-Z])"), " \$1")
    }
    private fun createInfoFragments(dataMap: Map<String, String>) {
        // Clear existing fragments
        binding.fragmentContainer.removeAllViews()

        // Create and add a new fragment for each key-value pair
        for ((label, value) in dataMap) {
            if (label=="status") continue
            val newLabel = addSpaceBeforeCapitals(label)
            val fragment = InfoItemFragment.newInstance((newLabel.replaceFirstChar(Char::titlecase)), value)
            addFragment(fragment)
        }
    }

    private fun addFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = childFragmentManager
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()
        transaction.add(binding.fragmentContainer.id, fragment)
        transaction.commit()
    }
}
package com.example.mythomash.ui.info

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mythomash.databinding.FragmentInfoBinding

class InfoViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InfoViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class InfoFragment : Fragment() {

    private lateinit var infoViewModel: InfoViewModel
    private lateinit var binding: FragmentInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment using ViewBinding
        binding = FragmentInfoBinding.inflate(inflater, container, false)

        // Initialize views through binding
        val textView: TextView = binding.responseInfo

        // Initialize ViewModel using ViewModelFactory with context
        infoViewModel = ViewModelProvider(this, InfoViewModelFactory(requireContext())).get(InfoViewModel::class.java)

        // Observe LiveData for changes
        infoViewModel.responseLiveData.observe(viewLifecycleOwner, Observer { response ->
            textView.text = response // Update the TextView with the response
        })

        infoViewModel.errorLiveData.observe(viewLifecycleOwner, Observer { error ->
            textView.text = error // Update the TextView with the error message
        })

        // Start fetching data (example IP address or endpoint)
        infoViewModel.fetchData("http://192.168.1.66:3000/")

        return binding.root
    }
}

package com.example.mythomash.ui.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mythomash.databinding.FragmentInfoBinding

class InfoItemFragment : Fragment() {

    private lateinit var binding: FragmentInfoBinding

    companion object {
        private const val ARG_LABEL = "label"
        private const val ARG_VALUE = "value"

        fun newInstance(label: String, value: String): InfoItemFragment {
            val fragment = InfoItemFragment()
            val args = Bundle()
            args.putString(ARG_LABEL, label)
            args.putString(ARG_VALUE, value)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val label = arguments?.getString(ARG_LABEL) ?: ""
        val value = arguments?.getString(ARG_VALUE) ?: ""
        binding.infoLabel.text = label
        binding.infoValue.text = value
    }
}
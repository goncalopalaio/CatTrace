package com.gplio.cattrace.application

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gplio.cattrace.CatTrace
import com.gplio.cattrace.application.databinding.FragmentFirstBinding
import com.gplio.cattrace.trace

private const val TAG = "FirstFragment"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = CatTrace.trace(name = "onCreateView", category = TAG) {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) =
        CatTrace.trace(name = "onViewCreated", category = TAG) {
            super.onViewCreated(view, savedInstanceState)

            binding.buttonFirst.setOnClickListener {
                findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            }

            binding.buttonTest4.setOnClickListener {
                CatTrace.sendThreadMetadata()
            }
        }

    override fun onDestroyView() = CatTrace.trace(name = "onDestroyView", category = TAG) {
        super.onDestroyView()
        _binding = null
    }
}
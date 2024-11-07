package com.example.jughead.ui.gesturemapping

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.jughead.databinding.FragmentGestureMappingBinding
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jughead.data.GestureMapping

class GestureMappingFragment : Fragment() {

    private var _binding: FragmentGestureMappingBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GestureMappingViewModel
    private lateinit var adapter: GestureMappingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentGestureMappingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(GestureMappingViewModel::class.java)

        setupRecyclerView()

        binding.addNewGestureButton.setOnClickListener {
            // Handle adding a new custom gesture
            Toast.makeText(requireContext(), "Add new gesture", Toast.LENGTH_SHORT).show()
            // TODO: Implement custom gesture creation
        }

        return root
    }

    private fun setupRecyclerView() {
        adapter = GestureMappingAdapter { gestureMapping ->
            // Handle the assign command button click
            showCommandSelectionDialog(gestureMapping)
        }

        binding.gestureRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.gestureRecyclerView.adapter = adapter

        // Observe the gesture mappings from the ViewModel
        viewModel.gestureMappings.observe(viewLifecycleOwner) { mappings ->
            adapter.submitList(mappings)
        }
    }

    private fun showCommandSelectionDialog(gestureMapping: GestureMapping) {
        val dialog = CommandSelectionDialogFragment(gestureMapping.gestureName) { selectedCommand ->
            // Update the mapping in the database
            viewModel.insertMapping(gestureMapping.gestureName, selectedCommand)
            Toast.makeText(requireContext(), "$selectedCommand assigned to ${gestureMapping.gestureName}", Toast.LENGTH_SHORT).show()
        }
        dialog.show(parentFragmentManager, "CommandSelectionDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

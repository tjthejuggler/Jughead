package com.example.jughead.ui.gesturemapping

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.jughead.databinding.FragmentGestureMappingBinding
import android.widget.Toast
import com.example.jughead.R

class GestureMappingFragment : Fragment() {

    private var _binding: FragmentGestureMappingBinding? = null
    private val binding get() = _binding!!

    // Example gesture list (this would normally come from a ViewModel or database)
    private val gestureList = listOf("Tap/Stomp", "Knee Lift", "Leg Rotation")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentGestureMappingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inflate gesture list dynamically
        val gestureListLayout = binding.gestureListLayout
        gestureList.forEach { gestureName ->
            val gestureItemView = layoutInflater.inflate(
                R.layout.item_gesture, gestureListLayout, false
            )

            val gestureNameTextView = gestureItemView.findViewById<TextView>(R.id.gesture_name)
            val assignCommandButton = gestureItemView.findViewById<Button>(R.id.assign_command_button)

            gestureNameTextView.text = gestureName
            assignCommandButton.setOnClickListener {
                val dialog = CommandSelectionDialogFragment(gestureName) { selectedCommand ->
                    // Handle the selected command assignment
                    Toast.makeText(requireContext(), "$selectedCommand assigned to $gestureName", Toast.LENGTH_SHORT).show()
                    // TODO: Save the mapping in your data storage
                }
                dialog.show(parentFragmentManager, "CommandSelectionDialog")
            }


            gestureListLayout.addView(gestureItemView)
        }

        binding.addNewGestureButton.setOnClickListener {
            // Handle adding a new custom gesture
            Toast.makeText(requireContext(), "Add new gesture", Toast.LENGTH_SHORT).show()
            // TODO: Implement custom gesture creation
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.jughead.ui.balls

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.jughead.R
import com.example.jughead.databinding.FragmentBallsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Button
import android.widget.GridLayout

class BallsFragment : Fragment() {
    private val predefinedColors = listOf(
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA,
        Color.WHITE,
        Color.rgb(255, 165, 0), // Orange
        Color.rgb(128, 0, 128), // Purple
        Color.rgb(255, 192, 203), // Pink
        Color.rgb(0, 255, 0), // Lime
        Color.rgb(0, 128, 128) // Teal
    )
    private var _binding: FragmentBallsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BallsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[BallsViewModel::class.java]
        _binding = FragmentBallsBinding.inflate(inflater, container, false)
        
        setupIpAddressListeners()
        setupColorButtons()
        observeViewModelState()
        
        return binding.root
    }

    private fun observeViewModelState() {
        viewModel.ballColors.observe(viewLifecycleOwner) { colors ->
            colors.forEach { (ballNumber, color) ->
                updateBallColor(ballNumber, color)
            }
        }

        viewModel.ballConnectionStates.observe(viewLifecycleOwner) { states ->
            states.forEach { (ballNumber, isConnected) ->
                val indicator = when (ballNumber) {
                    1 -> binding.ball1ColorIndicator
                    2 -> binding.ball2ColorIndicator
                    3 -> binding.ball3ColorIndicator
                    4 -> binding.ball4ColorIndicator
                    else -> null
                }
                indicator?.let {
                    updateColorIndicator(it, isConnected, viewModel.getBallColor(ballNumber))
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Error")
                    .setMessage(it)
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        viewModel.clearError()
                    }
                    .show()
            }
        }
    }

    private fun setupIpAddressListeners() {
        val ipAddressFields = listOf(
            binding.ball1IpAddress,
            binding.ball2IpAddress,
            binding.ball3IpAddress,
            binding.ball4IpAddress
        )

        val colorIndicators = listOf(
            binding.ball1ColorIndicator,
            binding.ball2ColorIndicator,
            binding.ball3ColorIndicator,
            binding.ball4ColorIndicator
        )

        ipAddressFields.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val ipAddress = s.toString()
                    viewModel.updateBallIpAddress(index + 1, ipAddress)
                    updateColorIndicator(colorIndicators[index], ipAddress.isNotEmpty())
                }
            })
        }

        // Set initial indicator states
        colorIndicators.forEachIndexed { index, indicator ->
            val ipAddress = ipAddressFields[index].text.toString()
            updateColorIndicator(indicator, ipAddress.isNotEmpty())
        }
    }

    private fun setupColorButtons() {
        val colorButtons = listOf(
            binding.ball1ColorButton,
            binding.ball2ColorButton,
            binding.ball3ColorButton,
            binding.ball4ColorButton
        )

        colorButtons.forEach { button ->
            button.apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.circle_shape)
                elevation = resources.getDimension(R.dimen.color_button_elevation)
                stateListAnimator = null // Disable default button animation
            }
        }

        colorButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                showColorPickerDialog(index + 1)
            }
        }
    }

    private fun updateBallColor(ballNumber: Int, color: Int) {
        val button = when (ballNumber) {
            1 -> binding.ball1ColorButton
            2 -> binding.ball2ColorButton
            3 -> binding.ball3ColorButton
            4 -> binding.ball4ColorButton
            else -> null
        }
        button?.background?.setColorFilter(color, PorterDuff.Mode.SRC_IN)

        val indicator = when (ballNumber) {
            1 -> binding.ball1ColorIndicator
            2 -> binding.ball2ColorIndicator
            3 -> binding.ball3ColorIndicator
            4 -> binding.ball4ColorIndicator
            else -> null
        }
        indicator?.let { updateColorIndicator(it, true, color) }
    }

    private fun updateColorIndicator(indicator: ImageView, isConnected: Boolean, color: Int = Color.WHITE) {
        if (isConnected) {
            indicator.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.circle_shape))
            indicator.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } else {
            indicator.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.disconnected_circle))
            indicator.clearColorFilter()
        }
    }

    private fun showColorPickerDialog(ballNumber: Int) {
        context?.let { ctx ->
            // Create a grid layout for the color buttons
            val gridLayout = GridLayout(ctx).apply {
                columnCount = 4
                useDefaultMargins = true
                setPadding(32, 32, 32, 32)
            }

            // Create color buttons
            predefinedColors.forEachIndexed { index, color ->
                val button = Button(ctx).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = resources.getDimensionPixelSize(R.dimen.color_button_size)
                        height = resources.getDimensionPixelSize(R.dimen.color_button_size)
                        setMargins(8, 8, 8, 8)
                    }
                    background = ContextCompat.getDrawable(ctx, R.drawable.circle_shape)?.apply {
                        setColorFilter(color, PorterDuff.Mode.SRC_IN)
                    }
                    elevation = resources.getDimension(R.dimen.color_button_elevation)
                    stateListAnimator = null // Disable default button animation
                    setOnClickListener {
                        viewModel.updateBallColor(ballNumber, color)
                        updateBallColor(ballNumber, color)
                        (parent.parent as? android.app.Dialog)?.dismiss()
                    }
                }
                gridLayout.addView(button)
            }

            // Show dialog with grid of colors
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Choose color for Ball $ballNumber")
                .setView(gridLayout)
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

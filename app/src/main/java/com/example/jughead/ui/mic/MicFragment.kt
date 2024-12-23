package com.example.jughead.ui.mic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.jughead.databinding.FragmentMicBinding
import kotlinx.coroutines.*

class MicFragment : Fragment(), RecognitionListener {
    private var _binding: FragmentMicBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MicViewModel
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    
    // Word matching configuration
    private val minWordSimilarity = 0.8 // Minimum similarity threshold for word matching

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required", Toast.LENGTH_SHORT).show()
            viewModel.toggleListening()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[MicViewModel::class.java]

        setupSpeechRecognizer()
        setupUI()
        observeViewModel()
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(context, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer.setRecognitionListener(this)

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500L)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireContext().packageName)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    private fun setupUI() {
        binding.toggleMicButton.setOnClickListener {
            viewModel.toggleListening()
        }

        binding.submitWordButton.setOnClickListener {
            val word = binding.targetWordInput.text.toString().trim()
            if (word.isNotEmpty()) {
                if (word.length >= 3) {
                    viewModel.setTargetWord(word)
                    binding.currentWordLabel.text = "Currently listening for: $word"
                    binding.currentWordLabel.setTextColor(Color.BLUE)
                    Toast.makeText(context, "Target word set to: $word", Toast.LENGTH_SHORT).show()
                    binding.targetWordInput.setText("") // Clear input after setting
                } else {
                    Toast.makeText(context, "Word must be at least 3 characters long", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please enter a word", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isListening.observe(viewLifecycleOwner) { isListening ->
            if (isListening) {
                checkPermissionAndStartRecording()
            } else {
                stopRecording()
            }
            binding.toggleMicButton.text = if (isListening) "Stop Listening" else "Start Listening"
            binding.toggleMicButton.setTextColor(if (isListening) Color.RED else Color.WHITE)
        }

        viewModel.status.observe(viewLifecycleOwner) { status ->
            binding.statusText.text = status
        }

        viewModel.targetWord.observe(viewLifecycleOwner) { word ->
            if (!word.isNullOrEmpty()) {
                binding.currentWordLabel.text = "Currently listening for: $word"
                binding.currentWordLabel.setTextColor(Color.CYAN)
            } else {
                binding.currentWordLabel.text = "Currently listening for: none"
                binding.currentWordLabel.setTextColor(Color.GRAY)
            }
        }
    }

    private fun checkPermissionAndStartRecording() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecording() {
        try {
            speechRecognizer.startListening(recognizerIntent)
            binding.transcriptionText.text = "Waiting for speech..."
            binding.transcriptionText.setTextColor(Color.CYAN)
            viewModel.updateStatus("Listening...")
        } catch (e: Exception) {
            viewModel.updateStatus("Error: ${e.message}")
            viewModel.toggleListening()
        }
    }

    private fun stopRecording() {
        try {
            speechRecognizer.stopListening()
        } catch (e: Exception) {
            // Handle any errors during stop
        }
        binding.transcriptionText.text = "Microphone off"
        binding.transcriptionText.setTextColor(Color.RED)
        binding.statusText.setTextColor(Color.GRAY)
        viewModel.updateStatus("Microphone is off")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecording()
        scope.cancel()
        speechRecognizer.destroy()
        _binding = null
    }

    // RecognitionListener implementation
    override fun onReadyForSpeech(params: Bundle?) {
        binding.transcriptionText.text = "ready..."
    }

    override fun onBeginningOfSpeech() {
        binding.transcriptionText.setTextColor(Color.BLUE)
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Not used
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Not used
    }

    private fun restartSpeechRecognition() {
        scope.launch(Dispatchers.Main) {
            delay(300) // Small delay before restarting
            if (viewModel.isListening.value == true) {
                try {
                    speechRecognizer.startListening(recognizerIntent)
                } catch (e: Exception) {
                    // Handle any errors during restart
                    viewModel.updateStatus("Speech recognition error: ${e.message}")
                }
            }
        }
    }

    override fun onEndOfSpeech() {
        restartSpeechRecognition()
    }

    override fun onError(error: Int) {
        // Only update text if we're still listening
        if (viewModel.isListening.value == true) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "Listening for speech..."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening..."
                else -> "Listening... (Error #$error)"
            }
            binding.transcriptionText.text = errorMessage
            binding.transcriptionText.setTextColor(Color.BLUE)
            
            if (error != SpeechRecognizer.ERROR_CLIENT) {
                restartSpeechRecognition()
            }
        }
    }

    // Calculate word similarity using Levenshtein distance
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = minOf(
                    dp[i-1][j] + 1,
                    dp[i][j-1] + 1,
                    dp[i-1][j-1] + if (s1[i-1] == s2[j-1]) 0 else 1
                )
            }
        }
        
        val maxLength = maxOf(s1.length, s2.length)
        return 1 - (dp[s1.length][s2.length].toDouble() / maxLength)
    }

    override fun onResults(results: Bundle?) {
        if (viewModel.isListening.value == true) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                if (text.isNotBlank()) {
                    binding.transcriptionText.text = text
                    binding.transcriptionText.setTextColor(Color.WHITE)
                    
                    viewModel.targetWord.value?.let { targetWord ->
                        // Split transcribed text into words and check each one
                        val words = text.lowercase().split(Regex("\\s+"))
                        var bestMatch = 0.0
                        var bestMatchingWord = ""
                        
                        for (word in words) {
                            val similarity = calculateSimilarity(word, targetWord.lowercase())
                            if (similarity > bestMatch) {
                                bestMatch = similarity
                                bestMatchingWord = word
                            }
                        }
                        
                        if (bestMatch >= minWordSimilarity) {
                            binding.transcriptionText.setTextColor(Color.GREEN)
                            viewModel.updateStatus("Target word match found: $bestMatchingWord (${(bestMatch * 100).toInt()}% match)")
                        }
                    }
                }
            }
            restartSpeechRecognition()
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // Only update text if we're still listening
        if (viewModel.isListening.value == true) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                if (text.isNotBlank()) {
                    binding.transcriptionText.text = text
                    binding.transcriptionText.setTextColor(Color.CYAN)
                }
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        // Not used
    }
}

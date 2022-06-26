package com.example.hydrohomie

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.content.ContextCompat
import com.example.hydrohomie.databinding.ActivityMainBinding
import com.example.hydrohomie.notifications.DataChangedReceiver
import com.example.hydrohomie.ui.MainUiState
import com.example.hydrohomie.ui.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The main screen with the progress of the daily goal.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels<MainViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as HydroHomieApp
                return MainViewModel(
                    settingsStore = app.settingsStore,
                    historyStore = app.historyStore,
                    reminders = app.reminderCoordinator,
                ) as T
            }
        }
    }

    /** Whether the glass size was already filled in. */
    private var hasSetGlassSize = false

    /**
     * Reloads the screen when the settings were changed or a glass was drunk by tapping a
     * notification.
     */
    /** Whether the receiver for changed data is registered. */
    private var isReceiverRegistered = false

    private val dataChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.reload()
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.reload()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.drinkButton.setOnClickListener { viewModel.drink() }
        binding.undoButton.setOnClickListener { viewModel.undo() }
        binding.openSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.glassSizeInput.doAfterTextChanged {
            viewModel.onGlassSizeChanged(it?.toString()?.toIntOrNull() ?: 0)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }

        askForNotificationPermission()
        showExactAlarmHint()
        openSettingsOnFirstStart()

        ContextCompat.registerReceiver(
            this,
            dataChangedReceiver,
            IntentFilter(DataChangedReceiver.ACTION_DATA_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        isReceiverRegistered = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(dataChangedReceiver)
            isReceiverRegistered = false
        }
    }

    /**
     * Opens the settings the first time, as long as nothing was configured yet.
     */
    private fun openSettingsOnFirstStart() {
        val app = application as HydroHomieApp
        if (!app.settingsStore.exists()) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    /**
     * Shows a hint if Android does not allow reminders at an exact time.
     */
    private fun showExactAlarmHint() {
        val app = application as HydroHomieApp
        val allowed = app.canScheduleExactAlarms()
        binding.exactAlarmHint.visibility = if (allowed) View.GONE else View.VISIBLE
        binding.exactAlarmHint.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(
                    Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        android.net.Uri.parse("package:$packageName"),
                    ),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The history can be changed by the notification or in the settings.
        viewModel.reload()
    }

    private fun render(state: MainUiState) {
        val progress = state.progress
        binding.progressAmount.text =
            getString(R.string.progress_amount, progress.drunkMl, progress.goalMl)
        binding.progressPercent.text = getString(R.string.progress_percent, progress.percent)
        binding.progressView.setProgress(progress.fraction)

        binding.goalStatus.text = if (progress.isGoalReached) {
            getString(R.string.goal_reached)
        } else {
            getString(R.string.remaining_glasses, progress.remainingGlasses(state.settings.glassSizeMl))
        }

        binding.nextReminder.text = when {
            !state.settings.notificationsEnabled -> getString(R.string.reminders_off)
            state.nextReminderMillis != null ->
                getString(R.string.next_reminder, formatTime(state.nextReminderMillis))
            else -> getString(R.string.no_reminder)
        }

        binding.undoButton.isEnabled = state.lastDrinkMl != null
        binding.undoButton.alpha = if (state.lastDrinkMl != null) 1f else 0.4f

        // The glass size is only filled in once, so that the user can clear the field.
        if (!hasSetGlassSize) {
            hasSetGlassSize = true
            binding.glassSizeInput.setText(state.glassSizeMl.toString())
        }
    }

    private fun formatTime(timestampMillis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampMillis))

    /** Asks for the permission to show notifications (Android 13 and newer). */
    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(permission)
        }
    }
}

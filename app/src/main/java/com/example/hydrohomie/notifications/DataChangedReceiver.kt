package com.example.hydrohomie.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Announces that the settings or the history have changed.
 *
 * The broadcast is only sent inside the app, it is used to update an open screen, for
 * example after a glass was drunk by tapping a notification.
 */
class DataChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // The screens reload the data themselves, the broadcast is only the signal.
    }

    companion object {
        /** The action of the broadcast that the data has changed. */
        const val ACTION_DATA_CHANGED = "com.example.hydrohomie.action.DATA_CHANGED"

        /** Sends the broadcast that the data has changed. */
        fun notifyChanged(context: Context) {
            context.sendBroadcast(
                Intent(ACTION_DATA_CHANGED).setPackage(context.packageName),
            )
        }
    }
}

package com.vkas.safekey.event
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

class SnackbarMessage: SingleLiveEvent<Int?>()  {
    fun observe(owner: LifecycleOwner?, observer: SnackbarObserver) {
        super.observe(owner!!, Observer { t ->
            if (t == null) {
                return@Observer
            }
            observer.onNewMessage(t)
        })
    }

    interface SnackbarObserver {
        /**
         * Called when there is a new message to be shown.
         * @param snackbarMessageResourceId The new message, non-null.
         */
        fun onNewMessage(@StringRes snackbarMessageResourceId: Int)
    }
}
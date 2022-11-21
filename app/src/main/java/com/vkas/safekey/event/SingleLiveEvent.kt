package com.vkas.safekey.event
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.vkas.safekey.utils.KLog
import java.util.concurrent.atomic.AtomicBoolean
open class SingleLiveEvent<T> : MutableLiveData<T?>() {
    private val mPending = AtomicBoolean(false)

    //  @MainThread
    //    public void observe(LifecycleOwner owner, final Observer<T> observer) {
    @MainThread
    fun mObserve(owner: LifecycleOwner?, observer: Observer<T?>) {
        if (hasActiveObservers()) {
            KLog.w(TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        // Observe the internal MutableLiveData
        super.observe(owner!!, Observer { t ->
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        })
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call() {
        value = null
    }

    companion object {
        private const val TAG = "SingleLiveEventSK"
    }
}
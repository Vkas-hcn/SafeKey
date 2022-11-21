package com.vkas.safekey.base
import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.vkas.safekey.event.StateLiveData

open class BaseViewModel(application: Application) : BaseViewModelMVVM(application) {
    var stateLiveData: StateLiveData<Any> = StateLiveData()
    fun getStateLiveData(): MutableLiveData<Any> {
        return stateLiveData
    }

}
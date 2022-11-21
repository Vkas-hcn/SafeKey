package com.vkas.safekey.ui.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceDataStore
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.safekey.base.BaseModelMVVM
import com.vkas.safekey.base.BaseViewModel
import com.vkas.safekey.bean.SkServiceBean
import com.vkas.safekey.utils.LocalDataUtils

class MainViewModel(application: Application) : BaseViewModel(application) {
    //初始化服务器数据
    val liveInitializeServerData: MutableLiveData<SkServiceBean> by lazy {
        MutableLiveData<SkServiceBean>()
    }
    //更新服务器数据(未连接)
    val liveNoUpdateServerData: MutableLiveData<SkServiceBean> by lazy {
        MutableLiveData<SkServiceBean>()
    }
    //更新服务器数据(已连接)
    val liveUpdateServerData: MutableLiveData<SkServiceBean> by lazy {
        MutableLiveData<SkServiceBean>()
    }

    //当前服务器
    var currentServerData: SkServiceBean = SkServiceBean()
    //断开后选中服务器
    var afterDisconnectionServerData: SkServiceBean = SkServiceBean()

    fun initializeServerData() {
        val bestData = LocalDataUtils.getFastIp()
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                ProfileManager.updateProfile(setSkServerData(it, bestData))
            } else {
                val profile = Profile()
                ProfileManager.createProfile(setSkServerData(profile, bestData))
            }
        }
        DataStore.profileId = 1L
        currentServerData = bestData
        liveInitializeServerData.postValue(bestData)
    }

    fun updateSkServer(skServiceBean: SkServiceBean,isConnect:Boolean) {
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                setSkServerData(it, skServiceBean)
                ProfileManager.updateProfile(it)
            } else {
                ProfileManager.createProfile(Profile())
            }
        }
        DataStore.profileId = 1L
        if(isConnect){
            afterDisconnectionServerData = skServiceBean
            liveUpdateServerData.postValue(skServiceBean)
        }else{
            currentServerData = skServiceBean
            liveNoUpdateServerData.postValue(skServiceBean)
        }
    }

    /**
     * 设置服务器数据
     */
    private fun setSkServerData(profile: Profile, bestData: SkServiceBean): Profile {
        profile.name = bestData.sk_country + "-" + bestData.sk_city
        profile.host = bestData.sk_ip.toString()
        profile.remotePort = bestData.sk_port!!
        profile.password = bestData.sk_pwd!!
        profile.method = bestData.sk_method!!
        return profile
    }
}
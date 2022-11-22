package com.vkas.safekey.ui.main

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceDataStore
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.safekey.application.App
import com.vkas.safekey.application.App.Companion.mmkv
import com.vkas.safekey.base.BaseModelMVVM
import com.vkas.safekey.base.BaseViewModel
import com.vkas.safekey.bean.SkServiceBean
import com.vkas.safekey.ui.result.ResultActivity
import com.vkas.safekey.utils.KLog
import com.vkas.safekey.utils.LocalDataUtils
import com.vkas.safekey.utils.MmkvUtils
import com.xuexiang.xutil.app.ActivityUtils.startActivityForResult
import com.xuexiang.xutil.net.JsonUtil

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
    //跳转结果页
    val liveJumpResultsPage: MutableLiveData<Bundle> by lazy {
        MutableLiveData<Bundle>()
    }
    fun initializeServerData() {
        val bestData = LocalDataUtils.getFastIp()
        KLog.e("TAG","ProfileManager.getProfile(DataStore.profileId)==${JsonUtil.toJson(ProfileManager.getProfile(DataStore.profileId))}")
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
        val serviceData = JsonUtil.toJson(currentServerData)
        MmkvUtils.set("currentServerData",serviceData)
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
            val serviceData = JsonUtil.toJson(currentServerData)
            MmkvUtils.set("currentServerData",serviceData)
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
    /**
     * 跳转连接结果页
     */
    fun jumpConnectionResultsPage(isConnection: Boolean){
        val bundle = Bundle()
        val serviceData = mmkv.decodeString("currentServerData", "").toString()
        bundle.putBoolean(com.vkas.safekey.key.Key.CONNECTION_SK_STATUS, isConnection)
        bundle.putString(com.vkas.safekey.key.Key.SERVER_SK_INFORMATION, serviceData)
        liveJumpResultsPage.postValue(bundle)
    }
}
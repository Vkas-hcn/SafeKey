package com.vkas.safekey.ui.list

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.vkas.safekey.application.App.Companion.mmkv
import com.vkas.safekey.base.BaseViewModel
import com.vkas.safekey.bean.SkServiceBean
import com.vkas.safekey.key.Key
import com.vkas.safekey.utils.LocalDataUtils
import com.vkas.safekey.utils.LocalDataUtils.getFastIp
import com.xuexiang.xui.utils.Utils.isNullOrEmpty
import com.xuexiang.xutil.net.JsonUtil

class SelectViewModel(application: Application) : BaseViewModel(application) {
    private lateinit var skServiceBean :SkServiceBean
    private lateinit var skServiceBeanList :MutableList<SkServiceBean>

    // 服务器列表数据
    val liveServerListData: MutableLiveData<MutableList<SkServiceBean>> by lazy {
        MutableLiveData<MutableList<SkServiceBean>>()
    }

    /**
     * 获取服务器列表
     */
    fun getServerListData(){
        skServiceBeanList = ArrayList()
        skServiceBean = SkServiceBean()
        skServiceBeanList = if (isNullOrEmpty(mmkv.decodeString(Key.PROFILE_SK_DATA))) {
            LocalDataUtils.getLocalServerData()
        } else {
            JsonUtil.fromJson(
                mmkv.decodeString(Key.PROFILE_SK_DATA),
                object : TypeToken<MutableList<SkServiceBean>?>() {}.type
            )
        }
        skServiceBeanList.add(0, getFastIp())
        liveServerListData.postValue(skServiceBeanList)
    }
}
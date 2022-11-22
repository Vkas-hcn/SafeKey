package com.vkas.safekey.ad


class LoadAds {
    companion object {
        fun getInstance() = InstanceHelper.adsLoad
    }

    object InstanceHelper {
        val adsLoad = LoadAds()
    }
    /**
     * 广告加载前判断
     */
     fun advertisementLoading(){

    }
}
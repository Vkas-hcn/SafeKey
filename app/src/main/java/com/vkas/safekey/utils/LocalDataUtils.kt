package com.vkas.safekey.utils

import com.google.gson.reflect.TypeToken
import com.vkas.safekey.R
import com.vkas.safekey.application.App.Companion.mmkv
import com.vkas.safekey.bean.SkServiceBean
import com.vkas.safekey.key.Key
import com.xuexiang.xui.utils.ResUtils.getString
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResourceUtils

object LocalDataUtils {
    /**
     * 获取Fast ip
     */
    fun getFastIp(): SkServiceBean {
        val skServiceBeans: MutableList<SkServiceBean> =
            if (Utils.isNullOrEmpty(mmkv.decodeString(Key.PROFILE_SK_DATA_FAST))) {
                getLocalServerData()
            } else {
                JsonUtil.fromJson(
                    mmkv.decodeString(Key.PROFILE_SK_DATA_FAST),
                    object : TypeToken<MutableList<SkServiceBean>?>() {}.type
                )
            }
        val intersectionList = findFastAndOrdinaryIntersection(skServiceBeans)
        intersectionList.shuffled().take(1).forEach {
            it.sk_bestServer = true
            it.sk_country = getString(R.string.faster_server)
            return it
        }
        intersectionList[0].sk_bestServer = true
        return intersectionList[0]
    }

    /**
     * 获取本地服务器数据
     */
    fun getLocalServerData(): MutableList<SkServiceBean> {
        return JsonUtil.fromJson(
            ResourceUtils.readStringFromAssert("skService.json"),
            object : TypeToken<MutableList<SkServiceBean>?>() {}.type
        )
    }

    /**
     * 获取本地Fast服务器数据
     */
    private fun getLocalFastServerData(): MutableList<String> {
        return JsonUtil.fromJson(
            ResourceUtils.readStringFromAssert("skServiceFast.json"),
            object : TypeToken<MutableList<String>?>() {}.type
        )
    }

    /**
     * 找出fast与普通交集
     */
    private fun findFastAndOrdinaryIntersection(skServiceBeans: MutableList<SkServiceBean>): MutableList<SkServiceBean> {
        val intersectionList: MutableList<SkServiceBean> = ArrayList()
        getLocalFastServerData().forEach { fast ->
            skServiceBeans.forEach { skServiceBean ->
                if (fast == skServiceBean.sk_ip) {
                    intersectionList.add(skServiceBean)
                }
            }
        }
        return intersectionList
    }

    /**
     * 通过国家获取国旗
     */
    fun getFlagThroughCountry(sk_country: String): Int {
        when (sk_country) {
            "Faster server" -> {
                return R.drawable.ic_fast
            }
            "Japan" -> {
                return R.drawable.ic_japan
            }
            "United Kingdom" -> {
                return R.drawable.ic_unitedkingdom
            }
            "United States" -> {
                return R.drawable.ic_usa
            }
            "Australia" -> {
                return R.drawable.ic_australia
            }
            "Belgium" -> {
                return R.drawable.ic_belgium
            }
            "Brazil" -> {
                return R.drawable.ic_brazil
            }
            "Canada" -> {
                return R.drawable.ic_canada
            }
            "France" -> {
                return R.drawable.ic_france
            }
            "Germany" -> {
                return R.drawable.ic_germany
            }
            "India" -> {
                return R.drawable.ic_india
            }
            "Ireland" -> {
                return R.drawable.ic_ireland
            }
            "Italy" -> {
                return R.drawable.ic_italy
            }
            "Koreasouth" -> {
                return R.drawable.ic_koreasouth
            }
            "Netherlands" -> {
                return R.drawable.ic_netherlands
            }
            "Newzealand" -> {
                return R.drawable.ic_newzealand
            }
            "Norway" -> {
                return R.drawable.ic_norway
            }
            "Russianfederation" -> {
                return R.drawable.ic_russianfederation
            }
            "Singapore" -> {
                return R.drawable.ic_singapore
            }
            "Sweden" -> {
                return R.drawable.ic_sweden
            }
            "Switzerland" -> {
                return R.drawable.ic_switzerland
            }
        }

        return R.drawable.ic_fast
    }
}
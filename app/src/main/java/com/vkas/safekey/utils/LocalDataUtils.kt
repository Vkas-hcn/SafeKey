package com.vkas.safekey.utils

import com.google.gson.reflect.TypeToken
import com.vkas.safekey.R
import com.vkas.safekey.application.App.Companion.mmkv
import com.vkas.safekey.bean.SkAdBean
import com.vkas.safekey.bean.SkAdDetails
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
        val skServiceBeans: MutableList<SkServiceBean> = getLocalServerData()
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
        return if (Utils.isNullOrEmpty(mmkv.decodeString(Key.PROFILE_SK_DATA))) {
            JsonUtil.fromJson(
                ResourceUtils.readStringFromAssert("skService.json"),
                object : TypeToken<MutableList<SkServiceBean>?>() {}.type
            )
        } else {
            JsonUtil.fromJson(
                mmkv.decodeString(Key.PROFILE_SK_DATA),
                object : TypeToken<MutableList<SkServiceBean>?>() {}.type
            )
        }
    }

    /**
     * 获取本地Fast服务器数据
     */
    private fun getLocalFastServerData(): MutableList<String> {
        return if (Utils.isNullOrEmpty(mmkv.decodeString(Key.PROFILE_SK_DATA_FAST))) {
            JsonUtil.fromJson(
                ResourceUtils.readStringFromAssert("skServiceFast.json"),
                object : TypeToken<MutableList<String>?>() {}.type
            )
        } else {
            JsonUtil.fromJson(
                mmkv.decodeString(Key.PROFILE_SK_DATA_FAST),
                object : TypeToken<MutableList<String>?>() {}.type
            )
        }
    }

    /**
     * 获取广告服务器数据
     */
    fun getAdServerData(): SkAdBean {
        val serviceData: SkAdBean =
            if (Utils.isNullOrEmpty(mmkv.decodeString(Key.ADVERTISING_SK_DATA))) {
                JsonUtil.fromJson(
                    ResourceUtils.readStringFromAssert("skAd.json"),
                    object : TypeToken<SkAdBean?>() {}.type
                )
            } else {
                JsonUtil.fromJson(
                    mmkv.decodeString(Key.ADVERTISING_SK_DATA),
                    object : TypeToken<SkAdBean?>() {}.type
                )
            }
        return adSorting(serviceData)
    }
    /**
     *
     */

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

    /**
     * 广告排序
     */
    private fun adSorting(skAdBean: SkAdBean): SkAdBean {
        val adBean = SkAdBean()
        val sk_open = skAdBean.sk_open.sortedWith(compareByDescending { it.sk_weight })
        val sk_home = skAdBean.sk_home.sortedWith(compareByDescending { it.sk_weight })
        val sk_result = skAdBean.sk_result.sortedWith(compareByDescending { it.sk_weight })
        val sk_connect = skAdBean.sk_connect.sortedWith(compareByDescending { it.sk_weight })
        val sk_back = skAdBean.sk_back.sortedWith(compareByDescending { it.sk_weight })
        adBean.sk_open = sk_open as MutableList<SkAdDetails>
        adBean.sk_home = sk_home as MutableList<SkAdDetails>
        adBean.sk_result = sk_result as MutableList<SkAdDetails>
        adBean.sk_connect = sk_connect as MutableList<SkAdDetails>
        adBean.sk_back = sk_back as MutableList<SkAdDetails>
        adBean.sk_show_num = skAdBean.sk_show_num
        adBean.sk_click_num = skAdBean.sk_click_num
        return adBean
    }

    /**
     * 取出排序后的广告ID
     */
    fun takeSortedAdID(index: Int, skAdDetails: MutableList<SkAdDetails>): String {
        return if (index < skAdDetails.size) {
            skAdDetails[index].sk_id
        } else {
            skAdDetails[0].sk_id
        }
    }
    /**
     * 广告上线
     */
    fun advertisingOnline():Boolean{
        val clicksCount = mmkv.decodeInt(Key.CLICKS_SK_COUNT, 0)
        val showCount = mmkv.decodeInt(Key.SHOW_SK_COUNT, 0)
        KLog.e("TAG","clicksCount=${clicksCount}, showCount=${showCount}")
        KLog.e("TAG","sk_click_num=${getAdServerData().sk_click_num}, getAdServerData().sk_show_num=${getAdServerData().sk_show_num}")
        if (clicksCount >= getAdServerData().sk_click_num || showCount >= getAdServerData().sk_show_num) {
            return true
        }
        return false
    }
    /**
     * 记录广告展示次数
     */
    fun recordNumberOfAdDisplays(){
        var showCount = mmkv.decodeInt(Key.SHOW_SK_COUNT, 0)
        showCount++
        MmkvUtils.set(Key.SHOW_SK_COUNT, showCount)
    }
    /**
     * 记录广告点击次数
     */
    fun recordNumberOfAdClick(){
        var clicksCount = mmkv.decodeInt(Key.CLICKS_SK_COUNT, 0)
        clicksCount++
        MmkvUtils.set(Key.CLICKS_SK_COUNT, clicksCount)
    }
}
package com.vkas.safekey.ui.result

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.safekey.BR
import com.vkas.safekey.R
import com.vkas.safekey.ad.LoadAds
import com.vkas.safekey.application.App
import com.vkas.safekey.application.App.Companion.mmkv
import com.vkas.safekey.application.App.Companion.skAdLog
import com.vkas.safekey.base.BaseActivity
import com.vkas.safekey.bean.SkServiceBean
import com.vkas.safekey.databinding.ActivityResultBinding
import com.vkas.safekey.key.Key
import com.vkas.safekey.utils.KLog
import com.vkas.safekey.utils.LocalDataUtils
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.*

class ResultActivity : BaseActivity<ActivityResultBinding, ResultViewModel>() {
    private var isConnection: Boolean = false
    private var jobResult: Job? = null

    //当前服务器
    private lateinit var currentServerBean: SkServiceBean
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_result
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        isConnection = bundle?.getBoolean(Key.CONNECTION_SK_STATUS) == true
        currentServerBean = JsonUtil.fromJson(
            bundle?.getString(Key.SERVER_SK_INFORMATION),
            object : TypeToken<SkServiceBean?>() {}.type
        )
    }

    override fun initToolbar() {
        super.initToolbar()
        with(resources.displayMetrics) {
            density = heightPixels / 780.0F
            densityDpi = (160 * density).toInt()
        }
        App.nativeAdRefresh = false
        displayTimer()
        binding.resultTitle.imgBack.visibility = View.VISIBLE
        binding.resultTitle.tvTitle.visibility = View.GONE
        binding.resultTitle.ivRight.visibility = View.GONE
        binding.resultTitle.imgBack.setOnClickListener { finish() }
    }

    override fun initData() {
        super.initData()
        if (isConnection) {
            binding.imgConnect.setImageResource(R.drawable.ic_connect)
            binding.txtConnectionStatus.text = getString(R.string.connected_s)
            binding.txtConnectionStatus.setTextColor(getColor(R.color.txt_connect_state))
            binding.txtTimer.setTextColor(getColor(R.color.txt_connect_time))
        } else {
            binding.imgConnect.setImageResource(R.drawable.ic_diss)
            binding.txtConnectionStatus.text = getString(R.string.disconnect)
            binding.txtConnectionStatus.setTextColor(getColor(R.color.txt_disconnect_state))
            binding.txtTimer.setTextColor(getColor(R.color.txt_disconnect_time))
            binding.txtTimer.text = mmkv.decodeString(Key.LAST_TIME, "").toString()
        }
        binding.imgFlag.setImageResource(LocalDataUtils.getFlagThroughCountry(currentServerBean.sk_country.toString()))
        binding.imgServiceFlag.setImageResource(
            LocalDataUtils.getFlagThroughCountry(
                currentServerBean.sk_country.toString()
            )
        )
        binding.txtCountryName.text = currentServerBean.sk_country
        initNativeAds()
    }
    private fun initNativeAds() {
        jobResult= GlobalScope.launch {
            withTimeout(10000L) {
                while (isActive) {
                    LoadAds.getInstanceResult().setDisplayResultNativeAd(this@ResultActivity,binding)
                    if (LoadAds.getInstanceResult().whetherToShow) {
                        jobResult?.cancel()
                        jobResult =null
                    }
                    delay(1000L)
                }
            }
        }
    }

    /**
     * 显示计时器
     */
    private fun displayTimer() {
        LiveEventBus
            .get(Key.TIMER_SK_DATA, String::class.java)
            .observeForever {
                if (isConnection) {
                    binding.txtTimer.text = it
                } else {
                    binding.txtTimer.text = mmkv.decodeString(Key.LAST_TIME, "").toString()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        LoadAds.getInstanceResult().whetherToShow =false
        if(App.nativeAdRefresh){
            if(LoadAds.getInstanceResult().appAdData !=null){
                LoadAds.getInstanceResult().setDisplayResultNativeAd(this,binding)
            }else{
                LoadAds.getInstanceResult().advertisementLoading(this)
                initNativeAds()
            }
        }
    }
}
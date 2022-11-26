package com.vkas.safekey.ui.start

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.safekey.BR
import com.vkas.safekey.BuildConfig
import com.vkas.safekey.R
import com.vkas.safekey.ad.LoadAds
import com.vkas.safekey.application.App
import com.vkas.safekey.application.App.Companion.mmkv
import com.vkas.safekey.application.App.Companion.skAdLog
import com.vkas.safekey.base.BaseActivity
import com.vkas.safekey.base.BaseViewModel
import com.vkas.safekey.databinding.ActivityStartupBinding
import com.vkas.safekey.key.Key
import com.vkas.safekey.ui.main.MainActivity
import com.vkas.safekey.utils.KLog
import com.vkas.safekey.utils.LocalDataUtils
import com.vkas.safekey.utils.LocalDataUtils.advertisingOnline
import com.vkas.safekey.utils.MmkvUtils
import com.xuexiang.xui.utils.StatusBarUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.*

class StartupActivity : BaseActivity<ActivityStartupBinding, StartupViewModel>(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    companion object {
        var whetherReturnCurrentPage: Boolean = false
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_startup
    }

    override fun initParam() {
        super.initParam()
        whetherReturnCurrentPage = intent.getBooleanExtra(Key.RETURN_SK_CURRENT_PAGE, false)
    }

    override fun initData() {
        super.initData()
        binding.pbStart.setProgressViewUpdateListener(this)
        binding.pbStart.startProgressAnimation()
        liveEventBusReceive()
        getFirebaseData()
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    private fun liveEventBusReceive() {
        LiveEventBus
            .get(Key.OPEN_CLOSE_JUMP, Boolean::class.java)
            .observeForever {
                KLog.d(skAdLog, "关闭开屏内容-接收==${this.lifecycle.currentState}")
                if (this.lifecycle.currentState == Lifecycle.State.STARTED) {
                    jumpPage()
                }
            }
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }

    /**
     * 获取Firebase数据
     */
    private fun getFirebaseData() {
        if (BuildConfig.DEBUG) {
            preloadedAdvertisement()
            return
        } else {
            val auth = Firebase.remoteConfig
            auth.fetchAndActivate().addOnSuccessListener {
                MmkvUtils.set(Key.PROFILE_SK_DATA, auth.getString("sk_service_data"))
                MmkvUtils.set(Key.PROFILE_SK_DATA_FAST, auth.getString("sk_service_data_fast"))
                MmkvUtils.set(Key.AROUND_SK_FLOW_DATA, auth.getString("sk_around_flow_Data"))
                MmkvUtils.set(Key.ADVERTISING_SK_DATA, auth.getString("sk_ad_Data"))
            }.addOnCompleteListener {
                preloadedAdvertisement()
            }
        }
    }

    /**
     * 预加载广告
     */
    private fun preloadedAdvertisement() {
        App.isAppOpenSameDay()
        if (advertisingOnline()) {
            KLog.d(skAdLog, "广告达到上线")
            lifecycleScope.launch {
                delay(2000L)
                jumpPage()
            }
        } else {
            loadAdvertisement()
        }
    }

    /**
     * 加载广告
     */
    private fun loadAdvertisement() {
        //开屏
        LoadAds.getInstanceOpen().advertisementLoading(this)
        //home
        LoadAds.getInstanceHome().advertisementLoading(this)
        //result
        LoadAds.getInstanceResult().advertisementLoading(this)
        //connect
        LoadAds.getInstanceConnect().advertisementLoading(this)
        rotationDisplayOpeningAd()
    }

    /**
     * 轮训展示开屏广告
     */
    private fun rotationDisplayOpeningAd() {
        lifecycleScope.launch {
            try {
                withTimeout(10000L) {
                    delay(1000L)
                    while (isActive) {
                        val showState =
                            if (LocalDataUtils.getAdServerData().sk_open[LoadAds.getInstanceOpen().adIndex].sk_type == "screen") {
                                KLog.d("TAG","screen==========>")
                                LoadAds.getInstanceOpen()
                                    .displayStartInsertAdvertisement(this@StartupActivity)
                            } else {
                                KLog.d("TAG","open==========>")

                                LoadAds.getInstanceOpen()
                                    .displayOpenAdvertisement(this@StartupActivity)
                            }
                        if (showState) {
                            lifecycleScope.cancel()
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.e("TimeoutCancellationException I'm sleeping $e")
                jumpPage()
            }
        }
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        KLog.e("TAG", "jumpPage=$whetherReturnCurrentPage")
        // 不是后台切回来的跳转，是后台切回来的直接finish启动页
        if (!whetherReturnCurrentPage) {
            val intent = Intent(this@StartupActivity, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.pbStart.stopProgressAnimation()
        binding.pbStart.setProgressViewUpdateListener(null)
    }

    override fun onHorizontalProgressStart(view: View?) {

    }

    override fun onHorizontalProgressUpdate(view: View?, progress: Float) {
    }

    override fun onHorizontalProgressFinished(view: View?) {
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        //屏蔽禁用返回键的功能
        return keyCode == KeyEvent.KEYCODE_BACK
    }
}
package com.vkas.safekey.ui.main

import android.os.Bundle
import android.os.RemoteException
import android.view.View
import androidx.core.view.isVisible
import androidx.preference.PreferenceDataStore
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.vkas.safekey.BR
import com.vkas.safekey.R
import com.vkas.safekey.base.BaseActivity
import com.vkas.safekey.bean.SkServiceBean
import com.vkas.safekey.databinding.ActivityMainBinding
import com.vkas.safekey.ui.list.SelectActivity
import com.vkas.safekey.utils.KLog
import com.vkas.safekey.utils.LocalDataUtils.getFlagThroughCountry
import com.xuexiang.xutil.net.NetworkUtils.isNetworkAvailable
import com.xuexiang.xutil.tip.ToastUtils
import android.view.animation.LinearInterpolator

import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.safekey.ui.result.ResultActivity
import com.vkas.safekey.utils.SkTimerThread
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.PackageManagerCompat.LOG_TAG
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.vkas.safekey.ad.LoadAds
import com.vkas.safekey.application.App
import com.vkas.safekey.application.App.Companion.mmkv
import com.vkas.safekey.application.App.Companion.skAdLog
import com.vkas.safekey.ui.web.PrivacyPolicyActivity
import com.vkas.safekey.utils.LocalDataUtils
import com.vkas.safekey.utils.MmkvUtils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.net.JsonUtil.toJson
import kotlinx.coroutines.*


class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(),
    ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    var state = BaseService.State.Idle
    private val connection = ShadowsocksConnection(true)
    private lateinit var animation: Animation
    private var jobNativeAds: Job? = null
    private var jobStart: Job? = null

    //动画是否进行
    private var whetherAnimation = false

    // 是否返回刷新服务器
    var whetherRefreshServer = false

    // 跳转结果页
    private var liveJumpResultsPage = MutableLiveData<Bundle>()
    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null

    }

    override fun initParam() {
        super.initParam()
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_main
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initToolbar() {
        super.initToolbar()
        with(resources.displayMetrics) {
            density = heightPixels / 780.0F
            densityDpi = (160 * density).toInt()
        }
        binding.presenter = Presenter()
        App.nativeAdRefresh = false
        setImgAnimation()
        binding.mainTitle.ivRight.setOnClickListener {
            if (!whetherAnimation) {
                binding.sidebarShows = binding.sidebarShows != true
            }
        }
        binding.linMain.setOnClickListener {
            if (binding.sidebarShows == true) {
                binding.sidebarShows = false
            }
        }
        sidebarClickEvent()
    }

    @DelicateCoroutinesApi
    override fun initData() {
        super.initData()
        liveEventBusReceive()
        changeState(BaseService.State.Idle, animate = false)
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
        KLog.e("TAG", "MainActivity-----state---->${SkTimerThread.getInstance().isStopThread}")
        if (SkTimerThread.getInstance().isStopThread) {
            viewModel.initializeServerData()
        } else {
            val serviceData = mmkv.decodeString("currentServerData", "").toString()
            val currentServerData: SkServiceBean = JsonUtil.fromJson(
                serviceData,
                object : TypeToken<SkServiceBean?>() {}.type
            )
            setFastInformation(currentServerData)
        }
        initNativeAds()
    }

    @DelicateCoroutinesApi
    private fun initNativeAds() {
        jobNativeAds = GlobalScope.launch {
                withTimeout(10000L) {
                    while (isActive) {
                        LoadAds.getInstanceHome().setDisplayHomeNativeAd(this@MainActivity,binding)
                        if (LoadAds.getInstanceHome().whetherToShow) {
                            jobNativeAds?.cancel()
                            jobNativeAds =null
                        }
                        delay(1000L)
                    }
                }
        }
    }


    private fun liveEventBusReceive() {
        LiveEventBus
            .get(com.vkas.safekey.key.Key.TIMER_SK_DATA, String::class.java)
            .observeForever {
                binding.txtTimer.text = it
            }
        //更新服务器(未连接)
        LiveEventBus
            .get(com.vkas.safekey.key.Key.NOT_CONNECTED_RETURN, SkServiceBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, false)
            }
        //更新服务器(已连接)
        LiveEventBus
            .get(com.vkas.safekey.key.Key.CONNECTED_RETURN, SkServiceBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, true)
            }
        //插屏关闭后跳转
        LiveEventBus
            .get(com.vkas.safekey.key.Key.PLUG_SK_ADVERTISEMENT_SHOW, Boolean::class.java)
            .observeForever {
                LoadAds.getInstanceConnect().advertisementLoading(this)
                connectOrDisconnect(it)
            }
    }

    override fun initViewObservable() {
        super.initViewObservable()
        setServiceData()
        // 跳转结果页
        jumpResultsPageData()
    }

    private fun jumpResultsPageData() {
        liveJumpResultsPage.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(300L)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    startActivityForResult(ResultActivity::class.java, 0x11, it)
                }
            }
        })
        viewModel.liveJumpResultsPage.observe(this, {
            liveJumpResultsPage.postValue(it)
        })
    }

    private fun setServiceData() {
        viewModel.liveInitializeServerData.observe(this, {
            setFastInformation(it)
        })
        viewModel.liveUpdateServerData.observe(this, {
            whetherRefreshServer = true
            connect.launch(null)

        })
        viewModel.liveNoUpdateServerData.observe(this, {
            whetherRefreshServer = false
            setFastInformation(it)
            connect.launch(null)
        })
    }

    inner class Presenter {
        fun linkService() {
            if (binding.sidebarShows != true && !whetherAnimation) {
                connect.launch(null)
            }
        }

        fun clickService() {
            if (binding.sidebarShows != true && !whetherAnimation) {
                jumpToServerList()
            }
        }

        fun clickMainMenu() {
        }
    }

    /**
     * 跳转服务器列表
     */
    fun jumpToServerList() {
        val bundle = Bundle()
        if (state.name == "Connected") {
            bundle.putBoolean(com.vkas.safekey.key.Key.WHETHER_SK_CONNECTED, true)
        } else {
            bundle.putBoolean(com.vkas.safekey.key.Key.WHETHER_SK_CONNECTED, false)
        }
        val serviceData = mmkv.decodeString("currentServerData", "").toString()
        bundle.putString(com.vkas.safekey.key.Key.CURRENT_SK_SERVICE, serviceData)
        startActivity(SelectActivity::class.java, bundle)
    }

    /**
     * 侧边栏点击事件
     */
    private fun sidebarClickEvent() {
        binding.incMenu.tvContactUs.setOnClickListener {
            val uri = Uri.parse("mailto:${com.vkas.safekey.key.Key.MAILBOX_SK_ADDRESS}")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            startActivity(Intent.createChooser(intent, "Please select mail application"))
        }
        binding.incMenu.tvPrivacyPolicy.setOnClickListener {
            startActivity(PrivacyPolicyActivity::class.java)
        }
        binding.incMenu.tvShare.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(
                Intent.EXTRA_TEXT,
                com.vkas.safekey.key.Key.SHARE_SK_ADDRESS + this.packageName
            )
            intent.type = "text/plain"
            startActivity(intent)
        }
    }

    private val connect = registerForActivityResult(StartService()) {
        if (it) {
            ToastUtils.toast(R.string.no_permissions)
        } else {
            if (isNetworkAvailable()) {
                startVpn()
            } else {
                ToastUtils.toast("The current device has no network")
            }
        }
    }

    /**
     * 启动VPN
     */
    private fun startVpn() {
        KLog.d("TAG","jobStart.isActive=${jobStart?.isActive}")
        jobStart?.cancel()
        jobStart =null
        whetherAnimation = true
        binding.imgSwitch.setImageResource(R.mipmap.ic_rotate)
        binding.imgSwitch.startAnimation(animation)
        if (state.canStop) {
            binding.txtConnectionStatus.text = getString(R.string.disconnecting)
        } else {
            binding.txtConnectionStatus.text = getString(R.string.connecting)
        }
        App.isAppOpenSameDay()
        if (LocalDataUtils.advertisingOnline()) {
            KLog.d(skAdLog, "广告达到上线")
            connectOrDisconnect(false)
            return
        }
        LoadAds.getInstanceConnect().advertisementLoading(this)
        LoadAds.getInstanceResult().advertisementLoading(this)

        jobStart= GlobalScope.launch {
            try {
                withTimeout(10000L) {
                    delay(1000L)
                    while (isActive) {
                        val showState =
                            LoadAds.getInstanceConnect().displayConnectAdvertisement(this@MainActivity)
                        if (showState) {
                            jobStart?.cancel()
                            jobStart =null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.d(skAdLog,"connect---插屏超时")
                if(jobStart!=null){
                    connectOrDisconnect(false)
                }
            }
        }
    }

    /**
     * 连接或断开
     * 是否后台关闭（true：后台关闭；false：手动关闭）
     */
    private fun connectOrDisconnect(isBackgroundClosed:Boolean){
        if (state.canStop) {
            if(!isBackgroundClosed){
                viewModel.jumpConnectionResultsPage(false)
            }
            Core.stopService()

        } else {
            if(!isBackgroundClosed){
                viewModel.jumpConnectionResultsPage(true)
            }
            Core.startService()
        }
    }
    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) =
        changeState(state)

    override fun onServiceConnected(service: IShadowsocksService) = run {
        changeState(
            try {
                BaseService.State.values()[service.state]
            } catch (_: RemoteException) {
                BaseService.State.Idle
            }
        )
    }

    private fun changeState(
        state: BaseService.State,
        animate: Boolean = true
    ) {
        this.state = state
        connectionStatusJudgment(state.name)
        stateListener?.invoke(state)
    }

    /**
     * 连接状态判断
     */
    private fun connectionStatusJudgment(state: String) {
        KLog.e("TAG", "connectionStatusJudgment=${state}")
        when (state) {
            "Connected" -> {
                // 连接成功
                connectionServerSuccessful()
                binding.txtConnectionStatus.text = getString(R.string.connected)
            }
            "Stopped" -> {
                disconnectServerSuccessful()
                binding.txtConnectionStatus.text = getString(R.string.connect)
                binding.txtTimer.text = getString(R.string._00_00_00)
            }
            else -> {
                binding.txtConnectionStatus.text = getString(R.string.connect)
            }
        }

    }

    /**
     * 连接服务器成功
     */
    private fun connectionServerSuccessful() {
        whetherAnimation = false
        SkTimerThread.getInstance().startTiming()
        binding.imgSwitch.clearAnimation()
        binding.imgConnectBg.setImageResource(R.drawable.ic_home_connect)
        binding.imgSwitch.setImageResource(R.drawable.ic_check)
        binding.imgProgressbar.setImageResource(R.drawable.ic_process_connect)
    }

    /**
     * 断开服务器
     */
    private fun disconnectServerSuccessful() {
        whetherAnimation = false
        SkTimerThread.getInstance().endTiming()
        binding.imgSwitch.clearAnimation()
        binding.imgConnectBg.setImageResource(R.drawable.ic_home_not_connect)
        binding.imgSwitch.setImageResource(R.drawable.ic_switch)
        binding.imgProgressbar.setImageResource(R.drawable.ic_process_not_connect)
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.serviceMode -> {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    /**
     * 设置fast信息
     */
    private fun setFastInformation(skServiceBean: SkServiceBean) {
        if (skServiceBean.sk_bestServer == true) {
            binding.txtCountry.text = com.vkas.safekey.key.Key.FASTER_SK_SERVER
            binding.imgCountry.setImageResource(getFlagThroughCountry(com.vkas.safekey.key.Key.FASTER_SK_SERVER))
        } else {
            binding.txtCountry.text =
                String.format(skServiceBean.sk_country + "-" + skServiceBean.sk_city)
            binding.imgCountry.setImageResource(getFlagThroughCountry(skServiceBean.sk_country.toString()))
        }
    }

    private fun setImgAnimation() {
        animation =
            AnimationUtils.loadAnimation(binding.imgSwitch.context, R.anim.img_rotare)
        val lin = LinearInterpolator() //设置动画匀速运动
        animation.interpolator = lin
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }

    override fun onResume() {
        super.onResume()
        LoadAds.getInstanceHome().whetherToShow =false
        if(App.nativeAdRefresh){
            if(LoadAds.getInstanceHome().appAdData !=null){
                KLog.d(skAdLog,"onResume------>1")
                LoadAds.getInstanceHome().setDisplayHomeNativeAd(this,binding)
            }else{
                KLog.d(skAdLog,"onResume------>2")
                LoadAds.getInstanceHome().advertisementLoading(this)
                initNativeAds()
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        connection.bandwidthTimeout = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
        jobStart?.cancel()
        jobStart =null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x11 && whetherRefreshServer) {
            setFastInformation(viewModel.afterDisconnectionServerData)
            val serviceData = toJson(viewModel.afterDisconnectionServerData)
            MmkvUtils.set("currentServerData", serviceData)
            viewModel.currentServerData = viewModel.afterDisconnectionServerData
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return true
    }
}
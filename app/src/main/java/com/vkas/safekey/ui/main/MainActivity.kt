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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import com.vkas.safekey.ui.web.PrivacyPolicyActivity
import com.xuexiang.xutil.net.JSONUtils
import com.xuexiang.xutil.net.JsonUtil.toJson


class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(),
    ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    var state = BaseService.State.Idle
    private val connection = ShadowsocksConnection(true)
    private lateinit var animation: Animation

    //动画是否进行
    private var whetherAnimation = false

    // 是否返回刷新服务器
    var whetherRefreshServer = false

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
        binding.presenter = Presenter()
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

    override fun initData() {
        super.initData()
        liveEventBusReceive()
        changeState(BaseService.State.Idle, animate = false)
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
        viewModel.initializeServerData()
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
    }

    override fun initViewObservable() {
        super.initViewObservable()
        setServiceData()
    }

    private fun setServiceData() {
        viewModel.liveInitializeServerData.observe(this, {
            setFastInformation(it)
        })
        viewModel.liveUpdateServerData.observe(this, {
            whetherRefreshServer = true
//            setFastInformation(it)
            startVpn()
        })
        viewModel.liveNoUpdateServerData.observe(this, {
            whetherRefreshServer = false
            setFastInformation(it)
            startVpn()
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
        val currentServerString = toJson(viewModel.currentServerData)
        bundle.putString(com.vkas.safekey.key.Key.CURRENT_SK_SERVICE, currentServerString)
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
        whetherAnimation = true
        binding.imgSwitch.setImageResource(R.mipmap.ic_rotate)
        binding.imgSwitch.startAnimation(animation)
        binding.txtConnectionStatus.text = getString(R.string.connecting)
        lifecycleScope.launch {
            delay(1000)
            if (state.canStop) {
                Core.stopService()
                connectionComplete(false)
            } else {
                Core.startService()
                connectionComplete(true)
            }
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
            "Connecting" -> {
                binding.txtConnectionStatus.text = getString(R.string.connecting)
            }
            "Connected" -> {
                // 连接成功
                connectionServerSuccessful()
                binding.txtConnectionStatus.text = getString(R.string.connected)
//                connectionComplete(true)
            }
            "Stopped" -> {
                disconnectServerSuccessful()
                binding.txtConnectionStatus.text = getString(R.string.connect)
//                connectionComplete(false)
            }
            else -> {
                binding.txtConnectionStatus.text = getString(R.string.configuring)
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

    /**
     * 连接完成
     */
    private fun connectionComplete(isConnection: Boolean) {
        val bundle = Bundle()
        val currentServerString = toJson(viewModel.currentServerData)
        bundle.putBoolean(com.vkas.safekey.key.Key.CONNECTION_SK_STATUS, isConnection)
        bundle.putString(com.vkas.safekey.key.Key.SERVER_SK_INFORMATION, currentServerString)
        startActivityForResult(ResultActivity::class.java, 0x11, bundle)
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x11 && whetherRefreshServer) {
            setFastInformation(viewModel.afterDisconnectionServerData)
            viewModel.currentServerData = viewModel.afterDisconnectionServerData
        }
    }
}
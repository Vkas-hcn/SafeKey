package com.vkas.safekey.ui.start

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.vkas.safekey.BR
import com.vkas.safekey.BuildConfig
import com.vkas.safekey.R
import com.vkas.safekey.application.App
import com.vkas.safekey.application.App.Companion.mmkv
import com.vkas.safekey.base.BaseActivity
import com.vkas.safekey.base.BaseViewModel
import com.vkas.safekey.databinding.ActivityStartupBinding
import com.vkas.safekey.key.Key
import com.vkas.safekey.ui.main.MainActivity
import com.vkas.safekey.utils.KLog
import com.vkas.safekey.utils.MmkvUtils
import com.xuexiang.xui.utils.StatusBarUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartupActivity : BaseActivity<ActivityStartupBinding, StartupViewModel>(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    private val LOG_TAG = "ad-log"

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
        getFirebaseData()
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    /**
     * 获取Firebase数据
     */
    private fun getFirebaseData() {
        if (BuildConfig.DEBUG) {
            loadAdvertisement()
            return
        } else {
            val auth = Firebase.remoteConfig
            auth.fetchAndActivate().addOnSuccessListener {
                MmkvUtils.set(Key.PROFILE_SK_DATA, auth.getString("sk_service_data"))
                MmkvUtils.set(Key.PROFILE_SK_DATA_FAST, auth.getString("sk_service_data_fast"))
                MmkvUtils.set(Key.AROUND_SK_FLOW_DATA, auth.getString("sk_around_flow_Data"))
                MmkvUtils.set(Key.ADVERTISING_SK_DATA, auth.getString("sk_ad_Data"))
            }.addOnCompleteListener {
                loadAdvertisement()
            }
        }
    }

    /**
     * 加载广告
     */
    private fun loadAdvertisement() {
        App.isAppOpenSameDay()
        lifecycleScope.launch {
            delay(2000L)
            jumpPage()
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
}
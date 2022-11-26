package com.vkas.safekey.application

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.ActivityUtils.finishActivity
import com.github.shadowsocks.Core
import com.google.android.gms.ads.MobileAds
import com.tencent.mmkv.MMKV
import com.blankj.utilcode.util.ProcessUtils
import com.google.android.gms.ads.AdActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.safekey.BuildConfig
import com.vkas.safekey.ui.main.MainActivity
import com.vkas.safekey.base.AppManagerMVVM
import com.vkas.safekey.key.Key
import com.vkas.safekey.ui.start.StartupActivity
import com.vkas.safekey.utils.*
import com.xuexiang.xui.XUI
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.app.ActivityLifecycleHelper
import kotlinx.coroutines.*

class App : Application(), androidx.work.Configuration.Provider by Core, LifecycleObserver {
    private var job_sk: Job? = null

    private var flag = 0
    private var ad_activity_sk: Activity? = null
    private var top_activity_sk: Activity? = null
    companion object {
        val skAdLog = "SkAdLog"
        // app当前是否在后台
        var isBackData = false

        // 是否进入后台（三秒后）
        var whetherBackground = false
        // 原生广告刷新
        var nativeAdRefresh = false
        val mmkv by lazy {
            //启用mmkv的多进程功能
            MMKV.mmkvWithID("SafeKey", MMKV.MULTI_PROCESS_MODE)
        }

        //当日日期
        var adDate = ""

        /**
         * 判断是否是当天打开
         */
        fun isAppOpenSameDay() {
            adDate = mmkv.decodeString(Key.CURRENT_SK_DATE, "").toString()
            if (adDate == "") {
                MmkvUtils.set(Key.CURRENT_SK_DATE, CalendarUtils.formatDateNow())
            } else {
                if (CalendarUtils.dateAfterDate(adDate, CalendarUtils.formatDateNow())) {
                    MmkvUtils.set(Key.CURRENT_SK_DATE, CalendarUtils.formatDateNow())
                    MmkvUtils.set(Key.CLICKS_SK_COUNT, 0)
                    MmkvUtils.set(Key.SHOW_SK_COUNT, 0)
                }
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
//        initCrash()
        setActivityLifecycle(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        if (ProcessUtils.isMainProcess()) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            Firebase.initialize(this)
            FirebaseApp.initializeApp(this)
            XUI.init(this) //初始化UI框架
            XUtil.init(this)
            LiveEventBus
                .config()
                .lifecycleObserverAlwaysActive(true)
            //是否开启打印日志
            KLog.init(BuildConfig.DEBUG)
        }
        Core.init(this, MainActivity::class)
        SkTimerThread.getInstance().sendTimerInformation()
        isAppOpenSameDay()
    }
    @DelicateCoroutinesApi
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        job_sk?.cancel()
        job_sk = null
        KLog.v("Lifecycle", "onMoveToForeground=$whetherBackground")
        //从后台切过来，跳转启动页
        if (whetherBackground&& !isBackData) {
            jumpGuidePage()
        }
    }
    @DelicateCoroutinesApi
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStopState(){
        nativeAdRefresh =true
        KLog.v("Lifecycle", "onSTOPJumpPage=$whetherBackground")
        job_sk = GlobalScope.launch {
            whetherBackground = false
            delay(3000L)
            whetherBackground = true
            ad_activity_sk?.finish()
            ActivityUtils.getActivity(StartupActivity::class.java)?.finish()
        }
    }
    /**
     * 跳转引导页
     */
    private fun jumpGuidePage(){
        whetherBackground = false
        val intent = Intent(top_activity_sk, StartupActivity::class.java)
        intent.putExtra(Key.RETURN_SK_CURRENT_PAGE, true)
        top_activity_sk?.startActivity(intent)
    }
    /**
     * 当主工程没有继承BaseApplication时，可以使用setApplication方法初始化BaseApplication
     *
     * @param application
     */
    @Synchronized
    fun setActivityLifecycle(application: Application) {
        //注册监听每个activity的生命周期,便于堆栈式管理
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                AppManagerMVVM.get().addActivity(activity)
                if (activity !is AdActivity) {
                    top_activity_sk = activity
                } else {
                    ad_activity_sk = activity
                }

                KLog.v("Lifecycle", "onActivityCreated" + activity.javaClass.name)
            }

            override fun onActivityStarted(activity: Activity) {
                KLog.v("Lifecycle", "onActivityStarted" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_sk = activity
                } else {
                    ad_activity_sk = activity
                }
                flag++
                isBackData = false
            }

            override fun onActivityResumed(activity: Activity) {
                KLog.v("Lifecycle", "onActivityResumed=" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_sk = activity
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity is AdActivity) {
                    ad_activity_sk = activity
                } else {
                    top_activity_sk = activity
                }
                KLog.v("Lifecycle", "onActivityPaused=" + activity.javaClass.name)
            }

            override fun onActivityStopped(activity: Activity) {
                flag--
                if (flag == 0) {
                    isBackData = true
                }
                KLog.v("Lifecycle", "onActivityStopped=" + activity.javaClass.name)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                KLog.v("Lifecycle", "onActivitySaveInstanceState=" + activity.javaClass.name)

            }

            override fun onActivityDestroyed(activity: Activity) {
                AppManagerMVVM.get().removeActivity(activity)
                KLog.v("Lifecycle", "onActivityDestroyed" + activity.javaClass.name)
                ad_activity_sk = null
                top_activity_sk = null
            }
        })
    }
}
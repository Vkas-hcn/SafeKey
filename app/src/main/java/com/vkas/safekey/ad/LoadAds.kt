package com.vkas.safekey.ad

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.safekey.R
import com.vkas.safekey.application.App
import com.vkas.safekey.application.App.Companion.skAdLog
import com.vkas.safekey.databinding.ActivityMainBinding
import com.vkas.safekey.databinding.ActivityResultBinding
import com.vkas.safekey.key.Key
import com.vkas.safekey.utils.KLog
import com.vkas.safekey.utils.LocalDataUtils
import com.vkas.safekey.utils.LocalDataUtils.getAdServerData
import com.vkas.safekey.utils.LocalDataUtils.recordNumberOfAdClick
import com.vkas.safekey.utils.LocalDataUtils.recordNumberOfAdDisplays
import com.vkas.safekey.utils.LocalDataUtils.takeSortedAdID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


class LoadAds private constructor(val pos: String) {
    companion object {
        fun getInstanceOpen() = InstanceHelper.openLoad
        fun getInstanceHome() = InstanceHelper.homeLoad
        fun getInstanceResult() = InstanceHelper.resultLoad
        fun getInstanceConnect() = InstanceHelper.connectLoad
        fun getInstanceBack() = InstanceHelper.backLoad

    }

    object InstanceHelper {
        val openLoad = LoadAds("open")
        val homeLoad = LoadAds("home")
        val resultLoad = LoadAds("result")
        val connectLoad = LoadAds("connect")
        val backLoad = LoadAds("back")

    }

    var appAdData: Any? = null

    // 是否正在加载中
    private var isLoading = false

    //加载时间
    private var loadTime: Long = Date().time

    // 是否展示
    var whetherToShow = false

    // openIndex
    var adIndex = 0

    /**
     * 广告加载前判断
     */
    fun advertisementLoading(context: Context) {
        App.isAppOpenSameDay()
        if (LocalDataUtils.advertisingOnline()) {
            KLog.d(skAdLog, "广告达到上线")
            return
        }
        KLog.d(skAdLog, "isLoading=${isLoading}")

        if (isLoading) {
            KLog.d(skAdLog, "广告加载中，不能再次加载")
            return
        }
        if (appAdData != null || !whetherAdExceedsOneHour(loadTime)) {
            return
        }

        isLoading = true
        when (pos) {
            "open" -> {
                loadStartupPageAdvertisement(context)
            }
            "home" -> {
                loadHomeAdvertisement(context)
            }
            "result" -> {
                loadResultAdvertisement(context)
            }
            "connect" -> {
                loadConnectAdvertisement(context)
            }
            "back" -> {
                loadBackAdvertisement(context)
            }
        }
    }

    /**
     * 广告是否超过过期（false:过期；true：未过期）
     */
    private fun whetherAdExceedsOneHour(loadTime: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour
    }

    /**
     * 加载启动页广告
     */
    private fun loadStartupPageAdvertisement(context: Context) {
        if (getAdServerData().sk_open[adIndex].sk_type == "screen") {
            loadStartInsertAd(context)
        } else {
            loadOpenAdvertisement(context)
        }
    }

    /**
     * 加载开屏广告
     */
    private fun loadOpenAdvertisement(context: Context) {
        val id = takeSortedAdID(adIndex, getAdServerData().sk_open)
        KLog.d(skAdLog, "开屏广告id=$id;权重=${getAdServerData().sk_open[adIndex].sk_weight}")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            id,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadTime = Date().time
                    isLoading = false
                    appAdData = ad
                    adIndex = 0
                    KLog.d(skAdLog, "开屏广告加载完成")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoading = false
                    appAdData = null
                    if (adIndex < getAdServerData().sk_open.size - 1) {
                        adIndex++
                        loadOpenAdvertisement(context)
                    }
                    KLog.d(skAdLog, "开屏广告加载失败: " + loadAdError.message)
                }
            }
        )
    }


    /**
     * 开屏广告回调
     */
    private fun advertisingOpenCallback() {
        (appAdData as AppOpenAd).fullScreenContentCallback = object : FullScreenContentCallback() {
            //取消全屏内容
            override fun onAdDismissedFullScreenContent() {
                KLog.d(skAdLog, "关闭开屏内容")
                whetherToShow = false
                appAdData = null
                if (!App.whetherBackground) {
                    LiveEventBus.get<Boolean>(Key.OPEN_CLOSE_JUMP)
                        .post(true)
                }
            }

            //全屏内容无法显示时调用
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                whetherToShow = false
                appAdData = null
                KLog.d(skAdLog, "全屏内容无法显示时调用")
            }

            //显示全屏内容时调用
            override fun onAdShowedFullScreenContent() {
                appAdData = null
                whetherToShow = true
                recordNumberOfAdDisplays()
                KLog.d(skAdLog, "open---开屏广告展示")
            }

            override fun onAdClicked() {
                super.onAdClicked()
                KLog.d(skAdLog, "open---点击open广告")
                recordNumberOfAdClick()
            }
        }
    }

    /**
     * 展示Open广告
     */
    fun displayOpenAdvertisement(activity: AppCompatActivity): Boolean {
        if (appAdData == null) {
            KLog.d(skAdLog, "open---开屏广告加载中。。。")
            return false
        }
        if (whetherToShow || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(skAdLog, "open---前一个开屏广告展示中或者生命周期不对")
            return false
        }
        advertisingOpenCallback()
        (appAdData as AppOpenAd).show(activity)
        return true
    }


    /**
     * 加载启动页插屏广告
     */
    private fun loadStartInsertAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdID(adIndex, getAdServerData().sk_connect)
        KLog.d(
            skAdLog,
            "StartInsert插屏广告id=$id;权重=${getAdServerData().sk_connect[adIndex].sk_weight}"
        )

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(skAdLog, "StartInsert---连接插屏加载失败=$it") }
                    isLoading = false
                    appAdData = null
                    if (adIndex < getAdServerData().sk_connect.size - 1) {
                        adIndex++
                        loadStartInsertAd(context)
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTime = Date().time
                    isLoading = false
                    appAdData = interstitialAd
                    adIndex = 0
                    KLog.d(skAdLog, "StartInsert---连接插屏加载完成")
                }
            })
    }

    /**
     * StartInsert插屏广告回调
     */
    private fun startInsertScreenAdCallback() {
        (appAdData as InterstitialAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(skAdLog, "StartInsert插屏广告点击")
                    recordNumberOfAdClick()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(skAdLog, "关闭StartInsert插屏广告${App.isBackData}")
                    KLog.d(skAdLog, "LiveEventBus=${App.isBackData}")
                    if (!App.whetherBackground) {
                        LiveEventBus.get<Boolean>(Key.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
                    appAdData = null
                    whetherToShow = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLog.d(skAdLog, "Ad failed to show fullscreen content.")
                    appAdData = null
                    whetherToShow = false
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    KLog.e("TAG", "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    appAdData = null
                    recordNumberOfAdDisplays()
                    // Called when ad is shown.
                    whetherToShow = true
                    KLog.d(skAdLog, "StartInsert----show")
                }
            }
    }

    /**
     * 展示StartInsert广告
     */
    fun displayStartInsertAdvertisement(activity: AppCompatActivity): Boolean {
        if (appAdData == null) {
            KLog.d(skAdLog, "StartInsert--插屏广告加载中。。。")
            return false
        }
        if (whetherToShow || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(skAdLog, "StartInsert--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        startInsertScreenAdCallback()
        (appAdData as InterstitialAd).show(activity)
        return true
    }


    /**
     * 加载home原生广告
     */
    private fun loadHomeAdvertisement(context: Context) {
        val id = takeSortedAdID(adIndex, getAdServerData().sk_home)
        KLog.d(skAdLog, "home---原生广告id=$id;权重=${getAdServerData().sk_home[adIndex].sk_weight}")

        val homeNativeAds = AdLoader.Builder(
            context.applicationContext,
            id
        )
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        homeNativeAds.withNativeAdOptions(adOptions)
        homeNativeAds.forNativeAd {
            appAdData = it
        }
        homeNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                isLoading = false
                appAdData = null
                KLog.d(skAdLog, "home---加载home原生加载失败: $error")

                if (adIndex < getAdServerData().sk_home.size - 1) {
                    adIndex++
                    loadHomeAdvertisement(context)
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(skAdLog, "home---加载home原生广告成功")
                loadTime = Date().time
                isLoading = false
                adIndex = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(skAdLog, "home---点击home原生广告")
                recordNumberOfAdClick()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 设置展示home原生广告
     */
    fun setDisplayHomeNativeAd(activity: AppCompatActivity, binding: ActivityMainBinding) {
        activity.runOnUiThread {
            appAdData.let {
                KLog.d(skAdLog, "whetherToShow====>${whetherToShow}")
                if (it != null && !whetherToShow) {
                    val activityDestroyed: Boolean = activity.isDestroyed
                    if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        (it as NativeAd).destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater
                        .inflate(R.layout.layout_home_ad, null) as NativeAdView
                    // 对应原生组件
                    setCorrespondingNativeComponent((it as NativeAd), adView)
                    binding.skAdFrame.removeAllViews()
                    binding.skAdFrame.addView(adView)
                    binding.homeAd = true
                    recordNumberOfAdDisplays()
                    whetherToShow = true
                    App.nativeAdRefresh = false
                    appAdData = null
                    KLog.d(skAdLog, "home--原生广告--展示")
                    //重新缓存
                    advertisementLoading(activity)
                }
            }

        }
    }

    private fun setCorrespondingNativeComponent(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let {
            adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }
                ?.setMediaContent(it)
        }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    }

    /**
     * 加载result原生广告
     */
    private fun loadResultAdvertisement(context: Context) {
        val id = takeSortedAdID(adIndex, getAdServerData().sk_result)
        KLog.d(skAdLog, "result---原生广告id=$id;权重=${getAdServerData().sk_result[adIndex].sk_weight}")

        val homeNativeAds = AdLoader.Builder(
            context.applicationContext,
            id
        )
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        homeNativeAds.withNativeAdOptions(adOptions)
        homeNativeAds.forNativeAd {
            appAdData = it
        }
        homeNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                isLoading = false
                appAdData = null
                KLog.d(skAdLog, "result---加载result原生加载失败: $error")

                if (adIndex < getAdServerData().sk_result.size - 1) {
                    adIndex++
                    loadResultAdvertisement(context)
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(skAdLog, "result---加载result原生广告成功")
                loadTime = Date().time
                isLoading = false
                adIndex = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(skAdLog, "result---点击result原生广告")
                recordNumberOfAdClick()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 设置展示home原生广告
     */
    fun setDisplayResultNativeAd(activity: AppCompatActivity, binding: ActivityResultBinding) {
        activity.runOnUiThread {
            appAdData.let {
                if (it != null && !whetherToShow) {
                    val activityDestroyed: Boolean = activity.isDestroyed
                    if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        (it as NativeAd).destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater
                        .inflate(R.layout.layout_result_ad, null) as NativeAdView
                    // 对应原生组件
                    setResultNativeComponent((it as NativeAd), adView)
                    binding.skAdFrame.removeAllViews()
                    binding.skAdFrame.addView(adView)
                    binding.resultAd = true
                    recordNumberOfAdDisplays()
                    whetherToShow = true
                    App.nativeAdRefresh = false
                    appAdData = null
                    KLog.d(skAdLog, "result--原生广告--展示")
                    //重新缓存
                    advertisementLoading(activity)
                }
            }

        }
    }

    private fun setResultNativeComponent(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let {
            adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }
                ?.setMediaContent(it)
        }
        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    }

    /**
     * 加载首页插屏广告
     */
    private fun loadConnectAdvertisement(context: Context) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdID(adIndex, getAdServerData().sk_connect)
        KLog.d(skAdLog, "connect插屏广告id=$id;权重=${getAdServerData().sk_connect[adIndex].sk_weight}")

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(skAdLog, "connect---连接插屏加载失败=$it") }
                    isLoading = false
                    appAdData = null
                    if (adIndex < getAdServerData().sk_connect.size - 1) {
                        adIndex++
                        loadConnectAdvertisement(context)
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTime = Date().time
                    isLoading = false
                    appAdData = interstitialAd
                    adIndex = 0
                    KLog.d(skAdLog, "connect---连接插屏加载完成")
                }
            })
    }

    /**
     * connect插屏广告回调
     */
    private fun connectScreenAdCallback() {
        (appAdData as InterstitialAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(skAdLog, "connect插屏广告点击")
                    recordNumberOfAdClick()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(skAdLog, "关闭connect插屏广告${App.isBackData}")
                    KLog.d(skAdLog, "LiveEventBus=${App.isBackData}")
                    LiveEventBus.get<Boolean>(Key.PLUG_SK_ADVERTISEMENT_SHOW)
                        .post(App.isBackData)
                    appAdData = null
                    whetherToShow = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLog.d(skAdLog, "Ad failed to show fullscreen content.")
                    appAdData = null
                    whetherToShow = false
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    KLog.e("TAG", "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    appAdData = null
                    recordNumberOfAdDisplays()
                    // Called when ad is shown.
                    whetherToShow = true
                    KLog.d(skAdLog, "connect----show")
                }
            }
    }

    /**
     * 展示Connect广告
     */
    fun displayConnectAdvertisement(activity: AppCompatActivity): Boolean {
        if (appAdData == null) {
            KLog.d(skAdLog, "connect--插屏广告加载中。。。")
            return false
        }
        if (whetherToShow || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(skAdLog, "connect--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        connectScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            (appAdData as InterstitialAd).show(activity)
        }
        return true
    }


    /**
     * 加载back插屏广告
     */
    private fun loadBackAdvertisement(context: Context) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdID(adIndex, getAdServerData().sk_back)
        KLog.d(
            skAdLog,
            "back-----back插屏广告id=$id;权重=${getAdServerData().sk_back[adIndex].sk_weight}"
        )

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(skAdLog, "back---连接插屏加载失败=$it") }
                    isLoading = false
                    appAdData = null
                    if (adIndex < getAdServerData().sk_back.size - 1) {
                        adIndex++
                        loadBackAdvertisement(context)
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTime = Date().time
                    isLoading = false
                    appAdData = interstitialAd
                    adIndex = 0
                    KLog.d(skAdLog, "back---连接插屏加载完成")
                }
            })
    }

    /**
     * back插屏广告回调
     */
    private fun backScreenAdCallback() {
        (appAdData as InterstitialAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(skAdLog, "back插屏广告点击")
                    recordNumberOfAdClick()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(skAdLog, "关闭back插屏广告${App.isBackData}")
                    LiveEventBus.get<Boolean>(Key.PLUG_SK_BACK_AD_SHOW)
                        .post(App.isBackData)
                    appAdData = null
                    whetherToShow = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLog.d(skAdLog, "Ad failed to show fullscreen content.")
                    appAdData = null
                    whetherToShow = false
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    KLog.e("TAG", "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    appAdData = null
                    recordNumberOfAdDisplays()
                    // Called when ad is shown.
                    whetherToShow = true
                    KLog.d(skAdLog, "back----show")
                }
            }
    }

    /**
     * 展示back广告
     */
    fun displayBackAdvertisement(activity: AppCompatActivity): Boolean {
        if (appAdData == null) {
            KLog.d(skAdLog, "back--插屏广告加载中。。。")
            return false
        }
        if (whetherToShow || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(skAdLog, "back--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        backScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            (appAdData as InterstitialAd).show(activity)
        }
        return true
    }
}
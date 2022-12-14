package com.vkas.safekey.ui.web

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.vkas.safekey.BR
import com.vkas.safekey.R
import com.vkas.safekey.base.BaseActivity
import com.vkas.safekey.base.BaseViewModel
import com.vkas.safekey.databinding.ActivityWebviewBinding
import com.vkas.safekey.key.Key

class PrivacyPolicyActivity : BaseActivity<ActivityWebviewBinding, BaseViewModel>() {
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_webview
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.webTitle.imgBack.visibility = View.VISIBLE
        binding.webTitle.imgBack.setOnClickListener {
            finish()
        }
        binding.webTitle.tvTitle.visibility = View.GONE
        binding.webTitle.ivRight.visibility = View.GONE
    }

    override fun initData() {
        super.initData()
        binding.ppWeb.loadUrl(Key.PRIVACY_SK_AGREEMENT)
        binding.ppWeb.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            }

            override fun onPageFinished(view: WebView, url: String) {
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed()
            }
        }

        binding.ppWeb.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val hit = view.hitTestResult
                //hit.getExtra()???null??????hit.getType() == 0????????????????????????URL??????????????????????????????????????????
                if (TextUtils.isEmpty(hit.extra) || hit.type == 0) {
                }
                //?????????url???http/https????????????
                return if (request.url.scheme!!.startsWith("http://") || request.url.scheme!!.startsWith(
                        "https://"
                    )
                ) {
                    view.loadUrl(request.url.toString())
                    false
                } else {
                    //?????????url????????????????????????
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString()))
                        this@PrivacyPolicyActivity.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
            }
        }
    }


    //????????????????????????????????????????????????
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.ppWeb.canGoBack()) {
            binding.ppWeb.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        binding.ppWeb.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
        binding.ppWeb.clearHistory()
        (binding.ppWeb.parent as ViewGroup).removeView(binding.ppWeb)
        binding.ppWeb.destroy()
        super.onDestroy()
    }
}
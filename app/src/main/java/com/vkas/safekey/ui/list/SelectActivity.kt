package com.vkas.safekey.ui.list

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.safekey.BR
import com.vkas.safekey.R
import com.vkas.safekey.base.BaseActivity
import com.vkas.safekey.bean.SkServiceBean
import com.vkas.safekey.databinding.ActivitySelectBinding
import com.vkas.safekey.key.Key
import com.vkas.safekey.utils.KLog
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResourceUtils

class SelectActivity : BaseActivity<ActivitySelectBinding, SelectViewModel>() {
    private lateinit var selectAdapter: SelectAdapter
    private var skServiceBeanList: MutableList<SkServiceBean> = ArrayList()

    //选中服务器
    private lateinit var checkSkServiceBean: SkServiceBean

    // 是否连接
    private var whetherToConnect = false
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_select
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        checkSkServiceBean = SkServiceBean()
        whetherToConnect = bundle?.getBoolean(Key.WHETHER_SK_CONNECTED) == true
        checkSkServiceBean = JsonUtil.fromJson(
            bundle?.getString(Key.CURRENT_SK_SERVICE),
            object : TypeToken<SkServiceBean?>() {}.type
        )
        KLog.e("TAG", "whetherToConnect=${whetherToConnect}")
        KLog.e("TAG", "checkSkServiceBean=${JsonUtil.toJson(checkSkServiceBean)}")

    }

    override fun initToolbar() {
        super.initToolbar()
        binding.selectTitle.imgBack.visibility = View.VISIBLE
        binding.selectTitle.tvTitle.text = getString(R.string.select_title)
        binding.selectTitle.ivRight.visibility = View.GONE
        binding.selectTitle.imgBack.setOnClickListener {
            returnToHomePage()
        }
    }

    override fun initData() {
        super.initData()
        initSelectRecyclerView()
        viewModel.getServerListData()
    }

    override fun initViewObservable() {
        super.initViewObservable()
        getServerListData()
    }

    private fun getServerListData() {
        viewModel.liveServerListData.observe(this, {
            echoServer(it)
        })
    }

    private fun initSelectRecyclerView() {
        selectAdapter = SelectAdapter(skServiceBeanList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.recyclerSelect.layoutManager = layoutManager
        binding.recyclerSelect.adapter = selectAdapter
        selectAdapter.setOnItemClickListener { _, _, pos ->
            run {
                selectServer(pos)
            }
        }
    }

    /**
     * 选中服务器
     */
    private fun selectServer(position: Int) {
        skServiceBeanList.forEachIndexed { index, _ ->
            skServiceBeanList[index].sk_cheek_state = position == index
            if (skServiceBeanList[index].sk_cheek_state == true) {
                checkSkServiceBean = skServiceBeanList[index]
            }
        }
        selectAdapter.notifyDataSetChanged()
        showDisconnectDialog()
    }

    /**
     * 回显服务器
     */
    private fun echoServer(it: MutableList<SkServiceBean>) {
        skServiceBeanList = it
        skServiceBeanList.forEachIndexed { index, _ ->
            if (checkSkServiceBean.sk_bestServer == true) {
                skServiceBeanList[0].sk_cheek_state = true
            } else {
                skServiceBeanList[index].sk_cheek_state =
                    skServiceBeanList[index].sk_ip == checkSkServiceBean.sk_ip
                skServiceBeanList[0].sk_cheek_state = false
            }
        }
        selectAdapter.setList(skServiceBeanList)
    }

    /**
     * 返回主页
     */
    private fun returnToHomePage() {
        finish()
    }

    /**
     * 是否断开连接
     */
    private fun showDisconnectDialog() {
        if (!whetherToConnect) {
            finish()
            LiveEventBus.get<SkServiceBean>(Key.NOT_CONNECTED_RETURN)
                .post(checkSkServiceBean)
            return
        }
        val dialog: android.app.AlertDialog? = android.app.AlertDialog.Builder(this)
            .setTitle("Are you sure to disconnect current server")
            //设置对话框的按钮
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("DISCONNECT") { dialog, _ ->
                dialog.dismiss()
                finish()
                LiveEventBus.get<SkServiceBean>(Key.CONNECTED_RETURN)
                    .post(checkSkServiceBean)
            }.create()
        dialog?.show()

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            returnToHomePage()
        }
        return true
    }

}
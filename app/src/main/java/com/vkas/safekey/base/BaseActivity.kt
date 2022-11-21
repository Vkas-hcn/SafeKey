package com.vkas.safekey.base
import android.os.Bundle
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.databinding.ViewDataBinding
abstract class BaseActivity<V : ViewDataBinding, VM : BaseViewModelMVVM> :
    BaseActivityMVVM<V, VM>()  {
    val tag = javaClass.simpleName
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun initData() {
        super.initData()
    }

    /**
     * 监听返回
     * */
//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        return if (this.isDisableBackKey) {
//            isDisableBackKey = false
//            keyCode == KeyEvent.KEYCODE_BACK
//        } else {
//            super.onKeyDown(keyCode, event)
//        }
//    }
//
//    override fun onBackPressed() {
//        if(isDisableBackKey){
//            isDisableBackKey = false
//           return
//        }else{
//            super.onBackPressed()
//        }
//    }

    /**
     * 获取点击事件
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action === MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (isHideInput(view, ev)) {
                hideSoftInput(view!!.windowToken)
                view.clearFocus()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * 判定是否需要隐藏
     */
    open fun isHideInput(v: View?, ev: MotionEvent): Boolean {
        if (v != null && v is EditText) {
            val l = intArrayOf(0, 0)
            v.getLocationInWindow(l)
            val left = l[0]
            val top = l[1]
            val bottom = top + v.height
            val right = left + v.width
            return !(ev.x > left && ev.x < right && ev.y > top && ev.y < bottom)
        }
        return false
    }

    /**
     * 隐藏软键盘
     */
    open fun hideSoftInput(token: IBinder?) {
        if (token != null) {
            val manager: InputMethodManager =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }
}
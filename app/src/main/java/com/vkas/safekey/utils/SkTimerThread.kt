package com.vkas.safekey.utils

import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.safekey.key.Key
import kotlinx.coroutines.*
import java.text.DecimalFormat

class SkTimerThread {
    companion object {
        fun getInstance() = InstanceHelper.skTimer
    }

    object InstanceHelper {
        val skTimer = SkTimerThread()
    }

    private val job = Job()
    private val timerThread = CoroutineScope(job)
    var skTime = 0
    var isStopThread =true
    /**
     * 发送定时器信息
     */
    fun sendTimerInformation() {
        timerThread.launch {
            while (isActive) {
                skTime++
                if (!isStopThread) {
                    LiveEventBus.get<String>(Key.TIMER_SK_DATA)
                        .post(formatTime(skTime))
                }
                delay(1000)
            }
        }
    }
    /**
     * 开始计时
     */
    fun startTiming(){
        if(isStopThread){
            skTime = 0
        }
        isStopThread =false
    }
    /**
     * 结束计时
     */
    fun endTiming(){
        MmkvUtils.set(Key.LAST_TIME,formatTime(skTime))
        skTime = 0
        isStopThread =true
        LiveEventBus.get<String>(Key.TIMER_SK_DATA)
            .post(formatTime(0))
    }
    /**
     * 设置时间格式
     */
    private fun formatTime(timerData:Int):String{
        val hh: String = DecimalFormat("00").format(timerData / 3600)
        val mm: String = DecimalFormat("00").format(timerData % 3600 / 60)
        val ss: String = DecimalFormat("00").format(timerData % 60)
        return "$hh:$mm:$ss"
    }

}
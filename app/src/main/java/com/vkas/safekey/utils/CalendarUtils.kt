package com.vkas.safekey.utils

import java.lang.Exception
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
object CalendarUtils {
    val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")

    private val dateThreadFormat: ThreadLocal<SimpleDateFormat?> =
        object : ThreadLocal<SimpleDateFormat?>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd")
            }
        }

    //判断一个时间在另一个时间之后
    fun dateAfterDate(startTime: String?, endTime: String?): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd")
        try {
            val startDate: Date = format.parse(startTime)
            val endDate: Date = format.parse(endTime)
            val start: Long = startDate.getTime()
            val end: Long = endDate.getTime()
            if (end > start) {
                return true
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            return false
        }
        return false
    }

    var CONST_WEEK = (3600 * 1000 * 24 * 7).toLong()

    /**
     * 质量压缩
     * 设置bitmap options属性，降低图片的质量，像素不会减少
     * 第一个参数为需要压缩的bitmap图片对象，第二个参数为压缩后图片保存的位置
     * 设置options 属性0-100，来实现压缩（因为png是无损压缩，所以该属性对png是无效的）
     *
     * @param bmp
     * @param file
     */
    fun qualityCompress(bmp: Bitmap, file: File?) {
        // 0-100 100为不压缩
        val quality = 20
        val baos = ByteArrayOutputStream()
        // 把压缩后的数据存放到baos中
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        try {
            val fos = FileOutputStream(file)
            fos.write(baos.toByteArray())
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*
     * 将时间转换为时间戳
     */
    @Throws(ParseException::class)
    fun dateToStamp(s: String?): String? {
        val res: String
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date: Date = simpleDateFormat.parse(s)
        val ts: Long = date.getTime()
        res = ts.toString()
        return res
    }

    /*
     * 将时间戳转换为时间
     */
    fun stampToDate(s: String): String? {
        val res: String
        @SuppressLint("SimpleDateFormat") val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        val lt: String = s
        val date = Date(lt)
        res = simpleDateFormat.format(date)
        return res
    }

    /**
     * @return 当前详细日期
     */
    fun formatDetailedDateNow(isEnd: Boolean): String? {
        val simpleDateFormat: SimpleDateFormat
        if (isEnd) {
            simpleDateFormat = SimpleDateFormat("yyyy-MM-dd 23:59:59")
        } else {
            simpleDateFormat = SimpleDateFormat("yyyy-MM-dd 00:00:00")
        }
        val date = Date()
        return simpleDateFormat.format(date)
    }

    /**
     * @return 当前日期
     */
    fun formatDateNow(): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = Date()
        return simpleDateFormat.format(date)
    }

    /**
     * 转2020-05-10样式
     *
     * @param time
     * @return
     */
    fun getTimeToDateFormat1(time: String?): String? {
        var cTime = ""
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日")
        try {
            val date: Date = dateFormat.parseObject(time) as Date
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
            cTime = simpleDateFormat.format(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return cTime
    }

    /**
     * 转年月日
     *
     * @param time
     * @return
     */
    fun getTimeToDateFormat2(time: String?): String? {
        var cTime = ""
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        try {
            val date: Date = dateFormat.parseObject(time) as Date
            val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日")
            cTime = simpleDateFormat.format(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return cTime
    }

    /**
     * 判断当前时间处于上午还是下午
     *
     * @return apm=0 表示上午，apm=1表示下午
     */
    fun getAmp(): Int {
        val time = System.currentTimeMillis()
        val mCalendar: Calendar = Calendar.getInstance()
        mCalendar.setTimeInMillis(time)
        val hour: Int = mCalendar.get(Calendar.HOUR)
        return mCalendar.get(Calendar.AM_PM)
    }

    fun getTodayTime(): String? {
        val date = Date()
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日")
        return dateFormat.format(date)
    }

    fun getNextDay(): String? {
        val date = Date()
        val time: Long = date.getTime()
        val nextTime = time + 1 * 24 * 60 * 60 * 1000
        val next = Date(nextTime)
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日")
        return dateFormat.format(next)
    }

    /**
     * 计算当月第一天
     *
     * @return
     */
    fun getFirstofMonth(dete_format: String?): String? {
        val format = SimpleDateFormat(dete_format)
        val ca: Calendar = Calendar.getInstance()
        ca.add(Calendar.MONTH, 0)
        ca.set(Calendar.DAY_OF_MONTH, 1)
        return format.format(ca.getTime())
    }

}
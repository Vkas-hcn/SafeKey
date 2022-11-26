package com.vkas.safekey.bean

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class SkAdBean(
    var sk_back: MutableList<SkAdDetails> = ArrayList(),
    var sk_click_num: Int = 0,
    var sk_connect: MutableList<SkAdDetails> = ArrayList(),
    var sk_home: MutableList<SkAdDetails> = ArrayList(),
    var sk_open: MutableList<SkAdDetails> = ArrayList(),
    var sk_result: MutableList<SkAdDetails> = ArrayList(),
    var sk_show_num: Int = 0
) : Serializable

@Keep
data class SkAdDetails(
    val sk_id: String,
    val sk_platform: String,
    val sk_type: String,
    val sk_weight: Int
)

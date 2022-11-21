package com.vkas.safekey.bean

import androidx.annotation.Keep

@Keep
class SkServiceBean {
    var sk_pwd: String? = null
    var sk_method: String? = null
    var sk_port: Int? = null
    var sk_country: String? = null
    var sk_city: String? = null
    var sk_ip: String? = null
    //是否选中
    var sk_cheek_state: Boolean? = false
    //是否是最佳服务器
    var sk_bestServer:Boolean? = false
}
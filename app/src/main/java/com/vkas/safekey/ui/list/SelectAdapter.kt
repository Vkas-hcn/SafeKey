package com.vkas.safekey.ui.list

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.vkas.safekey.R
import com.vkas.safekey.bean.SkServiceBean
import com.vkas.safekey.key.Key
import com.vkas.safekey.utils.LocalDataUtils.getFlagThroughCountry

class SelectAdapter(data: MutableList<SkServiceBean>?) :
    BaseQuickAdapter<SkServiceBean, BaseViewHolder>(
        R.layout.item_select,
        data
    ) {
    override fun convert(holder: BaseViewHolder, item: SkServiceBean) {
        if (item.sk_bestServer == true) {
            holder.setText(R.id.txt_country, Key.FASTER_SK_SERVER)
            holder.setImageResource(R.id.img_flag, getFlagThroughCountry(Key.FASTER_SK_SERVER))
        } else {
            holder.setText(R.id.txt_country, item.sk_country + "-" + item.sk_city)
            holder.setImageResource(R.id.img_flag, getFlagThroughCountry(item.sk_country.toString()))
        }
        if (item.sk_cheek_state == true) {
            holder.setImageResource(R.id.img_che, R.drawable.ic_select)
        } else {
            holder.setImageResource(R.id.img_che, R.drawable.ic_not_select)
        }
    }

}
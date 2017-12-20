package com.shiwenping.rxhpay

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.shiwenping.hpay.RxPay

class MainActivity : AppCompatActivity() {
    var rx : RxPay? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

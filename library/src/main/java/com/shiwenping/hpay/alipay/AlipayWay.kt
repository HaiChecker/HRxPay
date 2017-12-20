package com.shiwenping.hpay.alipay

import android.app.Activity

import com.alipay.sdk.app.PayTask
import com.shiwenping.hpay.PaymentStatus

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.schedulers.Schedulers

/**
 * Created by cuieney on 18/08/2017.
 */
object AlipayWay {

    fun payMoney(activity: Activity, orderInfo: String): Flowable<PaymentStatus> {
        return Flowable.create(FlowableOnSubscribe<PayTask> { e ->
            val alipay = PayTask(activity)
            e.onNext(alipay)
        }, BackpressureStrategy.ERROR)
                .map { payTask ->
                    createPaymentStatus(payTask, orderInfo)
                }
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())

    }


    private fun createPaymentStatus(payTask: PayTask, orderInfo: String): PaymentStatus {
        val result = payTask.payV2(orderInfo, true)
        val payResult = PayResult(result)
        val resultStatus = payResult.resultStatus
        return if (resultStatus.equals("9000")) {
            PaymentStatus(true)
        } else {
            PaymentStatus(false)
        }
    }

}

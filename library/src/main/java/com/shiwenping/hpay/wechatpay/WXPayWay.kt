package com.shiwenping.hpay.wechatpay

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.text.TextUtils
import android.util.Log
import com.shiwenping.hpay.PaymentStatus
import com.shiwenping.hpay.RxBus

import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.WXAPIFactory

import org.json.JSONObject

import java.security.MessageDigest
import java.util.LinkedList
import java.util.Random

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.schedulers.Schedulers
import java.nio.charset.Charset
import kotlin.experimental.and


/**
 * wechat Method of payment
 */
object WXPayWay {
    private val PARTNER_ID = "partnerid"
    private val NONCE_STR = "nonceStr"
    private val TIME_STAMP = "timeStamp"
    private val SIGN = "sign"
    private val META_WX_APPID = "WX_APPID"
    private val META_PARTNER_ID = "PARTNER_ID"
    private val META_API_KEY = "API_KEY"


    fun payMoney(context: Activity, json: JSONObject): Flowable<PaymentStatus> {

        return Flowable.create(FlowableOnSubscribe<PaymentStatus> { e ->
            val appId = getMetaData(context, META_WX_APPID)
            val api = WXAPIFactory.createWXAPI(context, appId)
            api.registerApp(appId)
            val req = PayReq()

            req.appId = appId
            Log.d("HaiChecker", json.toString())
            setValue(req, PARTNER_ID, json.optString("partnerid"), context)
            req.prepayId = json.optString("prepayid")
            setValue(req, NONCE_STR, json.optString("noncestr"), context)
            setValue(req, TIME_STAMP, json.optString("timestamp"), context)
            req.packageValue = json.optString("package", "Sign=WXPay")
            setValue(req, SIGN, json.optString("sign"), context)
            req.extData = "app data"

            val sendReq = api.sendReq(req)
            if (!sendReq) {
                e.onNext(PaymentStatus(false))
                e.onComplete()
            } else {
                RxBus.default.toFlowable(PaymentStatus::class.java)
                        .subscribe({ paymentStatus ->
                            e.onNext(paymentStatus)
                            e.onComplete()
                        }, {
                            e.onNext(PaymentStatus(false))
                            e.onComplete()
                        })
            }
        }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())


    }

    fun getMetaData(context: Activity, metaData: String): String? {
        var info: ApplicationInfo?
        try {
            info = context.application.packageManager
                    .getApplicationInfo(context.packageName,
                            PackageManager.GET_META_DATA)
            val data = info!!.metaData.get(metaData) ?: throw NullPointerException(metaData + " field cannot be empty")
            return data.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return null
    }


    private fun setValue(req: PayReq, value: String, rawValue: String?, context: Activity) {
        var configValue: String? = rawValue
        when (value) {
            PARTNER_ID -> {
                if (TextUtils.isEmpty(configValue)) {
                    configValue = getMetaData(context, META_PARTNER_ID)
                }
                req.partnerId = configValue
            }
            NONCE_STR -> {
                if (TextUtils.isEmpty(configValue)) {
                    configValue = genNonceStr()
                }

                req.nonceStr = configValue
            }
            TIME_STAMP -> {
                if (TextUtils.isEmpty(configValue)) {
                    configValue = genTimeStamp()
                }
                req.timeStamp = configValue
            }
            SIGN -> {
                if (TextUtils.isEmpty(configValue)) {
                    configValue = genAppSign(req, getMetaData(context, META_API_KEY))
                }
                req.sign = configValue
            }
            else -> {
            }
        }
    }

    private fun genNonceStr(): String? {
        val random = Random()
        return getMessageDigest(random.nextInt(10000).toString().toByteArray(Charset.defaultCharset()))
    }


    private fun genAppSign(payReq: PayReq, apiKey: String?): String {
        var params = LinkedList<NameValuePair>()
        params.add(NameValuePair("appid", payReq.appId))
        params.add(NameValuePair("noncestr", payReq.nonceStr))
        params.add(NameValuePair("package", payReq.packageValue))
        params.add(NameValuePair("partnerid", payReq.partnerId))
        params.add(NameValuePair("prepayid", payReq.prepayId))
        params.add(NameValuePair("timestamp", payReq.timeStamp))


        val tempSb = StringBuilder()
        for (i in params) {
            tempSb.append(i.name)
            tempSb.append('=')
            tempSb.append(i.name)
            tempSb.append('&')
        }
        tempSb.append("key=")
        tempSb.append(apiKey)
        return getMessageDigest(tempSb.toString().toByteArray())!!.toUpperCase()
    }

    private fun getMessageDigest(buffer: ByteArray): String? {
        val hexDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
        try {
            val mdTemp = MessageDigest.getInstance("MD5")
            mdTemp.update(buffer)
            val md = mdTemp.digest()
            val j = md.size
            val str = CharArray(j * 2)
            var k = 0
            for (i in 0 until j) {
                val byte0 = md[i]
                str[k++] = hexDigits[byte0.toInt().ushr(4) and 0xf]
                str[k++] = hexDigits[(byte0 and 0xf).toInt()]
            }
            return str.toString()
        } catch (e: Exception) {
            return null
        }

    }

    private fun genTimeStamp(): String {
        return (System.currentTimeMillis() / 1000).toString()
    }
}


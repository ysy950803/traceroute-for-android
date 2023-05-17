package com.wandroid.traceroute.sample

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStatsManager.INTERVAL_BEST
import android.content.Context
import android.util.Log
import com.wandroid.traceroute.sample.AppUsageManager.toTimeStr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object AppUsageManager {

    fun updateData(context: Context, callback1: (String) -> Unit, callback2: (String) -> Unit) {
        thread {
            // 获取UsageStatsManager实例
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            // 获取当前时间的时间戳
            val endTime = System.currentTimeMillis()
            // 设置查询的时间段
            val beginTime = endTime - TimeUnit.HOURS.toMillis(1)
            // 使用UsageStatsManager查询应用使用情况
            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            // 遍历所有应用使用事件
            var result1 = ""
            while (usageEvents.hasNextEvent()) {
                // 获取应用使用事件
                val usageEvent = UsageEvents.Event()
                usageEvents.getNextEvent(usageEvent)
                if (usageEvent.packageName == "com.ysy.mkcngrtag") {
                    Log.w(
                        "TEST-1",
                        "${usageEvent.packageName} ${usageEvent.eventType} ${usageEvent.timeStamp.toTimeStr()} ${usageEvent.className}"
                    )
                    result1 += "${usageEvent.eventType} ${usageEvent.timeStamp.toTimeStr()} ${usageEvent.className}\n"
                }
            }
            callback1.invoke(result1)

            usageStatsManager.queryEventStats(INTERVAL_BEST, beginTime, endTime).forEach {
                Log.d(
                    "TEST-1",
                    "${it.eventType} ${it.count} ${it.firstTimeStamp.toTimeStr()} ${it.lastTimeStamp.toTimeStr()} ${it.lastEventTime.toTimeStr()}" +
                            " ${TimeUnit.MILLISECONDS.toSeconds(it.totalTime)}s"
                )
            }

            var result2 = ""
            usageStatsManager.queryAndAggregateUsageStats(beginTime, endTime).forEach {
                val t = it.key
                val u = it.value
                if (t == "com.ysy.mkcngrtag") {
                    Log.i(
                        "TEST-1",
                        "$t ${u.firstTimeStamp.toTimeStr()} ${u.lastTimeStamp.toTimeStr()} ${u.lastTimeUsed.toTimeStr()} " +
                                "${u.lastTimeVisible.toTimeStr()} ${u.lastTimeForegroundServiceUsed.toTimeStr()} " +
                                "${TimeUnit.MILLISECONDS.toSeconds(u.totalTimeVisible)} ${
                                    TimeUnit.MILLISECONDS.toSeconds(
                                        u.totalTimeForegroundServiceUsed
                                    )
                                } ${TimeUnit.MILLISECONDS.toSeconds(u.totalTimeInForeground)}"
                    )
                    result2 += "firstTimeStamp=${u.firstTimeStamp.toTimeStr()}\n lastTimeStamp=${u.lastTimeStamp.toTimeStr()}\n lastTimeUsed=${u.lastTimeUsed.toTimeStr()}\n " +
                            "lastTimeVisible=${u.lastTimeVisible.toTimeStr()}\n lastTimeForegroundServiceUsed=${u.lastTimeForegroundServiceUsed.toTimeStr()}\n " +
                            "totalTimeVisible=${TimeUnit.MILLISECONDS.toSeconds(u.totalTimeVisible)}s\n " +
                            "totalTimeForegroundServiceUsed=${TimeUnit.MILLISECONDS.toSeconds(u.totalTimeForegroundServiceUsed)}s\n " +
                            "totalTimeInForeground=${TimeUnit.MILLISECONDS.toSeconds(u.totalTimeInForeground)}s\n"
                }
            }
            callback2.invoke(result2)
        }
    }

    private fun Long.toTimeStr(): String {
        return if (this == 0L) "--"
        else SimpleDateFormat("MM-dd HH:mm:ss").format(Date(this))
    }
}

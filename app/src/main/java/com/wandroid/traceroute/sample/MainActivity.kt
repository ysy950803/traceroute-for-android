package com.wandroid.traceroute.sample

import android.app.AppOpsManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.wandroid.traceroute.TraceRoute
import com.wandroid.traceroute.TraceRouteByPing
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_USAGE_STAT = 10001
    }

    private lateinit var button: AppCompatButton
    private lateinit var text: AppCompatEditText
    private lateinit var result: AppCompatTextView
    private lateinit var result2: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.sample_button)
        text = findViewById(R.id.sample_text)
        result = findViewById(R.id.result)
        result2 = findViewById(R.id.result2)

        button.setOnClickListener {
            it.isEnabled = false
            result.text = ""
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                thread {
                    TraceRouteByPing().run {
                        execute(30, text.text.toString()) { output, finished, _ ->
                            result.post {
                                result.append("$output\n")
                                if (finished) it.isEnabled = true
                            }
                        }
                        exit()
                    }
                }
            } else {
                doTraceRoute()
            }
        }

        if (!checkUsageStatPermission()) {
            // 如果没有授权，跳转至授权页面
            startActivityForResult(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                REQUEST_CODE_USAGE_STAT
            );
        } else {
            // 已经授权，可以执行相应操作
        }
    }

    private fun doTraceRoute() {
        TraceRoute.setCallback {
            success {
                result.append("* * * finished! * * *\n")
                button.isEnabled = true
            }
            update { text -> result.append(text) }
            failed { code, reason ->
                result.append("traceroute failed.code:$code, reason:$reason")
                button.isEnabled = true
            }
        }
        TraceRoute.traceRoute(text.text.toString(), true)
    }

    /**
     * 检查是否有授权访问 USAGE_STATS_SERVICE
     */
    private fun checkUsageStatPermission(): Boolean {
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        var mode = AppOpsManager.MODE_ALLOWED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                applicationContext.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_USAGE_STAT) {
            // 获取到授权页面返回后，检查是否已经获得了授权
            if (checkUsageStatPermission()) {
                // 已经授权，可以执行相应操作
            } else {
                Toast.makeText(this, "未获得授权，无法使用此功能", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun test() {
        AppUsageManager.updateData(applicationContext, {
            runOnUiThread { result.text = it }
        }, {
            runOnUiThread { result2.text = it }
        })
    }

    override fun onResume() {
        super.onResume()
        test()
    }
}

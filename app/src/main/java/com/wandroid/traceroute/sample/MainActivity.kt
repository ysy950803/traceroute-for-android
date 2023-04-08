package com.wandroid.traceroute.sample

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.wandroid.traceroute.TraceRoute
import com.wandroid.traceroute.TraceRouteByPing
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var button: AppCompatButton
    private lateinit var text: AppCompatEditText
    private lateinit var result: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.sample_button)
        text = findViewById(R.id.sample_text)
        result = findViewById(R.id.result)

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
}

package com.boo.sample.samplewindowmanager

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import androidx.window.WindowLayoutInfo
import com.boo.sample.samplewindowmanager.databinding.ActivityMainBinding
import java.util.concurrent.Executor
import java.util.function.Consumer


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var wm: androidx.window.WindowManager

    @RequiresApi(Build.VERSION_CODES.N)
    private val layoutStateChangeCallback = LayoutStateChangeCallback()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        wm = androidx.window.WindowManager(this)
        binding.windowMetrics.text =
            "CurrentWindowMetrics: ${wm.currentWindowMetrics.bounds.flattenToString()}\n" +
                    "MaximumWindowMetrics: ${wm.maximumWindowMetrics.bounds.flattenToString()}"

        binding.btnOpen.setOnClickListener {
            openActivityInAdjacentWindow()
        }

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wm.registerLayoutChangeCallback(runOnUiThreadExecutor(), layoutStateChangeCallback)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        wm.unregisterLayoutChangeCallback(layoutStateChangeCallback)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    inner class LayoutStateChangeCallback : androidx.core.util.Consumer<WindowLayoutInfo> {
        override fun accept(newLayoutInfo: WindowLayoutInfo) {
            printLayoutStateChange(newLayoutInfo)
            //alignViewToDeviceFeatureBoundaries(newLayoutInfo)
        }

        private fun printLayoutStateChange(newLayoutInfo: WindowLayoutInfo) {
            binding.layoutChange.text = newLayoutInfo.toString()
            if (newLayoutInfo.displayFeatures.isNotEmpty()) {
                binding.configurationChanged.text = "Spanned across displays"
                alignViewToDeviceFeatureBoundaries(newLayoutInfo)
            } else {
                binding.configurationChanged.text = "One logic/physical display - unspanned"
            }
        }

        private fun alignViewToDeviceFeatureBoundaries(newLayoutInfo: WindowLayoutInfo) {
            val constraintLayout = binding.constraintLayout
            val set = ConstraintSet()
            set.clone(constraintLayout)

            val rect = newLayoutInfo.displayFeatures[0].bounds  //힌지 크기
            set.constrainHeight(R.id.device_feature, rect.bottom - rect.top)
            set.constrainWidth(R.id.device_feature, rect.right - rect.left)

            //컨스트레인트 설정
            set.connect(
                R.id.device_feature, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START, 0
            )
            set.connect(
                R.id.device_feature, ConstraintSet.TOP,
                ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0
            )

            if (rect.top == 0) {    //힌지가 수직으로 배치 => 듀얼 화면 에뮬레이터
                set.setMargin(R.id.device_feature, ConstraintSet.START, rect.left)
                //connect: Create a constraint between two widgets
                set.connect(
                    R.id.layout_change, ConstraintSet.END,
                    R.id.device_feature, ConstraintSet.START, 0
                )
                set.connect(
                    R.id.btn_open, ConstraintSet.END,
                    R.id.device_feature, ConstraintSet.START
                )

            } else {    //힌지가 수평으로 배치 => 폴더블 폰
                //Device feature is placed horizontally
                val statusBarHeight = calculateStatusBarHeight()
                val toolBarHeight = calculateToolbarHeight()
                set.setMargin(
                    R.id.device_feature, ConstraintSet.TOP,
                    rect.top - statusBarHeight - toolBarHeight
                )
                //connect: Create a constraint between two widgets
                set.connect(
                    R.id.layout_change, ConstraintSet.TOP,
                    R.id.device_feature, ConstraintSet.BOTTOM, 0
                )
            }
            set.setVisibility(R.id.device_feature, View.VISIBLE)
            set.applyTo(constraintLayout)
        }

        private fun calculateToolbarHeight(): Int {
            val typedValue = TypedValue()
            return if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
                //boolean Returns true if the attribute was found and outValue is valid, else false.
                TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
            } else {
                0
            }
        }

        private fun calculateStatusBarHeight(): Int {
            val rect = Rect()
            window.decorView.getWindowVisibleDisplayFrame(rect)
            return rect.top
        }
    }

    private fun runOnUiThreadExecutor(): Executor {
        val handler = Handler(Looper.getMainLooper())
        return Executor() {
            handler.post(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun openActivityInAdjacentWindow() {
        val intent = Intent(this, SecondActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

}
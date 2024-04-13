package mo.edu.kaoyip.kyrobot.futurecampus.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

class DensityUtils {
    companion object {
        @JvmStatic
        fun dp2px(context: Context, scale: Float): Int {
            return (scale * context.resources.displayMetrics.density + 0.5f).toInt()
        }

        @JvmStatic
        fun px2dp(context: Context, scale: Float): Int {
            return (scale / context.resources.displayMetrics.density + 0.5f).toInt()
        }

        @JvmStatic
        fun getDensityDpi(context: Context): Int {
            val displayMetrics = DisplayMetrics()

            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(
                displayMetrics
            )
            return displayMetrics.densityDpi
        }

        @JvmStatic
        fun getScaledDensity(context: Context): Float {
            val displayMetrics = DisplayMetrics()
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(
                displayMetrics
            )
            return displayMetrics.scaledDensity
        }
        @JvmStatic
        fun pxWidthToMm(context: Context, value: Int): Int {
            val inch: Float = (value / getDensityDpi(context)).toFloat()
            return (inch * 25.4f).toInt()
        }
    }
}

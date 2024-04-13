package mo.edu.kaoyip.kyrobot.futurecampus.utils

import android.os.CountDownTimer


open class MyCountDownTimer(millisInFuture: Long, countDownInterval: Long) :
    CountDownTimer(millisInFuture, countDownInterval) {
    override fun onTick(millisUntilFinished: Long) {
        // 每次倒计时更新时调用
        // 更新UI显示
    }

    override fun onFinish() {
        // 倒计时结束时调用
        // 更新UI显示
    }
}


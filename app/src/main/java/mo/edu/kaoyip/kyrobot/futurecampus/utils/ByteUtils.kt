package mo.edu.kaoyip.kyrobot.futurecampus.utils

import java.nio.ByteBuffer
import java.util.Locale


/**
 * Byte工具
 *
 *
 * Created by yangle on 2018/7/5.
 * Website：http://www.yangle.tech
 */
object ByteUtils {
    /**
     * 十六进制字符串转byte[]
     *
     * @param hex 十六进制字符串
     * @return byte[]
     */
    fun hexStr2Byte(hex: String?): ByteArray {
        var hex = hex ?: return byteArrayOf()

        // 奇数位补0
        if (hex.length % 2 != 0) {
            hex = "0$hex"
        }
        val length = hex.length
        val buffer = ByteBuffer.allocate(length / 2)
        var i = 0
        while (i < length) {
            var hexStr = hex[i].toString() + ""
            i++
            hexStr += hex[i]
            val b = hexStr.toInt(16).toByte()
            buffer.put(b)
            i++
        }
        return buffer.array()
    }

    /**
     * byte[]转十六进制字符串
     *
     * @param array byte[]
     * @return 十六进制字符串
     */
    fun byteArrayToHexString(array: ByteArray?): String {
        if (array == null) {
            return ""
        }
        val buffer = StringBuffer()
        for (i in array.indices) {
            buffer.append(byteToHex(array[i]))
        }
        return buffer.toString()
    }

    /**
     * byte转十六进制字符
     *
     * @param b byte
     * @return 十六进制字符
     */
    fun byteToHex(b: Byte): String {
        var hex = Integer.toHexString(b.toInt() and 0xFF)
        if (hex.length == 1) {
            hex = "0$hex"
        }
        return hex.uppercase(Locale.getDefault())
    }
}
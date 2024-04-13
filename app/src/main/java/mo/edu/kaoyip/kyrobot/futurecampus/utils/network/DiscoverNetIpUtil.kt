package mo.edu.kaoyip.kyrobot.futurecampus.utils.network

import android.util.Log
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Enumeration


object DiscoverNetIpUtil {
    val hostIP: String?
        /**
         * 获取ip地址
         *
         * @return
         */
        get() {
            var hostIp: String? = null
            try {
                //这里以eth0为例
                hostIp = getIpAddress("eth0")
            } catch (e: SocketException) {
                e.printStackTrace()
            }
            return hostIp
        }

    /**
     * Get Ip address 自动获取IP地址
     * 可以传入eth1，eth0,wlan0,等
     *
     * @throws SocketException
     */
    @Throws(SocketException::class)
    fun getIpAddress(ipType: String): String? {
        var hostIp: String? = null
        try {
            val nis: Enumeration<*> = NetworkInterface.getNetworkInterfaces()
            var ia: InetAddress? = null
            while (nis.hasMoreElements()) {
                val ni = nis.nextElement() as NetworkInterface
                if (ni.name == ipType) {
                    val ias = ni.getInetAddresses()
                    while (ias.hasMoreElements()) {
                        ia = ias.nextElement()
                        if (ia is Inet6Address) {
                            continue  // skip ipv6
                        }
                        val ip = ia.hostAddress

                        // 过滤掉127段的ip地址
                        if ("127.0.0.1" != ip) {
                            hostIp = ia.hostAddress
                            break
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        Log.d(ipType, "get the IpAddress--> $hostIp")
        return hostIp
    }

    /**
     * 创建一个线程向本地所有ip发送一个数据
     */
    fun sendDataToLocal() {
        //局域网内存在的ip集合
        val ipList: List<String> = ArrayList()
        val map: Map<String, String> = HashMap()
        //获取本机所在的局域网地址
        val hostIP = hostIP
        val lastIndexOf = hostIP!!.lastIndexOf(".")
        val substring = hostIP.substring(0, lastIndexOf + 1)
        Thread {
            val dp = DatagramPacket(ByteArray(0), 0, 0)
            var socket: DatagramSocket
            try {
                socket = DatagramSocket()
                var position = 2
                while (position < 255) {
                    Log.e("Scanner ", "run: udp-$substring$position")
                    dp.address = InetAddress.getByName(substring + position.toString())
                    socket.send(dp)
                    position++
                    if (position == 125) {
                        //分两段掉包，一次性发的话，达到236左右，会耗时3秒左右再往下发
                        socket.close()
                        socket = DatagramSocket()
                    }
                }
                socket.close()
            } catch (e: SocketException) {
                e.printStackTrace()
            } catch (e: UnknownHostException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * 读取/proc/net/arp并且解析出来ip，mac,flag
     * flag 为0x00说明目前不在局域网内，曾经在过.0x02代表在局域网内
     */
    fun readArp() {
        try {
            val br = BufferedReader(FileReader("/proc/net/arp"))
            var line = ""
            var ip = ""
            //flag 为0x00说明目前不在局域网内，曾经在过.0x02代表在局域网内
            var flag = ""
            var mac = ""
            if (br.readLine() == null) {
                Log.e("scanner", "readArp: null")
            }
            while (br.readLine().also { line = it } != null) {
                line = line.trim { it <= ' ' }
                if (line.length < 63) continue
                if (line.uppercase().contains("IP")) continue
                ip = line.substring(0, 17).trim { it <= ' ' }
                flag = line.substring(29, 32).trim { it <= ' ' }
                mac = line.substring(41, 63).trim { it <= ' ' }
                if (mac.contains("00:00:00:00:00:00")) continue
                Log.e("scanner", "readArp: mac= $mac ; ip= $ip ;flag= $flag")
            }
            br.close()
        } catch (ignored: Exception) {
        }
    }
}


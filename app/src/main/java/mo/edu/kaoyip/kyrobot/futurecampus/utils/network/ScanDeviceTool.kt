package mo.edu.kaoyip.kyrobot.futurecampus.utils.network

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.text.TextUtils;
import android.util.Log;


/**
 * @ClassName：ScanDeviceTool
 * @Description：TODO<局域网扫描设备工具类>
 * @author：zihao
 * @date：2015年9月10日 下午3:36:40
 * @version：v1.0
</局域网扫描设备工具类> */
object ScanDeviceTool {
    private var mDevAddress: String? = null // 本机IP地址-完整
    private var mLocAddress: String? = null // 局域网IP地址头,如：192.168.1.
    private val mRun = Runtime.getRuntime() // 获取当前运行环境，来执行ping，相当于windows的cmd
    private var mProcess: Process? = null // 进程
    private val mPing = "ping -c 1 -w 3 " // 其中 -c 1为发送的次数，-w 表示发送后等待响应的时间
    private val mIpList: MutableList<String> = ArrayList() // ping成功的IP地址
    private var mExecutor: ThreadPoolExecutor? = null // 线程池对象

    private val TAG = ScanDeviceTool::class.java.getSimpleName()

    /** 核心池大小  */
    private const val CORE_POOL_SIZE = 1

    /** 线程池最大线程数  */
    private const val MAX_IMUM_POOL_SIZE = 255
    val hostIP: String?
        /**
         * 获取ip地址
         * @return
         */
        get() {
            var hostIp: String? = null
            try {
                val nis: Enumeration<*> = NetworkInterface.getNetworkInterfaces()
                var ia: InetAddress? = null
                while (nis.hasMoreElements()) {
                    val ni = nis.nextElement() as NetworkInterface
                    val ias = ni.getInetAddresses()
                    while (ias.hasMoreElements()) {
                        ia = ias.nextElement()
                        if (ia is Inet6Address) {
                            continue  // skip ipv6
                        }
                        val ip = ia.hostAddress
                        if ("127.0.0.1" != ip) {
                            hostIp = ia.hostAddress
                            break
                        }
                    }
                }
            } catch (e: SocketException) {
                Log.i("yao", "SocketException")
                e.printStackTrace()
            }
            return hostIp
        }

    /**
     * TODO<扫描局域网内ip></扫描局域网内ip>，找到对应服务器>
     *
     * @return void
     */
    fun scan(): MutableList<String>? {
        mDevAddress = hostIP // 获取本机IP地址
        mLocAddress = getLocAddrIndex(mDevAddress) // 获取本地ip前缀
        Log.d(TAG, "开始扫描设备,本机Ip为：$mDevAddress")
        if (TextUtils.isEmpty(mLocAddress)) {
            Log.e(TAG, "扫描失败，请检查wifi网络")
            return null
        }
        /**
         * 1.核心池大小 2.线程池最大线程数 3.表示线程没有任务执行时最多保持多久时间会终止
         * 4.参数keepAliveTime的时间单位，有7种取值,当前为毫秒
         * 5.一个阻塞队列，用来存储等待执行的任务，这个参数的选择也很重要，会对线程池的运行过程产生重大影响
         * ，一般来说，这里的阻塞队列有以下几种选择：
         */
        mExecutor = ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_IMUM_POOL_SIZE,
            2000, TimeUnit.MILLISECONDS, ArrayBlockingQueue<Runnable>(
                CORE_POOL_SIZE
            )
        )

        // 新建线程池
        for (i in 1..254) { // 创建256个线程分别去ping
            val run = Runnable {
                // TODO Auto-generated method stub
                val ping = (mPing + mLocAddress
                        + i)
                val currnetIp = mLocAddress + i
                if (mDevAddress == currnetIp) // 如果与本机IP地址相同,跳过
                    return@Runnable
                try {
                    mProcess = mRun.exec(ping)
                    val result = mProcess?.waitFor()
                    /*Log.d(
                        TAG,
                        "正在扫描的IP地址为：" + currnetIp + "返回值为：" + result
                    )*/
                    if (result == 0) {
                        /*Log.d(
                            TAG,
                            "扫描成功,Ip地址为：$currnetIp"
                        )*/
                        mIpList.add(currnetIp)
                    } else {
                        // 扫描失败
                        /*Log.d(TAG, "扫描失败")*/
                    }
                } catch (e: Exception) {
                    /*Log.e(TAG, "扫描异常$e")*/
                } finally {
                    if (mProcess != null) mProcess!!.destroy()
                }
            }
            mExecutor!!.execute(run)
        }
        mExecutor!!.shutdown()
        while (true) {
            try {
                if (mExecutor!!.isTerminated) { // 扫描结束,开始验证
                    Log.d(TAG, "扫描结束,总共成功扫描到" + mIpList.size + "个设备.")
                    return mIpList
                }
            } catch (e: Exception) {
                // TODO: handle exception
            }
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }

    /**
     * TODO<销毁正在执行的线程池>
     *
     * @return void
    </销毁正在执行的线程池> */
    fun destory() {
        if (mExecutor != null) {
            mExecutor!!.shutdownNow()
        }
    }

    val locAddress: String
        /**
         * TODO<获取本地ip地址>
         *
         * @return String
        </获取本地ip地址> */
        get() {
            var ipaddress = ""
            try {
                val en = NetworkInterface
                    .getNetworkInterfaces()
                // 遍历所用的网络接口
                while (en.hasMoreElements()) {
                    val networks = en.nextElement()
                    // 得到每一个网络接口绑定的所有ip
                    val address = networks.getInetAddresses()
                    // 遍历每一个接口绑定的所有ip
                    while (address.hasMoreElements()) {
                        val ip = address.nextElement()
                        if (!ip.isLoopbackAddress
                            && ip is Inet4Address
                        ) {
                            ipaddress = ip.getHostAddress()
                        }
                    }
                }
            } catch (e: SocketException) {
                Log.e("", "获取本地ip地址失败")
                e.printStackTrace()
            }
            // Log.i(TAG, "本机IP:$ipaddress")
            return ipaddress
        }

    /**
     * TODO<获取本机IP前缀>
     *
     * @param devAddress
     * // 本机IP地址
     * @return String
    </获取本机IP前缀> */
    fun getLocAddrIndex(devAddress: String?): String? {
        return if (devAddress != "") {
            devAddress!!.substring(0, devAddress.lastIndexOf(".") + 1)
        } else null
    }


}
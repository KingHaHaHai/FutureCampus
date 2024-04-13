package mo.edu.kaoyip.kyrobot.futurecampus.utils.network

import android.util.Log
import java.net.ServerSocket
import java.net.Socket

data class NetworkMessage(val ip: String, val message: String)

class PortListener(val port: Int) : Thread() {
    var isRunning = false

    var result: NetworkMessage = NetworkMessage("", "")

    var clientSocket: Socket? = null

    var serverSocket:ServerSocket? = null

    override fun run() {

        isRunning = true
        serverSocket = ServerSocket(port)
        println("正在监听网络端口：$port...")

        while (isRunning) {
            try {
                // 监听连接请求
                clientSocket = serverSocket!!.accept()

                // 处理连接请求
                handleConnection(clientSocket!!)
            } catch (e: Exception) {
                // 处理异常
                if (e.message == "Socket closed"){
                    continue
                }
                println("发生异常：${e.message}")
                e.printStackTrace()
            }
        }
        Log.d("PortListener", "run: 退出监听")
        serverSocket!!.close()
    }

    private fun handleConnection(clientSocket: Socket) {
        println("接收到该IP的连接: ${clientSocket.inetAddress.hostAddress} ")

        clientSocket.getOutputStream().write("I am the 设备".toByteArray())
        clientSocket.getOutputStream().flush()

        // 处理连接请求的线程
        val clientThread = Thread {
            val message = NetworkMessage("${clientSocket.inetAddress.hostAddress}:${clientSocket.port}", clientSocket.getInputStream().bufferedReader().readLine())
            result = message
            println("IP: ${clientSocket.inetAddress.hostAddress}, 带来的消息: ${message.message}")

            // 在此处处理客户端请求，例如读取/写入数据等


            // 关闭客户端连接
            clientSocket.close()
            println("关闭 ${clientSocket.inetAddress.hostAddress} 的连接")
        }

        // 启动线程处理连接请求
        clientThread.start()
    }

    fun stopListening() {
        isRunning = false
        // 关闭 serverSocket
        try {
            // 关闭 serverSocket 之前，先停止接收连接请求
            interrupt()
            // 关闭 serverSocket
            serverSocket!!.close()
        } catch (e: Exception) {
            // 处理异常
            e.printStackTrace()
        }
    }



}
package mo.edu.kaoyip.kyrobot.futurecampus.view.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mo.edu.kaoyip.kyrobot.futurecampus.R
import mo.edu.kaoyip.kyrobot.futurecampus.view.MsgAdapter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


data class Msg(val content: String, val type: Int) {
    companion object {
        const val TYPE_RECEIVED = 0
        const val TYPE_SEND = 1
    }
}


class ChatGptMainActivity: AppCompatActivity() {

    private var TAG = "ChatGptMainActivity"
    private var msgList: ArrayList<Msg> = ArrayList()
    private var msgRecyclerView: RecyclerView? = null
    private var inputText: EditText? = null
    private var send: Button? = null
    private var layoutManager: LinearLayoutManager? = null
    private var adapter: MsgAdapter? = null

    private var isChatting = false
        set(value){
            field = value
            if (value) {
                runOnUiThread{send!!.isEnabled = false}

            }else{
                runOnUiThread{send!!.isEnabled = true}
            }
        }

    private val ChatGptapiKey: String = "sk-ipsxVlglOOTJsntesLFlghplLNCZFqgEwxncsfYBBtQWxtoP" // "Bearer YOUR_API_KEY"
    private val ChatGpturi: String = "https://api.chatanywhere.com.cn/v1/chat/completions" // "https://api.openai.com/v1/completions"
    private var ChatGptModel: String = "gpt-3.5-turbo"
    val JSON: MediaType = "application/json".toMediaType()
    var client: OkHttpClient = OkHttpClient()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatgpt_main)

        // 恢复状态
        val prefs = getSharedPreferences("ChatGptMainActivity", MODE_PRIVATE)
        val text = prefs.getString("msg", "")
        Log.i(TAG, "onCreate getSharedPreferences: $text")
        if (text != null && text.trim().isNotEmpty()) {
            try{
                msgList = deCodeMsgList(text)
            }catch(e: NumberFormatException){}
        }

        msgRecyclerView = findViewById(R.id.gpt_msg_recycler_view)
        inputText = findViewById(R.id.gpt_input_text)
        send = findViewById(R.id.gpt_send)
        layoutManager = LinearLayoutManager(this)
        adapter = MsgAdapter(msgList)

        msgRecyclerView!!.setLayoutManager(layoutManager)
        msgRecyclerView!!.setAdapter(adapter)
        /*       我们还需要为button建立一个监听器，我们需要将编辑框的内容发送到 RecyclerView 上：
                    ①获取内容，将需要发送的消息添加到 List 当中去。
                    ②调用适配器的notifyItemInserted方法，通知有新的数据加入了，赶紧将这个数据加到 RecyclerView 上面去。
                    ③调用RecyclerView的scrollToPosition方法，以保证一定可以看的到最后发出的一条消息。*/
        send!!.setOnClickListener {
            var content: String = inputText!!.getText().toString()
            if (content != "") {

                Thread{
                    val uploadUrl: String = "http://${MainActivity.mMastersIp!!.split(":")[0]}:14515/api/gptUpload"

                    // Create a JSON object with the required fields
                    val json = JSONObject()
                    json.put("stdId", MainActivity.userID)
                    json.put("quest", content)

                    // Create the request body
                    val requestBody: RequestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
                    val request = Request.Builder()
                        .url(uploadUrl) // 设置请求的 URL
                        .post(requestBody)
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            // 请求失败的处理
                            e.printStackTrace()
                        }

                        override fun onResponse(call: Call, response: Response) {
                            // 请求成功的处理
                            val responseData = response.body?.string()
                            Log.d("ChatGptMainActivity", "onResponse: $responseData")
                        }
                    })

                }.start()









                msgList.add(Msg(content, Msg.TYPE_SEND))
                adapter!!.notifyItemInserted(msgList.size - 1)
                msgRecyclerView!!.scrollToPosition(msgList.size - 1)
                inputText!!.setText("")//清空输入框中的内容
                isChatting = true

                val jsonBody = JSONObject()

                Log.d("CHATGPT client", "question: $content")
                try {
                    // 创建messages数组，包含用户角色和问题内容
                    // 创建messages数组，包含用户角色和问题内容
                    val messagesArray = JSONArray()
                    val systemMessage = JSONObject()
                    systemMessage.put("role", "system")
                    systemMessage.put(
                        "content",
                        "You are ChatGPT, a large language model trained by OpenAI. Follow the user's instructions carefully. Respond using markdown.")
                    /*systemMessage.put(
                        "content",
                        /*"You are ChatGPT, a large language model trained by OpenAI. Follow the user's instructions carefully. Respond using markdown."*/
                        "你是一个机械狗的语言大模型。仔细遵循用户的指示，回答用户问题。\n" +
                                "功能、编号以及返回规则如下：\n" +
                                "   1. 帮我找什么：请返回1[物品名]\n" +
                                "   2.避开：\n" +
                                "   3.识别:请返回3\n" +
                                "   4.向前走:请返回4\n" +
                                "   5.向后走：请返回5\n" +
                                "   6.向左转：请返回6\n" +
                                "   7.向右转：请返回7\n" +
                                "   8.停下：请返回8\n" +
                                "   9.点头：请返回9\n" +
                                "   10.摇头：请返回10\n" +
                                "   8.停下：请返回8\n" /*+
                        "如果不包含以上需要实现的功能，那就返回对话就可以。"*/
                    )*/
                    val userMessage = JSONObject()
                    userMessage.put("role", "user")
                    userMessage.put("content", content)
                    messagesArray.put(systemMessage)
                    // messagesArray.put(MsgtoChangeToJSONArray(msgList))
                    messagesArray.put(userMessage)
                    // 构建完整的JSON请求体
                    jsonBody.put("model", ChatGptModel) // 模型选择
                    jsonBody.put("messages", messagesArray)
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
                //val body: RequestBody = RequestBody.create(jsonBody.toString(), JSON)
                val body: RequestBody = RequestBody.create(JSON,jsonBody.toString())

                val request: Request = Request.Builder()
                    .url(ChatGpturi) // 服务器地址
                    .addHeader("Authorization", "Bearer $ChatGptapiKey") // 填写你的key
                    .addHeader("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
                    .addHeader("Content-Type", "application/json")
                    .method("POST", body)
                    .build()
                client.newBuilder().readTimeout(50000, TimeUnit.MILLISECONDS).build().newCall(request)
                    .enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            // addResponse("响应失败" + e.message)
                            Log.d("CHATGPT client", "onFailure: ------" + e.message)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                var jsonObject: JSONObject? = null
                                val responseBody = response.body!!.string()
                                try {
                                    // 解析服务器响应的JSON数据
                                    jsonObject = JSONObject(responseBody)
                                    Log.d("CHATGPT client", "onResponse: ---------$responseBody")
                                    // 从JSON中提取需要的信息
                                    val choicesArray = jsonObject.getJSONArray("choices")
                                    val firstChoice = choicesArray.getJSONObject(0)
                                    val message = firstChoice.getJSONObject("message")
                                    val result = message.getString("content")
                                    Log.i("CHATGPT client", "RESULT: $result")
                                    msgList.add(Msg(result, Msg.TYPE_RECEIVED))
                                    runOnUiThread{
                                        adapter!!.notifyItemInserted(msgList.size - 1)
                                        msgRecyclerView!!.scrollToPosition(msgList.size - 1)
                                    }

                                } catch (e: JSONException) {
                                    throw java.lang.RuntimeException(e)
                                } finally {
                                    response.close()
                                }
                            } else {
                                // addResponse("响应失败" + response.body()!!.string())
                                Log.d(
                                    "CHATGPT client",
                                    "1onResponse: -------- ${response.body.toString()},respcode: ${response.code}, respmessage: ${response.message}, respReq: ${response.request}, respReq: ${response.body}"
                                )
                            }

                            isChatting = false
                        }
                    })

            }
            //                自定义一问一答
            /*if (msgList.size == 2) {
                msgList.add(new Msg ("What's your name?", Msg.TYPE_RECEIVED))
                adapter.notifyItemInserted(msgList.size() - 1)
                msgRecyclerView.scrollToPosition(msgList.size() - 1)
            }
            if (msgList.size == 4) {
                msgList.add(new Msg ("Nice to meet you,Bye!", Msg.TYPE_RECEIVED))
                adapter.notifyItemInserted(msgList.size() - 1)
                msgRecyclerView.scrollToPosition(msgList.size() - 1)
            }*/
        }

    
    }

    private fun getData(): ArrayList<Msg>{
        var list: ArrayList<Msg> = ArrayList()
        list.add(Msg("Hello",Msg.TYPE_RECEIVED))
        return list
    }

    override fun onPause() {
        super.onPause()

        // 保存状态

        var prefs: SharedPreferences = getSharedPreferences("ChatGptMainActivity", MODE_PRIVATE)
        var editor: SharedPreferences.Editor = prefs.edit()
        editor.putString("msg", enCodeMsgList(msgList))
        editor.apply()

//        println("onPause")
    }

    private fun enCodeMsgList(mL: ArrayList<Msg>): String {
        var str: String = ""
        for (msg in msgList) {
            str += msg.type.toString() + msg.content + "\n"
        }
//        println("enCodeMsgList: $str")
        return str
    }

    private fun deCodeMsgList(str: String): ArrayList<Msg> {
        val list: ArrayList<Msg> = ArrayList()
        val strList: List<String> = str.split("\n")
        for (s: String in strList) {
//            println("deCodeMsgList: $s")
            if (s.length > 1) {
                val type: Int = s.substring(0, 1).toInt()
                val content: String = s.substring(1)
                list.add(Msg(content, type))
            }
        }
        return list
    }

    private fun MsgtoChangeToJSONArray(msgList: ArrayList<Msg>): JSONArray {
        val messagesArray = JSONArray()
        for (msg in msgList) {
            val message = JSONObject()
            message.put("role", if (msg.type == Msg.TYPE_RECEIVED) "system" else "user")
            message.put("content", msg.content)
            messagesArray.put(message)
        }
        return messagesArray
    }




}
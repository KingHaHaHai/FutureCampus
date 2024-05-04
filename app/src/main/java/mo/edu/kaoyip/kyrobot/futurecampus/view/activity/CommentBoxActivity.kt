package mo.edu.kaoyip.kyrobot.futurecampus.view.activity

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import mo.edu.kaoyip.kyrobot.futurecampus.R
import okhttp3.Call
import okhttp3.Callback
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
import java.io.IOException
import java.util.concurrent.TimeUnit

class CommentBoxActivity: AppCompatActivity()  {

    var client: OkHttpClient = OkHttpClient()
    private var meditText: TextInputEditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment_box)

        var mSumitBtn = findViewById<Button>(R.id.buttonSubmit)
        meditText = findViewById<TextInputEditText>(R.id.editText)
        mSumitBtn.setOnClickListener {
            if (meditText!!.text.contentEquals("")){
                return@setOnClickListener
            }
//            Log.i("CommentBoxActivity", "onCreate: ${meditText.text}")
            gptClassification(meditText!!.text.toString())

        }
    }

    // GPT分类
    private var gptClassifResult: Int = -1
        set(value){
            field = value
            if (value == -1){
                return
            }
            Log.i("CommentBoxActivity", "gptClassifResult: $value")

            val client = OkHttpClient()
//            val requestBody = MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("stdID", MainActivity.userID!!)
//                .addFormDataPart("typeId", value.toString())
//                .addFormDataPart("comment", meditText!!.text.toString())
//                .build()
            val json = JSONObject()
            json.put("stdID", MainActivity.userID)
            json.put("typeID", value.toString())
            json.put("comment", meditText!!.text.toString())

            // Create the request body
            val requestBody: RequestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

            val request = Request.Builder()
                .url("http://${MainActivity.mMastersIp!!.split(":")[0]}:14515/commentCollect/") // 替换为实际的 URL
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 处理请求失败
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    // 处理请求成功
                    println(response.message)
                    println(response.code)
                    println(response.body)
                    runOnUiThread{ Toast.makeText(this@CommentBoxActivity, "提交成功", Toast.LENGTH_SHORT).show() }

                }
            })


        }
    private fun gptClassification(question: String) {
        if (question == ""){
            return
        }

        Thread{
            val jsonBody = JSONObject()
            try {
                // 创建messages数组，包含用户角色和问题内容
                // 创建messages数组，包含用户角色和问题内容
                val messagesArray = JSONArray()
                val systemMessage = JSONObject()
                systemMessage.put("role", "system")
                systemMessage.put(
                    "content",
                    "帮我将输入的内容进行分类，1.机械损坏。2.学习。3.人际关系。4.家庭问题。5.未来规划。6.学校霸凌。7.情绪问题。8.身体问题。9.其他。只需要输出分类的编号就行，例如属于机械损坏，那就输出 1")
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
                userMessage.put("content", question)
                messagesArray.put(systemMessage)
                // messagesArray.put(MsgtoChangeToJSONArray(msgList))
                messagesArray.put(userMessage)
                // 构建完整的JSON请求体
                jsonBody.put("model", "gpt-3.5-turbo") // 模型选择
                jsonBody.put("messages", messagesArray)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
            //val body: RequestBody = RequestBody.create(jsonBody.toString(), JSON)
            val body: RequestBody = RequestBody.create("application/json".toMediaType(),jsonBody.toString())

            val request: Request = Request.Builder()
                .url("https://api.chatanywhere.com.cn/v1/chat/completions") // 服务器地址
                .addHeader("Authorization", "Bearer sk-ipsxVlglOOTJsntesLFlghplLNCZFqgEwxncsfYBBtQWxtoP") // 填写你的key
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
                                val regex = Regex("\\d+")
                                val match = regex.find(result)
                                val number = match?.value?.toIntOrNull()
                                gptClassifResult = number!!
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
                    }
                })




        }.start()

    }
}
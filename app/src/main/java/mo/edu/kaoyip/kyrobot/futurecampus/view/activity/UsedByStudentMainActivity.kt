package mo.edu.kaoyip.kyrobot.futurecampus.view.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import mo.edu.kaoyip.kyrobot.futurecampus.R


class UsedByStudentMainActivity : AppCompatActivity() {

    private var main_right_drawer_layout: RelativeLayout? = null //右侧滑动栏
    private var drawerbar: ActionBarDrawerToggle? = null
    private var main_root: DrawerLayout? = null

    private var mWebView_main: WebView? = null
    private var mImageView_Gpt_Icon: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_used_by_student)

        //初始化控件
        initView()

    }

    @SuppressLint("SetJavaScriptEnabled", "RtlHardcoded")
    private fun initView() {
        main_root = findViewById<DrawerLayout>(R.id.root)
        //右边菜单
        main_right_drawer_layout =
            findViewById<RelativeLayout>(R.id.main_right_drawer_layout) // View

        //设置菜单内容之外其他区域的背景色
        main_root!!.setScrimColor(Color.TRANSPARENT)

        val mBtnOpenRightDrawerLayout = findViewById<Button>(R.id.btn_open_right_drawer_layout)
        mBtnOpenRightDrawerLayout.setOnClickListener{
            if (main_root!!.isDrawerOpen(main_right_drawer_layout!!)) {
                main_root!!.closeDrawer(main_right_drawer_layout!!)
            } else {
                main_root!!.openDrawer(main_right_drawer_layout!!)
            }
        }

        mWebView_main = findViewById<WebView>(R.id.web_view_main)
        mWebView_main!!.setWebViewClient(object : WebViewClient() {
            //使用webView打开网页，而不是Android默认浏览器，必须覆盖webView的WebViewClient对象
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                /*return super.shouldOverrideUrlLoading(view, url);*/
                view.loadUrl(url)
                return true
            }
        })
        mWebView_main!!.loadUrl("https://www.google.com/?hl=zh-TW")
        mWebView_main!!.settings.javaScriptEnabled = true


        mImageView_Gpt_Icon = findViewById<ImageView>(R.id.Iv_Gpt_Icon)
        mImageView_Gpt_Icon!!.setOnClickListener {
            // 设置Activity样式
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // 设置Activity位置和边界
            window.setGravity(Gravity.TOP or Gravity.LEFT)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )

            val intent = Intent(this, ChatGptMainActivity::class.java)
            startActivity(intent)

        }

    }





}
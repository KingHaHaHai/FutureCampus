package mo.edu.kaoyip.kyrobot.futurecampus.view.webbrowser

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import com.google.android.material.tabs.TabLayout
import mo.edu.kaoyip.kyrobot.futurecampus.R

class WebTabView constructor(context: Context,
                             attrs: AttributeSet? = null,
                             defStyleAttr: Int = 0): LinearLayout(context, attrs, defStyleAttr) {
    private var tabLayout: TabLayout? = null
    private var webView: WebView? = null
    private var addTabButton: ImageView? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_web_browser, this, true)
        orientation = VERTICAL

        tabLayout = findViewById(R.id.tab_layout)
        webView = findViewById(R.id.web_view)
        addTabButton = findViewById(R.id.add_tab_button)

        webView!!.webViewClient = WebViewClient()

        addTabButton!!.setOnClickListener {
            // 处理添加标签页的逻辑
            // 在这里执行您希望触发的操作，例如创建新的标签页或显示一个对话框等
        }

    }

    fun setTabs(tabItems: List<TabItem>) {
        tabLayout!!.removeAllTabs()

        for (tabItem in tabItems) {
            val tab = tabLayout!!.newTab()
            tab.text = tabItem.title
            tabItem.icon?.let { tab.setIcon(it) }
            tabLayout!!.addTab(tab)
        }
    }

    fun loadUrl(url: String) {
        webView!!.loadUrl(url)
    }

    data class TabItem(val title: String, @DrawableRes val icon: Int?)

}
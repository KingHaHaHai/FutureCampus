package mo.edu.kaoyip.kyrobot.futurecampus.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mo.edu.kaoyip.kyrobot.futurecampus.R
import mo.edu.kaoyip.kyrobot.futurecampus.view.activity.Msg


class MsgAdapter(private val list: List<Msg>) : RecyclerView.Adapter<MsgAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var leftLayout: LinearLayout
        var left_msg: TextView
        var rightLayout: LinearLayout
        var right_msg: TextView

        init {
            leftLayout = view.findViewById<LinearLayout>(R.id.left_layout)
            left_msg = view.findViewById<TextView>(R.id.left_msg)
            rightLayout = view.findViewById<LinearLayout>(R.id.right_layout)
            right_msg = view.findViewById<TextView>(R.id.right_msg)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.view_gpt_msg_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (content, type) = list[position]
        if (type == Msg.TYPE_RECEIVED) {
            //如果是收到的消息，则显示左边的消息布局，将右边的消息布局隐藏
            holder.leftLayout.visibility = View.VISIBLE
            holder.left_msg.text = content

            //注意此处隐藏右面的消息布局用的是 View.GONE
            holder.rightLayout.visibility = View.GONE
        } else if (type == Msg.TYPE_SEND) {
            //如果是发出的消息，则显示右边的消息布局，将左边的消息布局隐藏
            holder.rightLayout.visibility = View.VISIBLE
            holder.right_msg.text = content

            //同样使用View.GONE
            holder.leftLayout.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}


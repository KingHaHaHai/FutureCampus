package mo.edu.kaoyip.kyrobot.futurecampus.view.note

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mo.edu.kaoyip.kyrobot.futurecampus.R

class NotesAdapter(private var notes: List<Note>) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var noteContent: TextView = itemView.findViewById(R.id.editText)
        var noteTimeTitle: TextView = itemView.findViewById(R.id.noteTimeTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val currentNote = notes[position]
        holder.noteContent.text = currentNote.content
        holder.noteTimeTitle.text = currentNote.timeDate

        holder.noteContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val newText = s.toString()
                // 执行相应操作
                // 在这里处理每次修改后的操作
                currentNote.content = newText
            }
        })

    }

    override fun getItemCount() = notes.size
}

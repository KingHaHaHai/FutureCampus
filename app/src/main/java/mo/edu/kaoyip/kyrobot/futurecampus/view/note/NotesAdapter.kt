package mo.edu.kaoyip.kyrobot.futurecampus.view.note

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mo.edu.kaoyip.kyrobot.futurecampus.R

class NotesAdapter(private var notes: List<Note>) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var noteContent: TextView = itemView.findViewById(R.id.noteContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val currentNote = notes[position]
        holder.noteContent.text = currentNote.content
    }

    override fun getItemCount() = notes.size
}

package mo.edu.kaoyip.kyrobot.futurecampus.view.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mo.edu.kaoyip.kyrobot.futurecampus.R
import mo.edu.kaoyip.kyrobot.futurecampus.view.note.Note
import mo.edu.kaoyip.kyrobot.futurecampus.view.note.NotesAdapter


class NoteActivity: AppCompatActivity() {
    private lateinit var notesAdapter: NotesAdapter
    private var notesList = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        notesAdapter = NotesAdapter(notesList)
        recyclerView.adapter = notesAdapter

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            val noteContent = "New Note " + (notesList.size + 1)
            notesList.add(Note(noteContent))
            notesAdapter.notifyItemInserted(notesList.size - 1)
        }
    }

}
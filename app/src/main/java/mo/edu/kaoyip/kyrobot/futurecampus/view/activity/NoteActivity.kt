package mo.edu.kaoyip.kyrobot.futurecampus.view.activity

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mo.edu.kaoyip.kyrobot.futurecampus.R
import mo.edu.kaoyip.kyrobot.futurecampus.view.note.Note
import mo.edu.kaoyip.kyrobot.futurecampus.view.note.NotesAdapter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.IOException
import okio.Okio
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


@RequiresApi(Build.VERSION_CODES.O)
class NoteActivity : AppCompatActivity() {
    private lateinit var notesAdapter: NotesAdapter
    private val notesList = ArrayList<Note>()

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        //        Log.i("NoteActivity", "onCreate: ${this.getDatabasePath("noteDB")}")
        fun listFilesInDirectory(directoryPath: String) {
            val directory = File(directoryPath)
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                if (files != null) {
                    for (file in files) {
                        println(file.name)
                    }
                }
            }
        }
        val directoryPath = "/data/user/0/mo.edu.kaoyip.kyrobot.futurecampus/databases/"
        listFilesInDirectory(directoryPath)

//        val noteDBFile = File("/data/user/0/mo.edu.kaoyip.kyrobot.futurecampus/databases/noteDB")
//        val noteDBWalFile = File("/data/user/0/mo.edu.kaoyip.kyrobot.futurecampus/databases/noteDB-wal")
//        val noteDBShmFile = File("/data/user/0/mo.edu.kaoyip.kyrobot.futurecampus/databases/noteDB-shm")
//
//        noteDBFile.delete()
//        noteDBWalFile.delete()
//        noteDBShmFile.delete()

        var delList: ArrayList<String> = ArrayList()
        delList.add("/data/user/0/mo.edu.kaoyip.kyrobot.futurecampus/databases/noteDB")
        delList.add(this.getExternalFilesDir(null)!!.path + "noteDB")
        delList.add("/data/user/0/mo.edu.kaoyip.kyrobot.futurecampus/databases/noteDB-wal")
        delList.add(this.getExternalFilesDir(null)!!.path + "noteDB-wal")
        delList.add("/data/user/0/mo.edu.kaoyip.kyrobot.futurecampus/databases/noteDB-shm")
        delList.add(this.getExternalFilesDir(null)!!.path + "noteDB-shm")


        delList.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }

        }

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        notesAdapter = NotesAdapter(notesList)
        recyclerView.adapter = notesAdapter

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            val currentTime = getCurrentTime()
            val noteContent = "New Note ${notesList.size + 1}"
            notesList.add(Note(currentTime, noteContent))
            notesAdapter.notifyItemInserted(notesList.size - 1)
        }

        // 从服务器下载数据
        Thread{

            val client = OkHttpClient()
            var dbnameList = ArrayList<String>()
            dbnameList.add("")
            dbnameList.add("-wal")
            dbnameList.add("-shm")

            dbnameList.forEach { nameType ->
                val request = Request.Builder()
                    .url("http://${MainActivity.mMastersIp!!.split(":")[0]}:14515/getNote/?stdID=${MainActivity.userID}&name=${nameType}") // 替换为实际的 URL
                    .build()
                client.newBuilder().readTimeout(50000, TimeUnit.MILLISECONDS).build().newCall(request)
                    .enqueue(object : Callback {
                        override fun onFailure(call: Call, e: java.io.IOException) {
                            e.printStackTrace()
                        }

                        override fun onResponse(call: Call, response: Response) {
                            println(response.code)
                            if (response.code == 404) {
                                return
                            }
                            val downloadedFile: File = File(this@NoteActivity.getExternalFilesDir(null)!!.path + "/noteDB" + nameType)
                            val sink: BufferedSink = downloadedFile.sink().buffer()
                            sink.writeAll(response.body!!.source())
                            sink.close()

                            /*val database = DatabaseManager.getDatabase(this@NoteActivity)
                            val dao = database.noteDataDao()

                            val allData = dao.getAllData()*/
                            /*allData.forEach {
                                notesList.add(Note(it.time, it.noteData))


                            }
                            runOnUiThread {
                                notesAdapter.notifyDataSetChanged()
                            }*/
                            /*try {
                                var jsonObject = JSONObject(response.body!!.string())
                                Toast.makeText(this@NoteActivity, "远程获取笔记记录失败", Toast.LENGTH_SHORT).show()
                                return
                            }catch (e: JSONException){}
                            val outputFile = File(this@NoteActivity.getDatabasePath("noteDB").path) // 替换为实际的保存路径和文件名

                            response.body!!.let { responseBody ->
                                responseBody.source().use { source ->
                                    outputFile.sink().buffer().use { sink ->
                                        sink.writeAll(source)
                                    }
                                }
                            }*/

                        }

                    })
            }

            if (File(this@NoteActivity.getExternalFilesDir(null)!!.path + "/noteDB").exists()) {
                Log.i("note", "fileExists")
                copyFile(File(this.getExternalFilesDir(null), "noteDB"), this.getDatabasePath("noteDB"))

                copyFile(File(this.getExternalFilesDir(null), "noteDB-wal"), this.getDatabasePath("noteDB-wal"))
                copyFile(File(this.getExternalFilesDir(null), "noteDB-shm"), this.getDatabasePath("noteDB-shm"))
            }

            // 获取数据库实例
            val noteDatabase = NoteDatabase.getInstance(this@NoteActivity)

            // 使数据库无效并重新加载最新数据
            noteDatabase.close()
            val database = DatabaseManager.getDatabase(this@NoteActivity)
            val dao = database.noteDataDao()

            val allData = dao.getAllData()
            allData.forEach {
                Log.i("note", it.time)
                Log.i("note", it.noteData)
                notesList.add(Note(it.time, it.noteData))
            }
            runOnUiThread {
                notesAdapter.notifyDataSetChanged()
            }

        }.start()

        //TODO:: 从服务器下载数据
    }

    private fun getCurrentTime(): String {
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return currentTime.format(formatter)
    }

    override fun onDestroy() {
        Thread {
            saveDataToDatabase()

            copyFile(this.getDatabasePath("noteDB"), File(this.getExternalFilesDir(null), "noteDB"))

            copyFile(this.getDatabasePath("noteDB-wal"), File(this.getExternalFilesDir(null), "noteDB-wal"))

            copyFile(this.getDatabasePath("noteDB-shm"), File(this.getExternalFilesDir(null), "noteDB-shm"))

            val databaseName = "noteDB" // 数据库名称


            val databaseFile: File = this.getDatabasePath(databaseName)
            println("fileExists:${databaseFile.exists()}")

            fun copyFileToExternalStorage(context: Context, sourceFilePath: String, destinationFilePath: String): Boolean {
                try {
                    val sourceFile = File(sourceFilePath)
                    val destinationFile = File(context.getExternalFilesDir(null), destinationFilePath)

                    val inputStream = FileInputStream(sourceFile)
                    val outputStream = FileOutputStream(destinationFile)

                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
            }

            Log.i("note", copyFileToExternalStorage(this@NoteActivity, databaseFile.path, "noteDB").toString())

            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "noteDB", RequestBody.create("application/octet-stream".toMediaTypeOrNull(), File(this.getExternalFilesDir(null)!!.path + "/noteDB")))
                .addFormDataPart("fileWal", "noteDB-wal", RequestBody.create("application/octet-stream".toMediaTypeOrNull(), File(this.getExternalFilesDir(null)!!.path + "/noteDB-wal")))
                .addFormDataPart("fileShm", "noteDB-shm", RequestBody.create("application/octet-stream".toMediaTypeOrNull(), File(this.getExternalFilesDir(null)!!.path + "/noteDB-shm")))
                .addFormDataPart("stdID", MainActivity.userID!!)
                .build()

            val request = Request.Builder()
                .url("http://${MainActivity.mMastersIp!!.split(":")[0]}:14515/uploadNote/") // 替换为实际的 URL
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
                }
            })

        }.start()

        super.onDestroy()
    }

    private fun saveDataToDatabase() {
        val database = DatabaseManager.getDatabase(this)
        val dao = database.noteDataDao()
        dao.deleteAllData()
        database.runInTransaction {
            notesList.forEach {
                val data = noteData(time = it.timeDate, noteData = it.content)
                dao.insert(data)
            }
        }

    }

    @Throws(IOException::class)
    private fun copyFile(sourceFile: File, destFile: File) {
        if (!sourceFile.exists()) {
            return
        }
        if (!destFile.exists()) {
            destFile.createNewFile()
        }
        var sourceChannel: FileChannel? = null
        var destChannel: FileChannel? = null
        try {
            sourceChannel = FileInputStream(sourceFile).channel
            destChannel = FileOutputStream(destFile).channel
            destChannel!!.transferFrom(sourceChannel, 0, sourceChannel.size())
        } finally {
            sourceChannel?.close()
            destChannel?.close()
        }
    }



}

@Entity(tableName = "noteDB")
data class noteData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val time: String,
    val noteData: String
)

@Dao
interface noteDataDao {
    @Insert
    fun insert(data: noteData)

    @Query("SELECT * FROM noteDB")
    fun getAllData(): List<noteData>

    @Query("DELETE FROM noteDB")
    fun deleteAllData()
}

@Database(entities = [noteData::class], version = 1)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDataDao(): noteDataDao

    companion object {
        private var instance: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase {
            if (instance == null) {
                synchronized(NoteDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        NoteDatabase::class.java,
                        "noteDB"
                    ).build()
                }
            }
            return instance!!
        }
    }


}

object DatabaseManager {
    private var database: NoteDatabase? = null

    fun getDatabase(context: Context): NoteDatabase {
        if (database == null) {
            database = NoteDatabase.getInstance(context)
        }
        return database!!
    }
}



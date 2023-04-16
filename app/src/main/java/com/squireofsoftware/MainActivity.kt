package com.squireofsoftware

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.squireofsoftware.ui.theme.ImgTrashCanTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImgTrashCanTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    TextButton(onClick = { readPhotosFromDevice() }) {
                        Text(text = "Upload and delete photos off phone")
                    }
                }
            }
        }
        Log.d("Test", "DONE!!")
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted, read the photos
                readPhotosFromDevice()
            } else {
                // Permission is denied, show a message or do something else
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun readPhotosFromDevice() {
        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            requestPermissionLauncher.launch(READ_EXTERNAL_STORAGE)
            return
        }
        // Just discovered that with the new API changes you cannot easily access the external storage from android
//        if (ContextCompat.checkSelfPermission(this, MANAGE_EXTERNAL_STORAGE)
//            != PackageManager.PERMISSION_GRANTED) {
//            // Permission is not granted, request it
//            requestPermissionLauncher.launch(MANAGE_EXTERNAL_STORAGE)
//            return
//        }
        // Permission is already granted, read the photos
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATA
        )

        val selection = "${MediaStore.Images.Media.DATE_TAKEN} > 0"
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val query = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, null, sortOrder)
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val imagePath = cursor.getString(pathColumn)
                Log.d("Test", "$idColumn, $imagePath")

                val context = this.applicationContext
                val fileUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idColumn)
                )

                val file = copyFileFromUri(context = context, uri = fileUri)

                // Display a text popup of the image path
                Toast.makeText(this, "Image path: $imagePath", Toast.LENGTH_SHORT).show()
                if (file != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val fileHash = getFileChecksum(file)
                        uploadFile(file = file, fileHash = fileHash)
                    }
                } else {
                    Log.d("Test", "This is a weird file?")
                }
            }
            Toast.makeText(this, "We are done!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)!!

        val outputFile = File(context.cacheDir, "temp_file")
        FileOutputStream(outputFile).use { outputStream ->
            val buffer = ByteArray(4 * 1024) // buffer size
            var read: Int
            outputStream.use { outputStream ->
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
        }

        return outputFile
    }

    private fun getFileChecksum(file: File): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .fold("") { str, it -> str + "%02x".format(it) }
        Log.d("Test", "hash for ${file.absolutePath} at: $messageDigest")
        return messageDigest
    }

    private fun uploadFile(file: File, fileHash: String) {
        val context = this.applicationContext
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .addFormDataPart("file_hash", fileHash)
            .build()

        val request = Request.Builder()
            // set this to the webserver of your choice
            .url("http://localhost:8002/images")
            .post(requestBody)
            .header("Connection", "close")
            .build()

        Log.d("Test", "sending the file now")

        client.newCall(request).execute().use {
            if (it.isSuccessful) {
                Log.d("Test", "upload was complete, going to see if we can delete it")
                val payload = it.body?.string()!!
                Log.d("Test", "payload: $payload")
                // seems like json deserialisation is having some serious problems
                // i ended up keeping the interface simple, just return the hashes
                // so its easier to deserialise
                val result: Array<String> = Json.decodeFromString(payload)
                Log.d("Test", "decoded!!")

                if (result[0] == fileHash) {
                    Log.d("Test", "deleting the file now")
                    deleteFile(context = context, file = file)
                    Log.d("Test", "we are done?!")
                } else {
                    Log.d("Test", "The checksum does not match! ${result[0]} != $fileHash")
                }
            } else {
                Log.d("Test", "Image failed to get uploaded")
            }
        }
    }

    private fun deleteFile(context: Context, file: File): Boolean {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Images.Media.DATA + "=?"
        val args = arrayOf(file.absolutePath)
        Log.d("Test", "sending delete request now")
        val rows = context.contentResolver.delete(uri, selection, args)
        Log.d("Test", "delete is done")

        return rows > 0
    }
}

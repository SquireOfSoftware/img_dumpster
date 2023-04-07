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
import com.fasterxml.jackson.databind.ObjectMapper
import com.squireofsoftware.ui.theme.ImgTrashCanTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.experimental.and

class MainActivity : ComponentActivity() {
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
                        val response = uploadFile(file = file)
                        val checksum = getFileChecksum(file)
                        if (response.isSuccessful) {
                            val mapper = ObjectMapper()
                            val result: UploadResponse =
                                mapper.readValue(response.body?.bytes(), UploadResponse::class.java)

                            if (result.checksum == checksum) {
                                deleteFile(context = context, file = file)
                            } else {
                                Log.d("Test", "The checksum does not match!")
                            }
                        } else {
                            Log.d("Test", "Image failed to get uploaded")
                        }
                    }
                } else {
                    Log.d("Test", "This is a weird file?")
                }
            }
            cursor.close()
        }
    }

    private fun copyFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        inputStream ?: return null

        val outputFile = File(context.cacheDir, "temp_file")
        FileOutputStream(outputFile).use { outputStream ->
            val buffer = ByteArray(4 * 1024) // buffer size
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
        }

        return outputFile
    }

    private fun getFileChecksum(file: File): String {
        val messageDigest = MessageDigest.getInstance("MD5")
        val fileInputStream = file.inputStream()
        val byteArray = ByteArray(1024)
        var bytesCount: Int
        while (fileInputStream.read(byteArray).also { bytesCount = it } != -1) {
            messageDigest.update(byteArray, 0, bytesCount)
        }
        fileInputStream.close()

        val bytes = messageDigest.digest()
        val stringBuilder = StringBuilder()
        for (i in bytes.indices) {
            stringBuilder.append(((bytes[i] and 0xff.toByte()) + 0x100).toString(16).substring(1))
        }
        return stringBuilder.toString()
    }

    private suspend fun uploadFile(file: File): Response {
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("http://192.168.1.110:8002/file")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) { client.newCall(request).execute() }
    }

    private suspend fun deleteFile(context: Context, file: File): Boolean {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Images.Media.DATA + "=?"
        val args = arrayOf(file.absolutePath)
        val rows = withContext(Dispatchers.IO) {context.contentResolver.delete(uri, selection, args) }

        return rows > 0
    }
}

class UploadResponse(val checksum: String, val filePath: String)
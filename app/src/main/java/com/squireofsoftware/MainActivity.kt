package com.squireofsoftware

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.squireofsoftware.ui.theme.ImgTrashCanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImgTrashCanTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {

                }
            }
        }

        readPhotosFromDevice()
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
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                // Display a text popup of the image path
                Toast.makeText(this, "Image path: $imagePath", Toast.LENGTH_SHORT).show()
            }
            cursor.close()
        }
    }

}

@Preview(showBackground = false,
    name = "Test",
    showSystemUi = true)
@Composable
fun TrashCanPreview() {
    ImgTrashCanTheme {

    }
}
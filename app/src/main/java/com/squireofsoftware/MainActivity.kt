package com.squireofsoftware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.squireofsoftware.ui.theme.ImgTrashCanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImgTrashCanTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    CurrentDirectory("Mum", from = "John")
                }
            }
        }
    }
}

@Composable
fun CurrentDirectory(message: String, from: String = "", modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Hi $message",
            fontSize = 36.sp
        )
        Text(
            text = "~ $from",
            fontSize = 24.sp
        )
    }
}

@Preview(showBackground = false,
    name = "Test",
    showSystemUi = true)
@Composable
fun TrashCanPreview() {
    ImgTrashCanTheme {
        CurrentDirectory(message = "Mum", from = "John")
    }
}
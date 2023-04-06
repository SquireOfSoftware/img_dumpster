package com.squireofsoftware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.squireofsoftware.ui.theme.ImgTrashCanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImgTrashCanTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    Greeting("Mum")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hi $name!!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ImgTrashCanTheme {
        Greeting("Android")
    }
}
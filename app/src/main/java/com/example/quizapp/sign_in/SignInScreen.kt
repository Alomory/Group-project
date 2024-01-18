package com.example.quizapp.sign_in

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quizapp.R

@Composable
fun SignInScreen(
    state: SignInState,
    onSignInClick: () -> Unit
) {
    val context = LocalContext.current

    Box {
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = "background image for the app",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
            alpha = 0.7F


            )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .shadow(2.dp)
                .background(color = Color.LightGray.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 100.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            Text(
                text = "Unlock the power of learning with our quiz app!",
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 50.sp
            )
            Spacer(modifier = Modifier.height(15.dp))
            Image(

                painter = painterResource(id = R.drawable.small_image),
                contentDescription = "background image for the app",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(200.dp).height(200.dp),
                alpha = 0.9F
            )
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = "Ignite your curiosity and sign in for a rewarding journey!",
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 50.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Your existing UI content
                Button(onClick = onSignInClick) {
                    Text(text = "Sign in")
                }
            }
        }

    }
    LaunchedEffect(key1 = state.signInError) {
        state.signInError?.let { error ->
            Toast.makeText(
                context,
                error,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

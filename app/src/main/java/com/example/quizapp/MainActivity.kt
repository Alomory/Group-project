@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.quizapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.quizapp.profile.ProfileScreen
import com.google.android.gms.auth.api.identity.Identity
import com.example.quizapp.sign_in.GoogleAuthUiClient
import com.example.quizapp.sign_in.SignInScreen
import com.example.quizapp.sign_in.SignInViewModel
import com.example.quizapp.sign_in.UserData
import com.example.quizapp.ui.theme.QuizAppTheme
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "sign_in") {
                        composable("sign_in") {
                            val viewModel = viewModel<SignInViewModel>()
                            val state by viewModel.state.collectAsStateWithLifecycle()
                            LaunchedEffect(key1 = Unit) {
                                if (googleAuthUiClient.getSignedInUser() != null) {
                                    navController.navigate("home")
                                }
                            }

                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartIntentSenderForResult(),
                                onResult = { result ->
                                    if (result.resultCode == RESULT_OK) {
                                        lifecycleScope.launch {
                                            val signInResult = googleAuthUiClient.signInWithIntent(
                                                intent = result.data ?: return@launch
                                            )
                                            viewModel.onSignInResult(signInResult)
                                        }
                                    }
                                }
                            )

                            LaunchedEffect(key1 = state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Sign in successful",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    navController.navigate("home")
                                    viewModel.resetState()
                                }
                            }

                            SignInScreen(
                                state = state,
                                onSignInClick = {
                                    lifecycleScope.launch {
                                        val signInIntentSender = googleAuthUiClient.signIn()
                                        launcher.launch(
                                            IntentSenderRequest.Builder(
                                                signInIntentSender ?: return@launch
                                            ).build()
                                        )
                                    }
                                }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                onGoBack= {navController.popBackStack()},
                                userData = googleAuthUiClient.getSignedInUser(),
                                onSignOut = {
                                    lifecycleScope.launch {
                                        googleAuthUiClient.signOut()
                                        Toast.makeText(
                                            applicationContext,
                                            "Signed out",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        navController.navigate("sign_in")
                                    }
                                }
                            )
                        }
                        composable("home") {
                            HomePage(
                                userData = googleAuthUiClient.getSignedInUser(),
                                navController = navController,

                            )
                        }
                        composable("finalScore?score={score}",
                            arguments = listOf(navArgument("score") { type = NavType.IntType })
                        ) {
                            val score = it.arguments?.getInt("score") ?: 0
                            FinalScoreScreen(
                                userData = googleAuthUiClient.getSignedInUser(),
                                navController = navController,
                                finalScore = score,
                                )
                        }
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    userData:UserData?,
    navController: NavHostController,
    modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Quiz") },
                navigationIcon= {
                    ElevatedButton(onClick = { navController.navigate("profile") }) {
                        Text(userData?.username.toString())
                    }
                }

            )
        }
    ) { innerPadding ->
        QuizView(
            userData =userData,
            navController = navController,
            modifier = modifier.padding(innerPadding)
        )
    }
}
@Composable
fun FinalScoreScreen(
    userData: UserData?,
    navController: NavHostController,
    finalScore: Int, modifier: Modifier = Modifier) {
    //TODO: CHANGE THE FIREBASE URL OR IF YOU WANT TO USE MINE THEN FINE
    val database = Firebase.database("FirebaseUrl")
    val myRef = database.getReference(userData?.userId.toString())
    var congrats by remember { mutableStateOf("Congratulations!") }
    var textFinalScore by remember { mutableStateOf("Your final score is") }
    Column(
        modifier = modifier
            .padding(24.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {


        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val fromFirebase = snapshot.value // This value is of type Any

                    if (fromFirebase is Long) {
                        // If the value is a Long, you can convert it to Int
                        val intValue = fromFirebase.toLong()

                        // Now you can compare it with the correct answers or perform other actions
                        if (intValue > finalScore) {
                            Log.e("firebase", "Value in firebase is less than correct")
                            congrats = "Shame on you!"
                            textFinalScore = "You don't feel shy getting"

                        } else {
                            myRef.setValue(finalScore)
                            Log.e("firebase", "Value in firebase is greater than correct")
                            congrats = "Congratulations!"
                            textFinalScore = "Your final score is"
                        }
                    } else {
                        Log.e("firebase", "Data type is not long")
                    }
                } else {
                    Log.e("firebase", "There is no data in firebase yet")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                Log.e("firebase", "Error getting data", error.toException())
            }
        })
        Text(congrats, fontSize = 26.sp)
        Text("$textFinalScore $finalScore", fontSize = 24.sp)

        Button(onClick = {
            navController.navigate("home")

        }) {
            Text(text = "Try Again..!")
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    QuizAppTheme { HomePage() }
//}
//val databaseRef = Firebase.database
//val ref = databaseRef.getReference("message")
//ref.setValue("Hello")

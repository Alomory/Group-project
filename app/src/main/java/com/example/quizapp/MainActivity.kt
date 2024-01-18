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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import org.json.JSONObject
import kotlin.math.log


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
    finalScore: Int,
    modifier: Modifier = Modifier
) {
    val database = Firebase.database("https://quizapp-3ee94-default-rtdb.asia-southeast1.firebasedatabase.app/")
    val myRef = database.getReference("user-score")

    var congrats by remember { mutableStateOf("Congratulations!") }
    var textFinalScore by remember { mutableStateOf("Your final score is") }
    var topUsers by remember { mutableStateOf(mutableListOf<UserData>()) }

    // Use orderByChild("score") and limitToLast(3) to get the top 3 scores
    // Fetch all data and then sort it on the client side
    LaunchedEffect(topUsers){
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allUsers = mutableListOf<UserData>()

                for (childSnapshot in snapshot.children) {
                    val userId = childSnapshot.key
                    val username = childSnapshot.child("username").getValue(String::class.java)
                    val score = childSnapshot.child("score").getValue(Long::class.java)

                    if (userId != null && username != null && score != null) {
                        val userData = UserData(userId, username, score.toString())
                        allUsers.add(userData)
                    }
                }

                // Sort allUsers based on score in descending order
                allUsers.sortByDescending { it.profilePictureUrl}

                // Take the top 3 users
                topUsers = allUsers.take(3).toMutableList()

                // Now, topUsers list contains the top 3 users with the highest scores
                // You can use this list to display the high scores in your UI
                for (user in topUsers) {
                    Log.d("TopUser", "${user.username} - Score: ${user.profilePictureUrl}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                Log.e("firebase", "Error getting data", error.toException())
            }
        })
    }

    LaunchedEffect(userData, finalScore) {
        myRef.child(userData?.userId.toString()).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val fromFirebase = snapshot.value

                    when (fromFirebase) {
                        is Map<*, *> -> {
                            val username = fromFirebase["username"]
                            val score = fromFirebase["score"] as Long

                            if (score < finalScore) {
                                val updateMap = mapOf(
                                    "username" to userData?.username.toString(),
                                    "score" to finalScore
                                )
                                myRef.child(userData?.userId.toString()).updateChildren(updateMap).addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        congrats = "Congratulations ${username.toString().split(" ")[0]}!"
                                        textFinalScore =
                                            "Your Highest score is"
                                    } else {
                                        // Handle update failure
                                        Log.e("firebase", "Failed to update data: ${it.exception}")
                                    }
                                }
                            } else {
                                congrats = "You need to improve ${username.toString().split(" ")[0]}!"
                                textFinalScore = "Try again after one year"
                            }
                        }
                        else -> {
                            Log.e("firebase", "Unknown data type: ${fromFirebase?.javaClass}")
                        }
                    }
                } else {
                    val updateMap = mapOf(
                        "username" to userData?.username.toString(),
                        "score" to finalScore
                    )
                    myRef.child(userData?.userId.toString()).updateChildren(updateMap).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Log.e("firebase", "Value set successfully")
                        } else {
                            // Handle set value failure
                            Log.e("firebase", "Failed to set value: ${it.exception}")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                Log.e("firebase", "Error getting data", error.toException())
            }
        })
    }

    Column(
        modifier = modifier
            .padding(24.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Top 3 Heights Score", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Divider()
        Spacer(modifier = Modifier.height(20.dp))
        LazyColumn {
            items(topUsers) { users ->
                Text(
                    text = "${users.username.toString()}: ${users.profilePictureUrl.toString()}",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Divider()
        Spacer(modifier = Modifier.height(20.dp))
        Text(congrats, fontSize = 20.sp)
        Text("$textFinalScore $finalScore", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {
            navController.navigate("home")
        }) {
            Text(text = "Try Again..!")
        }
    }
}

//@Composable
//fun FinalScoreScreen(
//    userData: UserData?,
//    navController: NavHostController,
//    finalScore: Int, modifier: Modifier = Modifier) {
//    val database = Firebase.database("https://quizapp-3ee94-default-rtdb.asia-southeast1.firebasedatabase.app/")
//    val myRef = database.getReference("user-score")
//
//    var congrats by remember { mutableStateOf("Congratulations!") }
//    var textFinalScore by remember { mutableStateOf("Your final score is") }
//    Column(
//        modifier = modifier
//            .padding(24.dp)
//            .fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center,
//    ) {
//
////            Text(text = "Heights Score")
////            Divider()
////            Spacer(modifier = Modifier.height(20.dp))
//
//
//
//        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.exists()) {
//                    val userRef = myRef.child(userData?.userId.toString())
//                    val fromFirebase = snapshot.child(userRef.key.toString()).value // This value is of type Any
//                    when (fromFirebase) {
//                        is Map<*, *> -> {
//                            // Handle map case if needed
//                            // For example, you can check map entries
//                            val username = fromFirebase["username"]
//                            val score = fromFirebase["score"] as Long
//
//                            if (score < finalScore) {
//                                val updateMap = mapOf(
//                                    "username" to userData?.username.toString(),
//                                    "score" to finalScore
//                                )
//                                userRef.updateChildren(updateMap).addOnCompleteListener {
//                                    if (it.isSuccessful) {
//                                        Log.e("firebase", "Value($score) in firebase is greater than correct")
//                                        congrats = "Congratulations!"
//                                        textFinalScore =
//                                            "${username.toString().split(" ")} you got a new score..\nYour final score is"
//                                    } else {
//                                        // Handle update failure
//                                        Log.e("firebase", "Failed to update data: ${it.exception}")
//                                    }
//                                }
//                            } else {
//                                Log.e(
//                                    "firebase",
//                                    "Value ($score) in ($username) firebase is less than correct"
//                                )
//                                congrats = "Shame on you ${username.toString().split(" ")[0]}!"
//                                textFinalScore = "You don't feel shy getting"
//                            }
//                        }
//                        else -> {
//                            Log.e("firebase", "Unknown data type: ${fromFirebase?.javaClass}")
//                        }
//                    }
//                } else {
//                    // If the snapshot doesn't exist, set a new value
//                    val updateMap = mapOf(
//                        "username" to userData?.username.toString(),
//                        "score" to finalScore
//                    )
//                    myRef.updateChildren(updateMap).addOnCompleteListener {
//                        if (it.isSuccessful) {
//                            Log.e("firebase", "Value set successfully")
//                        } else {
//                            // Handle set value failure
//                            Log.e("firebase", "Failed to set value: ${it.exception}")
//                        }
//                    }
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                // Handle errors
//                Log.e("firebase", "Error getting data", error.toException())
//            }
//        })
//        Text(congrats, fontSize = 26.sp)
//        Text("$textFinalScore $finalScore", fontSize = 24.sp)
//
//        Button(onClick = {
//            navController.navigate("home")
//
//        }) {
//            Text(text = "Try Again..!")
//        }
//    }
//}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    QuizAppTheme { HomePage() }
//}
//val databaseRef = Firebase.database
//val ref = databaseRef.getReference("message")
//ref.setValue("Hello")

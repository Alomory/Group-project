package com.example.quizapp

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.navigation.NavHostController
import com.example.quizapp.sign_in.UserData
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread
import com.google.firebase.database.database


@Composable
fun QuizView(
    userData: UserData?,
    navController: NavHostController,
    modifier: Modifier = Modifier) {
    var data by remember { mutableStateOf(JSONObject()) }
    var correct by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    val url = "https://opentdb.com/api.php?amount=1"
    fun getQuestion() {
        thread {
            do {
                try {
                    data = JSONObject(URL(url).readText())
                } catch (e: Exception) {
                    Thread.sleep(3_000)
                }
            } while (data.length() == 0)
        }

    }


    fun htmlString(of: String): String {
        return HtmlCompat.fromHtml(of, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    fun capitalizedValue(data: JSONObject, key: String): String {
        return htmlString(data[key] as String).replaceFirstChar { it.uppercase() }
    }

    getQuestion()

    Column(
        modifier = modifier
            .padding(24.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
//        item { Text(data.toString()) }
        if (data.length() == 0) {
            Spacer(modifier = modifier.weight(1.0f))
            CircularProgressIndicator(
                modifier = Modifier
                    .width(64.dp)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.secondary,
            )
            Text("Getting Next Question...")
        } else if (data["response_code"] == 0) {
//            parse background story
            val question = data.getJSONArray("results").getJSONObject(0)
            Text(
                "Difficulty: ${capitalizedValue(question, "difficulty")}",
                fontSize = 24.sp,
            )
            Text("Category: ${capitalizedValue(question, "category")}")
            Spacer(modifier = modifier.weight(0.5f))
            Text(
                htmlString(question.getString("question")),
                fontSize = 24.sp,
                lineHeight = 32.sp,
            )
            Spacer(modifier = modifier.weight(1.0f))

//            parse answers
            val wrongAnswers = question.getJSONArray("incorrect_answers")
            val answers: ArrayList<String> = arrayListOf()
            val correctAnswer = question.getString("correct_answer")
            answers.add(htmlString(correctAnswer))
            for (i in 0 until wrongAnswers.length()) {
                answers.add(htmlString(wrongAnswers.getString(i)))
            }
            answers.shuffle()

//            generate answer buttons
            LazyColumn {
                items(answers) { answer ->
                    Button(
                        onClick = {
                            if (answer == correctAnswer) {
                                correct++
                            }
                            total++
                            data = JSONObject()
                            getQuestion()
                            Log.d("to", "getting")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(answer) }
                }
            }
        }
        Spacer(modifier = modifier.weight(1.0f))
        Row(horizontalArrangement = Arrangement.Center) {
            Text("Score: $correct / $total", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    isFinished = true
                    // Navigate to the final score screen
                    navController.navigate("finalScore?score=${correct}")
                },
            ) {
                Text("Finish")
            }
        }
    }

}
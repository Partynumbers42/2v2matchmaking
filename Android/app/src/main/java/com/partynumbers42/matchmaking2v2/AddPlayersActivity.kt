package com.partynumbers42.matchmaking2v2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.partynumbers42.matchmaking2v2.models.ActivitiesResponse
import com.partynumbers42.matchmaking2v2.models.Activity
import com.partynumbers42.matchmaking2v2.models.Player
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Call
import okhttp3.Callback

import java.io.IOException
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.collections.forEach

private const val TAG = "AddPlayersActivity"

class AddPlayersActivity : AppCompatActivity() {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listAdapterActivities = moshi.adapter<List<ActivitiesResponse>>(
        Types.newParameterizedType(List::class.java, ActivitiesResponse::class.java)
    )

    private var nonParticipants: MutableList<Player> = AppState.currentClubMembers

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_players)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        showParticipatingMembers(AppState.currentParticipants)
        showNonParticipatingMembers(nonParticipants)

        val b_updateactivities = findViewById<Button>(R.id.b_updateactivities)
        b_updateactivities.setOnClickListener {
            handlePullParticipants()
        }

    }

    private fun handlePullParticipants() {
        if (AppState.signedInClub == null) {
            Toast.makeText(this, "No signed in club, please wait", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "No signed in club")
            return
        }

        // Add username and password check

        Log.i(TAG,"Starting updating database ...")

        pullActivitiesSportmember { activities ->

            Toast.makeText(this, "Found ${activities.size} activities on Sportmember", Toast.LENGTH_SHORT).show()

            val activity = activities.firstOrNull()
            Log.i(TAG, "Next activity: ${activity?.name}")
            //val activityName = activity?.name
            val players = activity?.players

            AppState.currentParticipants = players as MutableList<Player>
            nonParticipants = AppState.currentClubMembers.filter { !AppState.currentParticipants.contains(it) }.toMutableList()

            for (player in AppState.currentParticipants){
                Log.i(TAG, "Found participant: ${player.firstname} ${player.lastname}")
            }

            runOnUiThread {
                showParticipatingMembers(AppState.currentParticipants)
                showNonParticipatingMembers(nonParticipants)
            }

        }

    }

    private fun showParticipatingMembers(currentParticipants: MutableList<Player>) {

        val rv_participants = findViewById<RecyclerView>(R.id.rv_participants)

        rv_participants.adapter = SmallPlayersAdapter(this, currentParticipants) // change the whole SmallPlayerAdapter into a button???
        rv_participants.layoutManager = GridLayoutManager(this, 3)

    }

    private fun showNonParticipatingMembers(nonParticipants: MutableList<Player>) {

        val rv_members = findViewById<RecyclerView>(R.id.rv_members)

        rv_members.adapter = SmallPlayersAdapter(this, nonParticipants)
        rv_members.layoutManager = GridLayoutManager(this,3)

    }



    private fun pullActivitiesSportmember(onComplete: (List<Activity>) -> Unit) {

        val sportmember_id = AppState.signedInClubSportmemberId
        val username = resources.getString(R.string.sportmember_user) // update to user entry
        val password = resources.getString(R.string.sportmember_password) //update to user entry

        var apiActivtiesResult: List<ActivitiesResponse>? = null

        val client = OkHttpClient()
        val credential = Credentials.basic(username, password);

        val requestActivities = Request.Builder()
            .url("https://api.holdsport.dk/v1/teams/" + sportmember_id + "/activities")
            .header("Accept", "application/json")
            .header("Authorization", credential)
            .build()

        client.newCall(requestActivities).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "onFailure", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val bodyString = response.body?.string()
                    //Log.i(TAG, bodyString.toString())
                    apiActivtiesResult = listAdapterActivities.fromJson(bodyString.toString())

                    runOnUiThread {

                        val activitiesCurrentCall = mutableListOf<Activity>()
                        apiActivtiesResult?.forEach { activity ->
                            val playersCurrentCall = mutableListOf<Player>()
                            activity.players.forEach { player ->
                                val member = AppState.currentClubMembers.find { it.sportmemberUserId == player.userId }
                                playersCurrentCall.add(member as Player)
                            }
                            activitiesCurrentCall.add(Activity(activity.name,activity.starttime,activity.endtime,playersCurrentCall))
                            Log.i(TAG, "Found activity: ${activity.name} (${activity.starttime} ${activity.endtime}): ${playersCurrentCall.size} players ")
                        }
                        onComplete(activitiesCurrentCall)

                    }

                }
            }
        })

    }

}

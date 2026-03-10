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
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.partynumbers42.matchmaking2v2.models.Player

private const val TAG = "AddPlayersActivity"

class AddPlayersActivity : AppCompatActivity() {
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

        SportmemberRepository.fetchActivities(onComplete = { activities ->
            runOnUiThread {
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

                showParticipatingMembers(AppState.currentParticipants)
                showNonParticipatingMembers(nonParticipants)
            }
        },
            onError = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Failed to fetch players from Sportmember", Toast.LENGTH_SHORT).show()
                }
            })

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

}

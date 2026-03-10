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
            //handlePullParticipants()
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

}

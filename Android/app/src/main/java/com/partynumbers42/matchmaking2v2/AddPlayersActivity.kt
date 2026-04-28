package com.partynumbers42.matchmaking2v2

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "AddPlayersActivity"

class AddPlayersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_players)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        showParticipatingMembers()
        showNonParticipatingMembers()

        val button = findViewById<Button>(R.id.button)
        if (!AppState.usesSportmember) {
            button.visibility = View.GONE
        }
        button.setOnClickListener {
            SportmemberRepository.fetchParticipants { success ->
                if (!success) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to load participants", Toast.LENGTH_SHORT).show()
                    }
                    return@fetchParticipants
                }
                runOnUiThread {
                    showParticipatingMembers()
                    showNonParticipatingMembers()
                }

            }
        }

    }

    private fun showParticipatingMembers() {

        val rv_participants = findViewById<RecyclerView>(R.id.rv_participants)
        val participantsAdapter = SmallPlayersAdapter(this, AppState.currentParticipants, true) { player ->
            AppState.currentParticipants.remove(player)
            AppState.currentParticipants.sortWith(compareBy({ it.firstname }, { it.lastname }))
            AppState.nonParticipants.add(player)
            AppState.nonParticipants.sortWith(compareBy({ it.firstname }, { it.lastname }))
            showParticipatingMembers()
            showNonParticipatingMembers()
        }

        rv_participants.adapter = participantsAdapter
        rv_participants.layoutManager = GridLayoutManager(this, 4)

    }

    private fun showNonParticipatingMembers() {

        val rv_members = findViewById<RecyclerView>(R.id.rv_members)
        val nonParticipantsAdapter = SmallPlayersAdapter(this, AppState.nonParticipants, true) { player ->
            AppState.nonParticipants.remove(player)
            AppState.nonParticipants.sortWith(compareBy({ it.firstname }, { it.lastname }))
            AppState.currentParticipants.add(player)
            AppState.currentParticipants.sortWith(compareBy({ it.firstname }, { it.lastname }))
            showParticipatingMembers()
            showNonParticipatingMembers()
        }

        rv_members.adapter = nonParticipantsAdapter
        rv_members.layoutManager = GridLayoutManager(this,4)

    }

}

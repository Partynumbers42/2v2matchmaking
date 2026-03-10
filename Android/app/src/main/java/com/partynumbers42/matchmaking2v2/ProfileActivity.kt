package com.partynumbers42.matchmaking2v2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.partynumbers42.matchmaking2v2.models.Club
import com.partynumbers42.matchmaking2v2.models.Player

private const val TAG = "ProfileActivity"

class ProfileActivity : AppCompatActivity() {

    private lateinit var currentMembersIds: MutableList<Long>


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        showMembers(AppState.currentClubMembers)
        showClubInfo(AppState.signedInClub)

        //currentMembers = mutableListOf<Player>()
        currentMembersIds = mutableListOf<Long>()
        AppState.currentClubMembers.forEach { member ->
            currentMembersIds.add(member.sportmemberUserId)
        }

        val b_updateplayers = findViewById<Button>(R.id.b_updateplayers)
        b_updateplayers.setOnClickListener {
            handleUpdatePlayers()
        }

        val b_updatelevels = findViewById<Button>(R.id.b_updatelevels)
        b_updatelevels.setOnClickListener {

            // update firebase database
            FirebaseRepository.saveCurrentMembers(this) { success ->
                if (success) {
                    Toast.makeText(this, "Database updated!", Toast.LENGTH_SHORT).show()
                    showMembers(AppState.currentClubMembers)
                } else {
                    Toast.makeText(this, "Failed to update Database", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun showClubInfo(signedInClub: Club?) {

        val tv_club_name = findViewById<TextView>(R.id.tv_clubname)
        val tv_clubid = findViewById<TextView>(R.id.tv_clubid)

        tv_club_name.text = signedInClub?.name
        tv_clubid.text = "(id:${signedInClub?.sportmemberId.toString()})"

    }

    private fun handleUpdatePlayers() {
        if (AppState.signedInClub == null) {
            Toast.makeText(this, "No signed in club, please wait", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "No signed in club")
            return
        }

        Log.i(TAG,"Starting updating database ...")

        SportmemberRepository.fetchMembers( onComplete = { players ->

            runOnUiThread {
                Toast.makeText(this, "Found ${players.size} players on Sportmember", Toast.LENGTH_SHORT).show()

                // update current members
                updateCurrentMembers(players)

                // update firebase database
                FirebaseRepository.saveCurrentMembers(this) { success ->
                    if (success) {
                        Toast.makeText(this, "Database updated!", Toast.LENGTH_SHORT).show()
                        showMembers(AppState.currentClubMembers)
                    } else {
                        Toast.makeText(this, "Failed to update Database", Toast.LENGTH_SHORT).show()
                    }
                }

                showMembers(AppState.currentClubMembers)

            }

        },
            onError = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Failed to fetch players from Sportmember", Toast.LENGTH_SHORT).show()
                }
        })

    }

    private fun updateCurrentMembers(players: List<Player>) {

        Log.i(TAG,"Updating database ...")

        players?.forEach { player ->
            if (!currentMembersIds.contains(player.sportmemberUserId)) {
                // add new players
                AppState.currentClubMembers.add(player)
                currentMembersIds.add(player.sportmemberUserId)
                Log.i(TAG, "Added new member: ${player.firstname} ${player.lastname}")
            } else {
                // update name changes from Sportmember
                val member = AppState.currentClubMembers.find { it.sportmemberUserId == player.sportmemberUserId }
                if (member?.firstname != player.firstname || member?.lastname != player.lastname  ) {
                    member?.firstname = player.firstname
                    member?.lastname = player.lastname
                    Log.i(TAG, "Updated member name: ${member?.firstname} ${member?.lastname}")
                }
            }

        }

        val playerIds = mutableListOf<Long>()
        players.forEach { player ->
            playerIds.add(player.sportmemberUserId)
        }

        AppState.currentClubMembers.removeAll { member ->
            !playerIds.contains(member.sportmemberUserId)
        }

    }

    private fun showMembers(players: MutableList<Player>) {

        val rv_membersBase = findViewById<RecyclerView>(R.id.rv_membersBase)

        rv_membersBase.adapter = PlayersAdapter(this, players)
        rv_membersBase.layoutManager = LinearLayoutManager(this)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_profile, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_logout) {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)

    }



}
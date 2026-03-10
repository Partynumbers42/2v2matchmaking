package com.partynumbers42.matchmaking2v2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.partynumbers42.matchmaking2v2.models.Player

private const val TAG = "MainActivity"

private var isDataLoaded = false

class MainActivity : AppCompatActivity() {

    data class Matchup(val player1: Player, val player2: Player, val player3: Player, val player4: Player)
    data class Round(val matchups: List<Matchup>, val breakers: List<Player>)
    private lateinit var et_nrgames: EditText
    private lateinit var b_generate: Button
    private lateinit var fabAddPlayers: FloatingActionButton


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        et_nrgames = findViewById(R.id.et_nrgames)
        b_generate = findViewById(R.id.b_generate)
        fabAddPlayers = findViewById(R.id.fabAddPlayers)

        // buttons only enabled when firebase is successful
        b_generate.isEnabled = false
        fabAddPlayers.isEnabled = false

        // fetch data from firebase
        FirebaseRepository.fetchSignedInClub(this) { success ->
            if (success) {
                isDataLoaded = true
                invalidateOptionsMenu()
                b_generate.isEnabled = true
                fabAddPlayers.isEnabled = true
            } else {
                Toast.makeText(this, "Failed to load club data", Toast.LENGTH_SHORT).show()
            }
        }

        // these should only run once the asynchronous call above is done

        // TODO move these also to the firebase repository

        //Update data from Sportmember
        handleUpdatePlayers()

        // TODO make sure the Player Update is done

        //Pull participants for next activity from Sportmember
        handlePullParticipants()



        et_nrgames.setText("0")

        b_generate.setOnClickListener {

            val nrgamesString = et_nrgames.text.toString()
            val nrgames = if (nrgamesString.isNotEmpty()) nrgamesString.toInt() else 0
            Log.i(TAG,"onProgressChanged $nrgames")
            if (AppState.currentParticipants.size!=0){
                generateRounds(nrgames)
            } else {
                clearRounds()
                Toast.makeText(this, "Add participants first", Toast.LENGTH_SHORT).show()
            }
        }

        fabAddPlayers.setOnClickListener {
            val intent = Intent(this, AddPlayersActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStart() {
        super.onStart()
        if (isDataLoaded) {
            showParticipatingMembers(AppState.currentParticipants)
        }
        Log.i(TAG, "onStart: Refreshing UI with ${AppState.currentParticipants.size} participants")
    }

    private fun showParticipatingMembers(currentParticipants: MutableList<Player>) {

        val rv_participants = findViewById<RecyclerView>(R.id.rvplayers)

        rv_participants.adapter = SmallPlayersAdapter(this, currentParticipants)
        rv_participants.layoutManager = GridLayoutManager(this,3)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        //enable only if data is loaded
        val profileItem = menu?.findItem(R.id.menu_profile)
        profileItem?.isEnabled = isDataLoaded
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_profile) {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
            return super.onOptionsItemSelected(item)
        }


    private fun generateRounds(nrgames: Int){

        val rvRounds = findViewById<RecyclerView>(R.id.rvRounds)
        val rounds: List<Round> = setUpRounds(nrgames)

        if (nrgames>0) {
            rvRounds.adapter = RoundsAdapter(this, rounds)
            rvRounds.layoutManager = LinearLayoutManager(this)
        } else {
            clearRounds()
        }

    }


    private fun clearRounds(){
        val rvMatchups = findViewById<RecyclerView>(R.id.rvRounds)
        rvMatchups.adapter = null
    }

    private fun setUpMatchups(players: List<Player>): Round{

        val matchups: MutableList<Matchup> = mutableListOf<Matchup>()
        val breakers: MutableList<Player> = mutableListOf<Player>()

        var counter: Int = 0
        val playersInMatchup: MutableList<Player> = mutableListOf<Player>()

        for (player in players){
            playersInMatchup.add(player)
            counter+=1
            if (counter==4) {
                counter=0
                matchups.add(Matchup(playersInMatchup[0],playersInMatchup[1],playersInMatchup[2],playersInMatchup[3]))
                playersInMatchup.clear()
            }

        }
        for (player in playersInMatchup){
            breakers.add(player)
        }

        return Round(matchups,breakers)
    }

    private fun setUpRounds(nrgames: Int): List<Round> {
        val rounds: MutableList<Round> = mutableListOf<Round>()
        for (i in 1..nrgames){
            rounds.add(setUpMatchups(AppState.currentParticipants))
        }
        return rounds
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
                        //showMembers(AppState.currentClubMembers)
                    } else {
                        Toast.makeText(this, "Failed to update Database", Toast.LENGTH_SHORT).show()
                    }
                }

                //showMembers(AppState.currentClubMembers)

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
            if (!AppState.currentClubMembersIds.contains(player.sportmemberUserId)) {
                // add new players
                AppState.currentClubMembers.add(player)
                AppState.currentClubMembersIds.add(player.sportmemberUserId)
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
                AppState.nonParticipants = AppState.currentClubMembers.filter { !AppState.currentParticipants.contains(it) }.toMutableList()

                for (player in AppState.currentParticipants){
                    Log.i(TAG, "Found participant: ${player.firstname} ${player.lastname}")
                }
                showParticipatingMembers(AppState.currentParticipants)
            }
        },
            onError = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Failed to fetch players from Sportmember", Toast.LENGTH_SHORT).show()
                }
            })

    }


}


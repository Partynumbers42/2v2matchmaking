package com.partynumbers42.matchmaking2v2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
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
import androidx.core.view.isVisible

private const val TAG = "MainActivity"

private var isDataLoaded = false

class MainActivity : AppCompatActivity() {

    data class Matchup(val player1: Player, val player2: Player, val player3: Player, val player4: Player)
    data class Round(val matchups: List<Matchup>, val breakers: List<Player>)
    private lateinit var tv_nrgames: TextView
    private lateinit var et_nrgames: EditText
    private lateinit var b_generate: Button
    private lateinit var b_seed: Button
    private lateinit var et_seed: EditText
    private lateinit var tv_seed: TextView
    private lateinit var spinner_mode: Spinner
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

        b_generate = findViewById(R.id.b_generate)
        fabAddPlayers = findViewById(R.id.fabAddPlayers)

        tv_nrgames = findViewById(R.id.tv_nrgames)
        et_nrgames = findViewById(R.id.et_nrgames)
        spinner_mode = findViewById(R.id.spinner_mode)
        tv_nrgames.visibility = View.GONE
        et_nrgames.visibility = View.GONE
        spinner_mode.visibility = View.GONE

        b_seed = findViewById(R.id.bu_seed)
        et_seed = findViewById(R.id.et_seed)
        tv_seed = findViewById(R.id.tv_seed)
        et_seed.visibility = View.GONE
        tv_seed.visibility = View.GONE

        // buttons only enabled when firebase is successful
        b_generate.isEnabled = false
        fabAddPlayers.isEnabled = false
        b_seed.isEnabled = false
        et_seed.isEnabled = false

        if (isDataLoaded) {
            invalidateOptionsMenu()
            b_generate.isEnabled = true
            fabAddPlayers.isEnabled = true
            b_seed.isEnabled = true
            et_seed.isEnabled = true
            showParticipatingMembers(AppState.currentParticipants)
        } else {

            // Fetch data from Firebase
            FirebaseRepository.fetchSignedInClub(this) { success ->
                if (!success) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to load club data", Toast.LENGTH_SHORT).show()
                    }
                    return@fetchSignedInClub
                }
                //Update data from Sportmember and update Firebase
                AppState.fetchAndPushMembers(this) { success ->
                    if (!success) {
                        runOnUiThread {
                            Toast.makeText(this, "Failed to update club data", Toast.LENGTH_SHORT)
                                .show()
                        }
                        return@fetchAndPushMembers
                    }
                    //Pull participants for next activity from Sportmember
                    SportmemberRepository.fetchParticipants { success ->
                        if (!success) {
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Failed to load next participants",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@fetchParticipants
                        }
                        runOnUiThread {
                            isDataLoaded = true
                            invalidateOptionsMenu()
                            b_generate.isEnabled = true
                            fabAddPlayers.isEnabled = true
                            b_seed.isEnabled = true
                            et_seed.isEnabled = true
                            et_nrgames.setText(AppState.nrOfMatchups.toString())
                            showParticipatingMembers(AppState.currentParticipants)
                            AppState.seed =AppState.currentActivityId // seed stays fixed per activity
                        }
                    }
                }
            }
        }

        b_generate.setOnClickListener {

            val nrgamesString = et_nrgames.text.toString()
            val nrgames = if (nrgamesString.isNotEmpty()) nrgamesString.toInt() else 0
            AppState.nrOfMatchups = nrgames

            Log.i(TAG,"Number of games to generate: $nrgames")
            if (AppState.currentParticipants.isNotEmpty()){
                generateRounds(nrgames)
            } else {
                clearRounds()
                Toast.makeText(this, "Add participants first", Toast.LENGTH_SHORT).show()
            }
        }

        b_seed.setOnClickListener {
            if (tv_seed.isVisible) {
                tv_seed.visibility = View.GONE
                et_seed.visibility = View.GONE
                tv_nrgames.visibility = View.GONE
                et_nrgames.visibility = View.GONE
                spinner_mode.visibility = View.GONE
            } else {
                tv_seed.visibility = View.VISIBLE
                et_seed.visibility = View.VISIBLE
                et_seed.setText(AppState.seed.toString())
                tv_nrgames.visibility = View.VISIBLE
                et_nrgames.visibility = View.VISIBLE
                et_nrgames.setText(AppState.nrOfMatchups.toString())
                spinner_mode.visibility = View.VISIBLE
            }
        }

        et_seed.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val seedString = et_seed.text.toString()
                if (seedString.isNotEmpty()) {
                    AppState.seed = seedString.toLong()
                }
            }
        })

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
        rv_participants.layoutManager = GridLayoutManager(this,4)

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

    private fun setUpMatchups(players: MutableList<Player>, seed: Long = AppState.seed): MutableList<Matchup>{
        // Number of players need to be divisible by 4 for this function, exclude breakers before

        if (players.size%4 != 0){
            Log.e(TAG, "Number of players not divisible by 4")
        }

        val playersRandomized = randomizePlayers(players,seed)
        val matchups: MutableList<Matchup> = mutableListOf<Matchup>()

        playersRandomized.chunked(4).forEach {
            matchups.add(Matchup(it[0],it[1],it[2],it[3]))
        }

        return matchups
    }

    private fun setUpRounds(nrgames: Int): List<Round> {

        val breakersLoop = randomizePlayers(AppState.currentParticipants, AppState.seed)
        val breakersCount  = AppState.currentParticipants.size%4

        val rounds: MutableList<Round> = mutableListOf<Round>()

        for (i in 1..nrgames){

            val breakers = breakersLoop.take(breakersCount).toMutableList()
            val players = breakersLoop.drop(breakersCount).toMutableList()
            java.util.Collections.rotate(breakersLoop,-breakersCount)

            val roundSeed = 1000*AppState.seed + i

            rounds.add(Round(setUpMatchups(players,roundSeed),breakers))
        }

        return rounds
    }

    private fun randomizePlayers(players: MutableList<Player>, seed: Long = AppState.seed): MutableList<Player> {
        val randomizer = java.util.Random(seed)
        val playersShuffled = players.shuffled(randomizer)
        return playersShuffled as MutableList<Player>
    }


}

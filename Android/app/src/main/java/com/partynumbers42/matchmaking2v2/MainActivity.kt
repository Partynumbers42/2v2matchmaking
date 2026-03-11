package com.partynumbers42.matchmaking2v2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
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
import kotlin.random.Random
import androidx.core.view.isVisible

private const val TAG = "MainActivity"

private var isDataLoaded = false

class MainActivity : AppCompatActivity() {

    data class Matchup(val player1: Player, val player2: Player, val player3: Player, val player4: Player)
    data class Round(val matchups: List<Matchup>, val breakers: List<Player>)
    private lateinit var et_nrgames: EditText
    private lateinit var b_generate: Button
    private lateinit var b_seed: Button
    private lateinit var et_seed: EditText
    private lateinit var tv_seed: TextView
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
                        Toast.makeText(this, "Failed to update club data", Toast.LENGTH_SHORT).show()
                    }
                    return@fetchAndPushMembers
                }
                //Pull participants for next activity from Sportmember
                SportmemberRepository.fetchParticipants { success ->
                    if (!success) {
                        runOnUiThread {
                            Toast.makeText(this,"Failed to load next participants",Toast.LENGTH_SHORT).show()
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
                        showParticipatingMembers(AppState.currentParticipants)
                    }
                }
            }
        }

        AppState.seed = System.currentTimeMillis()

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

        b_seed.setOnClickListener {
            if (tv_seed.isVisible) {
                tv_seed.visibility = View.GONE
                et_seed.visibility = View.GONE
            } else {
                tv_seed.visibility = View.VISIBLE
                et_seed.visibility = View.VISIBLE
                et_seed.setText(AppState.seed.toString())
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

        val randomizer = java.util.Random(AppState.seed)
        val players_ordered = players.shuffled(randomizer)
        
        val matchups: MutableList<Matchup> = mutableListOf<Matchup>()
        val breakers: MutableList<Player> = mutableListOf<Player>()

        var counter: Int = 0
        val playersInMatchup: MutableList<Player> = mutableListOf<Player>()

        for (player in players_ordered){
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


}

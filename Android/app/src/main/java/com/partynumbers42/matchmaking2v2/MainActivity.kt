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
import com.partynumbers42.matchmaking2v2.SportmemberRepository.updateParticipants
import com.partynumbers42.matchmaking2v2.models.Activity
import com.partynumbers42.matchmaking2v2.models.Pools
import java.util.Collections.rotate
import kotlin.random.Random

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {

    data class Matchup(val player1: Player, val player2: Player, val player3: Player, val player4: Player)
    data class Round(val matchups: List<Matchup>, val breakers: List<Player>)
    private lateinit var tv_nrgames: TextView
    private lateinit var et_nrgames: EditText
    private lateinit var b_generate: Button
    private lateinit var b_seed: Button
    private lateinit var et_seed: EditText
    private lateinit var tv_seed: TextView
    private lateinit var spinner_activity: Spinner
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
        spinner_activity = findViewById(R.id.spinner_activity)
        spinner_activity.visibility = View.GONE

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

        if (AppState.isDataLoaded) {
            invalidateOptionsMenu()
            b_generate.isEnabled = true
            fabAddPlayers.isEnabled = true
            b_seed.isEnabled = true
            et_seed.isEnabled = true
            showActivites(spinner_activity)
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

                if (AppState.usesSportmember) {
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
                                AppState.isDataLoaded = true
                                invalidateOptionsMenu()
                                b_generate.isEnabled = true
                                fabAddPlayers.isEnabled = true
                                b_seed.isEnabled = true
                                et_seed.isEnabled = true
                                et_nrgames.setText(AppState.nrOfMatchups.toString())
                                showActivites(spinner_activity)
                                showParticipatingMembers(AppState.currentParticipants)
                                AppState.seed = AppState.nextActivityId // seed stays fixed per activity
                            }
                        }
                    }
                } else {
                    AppState.isDataLoaded = true
                    invalidateOptionsMenu()
                    b_generate.isEnabled = true
                    fabAddPlayers.isEnabled = true
                    b_seed.isEnabled = true
                    et_seed.isEnabled = true
                    et_nrgames.setText(AppState.nrOfMatchups.toString())
                    showActivites(spinner_activity)
                    showParticipatingMembers(AppState.currentParticipants)
                    AppState.seed = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
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

        val adapterMode = ArrayAdapter(this,R.layout.spinner_mode_item, AppState.matchmakingModes)
        adapterMode.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_mode.adapter = adapterMode
        spinner_mode.setSelection(0)
        spinner_mode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?,view: View?,position: Int,id: Long) {
                val selectedMode = AppState.matchmakingModes[position]
                Log.i(TAG,"Selected mode: $selectedMode")
                AppState.currentMatchmakingMode = selectedMode
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                spinner_mode.setSelection(0)
                AppState.currentMatchmakingMode = AppState.matchmakingModes.first()
            }

        }

        fabAddPlayers.setOnClickListener {
            val intent = Intent(this, AddPlayersActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStart() {
        super.onStart()
        if (AppState.isDataLoaded) {
            showParticipatingMembers(AppState.currentParticipants)
        }
        Log.i(TAG, "onStart: Refreshing UI with ${AppState.currentParticipants.size} participants")
    }


    private fun showActivites(spinner_activity: Spinner) {

        spinner_activity.visibility = View.VISIBLE

        Log.i(TAG,"Found activities: ${AppState.ActivityNames}")
        val adapterActivity = ArrayAdapter(this,R.layout.spinner_mode_item, AppState.ActivityNames)
        adapterActivity.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_activity.adapter = adapterActivity
        spinner_activity.setSelection(0)
        spinner_activity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?,view: View?,position: Int,id: Long) {
                val selectedActivity = AppState.ActivityNames[position]
                val selectedId = AppState.ActivityIds[position]
                Log.i(TAG,"Selected activity: $selectedActivity with $selectedId")
                updateParticipants(AppState.Activities,selectedId)
                Log.i(TAG,"Updated participants: ${AppState.currentParticipants.map { it.firstname + " " + it.lastname }}")
                showParticipatingMembers(AppState.currentParticipants)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                spinner_activity.setSelection(0)
                updateParticipants(AppState.Activities, AppState.Activities.first().sportmemberId)
                showParticipatingMembers(AppState.currentParticipants)
            }
        }

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
        profileItem?.isEnabled = AppState.isDataLoaded
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

    private fun setUpMatchups(pools: Pools, mode: String = "Open", seed: Long = AppState.seed, modeSetting: Int = 0): MutableList<Matchup>{
        // Number of players need to be divisible by 4 for this function, exclude breakers before

        if (pools.size%4 != 0){
            Log.e(TAG, "Number of players not divisible by 4")
        }

        val matchups = mutableListOf<Matchup>()
        var playersRandomized = mutableListOf<Player>()

        when (mode) {
            "Random" -> {
                playersRandomized = randomizePlayers(pools.players, seed)
            }
            "Division" -> {
                playersRandomized = pools.players
            }
            "Open" -> {
                val poolsIndices1 = pools.pools.indices.filter { pools.pools[it].size != 0 }.toMutableList()
                val nrgames = pools.size / 4
                for (i in 1..nrgames) {
                    val seed1 = 1000*seed + 10*i + 1
                    val index1 = randomizeLevels(poolsIndices1, seed1)
                    val pool1 = pools.pools[index1]
                    var poolsIndices2 = poolsIndices1.filter { it != index1 }.toMutableList()
                    when (modeSetting) {
                        0 -> {
                            // allow the same pool to be used twice
                            if (pool1.size>=4) {
                                poolsIndices2 = poolsIndices1.toMutableList()
                            }
                        }
                        1 -> {

                        }
                        else -> {
                            Log.e(TAG,"Unknown mode setting")
                        }
                    }
                    val thisPlayers = mutableListOf<Player>()
                    if (poolsIndices2.isEmpty()) {
                        Log.i(TAG, "Match $i: index $index1 vs index $index1")
                        for (j in 1..4) {
                            thisPlayers.add(pool1.players.first())
                            pool1.removeFirstPlayer()
                        }
                    } else {
                        val seed2 = 1000*seed + 10*i + 2
                        val index2 = randomizeLevels(poolsIndices2, seed2)
                        Log.i(TAG, "Match $i: lvl $index1 vs lvl $index2")
                        val pool2 = pools.pools[index2]
                        val thisPools = listOf(pool1, pool2)
                        Log.i(TAG, thisPools.toString())
                        for (pool in thisPools) {
                            for (j in 1..2) {
                                thisPlayers.add(pool.players.first())
                                pool.removeFirstPlayer()
                            }
                        }
                        if (pool2.size == 0) {
                            poolsIndices1.remove(index2)
                        }
                    }
                    if (pool1.size == 0) {
                        poolsIndices1.remove(index1)
                    }

                    playersRandomized.addAll(thisPlayers)
                }
            }
            else -> {
                Log.e(TAG,"Matchmaking mode not implemented")
            }
        }

        playersRandomized.chunked(4).forEach {
            matchups.add(Matchup(it[0], it[2], it[1], it[3]))
        }

        return matchups
    }

    private fun setUpRounds(nrgames: Int): List<Round> {

        //TODO try to avoid repetitions of matchups between rounds

        val breakersLoop = randomizePlayers(AppState.currentParticipants, AppState.seed)
        val breakersCount  = AppState.currentParticipants.size%4

        val rounds: MutableList<Round> = mutableListOf<Round>()

        for (i in 1..nrgames){

            Log.i(TAG,"Round $i")

            val breakers = breakersLoop.take(breakersCount).toMutableList()
            val players = breakersLoop.drop(breakersCount).toMutableList()
            rotate(breakersLoop,-breakersCount)

            Log.i(TAG,"Players: ${players.map { it.firstname + " " + it.lastname }}")
            Log.i(TAG,"Breakers: ${breakers.map { it.firstname + " " + it.lastname }}")

            val roundSeed = 1000*AppState.seed + i

            when (AppState.currentMatchmakingMode) {
                "True Open" -> {
                    val pools = Pools(players, seed = roundSeed)
                    pools.equalizePoolCollections(2)
                    pools.randomize()
                    rounds.add(Round(setUpMatchups(pools,"Open",roundSeed,0),breakers))
                }
                "Soft Open" -> {

                    // TODO include Delta Level = 2 limitation

                    if (i%2==0) {
                        val pools = Pools(players, seed = roundSeed)
                        pools.equalizePoolCollections(2)
                        pools.randomize()
                        rounds.add(Round(setUpMatchups(pools,"Open",roundSeed,1),breakers))
                    } else {
                        val pools = Pools(players, seed = roundSeed)
                        pools.equalizePoolCollections(4)
                        pools.randomize()
                        rounds.add(Round(setUpMatchups(pools, "Division", roundSeed), breakers))
                    }
                }
                "Division All" -> {

                    val pools = Pools(players, seed = roundSeed)
                    pools.equalizePoolCollections(4)
                    pools.randomize()

                    rounds.add(Round(setUpMatchups(pools, "Division", roundSeed),breakers))
                }
                "Division 3" -> {

                    val pools = Pools(players, divisor = 3, seed = roundSeed)
                    pools.equalizePoolCollections(4)

                    val matchups = mutableListOf<Matchup>()
                    for (poolCollection in pools.pools) {
                        val thisPlayers = poolCollection.players
                        val thisPools = Pools(thisPlayers, seed = roundSeed)
                        thisPools.equalizePoolCollections(2)
                        thisPools.randomize()
                        val thisMatchups = setUpMatchups(thisPools, "Open", roundSeed,0)
                        matchups.addAll(thisMatchups)
                    }
                    rounds.add(Round(matchups,breakers))
                }
                "Division 2" -> {
                    val pools = Pools(players, divisor = 2, seed = roundSeed)
                    pools.equalizePoolCollections(4)

                    val matchups = mutableListOf<Matchup>()
                    for (poolCollection in pools.pools) {
                        val thisPlayers = poolCollection.players
                        val thisPools = Pools(thisPlayers, seed = roundSeed)
                        thisPools.equalizePoolCollections(2)
                        thisPools.randomize()
                        val thisMatchups = setUpMatchups(thisPools, "Open", roundSeed,0)
                        matchups.addAll(thisMatchups)
                    }
                    rounds.add(Round(matchups,breakers))
                }
                "All Random" -> {
                    val pools = Pools(players, seed = roundSeed)
                    rounds.add(Round(setUpMatchups(pools, "Random", roundSeed),breakers))
                }
                else -> {
                    Log.e(TAG,"Matchmaking mode not implemented")
                }
            }

        }

        return rounds
    }


    private fun randomizePlayers(players: MutableList<Player>, seed: Long = AppState.seed): MutableList<Player> {
        val randomizer = Random(seed)
        return players.shuffled(randomizer).toMutableList()
    }

    private fun randomizeLevels(levels: List<Int>, seed: Long = AppState.seed): Int {
        val randomizer = Random(seed)
        return levels.random(randomizer)
    }

}

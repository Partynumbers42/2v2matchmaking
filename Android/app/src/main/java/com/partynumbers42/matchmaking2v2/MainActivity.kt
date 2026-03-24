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
import java.util.Collections.rotate
import kotlin.collections.last
import kotlin.random.Random

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

        val adapter = ArrayAdapter(this,R.layout.spinner_mode_item, AppState.matchmakingModes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_mode.adapter = adapter
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

    private fun setUpMatchups(players: MutableList<Player>, mode: String = "Open", seed: Long = AppState.seed, mode_setting: Int = 0): MutableList<Matchup>{
        // Number of players need to be divisible by 4 for this function, exclude breakers before

        if (players.size%4 != 0){
            Log.e(TAG, "Number of players not divisible by 4")
        }

        var level_modulus: Int = 4
        if (mode in listOf("Open")){
            level_modulus = 2
        }

        val pools = mutableListOf<MutableList<Player>>()
        AppState.levels.forEach { levelString ->
            val level = levelString.toInt()
            val playersAtLevel = players.filter { it.level == level }
            pools.add(playersAtLevel.toMutableList())
        }

        // Randomize Pools
        val randomizedPools = randomizePools(pools, seed)

        // Promote Players to equal out Pools
        val tempPool = mutableListOf<Player>()
        for (i in randomizedPools.indices) {
            val pool = randomizedPools[i]
            pool.addAll(tempPool)
            tempPool.clear()
            val overflow = (pool.size) % level_modulus
            if (overflow != 0) {
                Log.i(TAG, "Pool $i Overflow: $overflow")
                repeat(overflow) {
                    val chosenPlayer = pool.last()
                    Log.i(TAG, "Promote: ${chosenPlayer.firstname + " " + chosenPlayer.lastname}")
                    tempPool.add(chosenPlayer)
                    pool.remove(chosenPlayer)
                }
            }
        }

        val existingLevels = randomizedPools.indices.filter { randomizedPools[it].isNotEmpty() }.toMutableList()
        Log.i(TAG,"existing Lvls: $existingLevels")

        for (i in randomizedPools.indices) {
            val pool = randomizedPools[i]
            if (pool.isNotEmpty()) {
                Log.i(TAG, "Pool lvl $i: ${pool.map { it.firstname + " " + it.lastname[0] + "." }}")
            }
        }

        val matchups = mutableListOf<Matchup>()
        var playersRandomized = mutableListOf<Player>()

        when (mode) {
            "Random" -> {
                playersRandomized = randomizePlayers(players, seed)
            }
            "Division" -> {
                playersRandomized = randomizedPools.flatten().toMutableList()
            }
            "Open" -> {
                val nrgames = players.size / 4
                for (i in 1..nrgames) {
                    val level1Seed = 1000*seed + 10*i + 1
                    val level1 = randomizeLevels(existingLevels, level1Seed)
                    val pool1 = randomizedPools[level1]
                    var existingLevels2 = existingLevels
                    when (mode_setting) {
                        0 -> {
                            if (pool1.size<4) {
                                existingLevels2 = existingLevels.filter { it != level1 }.toMutableList()
                            }
                        }
                        1 -> {
                            existingLevels2 = existingLevels.filter { it != level1 }.toMutableList()
                        }
                        else -> {
                            Log.e(TAG,"Unknown mode setting")
                            existingLevels2 = existingLevels.filter { it != level1 }.toMutableList()
                        }
                    }
                    val thisPlayers = mutableListOf<Player>()
                    if (existingLevels2.isEmpty()) {
                        Log.i(TAG, "Match $i: lvl $level1 vs lvl $level1")
                        for (j in 1..4) {
                            val pool1 = randomizedPools[level1]
                            val choosenPlayer = pool1.first()
                            thisPlayers.add(choosenPlayer)
                            pool1.remove(choosenPlayer)
                        }
                    } else {
                        val level2Seed = 1000*seed + 10*i + 2
                        val level2 = randomizeLevels(existingLevels2, level2Seed)
                        Log.i(TAG, "Match $i: lvl $level1 vs lvl $level2")
                        val pool2 = randomizedPools[level2]
                        val thisPools = listOf(pool1, pool2)
                        Log.i(TAG, thisPools.toString())
                        for (pool in thisPools) {
                            for (j in 1..2) {
                                val choosenPlayer = pool.first()
                                thisPlayers.add(choosenPlayer)
                                pool.remove(choosenPlayer)
                            }
                        }
                        if (pool2.isEmpty()) {
                            existingLevels.remove(level2)
                        }
                    }
                    if (pool1.isEmpty()) {
                        existingLevels.remove(level1)
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
                    rounds.add(Round(setUpMatchups(players,"Open",roundSeed,0),breakers))
                }
                "Soft Open" -> {
                    // TODO include Delta Level = 2 limitation
                    //Toast.makeText(this,"Soft Open not fully implemented",Toast.LENGTH_SHORT).show()
                    if (i%2==0) {
                        rounds.add(Round(setUpMatchups(players, "Open", roundSeed,1), breakers))
                    } else {
                        rounds.add(Round(setUpMatchups(players, "Division", roundSeed), breakers))
                    }
                }
                "Division All" -> {
                    rounds.add(Round(setUpMatchups(players, "Division", roundSeed),breakers))
                }
                "Division 3" -> {
                    val divisor = 3
                    // TODO
                    // Outsource this function (for both cases)
                    // Potentially make the Pool an object which can be randomized while keeping added players of different level at the end
                    // Or randomize in the very beginning
                    val playerBlocks: MutableList<MutableList<Player>> = mutableListOf<MutableList<Player>>()
                    for (i in 0..divisor) {
                        val levels = AppState.levels.filter { (i-1)*AppState.levels.last().toInt()/divisor < it.toInt() && it.toInt() <= i*AppState.levels.last().toInt()/divisor }
                        val thisPlayers = players.filter { it.level.toString() in levels }.toMutableList()
                        playerBlocks.add(thisPlayers)
                    }
                    val block_modulus = 4
                    val tempBlock = mutableListOf<Player>()
                    for (playersBlock in playerBlocks) {
                        playersBlock.addAll(tempBlock)
                        tempBlock.clear()
                        val overflow = (playersBlock.size) % block_modulus
                        if (overflow != 0) {
                            repeat(overflow) {
                                val chosenPlayer = tempBlock.last()
                                tempBlock.add(chosenPlayer)
                                playersBlock.remove(chosenPlayer)
                            }
                        }
                    }
                    val matchups = mutableListOf<Matchup>()
                    for (playersBlock in playerBlocks) {
                        val thisMatchups = setUpMatchups(playersBlock, "Open", roundSeed,0)
                        matchups.addAll(thisMatchups)
                    }
                    rounds.add(Round(matchups,breakers))
                }
                "Division 2" -> {
                    val divisor = 2
                    val playerBlocks: MutableList<MutableList<Player>> = mutableListOf<MutableList<Player>>()
                    for (i in 0..divisor) {
                        val levels = AppState.levels.filter { (i-1)*AppState.levels.last().toInt()/divisor < it.toInt() && it.toInt() <= i*AppState.levels.last().toInt()/divisor }
                        val thisPlayers = players.filter { it.level.toString() in levels }.toMutableList()
                        playerBlocks.add(thisPlayers)
                    }
                    val block_modulus = 4
                    val tempBlock = mutableListOf<Player>()
                    for (playersBlock in playerBlocks) {
                        playersBlock.addAll(tempBlock)
                        tempBlock.clear()
                        val overflow = (playersBlock.size) % block_modulus
                        if (overflow != 0) {
                            repeat(overflow) {
                                val chosenPlayer = tempBlock.last()
                                tempBlock.add(chosenPlayer)
                                playersBlock.remove(chosenPlayer)
                            }
                        }
                    }
                    val matchups = mutableListOf<Matchup>()
                    for (playersBlock in playerBlocks) {
                        val thisMatchups = setUpMatchups(playersBlock, "Open", roundSeed,0)
                        matchups.addAll(thisMatchups)
                    }
                    rounds.add(Round(matchups,breakers))
                }
                "All Random" -> {
                    rounds.add(Round(setUpMatchups(players, "Random", roundSeed),breakers))
                }
                else -> {
                    Log.e(TAG,"Matchmaking mode not implemented")
                }
            }

        }

        return rounds
    }

    // TODO
    //private fun dividePlayers()

    private fun randomizePlayers(players: MutableList<Player>, seed: Long = AppState.seed): MutableList<Player> {
        val randomizer = Random(seed)
        return players.shuffled(randomizer).toMutableList()
    }

    private fun randomizeLevels(levels: List<Int>, seed: Long = AppState.seed): Int {
        val randomizer = Random(seed)
        return levels.random(randomizer)
    }

    private fun randomizePools(pools: List<MutableList<Player>>, seed: Long = AppState.seed): List<MutableList<Player>> {
        val randomizer = Random(seed)
        return pools.map { pool ->
            pool.shuffled(randomizer).toMutableList()
        }
    }

}

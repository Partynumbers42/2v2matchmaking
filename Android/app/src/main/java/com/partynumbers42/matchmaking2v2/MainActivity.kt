package com.partynumbers42.matchmaking2v2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.partynumbers42.matchmaking2v2.models.Club
import com.partynumbers42.matchmaking2v2.models.Player
import com.partynumbers42.matchmaking2v2.models.User

private const val TAG = "MainActivity"
const val EXTRA_SPORTMEMBER_ID = "EXTRA_SPORTMEMBER_ID"

class MainActivity : AppCompatActivity() {

    private var signedInClub: Club? = null

    //data class Player(val playerId: Int, val playerName: String, val playerLevel: Int)
    data class Matchup(val player1: Player, val player2: Player, val player3: Player, val player4: Player)
    data class Round(val matchups: List<Matchup>, val breakers: List<Player>)

    private lateinit var et_nrgames: EditText
    private lateinit var b_generate: Button
    private lateinit var fabAddPlayers: FloatingActionButton

    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var users: MutableList<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        users = mutableListOf<User>()

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        firestoreDb = FirebaseFirestore.getInstance()

        firestoreDb.collection("clubs").document(FirebaseAuth.getInstance().currentUser?.uid  as String)
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInClub = userSnapshot.toObject(Club::class.java)
                Log.i(TAG, "signed in club: $signedInClub")
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failure fetching signed in club", exception)
            }



        val usersReference = firestoreDb.collection("users") 
        usersReference.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                Log.e(TAG, "listen error", exception)
                return@addSnapshotListener
            }
            val userList = snapshot.toObjects(User::class.java)
            users.clear()
            users.addAll(userList)

            // adapter.notifyDataSetChanged() TODO

            for (user in userList) {
                Log.i(TAG, "User ${user}")
            }

        }

        //var playerList = users

        et_nrgames = findViewById(R.id.et_nrgames)
        b_generate = findViewById(R.id.b_generate)

        b_generate.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                val nrgames = et_nrgames.text.toString().toInt()
                Log.i(TAG,"onProgressChanged $nrgames")
                generateRounds(nrgames)
            }

        })

        fabAddPlayers = findViewById(R.id.fabAddPlayers)
        fabAddPlayers.setOnClickListener {
            val intent = Intent(this, AddPlayersActivity::class.java)
            intent.putExtra(EXTRA_SPORTMEMBER_ID, signedInClub?.sportmemberId)
            startActivity(intent)
        }

        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
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

    private fun generateMatchups(): RecyclerView? {

        val rvMatchups = findViewById<RecyclerView>(R.id.rvMatchups)

        val players: List<Player> = setUpPlayers()
        val round: Round = setUpMatchups(players)
        val matchups: List<Matchup> = round.matchups

        rvMatchups.adapter = MatchupsAdapter(this, matchups)
        rvMatchups.layoutManager = LinearLayoutManager(this)

        return rvMatchups

    }

    private fun clearRounds(){
        val rvMatchups = findViewById<RecyclerView>(R.id.rvRounds)
        rvMatchups.adapter = null
    }

    private fun setUpPlayers(): List<Player>{
        val players: MutableList<Player> = mutableListOf<Player>()
        val playerNames = resources.getStringArray(R.array.people_names)
        var id : Long = 1

        for (playerName in playerNames){
            players.add(Player(playerName,1,id))
            id+=1
        }
        return players
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
            rounds.add(setUpMatchups(setUpPlayers()))
        }
        return rounds
    }


}


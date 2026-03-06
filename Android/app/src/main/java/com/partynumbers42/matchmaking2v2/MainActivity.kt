package com.partynumbers42.matchmaking2v2

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    data class Player(val playerId: Int, val playerName: String, val playerLevel: Int)

    data class Matchup(val player1: Player, val player2: Player, val player3: Player, val player4: Player)

    //data class MatchupList(val matchups: List<Matchup>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val players: List<Player> = setUpPlayers()
        val matchups: List<Matchup> = setUpMatchups(players)
        //val Rounds: List<Matchup> = setUpRounds()

        val rvMatchups = findViewById<RecyclerView>(R.id.rvRounds)

        rvMatchups.adapter = MatchupsAdapter(this, matchups)
        rvMatchups.layoutManager = LinearLayoutManager(this)

    }

    private fun setUpPlayers(): List<Player>{
        val players: MutableList<Player> = mutableListOf<Player>()
        val playerNames = resources.getStringArray(R.array.people_names)
        var id : Int = 1

        for (playerName in playerNames){
            players.add(Player(id,playerName,1))
            id+=1
        }
        return players
    }

    private fun setUpMatchups(players: List<Player>): List<Matchup>{

        val matchups: MutableList<Matchup> = mutableListOf<Matchup>()

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

        return matchups
    }

}


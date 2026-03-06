package com.partynumbers42.matchmaking2v2

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    data class Player(val playerId: Int, val playerName: String, val playerLevel: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
        //    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        //    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        //    insets
        //}

        val players: List<Player> = setUpPlayers()

        rvNames.adapter = PlayersAdapter(this, players)

        //rvNames.layoutManger

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

}


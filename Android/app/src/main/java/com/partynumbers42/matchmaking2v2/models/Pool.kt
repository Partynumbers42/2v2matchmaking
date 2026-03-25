package com.partynumbers42.matchmaking2v2.models

import kotlin.random.Random

public data class Pool(
    var players: MutableList<Player> = mutableListOf<Player>(),
    var level: Int = 0,
    var seed: Long = 0
) {
    var size = players.size
    fun addPlayers(players: MutableList<Player>) {
        //assumes players have the correct level
        this.players.addAll(players)
        updateSize()
    }
    fun removeLastPlayer() {
        players.removeAt(players.lastIndex)
        updateSize()
    }
    fun removeFirstPlayer() {
        players.removeAt(0)
        updateSize()
    }
    fun updateSize() {
        size = players.size
    }
    fun randomize() {
        val randomizer = Random(seed)
        players.shuffle(randomizer)
    }
}
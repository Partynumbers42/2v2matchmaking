package com.partynumbers42.matchmaking2v2.models

import android.util.Log
import com.partynumbers42.matchmaking2v2.AppState

private const val TAG = "PoolCollection"

public data class PoolCollection(
    var players: MutableList<Player> = mutableListOf<Player>(),
    var levels: MutableList<Int> = AppState.levels.map { it.toInt() }.toMutableList(),
    var seed: Long = 0
) {
    var size: Int = players.size
    var poolCollection: MutableList<Pool> = mutableListOf<Pool>()
    init {
        levels.retainAll { level -> players.any { it.level == level } }
        for (level in levels) {
            val thisPlayers = players.filter { it.level == level }.toMutableList()
            val thisPool = Pool(thisPlayers, level, seed)
            poolCollection.add(thisPool)
        }
        updatePlayers()
    }
    fun addPlayers(newPlayers: MutableList<Player>) {
        val newLevels = newPlayers.map { it.level }.distinct().toMutableList()
        for (pool in poolCollection) {
            val thisLevel = pool.level
            if (thisLevel in newLevels) {
                newLevels.remove(thisLevel)
            }
            val thisPlayers = newPlayers.filter { it.level == thisLevel }.toMutableList()
            pool.addPlayers(thisPlayers)
        }
        if (newLevels.isNotEmpty()) {
            levels.addAll(newLevels)
            levels.sort()
            for (level in newLevels) {
                val thisPlayers = newPlayers.filter { it.level == level }.toMutableList()
                val thisPool = Pool(thisPlayers, level, seed)
                poolCollection.add(thisPool)
            }
            poolCollection.sortBy { it.level }
        }
        updatePlayers()
        updateSize()
    }
    fun removeLastPlayer() {
        val lastPool = poolCollection.lastOrNull() ?: return
        lastPool.removeLastPlayer()
        if (lastPool.size == 0) {
            poolCollection.removeAt(poolCollection.lastIndex)
            levels.removeAt(levels.lastIndex)
        }
        updatePlayers()
        updateSize()
    }
    fun removeFirstPlayer() {
        val firstPool = poolCollection.firstOrNull() ?: return
        firstPool.removeFirstPlayer()
        if (firstPool.size == 0) {
            poolCollection.removeAt(0)
            levels.removeAt(0)
        }
        updatePlayers()
        updateSize()
    }
    fun updateSize() {
        size = players.size
    }
    fun updatePlayers() {
        players = poolCollection.flatMap { it.players }.toMutableList()
    }
    fun randomize() {
        for (pool in poolCollection) {
            pool.randomize()
        }
        updatePlayers()
    }
}
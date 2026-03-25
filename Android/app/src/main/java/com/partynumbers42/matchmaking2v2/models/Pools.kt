package com.partynumbers42.matchmaking2v2.models

import android.util.Log
import com.partynumbers42.matchmaking2v2.AppState

private const val TAG = "Pools"

public data class Pools(
    var players: MutableList<Player> = mutableListOf<Player>(),
    var divisor: Int = AppState.levels.last().toInt(),
    var seed: Long = 0,
) {
    var pools: MutableList<PoolCollection> = mutableListOf<PoolCollection>()
    var size: Int = players.size
    init {
        for (i in 0..divisor) {
            val thisLevelMin = AppState.levels.last().toInt() * (i - 1)/divisor + 1
            val thisLevelMax = AppState.levels.last().toInt() * i/divisor
            var thisLevels = AppState.levels.filter { it.toInt() in thisLevelMin..thisLevelMax }.map { it.toInt() }.toMutableList()
            val thisPlayers = players.filter { it.level in thisLevels }.toMutableList()
            if (thisPlayers.isEmpty()) {
                continue
            }
            Log.i(TAG, "Players with levels $thisLevels: ${thisPlayers.map { it.firstname + " " + it.lastname }}")
            val thisPoolCollection = PoolCollection(thisPlayers, thisLevels, seed)
            pools.add(thisPoolCollection)
        }
        Log.i(TAG, "Pools: ${pools.map { it.players.map { it.firstname + " " + it.lastname } }}")
        updatePlayers()
    }
    fun updatePlayers() {
        players = pools.flatMap { it.players }.toMutableList()
    }
    fun randomize() {
        for (pool in pools) {
            pool.randomize()
        }
        updatePlayers()
    }
    fun equalizePoolCollections(mod: Int = 4) {
        // Promote Players to equal out Pools
        val tempPool = mutableListOf<Player>()
        for (i in pools.indices) {
            val pool = pools[i]
            pool.addPlayers(tempPool)
            tempPool.clear()
            val overflow = (pool.size) % mod
            if (overflow != 0) {
                repeat(overflow) {
                    tempPool.add(pool.players.last())
                    pool.removeLastPlayer()
                }
            }
        }
    }
}
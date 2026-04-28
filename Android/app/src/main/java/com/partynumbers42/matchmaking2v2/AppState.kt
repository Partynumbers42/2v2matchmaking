package com.partynumbers42.matchmaking2v2

import android.util.Log
import android.content.Context
import com.partynumbers42.matchmaking2v2.models.Club
import com.partynumbers42.matchmaking2v2.models.Player

private const val TAG = "AppState"

object AppState {
    var seed: Long = 0
    var signedInClub: Club? = null
    var signedInClubFirebaseIdString: String = ""
    var usesSportmember: Boolean = false
    var currentClubMembers: MutableList<Player> = mutableListOf<Player>()
    var currentClubMembersIds: MutableList<Long> = mutableListOf<Long>()
    var currentActivityId: Long = 0
    var currentParticipants: MutableList<Player> = mutableListOf<Player>()
    var nonParticipants: MutableList<Player> = mutableListOf<Player>()
    var nrOfMatchups: Int = 4
    val matchmakingModes = listOf("True Open","Soft Open","Division All","Division 3","Division 2","All Random")
    var currentMatchmakingMode: String = matchmakingModes.first()
    val levels = listOf("0","1","2","3","4","5","6")
    fun fetchAndPushMembers(context: Context,onComplete: (Boolean) -> Unit) {

        Log.i(TAG,"Fetching members from Sportmember ...")

        SportmemberRepository.fetchMembers( onComplete = { players ->
            // update current members
            updateCurrentMembers(players)
            // update firebase database
            FirebaseRepository.saveCurrentMembers(context) { success ->
                onComplete(success)
            }
        },
            onError = { exception ->
                onComplete(false)
            })
    }

    private fun updateCurrentMembers(players: List<Player>) {

        Log.i(TAG,"Updating database ...")

        players?.forEach { player ->
            if (!currentClubMembersIds.contains(player.sportmemberUserId)) {
                // add new players
                currentClubMembers.add(player)
                currentClubMembersIds.add(player.sportmemberUserId)
                Log.i(TAG, "Added new member: ${player.firstname} ${player.lastname}")
            } else {
                // update name changes from Sportmember
                val member = currentClubMembers.find { it.sportmemberUserId == player.sportmemberUserId }
                if (member?.firstname != player.firstname || member?.lastname != player.lastname  ) {
                    member?.firstname = player.firstname
                    member?.lastname = player.lastname
                    Log.i(TAG, "Updated member name: ${member?.firstname} ${member?.lastname}")
                }
            }

        }

        val playerIds = mutableListOf<Long>()
        players.forEach { player ->
            playerIds.add(player.sportmemberUserId)
        }

        currentClubMembers.removeAll { member ->
            !playerIds.contains(member.sportmemberUserId)
        }

        // just for diagnostics
        val names = currentClubMembers.map { it.firstname + " " + it.lastname }
        if (names.distinct() != names) {
            Log.e(TAG,"${names.size - names.distinct().size} duplicate names in current members")
        }
        val sortedClubMembers = currentClubMembers.sortedBy { it.firstname }
        Log.i(TAG,"Current Members ${sortedClubMembers.map { it.firstname + " " + it.lastname + " (" + it.sportmemberUserId + ")" }}")

    }



}
package com.partynumbers42.matchmaking2v2

import com.partynumbers42.matchmaking2v2.models.Club
import com.partynumbers42.matchmaking2v2.models.Player

object AppState {
    var signedInClub: Club? = null
    var signedInClubFirebaseIdString: String = ""
    var currentClubMembers: MutableList<Player> = mutableListOf<Player>()
    var currentClubMembersIds: MutableList<Long> = mutableListOf<Long>()
    var currentParticipants: MutableList<Player> = mutableListOf<Player>()
    var nonParticipants: MutableList<Player> = mutableListOf<Player>()
}
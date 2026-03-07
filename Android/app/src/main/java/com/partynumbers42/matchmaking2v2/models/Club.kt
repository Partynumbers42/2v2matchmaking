package com.partynumbers42.matchmaking2v2.models

import com.google.firebase.firestore.PropertyName

public data class Club(
    var name: String = "",
    @get:PropertyName("sportmember_id") @set:PropertyName("sportmember_id") var sportmemberId: Long = 1,
    var members: MutableList<User> = mutableListOf<User>()
)
package com.partynumbers42.matchmaking2v2.models

import com.google.firebase.firestore.PropertyName

public data class Club(
    var name: String = "",
    @get:PropertyName("sportmember_id") @set:PropertyName("sportmember_id") var sportmemberId: Long = 1,
    var members: MutableList<Player> = mutableListOf<Player>(),
    @get:PropertyName("sportmember_username") @set:PropertyName("sportmember_username") var sportmemberUsername: String = "",
    @get:PropertyName("sportmember_password") @set:PropertyName("sportmember_password") var sportmemberPassword: String = "",
    @get:PropertyName("sportmember_user_id") @set:PropertyName("sportmember_user_id") var sportmemberUserId: Long = 1,
    @get:PropertyName("exclude_sportmember_user") @set:PropertyName("exclude_sportmember_user") var excludeSportmemberUser: Boolean = false,
)
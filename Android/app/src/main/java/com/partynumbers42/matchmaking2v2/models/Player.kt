package com.partynumbers42.matchmaking2v2.models

import com.google.firebase.firestore.PropertyName

public data class Player(
    var firstname: String = "",
    var lastname: String = "",
    var level: Int = 0,
    @get:PropertyName("sportmember_user_id") @set:PropertyName("sportmember_user_id") var sportmemberUserId: Long = 1)
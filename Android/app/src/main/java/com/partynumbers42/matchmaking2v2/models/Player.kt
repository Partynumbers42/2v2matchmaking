package com.partynumbers42.matchmaking2v2.models

import com.google.firebase.firestore.PropertyName

public data class Player(
    var name: String = "",
    var level: Int = 1,
    @get:PropertyName("sportmember_user_id") @set:PropertyName("sportmember_user_id") var sportmemberUserId: Long = 1)
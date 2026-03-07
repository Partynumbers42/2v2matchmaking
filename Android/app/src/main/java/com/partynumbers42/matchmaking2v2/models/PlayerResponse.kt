package com.partynumbers42.matchmaking2v2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
public data class PlayerResponse(
    //val id: Long,
    val name: String,
    //val status: String,
    @Json(name = "status_code") val statusCode: Int,
    @Json(name = "user_id") val userId: Long
    )

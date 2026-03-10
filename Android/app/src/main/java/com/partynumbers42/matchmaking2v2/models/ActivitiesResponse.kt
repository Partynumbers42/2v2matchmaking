package com.partynumbers42.matchmaking2v2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
public data class ActivitiesResponse(
    val id: Long,
    val name: String,
    val starttime: String,
    val endtime: String,
    @Json(name = "activities_users") val players: List<PlayerResponse>,
)

package com.partynumbers42.matchmaking2v2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
public data class MembersResponse(
    val id: Long,
    val firstname: String,
    val lastname: String,
    val role: String,
    //@Json(name = "email_address") val email: String? // Use @Json if the API name differs from your variable name
)
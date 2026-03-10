package com.partynumbers42.matchmaking2v2.models

public data class Activity(
    val name: String,
    val starttime: String,
    val endtime: String,
    val players: List<Player>,
)
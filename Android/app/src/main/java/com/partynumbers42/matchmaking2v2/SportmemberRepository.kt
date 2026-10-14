package com.partynumbers42.matchmaking2v2

import android.util.Log
import com.partynumbers42.matchmaking2v2.models.ActivitiesResponse
import com.partynumbers42.matchmaking2v2.models.Activity
import com.partynumbers42.matchmaking2v2.models.MembersResponse
import com.partynumbers42.matchmaking2v2.models.Player
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.collections.forEach
import kotlin.text.substring

private const val TAG = "SportmemberRepository"

object SportmemberRepository {
    var signedInClubSportmemberId: Long = 0
    var sportmemberUsername: String = ""
    var sportmemberPassword: String = ""
    var sportmemberUserId: Long = 0
    var excludeSportmemberUser: Boolean = false

    val client = OkHttpClient()
    private val credential: String
        get() = Credentials.basic(sportmemberUsername, sportmemberPassword)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listAdapterMembers = moshi.adapter<List<MembersResponse>>(
        Types.newParameterizedType(List::class.java, MembersResponse::class.java)
    )
    private val listAdapterActivities = moshi.adapter<List<ActivitiesResponse>>(
        Types.newParameterizedType(List::class.java, ActivitiesResponse::class.java)
    )

    fun fetchMembers(onComplete: (List<Player>) -> Unit, onError: (Exception) -> Unit) {

        var apiMembersResult: List<MembersResponse>? = null

        val requestMembers = Request.Builder()
            .url("https://api.holdsport.dk/v1/teams/$signedInClubSportmemberId/members")
            .header("Accept", "application/json")
            .header("Authorization", credential)
            .build()

        client.newCall(requestMembers).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "onFailure", e)
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val bodyString = response.body?.string() ?: ""

                    apiMembersResult = listAdapterMembers.fromJson(bodyString)

                    if (excludeSportmemberUser){
                        apiMembersResult = apiMembersResult?.filter { it.id != sportmemberUserId }
                    }


                    val membersCurrentCall = mutableListOf<Player>()
                    apiMembersResult?.forEach { member ->
                        membersCurrentCall.add(Player(member.firstname, member.lastname,0,member.id))
                        Log.i(TAG, "Found member: ${member.firstname} ${member.lastname} ${member.id}")
                    }
                    onComplete(membersCurrentCall)

                }
            }
        })

    }

    fun fetchActivities(onComplete: (List<Activity>) -> Unit,onError: (Exception) -> Unit) {

        var apiActivtiesResult: List<ActivitiesResponse>? = null

        val requestActivities = Request.Builder()
            .url("https://api.holdsport.dk/v1/teams/$signedInClubSportmemberId/activities")
            .header("Accept", "application/json")
            .header("Authorization", credential)
            .build()

        client.newCall(requestActivities).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "onFailure", e)
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val bodyString = response.body?.string() ?: ""

                    apiActivtiesResult = listAdapterActivities.fromJson(bodyString)

                    val activitiesCurrentCall = mutableListOf<Activity>()
                    apiActivtiesResult?.forEach { activity ->
                        val playersCurrentCall = mutableListOf<Player>()
                        activity.players.forEach { player ->
                            if (player.statusCode==1) {
                                val member = AppState.currentClubMembers.find { it.sportmemberUserId == player.userId }
                                if (member != null) {
                                    playersCurrentCall.add(member)
                                } else {
                                    Log.e(TAG, "Member not found: ${player.userId}")
                                }
                            } else {
                                Log.i(TAG, "Player canceled: ${player.name}")
                            }
                        }
                        val timename = "${activity.starttime.substring(8, 10)}.${activity.starttime.substring(5, 7)}.${activity.starttime.substring(0,4)} (${activity.starttime.substring(11, 16)})"
                        activitiesCurrentCall.add(Activity(activity.id,activity.name,activity.starttime,activity.endtime,timename,playersCurrentCall))
                        Log.i(TAG, "Found activity: ${activity.name} (${activity.starttime} ${activity.endtime}): ${playersCurrentCall.size} players ")
                    }
                    onComplete(activitiesCurrentCall)
                }
            }
        })

    }

    fun fetchParticipants(onComplete: (Boolean) -> Unit) {
        fetchActivities(onComplete = { activities ->

            AppState.Activities = activities
            for (activity in AppState.Activities) {
                Log.i(TAG, "Found activity on day: ${activity.timename}")
            }
            AppState.ActivityNames = activities.map { it.timename }.toMutableList()
            AppState.ActivityIds = activities.map { it.sportmemberId }.toMutableList()
            Log.i(TAG, "Activities on: ${AppState.ActivityNames} with IDs: ${AppState.ActivityIds}")

            if (activities.isNotEmpty()) {
                var startId = 0L
                if (AppState.nextActivityId in AppState.ActivityIds) {
                    startId = AppState.nextActivityId
                } else {
                    startId = activities.first().sportmemberId
                }
                updateParticipants(activities, startId)
            } else {
                Log.i(TAG, "No activities found")
            }

            onComplete(true)
        },
            onError = { exception ->
                Log.i(TAG, "Failure fetching participants", exception)
                onComplete(false)
            })

    }

    fun updateParticipants(activities: List<Activity>, id: Long) {
        AppState.nextActivityId = id
        val selectedActivity = activities.find { it.sportmemberId == id }
        Log.i(TAG, "Selected activity: ${selectedActivity?.name} on ${selectedActivity?.timename} (id:${selectedActivity?.sportmemberId})")
        val players = selectedActivity?.players
        AppState.currentParticipants = players as MutableList<Player>
        AppState.nonParticipants = AppState.currentClubMembers.filter { !AppState.currentParticipants.contains(it) }.toMutableList()

        for (player in AppState.currentParticipants){
            Log.i(TAG, "Found participant: ${player.firstname} ${player.lastname}")
        }
    }


}

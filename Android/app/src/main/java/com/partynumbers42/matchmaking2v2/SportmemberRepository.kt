package com.partynumbers42.matchmaking2v2

import android.util.Log
import android.widget.Toast
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

private const val TAG = "SportmemberRepository"

object SportmemberRepository {
    var signedInClubSportmemberId: Long = 0
    var sportmemberUsername: String = ""
    var sportmemberPassword: String = ""
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
                            val member = AppState.currentClubMembers.find { it.sportmemberUserId == player.userId }
                            if (member != null) {
                                playersCurrentCall.add(member)
                            } else {
                                Log.e(TAG, "Member not found: ${player.userId}")
                            }
                        }
                        activitiesCurrentCall.add(Activity(activity.name,activity.starttime,activity.endtime,playersCurrentCall))
                        Log.i(TAG, "Found activity: ${activity.name} (${activity.starttime} ${activity.endtime}): ${playersCurrentCall.size} players ")
                    }
                    onComplete(activitiesCurrentCall)
                }
            }
        })

    }

    fun fetchParticipants(onComplete: (Boolean) -> Unit) {
        fetchActivities(onComplete = { activities ->
            val activity = activities.firstOrNull()
            Log.i(TAG, "Next activity: ${activity?.name}")
            val players = activity?.players

            AppState.currentParticipants = players as MutableList<Player>
            AppState.nonParticipants = AppState.currentClubMembers.filter { !AppState.currentParticipants.contains(it) }.toMutableList()

            for (player in AppState.currentParticipants){
                Log.i(TAG, "Found participant: ${player.firstname} ${player.lastname}")
            }
            onComplete(true)
        },
            onError = { exception ->
                Log.i(TAG, "Failure fetching participants", exception)
                onComplete(false)
            })

    }


}

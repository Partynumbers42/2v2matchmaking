package com.partynumbers42.matchmaking2v2

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.partynumbers42.matchmaking2v2.models.MembersResponse
import com.partynumbers42.matchmaking2v2.models.Player
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Call
import okhttp3.Callback

import java.io.IOException
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

private const val TAG = "AddPlayersActivity"

class AddPlayersActivity : AppCompatActivity() {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listAdapter = moshi.adapter<List<MembersResponse>>(
        Types.newParameterizedType(List::class.java, MembersResponse::class.java)
    )

    private lateinit var members: MutableList<Player>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_players)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // TODO move too on button press

        val sportmember_id = intent.getLongExtra(EXTRA_SPORTMEMBER_ID,0)
        val username = resources.getString(R.string.sportmember_user) // update to user entry
        val password = resources.getString(R.string.sportmember_password) //update to user entry

        val client = OkHttpClient()
        val credential = Credentials.basic(username, password);

        val request = Request.Builder()
            .url("https://api.holdsport.dk/v1/teams/"+sportmember_id+"/members")
            .header("Accept", "application/json")
            .header("Authorization", credential)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "onFailure", e)
                //e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val bodyString = response.body?.string()
                    Log.i(TAG, bodyString.toString())

                    val membersList = listAdapter.fromJson(bodyString.toString())

                    membersList?.forEach { member ->
                        Log.i(TAG, "Found member: ${member.firstname} ${member.lastname}")
                    }

                //println(body) // Note: To update UI, you must use runOnUiThread { }
                }
            }
        })


        //client.newCall(request).execute().use { response ->
        //    if (!response.isSuccessful) throw IOException("Unexpected code $response")
        //    response.body!!.string()
        //}


    }

}

package com.partynumbers42.matchmaking2v2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.partynumbers42.matchmaking2v2.models.MembersResponse
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

private const val TAG = "ProfileActivity"

class ProfileActivity : AppCompatActivity() {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val listAdapterMembers = moshi.adapter<List<MembersResponse>>(
        Types.newParameterizedType(List::class.java, MembersResponse::class.java)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
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

        val requestMembers = Request.Builder()
            .url("https://api.holdsport.dk/v1/teams/"+sportmember_id+"/members")
            .header("Accept", "application/json")
            .header("Authorization", credential)
            .build()

        client.newCall(requestMembers).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "onFailure", e)
                //e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val bodyString = response.body?.string()
                    Log.i(TAG, bodyString.toString())

                    val membersList = listAdapterMembers.fromJson(bodyString.toString())

                    membersList?.forEach { member ->
                        Log.i(TAG, "Found member: ${member.firstname} ${member.lastname}")
                    }

                    //println(body) // Note: To update UI, you must use runOnUiThread { }
                }
            }
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_profile, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_logout) {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)

    }

}
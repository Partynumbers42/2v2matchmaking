package com.partynumbers42.matchmaking2v2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.partynumbers42.matchmaking2v2.models.Club
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

private const val TAG = "ProfileActivity"

class ProfileActivity : AppCompatActivity() {
    private var signedInClub: Club? = null
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val listAdapterMembers = moshi.adapter<List<MembersResponse>>(
        Types.newParameterizedType(List::class.java, MembersResponse::class.java)
    )

    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var currentMembers: MutableList<Player>
    //private lateinit var updatedMembers: MutableList<Player>
    private lateinit var currentMembersIds: MutableList<Long>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        currentMembers = mutableListOf<Player>()
        currentMembersIds = mutableListOf<Long>()

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        firestoreDb = FirebaseFirestore.getInstance()

        val clubReference = firestoreDb.collection("clubs")
        clubReference.document(FirebaseAuth.getInstance().currentUser?.uid as String)
            .get()
            .addOnSuccessListener { clubSnapshot ->
                signedInClub = clubSnapshot.toObject(Club::class.java)
                signedInClub?.members?.let {
                    currentMembers = it.toMutableList()
                }
                currentMembers.forEach { member ->
                    currentMembersIds.add(member.sportmemberUserId)
                }
                Log.i(TAG, "signed in club: $signedInClub")
                Log.i(TAG, "current members: $currentMembers")
                showMembers(currentMembers)
                showClubInfo(signedInClub)
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failure fetching signed in club", exception)
            }

        val b_updateplayers = findViewById<Button>(R.id.b_updateplayers)
        b_updateplayers.setOnClickListener {
            handleUpdatePlayers()
        }

        val b_updatelevels = findViewById<Button>(R.id.b_updatelevels)
        b_updatelevels.setOnClickListener {

            firestoreDb.collection("clubs").document(FirebaseAuth.getInstance().currentUser?.uid as String)
                .update("members", currentMembers)
                .addOnSuccessListener {
                    Log.i(TAG, "Database updated")
                    Toast.makeText(this, "Levels updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Log.i(TAG, "Failure updating database", exception)
                }

        }

    }

    private fun showClubInfo(signedInClub: Club?) {

        val tv_club_name = findViewById<TextView>(R.id.tv_clubname)
        val tv_clubid = findViewById<TextView>(R.id.tv_clubid)

        tv_club_name.text = signedInClub?.name
        tv_clubid.text = "(id:${signedInClub?.sportmemberId.toString()})"

    }

    private fun handleUpdatePlayers() { // View.OnClickListener?
        if (signedInClub == null) {
            Toast.makeText(this, "No signed in club, please wait", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "No signed in club")
            return
        }

        // Add username and password check

        Log.i(TAG,"Starting updating database ...")

        pullMembersSportmember { players ->

            Toast.makeText(this, "Found ${players.size} players on Sportmember", Toast.LENGTH_SHORT).show()

            Log.i(TAG,"Updating database ...")

            players?.forEach { player ->
                if (!currentMembersIds.contains(player.sportmemberUserId)) {
                    // add new players
                    currentMembers.add(player)
                    currentMembersIds.add(player.sportmemberUserId)
                    Log.i(TAG, "Added new member: ${player.firstname} ${player.lastname}")
                } else {
                    // update name changes from playerzone
                    val member = currentMembers.find { it.sportmemberUserId == player.sportmemberUserId }
                    if (member?.firstname != player.firstname || member?.lastname != player.lastname  ) {
                        member?.firstname = player.firstname
                        member?.lastname = player.lastname
                        Log.i(TAG, "Updated member name: ${member?.firstname} ${member?.lastname}")
                    }
                }

            }

            firestoreDb.collection("clubs").document(FirebaseAuth.getInstance().currentUser?.uid as String)
                .update("members", currentMembers)
                .addOnSuccessListener {
                    Log.i(TAG, "Database updated")
                }
                .addOnFailureListener { exception ->
                    Log.i(TAG, "Failure updating database", exception)
                }
        }

        showMembers(currentMembers)

    }

    private fun showMembers(players: MutableList<Player>) {

        val rv_membersBase = findViewById<RecyclerView>(R.id.rv_membersBase)

        rv_membersBase.adapter = PlayersAdapter(this, players)
        rv_membersBase.layoutManager = LinearLayoutManager(this)

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


    private fun pullMembersSportmember(onComplete: (List<Player>) -> Unit) {
        val sportmember_id = signedInClub?.sportmemberId
        val username = resources.getString(R.string.sportmember_user) // update to user entry
        val password = resources.getString(R.string.sportmember_password) //update to user entry

        var apiMembersResult: List<MembersResponse>? = null

        val client = OkHttpClient()
        val credential = Credentials.basic(username, password);

        val requestMembers = Request.Builder()
            .url("https://api.holdsport.dk/v1/teams/" + sportmember_id + "/members")
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
                    apiMembersResult = listAdapterMembers.fromJson(bodyString.toString())

                    runOnUiThread {
                        val membersCurrentCall = mutableListOf<Player>()
                        apiMembersResult?.forEach { member ->
                            membersCurrentCall.add(Player(member.firstname, member.lastname,0,member.id))
                            //Log.i(TAG, "Found member: ${member.firstname} ${member.lastname}")
                        }
                        onComplete(membersCurrentCall)
                    }

                }
            }
        })

    }
}
package com.partynumbers42.matchmaking2v2

import android.content.Intent
import android.os.Bundle
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
import com.partynumbers42.matchmaking2v2.models.Club
import com.partynumbers42.matchmaking2v2.models.Player

private const val TAG = "ProfileActivity"

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //var levelSortedMembers = AppState.currentClubMembers.sortedWith(compareBy({ it.level }, { it.firstname }, { it.lastname })).toMutableList()
        showMembers(AppState.currentClubMembers)
        showClubInfo(AppState.signedInClub)

        val b_updatelevels = findViewById<Button>(R.id.b_updatelevels)
        b_updatelevels.setOnClickListener {

            // update firebase database
            FirebaseRepository.saveCurrentMembers(this) { success ->
                if (success) {
                    Toast.makeText(this, "Database updated!", Toast.LENGTH_SHORT).show()
                    //levelSortedMembers = AppState.currentClubMembers.sortedWith(compareBy({ it.level }, { it.firstname }, { it.lastname })).toMutableList()
                    showMembers(AppState.currentClubMembers) //only necessary if changes change order of / existence in of UI
                } else {
                    Toast.makeText(this, "Failed to update Database", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun showClubInfo(signedInClub: Club?) {

        val tv_club_name = findViewById<TextView>(R.id.tv_clubname)
        val tv_clubid = findViewById<TextView>(R.id.tv_clubid)

        tv_club_name.text = signedInClub?.name
        if (AppState.usesSportmember) {
            tv_clubid.text = "(id:${signedInClub?.sportmemberId.toString()})"
        } else {
            tv_clubid.text = ""
        }

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
            // reset app state
            AppState.isDataLoaded = false
            AppState.signedInClub = null
            AppState.signedInClubFirebaseIdString = ""
            AppState.usesSportmember = false
            AppState.currentClubMembers.clear()
            AppState.currentClubMembersIds.clear()
            AppState.nextActivityId = 0
            AppState.currentParticipants.clear()
            AppState.nonParticipants.clear()
            // reset sportmember state
            SportmemberRepository.signedInClubSportmemberId = 0
            SportmemberRepository.sportmemberUsername = ""
            SportmemberRepository.sportmemberPassword = ""
            SportmemberRepository.sportmemberUserId = 0
            //
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)

    }



}
package com.partynumbers42.matchmaking2v2

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.partynumbers42.matchmaking2v2.models.Club

private const val TAG = "FirebaseRepository"

object FirebaseRepository {

    private val firestoreDb by lazy {FirebaseFirestore.getInstance()}
    private val firebaseAuth by lazy {FirebaseAuth.getInstance()}

    fun redirectToLogin(context: Context){
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    fun fetchSignedInClub(context: Context, onComplete: (Boolean) -> Unit) {

        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e(TAG, "No user logged in")
            redirectToLogin(context)
            onComplete(false)
            return
        }

        AppState.signedInClubFirebaseIdString = uid

        firestoreDb.collection("clubs").document(AppState.signedInClubFirebaseIdString)
            .get()
            .addOnSuccessListener { userSnapshot ->

                // Load Club data
                AppState.signedInClub  = userSnapshot.toObject(Club::class.java)
                Log.i(TAG, "signed in club: ${AppState.signedInClub}")

                AppState.signedInClub?.members?.let {
                    AppState.currentClubMembers = it.toMutableList()
                }
                AppState.currentClubMembers.forEach { member ->
                    AppState.currentClubMembersIds.add(member.sportmemberUserId)
                }
                Log.i(TAG, "current members: ${AppState.currentClubMembers}")

                // Load Sportmember username and password
                SportmemberRepository.signedInClubSportmemberId = AppState.signedInClub?.sportmemberId ?: 0L
                SportmemberRepository.sportmemberUsername = AppState.signedInClub?.sportmemberUsername ?: ""
                SportmemberRepository.sportmemberPassword = AppState.signedInClub?.sportmemberPassword ?: ""
                SportmemberRepository.excludeSportmemberUser = AppState.signedInClub?.excludeSportmemberUser ?: false
                SportmemberRepository.sportmemberUserId = AppState.signedInClub?.sportmemberUserId ?: 0L

                onComplete(true)
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failure fetching signed in club", exception)
                onComplete(false)
            }

    }

    fun saveCurrentMembers(context: Context, onComplete: (Boolean) -> Unit) {

        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e(TAG, "No user logged in")
            redirectToLogin(context)
            onComplete(false)
            return
        }

        firestoreDb.collection("clubs").document(AppState.signedInClubFirebaseIdString)
            .update("members", AppState.currentClubMembers)
            .addOnSuccessListener {
                Log.i(TAG, "Database updated")
                onComplete(true)
            }
            .addOnFailureListener { exception ->
                Log.i(TAG, "Failure updating database", exception)
                onComplete(false)
            }

    }

    // suggestion from the chatbot not used yet (check if needed)
    fun updateClubInfo(context: Context, updates: Map<String, Any>, onComplete: (Boolean) -> Unit = {}) {
        val uid = firebaseAuth.currentUser?.uid ?: return onComplete(false)

        firestoreDb.collection("clubs").document(uid)
            .update(updates)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

}
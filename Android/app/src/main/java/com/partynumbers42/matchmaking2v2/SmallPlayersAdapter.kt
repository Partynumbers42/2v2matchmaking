package com.partynumbers42.matchmaking2v2

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.partynumbers42.matchmaking2v2.models.Player


class SmallPlayersAdapter(
    val context: Context,
    val players: MutableList<Player>
) : RecyclerView.Adapter<SmallPlayersAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.small_player_view,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val player = players[position]
        holder.bind(player)
    }

    override fun getItemCount() = players.size

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val tv_playername = itemView.findViewById<TextView>(R.id.tv_player1)

        fun bind(player: Player) {

            tv_playername.text = player.firstname.split(" ").first() + " " + player.lastname.first() + "."


        }

    }

    private val levels = listOf("0","1","2","3")

}

package com.partynumbers42.matchmaking2v2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView
import androidx.recyclerview.widget.RecyclerView

class PlayersAdapter(
    val context: Context,
//    activity: MainActivity,
    val players: List<MainActivity.Player>
) : RecyclerView.Adapter<PlayersAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.names_recycler_view_row,parent,false)
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

        val tv_player1 = itemView.findViewById<TextView>(R.id.tv_player1)
        //val tv_player2 = itemView.findViewById<TextView>(R.id.tv_player2)
        //val tv_player3 = itemView.findViewById<TextView>(R.id.tv_player3)
        //val tv_player4 = itemView.findViewById<TextView>(R.id.tv_player4)

        fun bind(player: MainActivity.Player) {
            tv_player1.text = player.playerName
        }

    }

}

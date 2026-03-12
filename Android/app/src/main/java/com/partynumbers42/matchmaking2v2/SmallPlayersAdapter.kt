package com.partynumbers42.matchmaking2v2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.partynumbers42.matchmaking2v2.models.Player


class SmallPlayersAdapter(
    val context: Context,
    val players: MutableList<Player>,
    val button: Boolean = false,
    val onPlayerClick: (Player) -> Unit = {}
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
        holder.bind(player,button)
        holder.b_player.setOnClickListener {
            onPlayerClick(player)
        }
    }

    override fun getItemCount() = players.size

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val b_player = itemView.findViewById<Button>(R.id.b_player)

        fun bind(player: Player, button: Boolean) {
            b_player.isEnabled = button
            val firstName = player.firstname.split(" ").firstOrNull() ?: ""
            val lastInitial = player.lastname.firstOrNull() ?: ""
            b_player.text = "$firstName $lastInitial."

        }

    }

}

package com.partynumbers42.matchmaking2v2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MatchupsAdapter(
    val context: Context,
    val matchups: List<MainActivity.Matchup>
) : RecyclerView.Adapter<MatchupsAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.matchup_view_row,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val matchup = matchups[position]
        holder.bind(matchup)
    }

    override fun getItemCount() = matchups.size

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val tv_player1 = itemView.findViewById<TextView>(R.id.tv_player1)
        val tv_player2 = itemView.findViewById<TextView>(R.id.tv_player2)
        val tv_player3 = itemView.findViewById<TextView>(R.id.tv_player3)
        val tv_player4 = itemView.findViewById<TextView>(R.id.tv_player4)

        fun bind(matchup: MainActivity.Matchup) {
            tv_player1.text = matchup.player1.firstname.split(" ").first() + " " + matchup.player1.lastname.first() + "."
            tv_player2.text = matchup.player2.firstname.split(" ").first() + " " + matchup.player2.lastname.first() + "."
            tv_player3.text = matchup.player3.firstname.split(" ").first() + " " + matchup.player3.lastname.first() + "."
            tv_player4.text = matchup.player4.firstname.split(" ").first() + " " + matchup.player4.lastname.first() + "."
        }

    }

}

package com.partynumbers42.matchmaking2v2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RoundsAdapter(
    val context: Context,
    val rounds: List<MainActivity.Round>
) : RecyclerView.Adapter<RoundsAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.rounds_view_row,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val round = rounds[position]
        holder.bind(round,position)
    }

    override fun getItemCount() = rounds.size

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val rvMatchups = itemView.findViewById<RecyclerView>(R.id.rvMatchups)
        val tv_round = itemView.findViewById<TextView>(R.id.tv_round)
        val tv_breakers = itemView.findViewById<TextView>(R.id.tv_breakers)

        fun bind(round: MainActivity.Round,position: Int) {
            rvMatchups.adapter = MatchupsAdapter(context, round.matchups)
            rvMatchups.layoutManager = LinearLayoutManager(context)
            tv_round.text = "Round ${position+1}"
            tv_breakers.text = "Breakers: ${round.breakers.joinToString { it.playerName }}"
        }
    }
}

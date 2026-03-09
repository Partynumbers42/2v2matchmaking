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


class PlayersAdapter(
    val context: Context,
    val players: MutableList<Player>,
    val button_state: Boolean = false
) : RecyclerView.Adapter<PlayersAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.player_view_row,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val player = players[position]
        holder.bind(player,button_state)
    }

    override fun getItemCount() = players.size

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val tv_playername = itemView.findViewById<TextView>(R.id.tv_player1)
        val b_addplayer = itemView.findViewById<ImageButton>(R.id.b_addplayer)
        val spinner = itemView.findViewById<Spinner>(R.id.spinner)

        fun bind(player: Player,button_state: Boolean) {

            // set name
            tv_playername.text = player.firstname + " " + player.lastname

            //remove button if unwanted
            if (button_state) {
                b_addplayer.visibility = View.VISIBLE
            } else {
                b_addplayer.visibility = View.GONE
            }

            val adapter = ArrayAdapter(
                itemView.context,
                R.layout.spinner_item, // Standard layout for the spinner closed view
                levels
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinner.adapter = adapter

            spinner.setSelection(levels.indexOf(player.level.toString()))

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedLevel = levels[position]
                    player.level = selectedLevel.toInt()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

    }

    private val levels = listOf("0","1","2","3")

}

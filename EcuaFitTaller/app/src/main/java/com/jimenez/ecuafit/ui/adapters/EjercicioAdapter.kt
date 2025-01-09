package com.jimenez.ecuafit.ui.adapters


import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jimenez.ecuafit.R
import com.jimenez.ecuafit.data.entities.Comida
import com.jimenez.ecuafit.data.entities.Ejercicio
import com.jimenez.ecuafit.databinding.LayoutEjercicioBinding
import com.squareup.picasso.Picasso


class EjercicioAdapter(
    private var fnClick: (Ejercicio) -> Unit,



    ) :

    RecyclerView.Adapter<EjercicioAdapter.EjercicioViewHolder>() {

    var items: List<Ejercicio> = listOf()

    class EjercicioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding: LayoutEjercicioBinding = LayoutEjercicioBinding.bind(view)

        fun render(
            item: Ejercicio,fnClick: (Ejercicio) -> Unit


        ) {
            binding.txtNameEje.text = item.nombre
            Picasso.get().load(item.cuerpo).into(binding.imgCuerpo)
            binding.tarjeta.setOnClickListener {
                fnClick(item)
            }


        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EjercicioAdapter.EjercicioViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return EjercicioViewHolder(
            inflater.inflate(
                R.layout.layout_ejercicio,
                parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: EjercicioAdapter.EjercicioViewHolder, position: Int) {
        holder.render(items[position] ,fnClick)
    }

    override fun getItemCount(): Int = items.size

    fun updateListItems(newItems: List<Ejercicio>) {
        this.items = this.items.plus(newItems)
        notifyDataSetChanged()

    }

    fun replaceListAdapter(newItems: List<Ejercicio>) {
        this.items = newItems
        notifyDataSetChanged()

    }

}
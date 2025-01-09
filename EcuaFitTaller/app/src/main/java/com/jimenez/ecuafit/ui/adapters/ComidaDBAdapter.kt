package com.jimenez.ecuafit.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jimenez.ecuafit.R
import com.jimenez.ecuafit.data.entities.Comida
import com.jimenez.ecuafit.data.entities.ComidaDB
import com.jimenez.ecuafit.databinding.ComidaItemBinding
import com.jimenez.ecuafit.databinding.LayoutComidasBinding
import com.squareup.picasso.Picasso

class ComidaDBAdapter() : RecyclerView.Adapter<ComidaDBAdapter.ComidaViewHolder>() {
    var items: List<ComidaDB> = listOf()

    class ComidaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding: ComidaItemBinding = ComidaItemBinding.bind(view)
        fun render(
            item: ComidaDB
        ) {
            binding.txtCantidad.text= item.cantidad.toString()
            binding.txtNombre.text = item.nombre
            binding.txtCalorias.text = item.calorias.toString() + " kcals"
            Picasso.get().load(item.foto).into(binding.imgComida)
            binding.txtFecha.text = item.fecha.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComidaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ComidaDBAdapter.ComidaViewHolder(
            inflater.inflate(
                R.layout.comida_item,
                parent, false
            )
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ComidaViewHolder, position: Int) {
        holder.render(items[position])
    }


}
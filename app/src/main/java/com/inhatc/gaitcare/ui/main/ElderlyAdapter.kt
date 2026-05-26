package com.inhatc.gaitcare.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.inhatc.gaitcare.data.db.entity.Elderly
import com.inhatc.gaitcare.databinding.ItemElderlyBinding

class ElderlyAdapter(
    private val onClick: (Elderly) -> Unit
) : ListAdapter<Elderly, ElderlyAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemElderlyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemElderlyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(elderly: Elderly) {
            binding.tvName.text = elderly.name
            binding.tvAge.text = "${elderly.age}세 · ${elderly.gender}"
            binding.tvRoom.text = if (elderly.roomNumber.isNotBlank()) elderly.roomNumber else "-"
            binding.tvCareLevel.text = "돌봄 ${elderly.careLevel}등급"
            binding.tvInitial.text = elderly.name.take(1)

            if (elderly.diagnosisNote.isNotBlank()) {
                binding.tvDiagnosis.text = elderly.diagnosisNote
                binding.tvDiagnosis.visibility = android.view.View.VISIBLE
            } else {
                binding.tvDiagnosis.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick(elderly) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Elderly>() {
            override fun areItemsTheSame(a: Elderly, b: Elderly) = a.id == b.id
            override fun areContentsTheSame(a: Elderly, b: Elderly) = a == b
        }
    }
}

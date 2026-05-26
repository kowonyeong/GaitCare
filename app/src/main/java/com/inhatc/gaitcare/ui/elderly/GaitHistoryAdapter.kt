package com.inhatc.gaitcare.ui.elderly

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.inhatc.gaitcare.R
import com.inhatc.gaitcare.data.db.entity.GaitSession
import com.inhatc.gaitcare.databinding.ItemGaitHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GaitHistoryAdapter(
    private val onItemClick: (GaitSession) -> Unit,
    private val onDeleteClick: (GaitSession) -> Unit
) : ListAdapter<GaitSession, GaitHistoryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGaitHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemGaitHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(session: GaitSession) {
            val ctx = binding.root.context
            val fmt = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)

            binding.tvDate.text = fmt.format(Date(session.measuredAt))
            binding.tvScore.text = session.totalScore.toString()
            binding.tvScoreLabel.text = getLabel(session.totalScore)
            binding.tvDuration.text = "${session.durationSeconds}초"
            binding.tvSteps.text = "—"
            binding.tvCadence.text = "%.3f g".format(session.cadenceSpm)

            val color = ContextCompat.getColor(ctx, getScoreColor(session.totalScore))
            binding.tvScore.setTextColor(color)
            binding.tvScoreLabel.setTextColor(color)

            if (session.memo.isNotBlank()) {
                binding.tvMemo.text = session.memo
                binding.tvMemo.visibility = android.view.View.VISIBLE
            } else {
                binding.tvMemo.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onItemClick(session) }
            binding.btnDelete.setOnClickListener { onDeleteClick(session) }
        }
    }

    private fun getLabel(score: Int) = when (score) {
        in 80..100 -> "매우 좋음"
        in 65..79 -> "좋음"
        in 50..64 -> "보통"
        in 35..49 -> "주의 필요"
        else -> "위험"
    }

    private fun getScoreColor(score: Int) = when (score) {
        in 80..100 -> R.color.score_excellent
        in 65..79 -> R.color.score_good
        in 50..64 -> R.color.score_normal
        in 35..49 -> R.color.score_poor
        else -> R.color.score_bad
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<GaitSession>() {
            override fun areItemsTheSame(a: GaitSession, b: GaitSession) = a.id == b.id
            override fun areContentsTheSame(a: GaitSession, b: GaitSession) = a == b
        }
    }
}

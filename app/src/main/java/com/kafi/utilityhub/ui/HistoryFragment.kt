package com.kafi.utilityhub.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kafi.utilityhub.R
import com.kafi.utilityhub.data.HistoryRepository
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_history) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = HistoryRepository(requireContext())
        val rv = view.findViewById<RecyclerView>(R.id.rvHistory)
        val empty = view.findViewById<TextView>(R.id.tvEmpty)

        val adapter = HistoryAdapter { item ->
            viewLifecycleOwner.lifecycleScope.launch { repo.delete(item) }
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeAll().collect { list ->
                adapter.submitList(list)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
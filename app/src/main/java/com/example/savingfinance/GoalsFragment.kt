package com.example.savingfinance

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class GoalsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userId: String
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_goals, container, false)
        recyclerView = view.findViewById(R.id.goalsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        firestore = FirebaseFirestore.getInstance()
        userId = arguments?.getString("USER_ID") ?: ""

        fetchGoals()

        return view
    }

    private fun fetchGoals() {
        if (userId.isEmpty()) return

        firestore.collection("users").document(userId)
            .collection("goals")
            .get()
            .addOnSuccessListener { documents ->
                // Pass the Firestore documents directly to the adapter
                recyclerView.adapter = GoalsAdapter(documents)
            }
            .addOnFailureListener { e ->
                Log.e("GoalsFragment", "Error fetching goals", e)
            }
    }

    companion object {
        fun newInstance(userId: String): GoalsFragment {
            val fragment = GoalsFragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            fragment.arguments = args
            return fragment
        }
    }

    // Adapter that works directly with Firestore QueryDocumentSnapshot
    inner class GoalsAdapter(private val goals: QuerySnapshot) :
        RecyclerView.Adapter<GoalsAdapter.GoalViewHolder>() {

        inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val categoryText: TextView = itemView.findViewById(R.id.goalCategory)
            val progressText: TextView = itemView.findViewById(R.id.goalProgress)
            val progressBar: ProgressBar = itemView.findViewById(R.id.goalProgressBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
            return GoalViewHolder(view)
        }

        override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
            val document = goals.documents[position]

            // Extract data directly from the Firestore document
            val currentAmount = document.getDouble("currentAmount")?.toInt() ?: 0
            val goalAmount = document.getDouble("goalAmount")?.toInt() ?: 0
            val category = document.getString("category") ?: "Unknown"

            holder.categoryText.text = category
            holder.progressText.text = "$${currentAmount} of $${goalAmount}"
            holder.progressBar.max = goalAmount
            holder.progressBar.progress = currentAmount
        }

        override fun getItemCount() = goals.size()
    }
}
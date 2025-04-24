package com.example.savingfinance

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch

class GoalsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userId: String
    private lateinit var firestore: FirebaseFirestore

    private var currencySymbol: String = "$"

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

        fetchPreferredCurrency()

        return view
    }

    override fun onResume() {
        super.onResume()
        // Fetch the current currency preference whenever the fragment becomes visible
        fetchPreferredCurrency()
    }

    private fun fetchPreferredCurrency() {
        if (userId.isEmpty()) {
            fetchGoals() // Proceed with default $ symbol
            return
        }

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Get the preferred currency or use $ as default
                    currencySymbol = document.getString("preferredCurrency") ?: "$"
                    Log.d("GoalsFragment", "Using currency symbol: $currencySymbol")
                }
                // Now that we have the currency, fetch the goals
                fetchGoals()
            }
            .addOnFailureListener { e ->
                Log.e("GoalsFragment", "Error fetching currency preference", e)
                // Continue with default $ symbol
                fetchGoals()
            }
    }

    private fun fetchGoals() {
        if (userId.isEmpty()) return

        firestore.collection("users").document(userId)
            .collection("goals")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Handle empty collection
                    val emptyView = view?.findViewById<TextView>(R.id.empty_view)
                    emptyView?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    
                    // If there are no goals, update the main goal display with default values
                    (activity as? ActivityHome)?.updateMainGoalDisplay("No goals set yet", 0, 0)
                } else {
                    // Sort documents by currentAmount in descending order
                    val sortedDocuments = documents.documents.sortedByDescending { 
                        it.getDouble("currentAmount") ?: 0.0 
                    }
                    val emptyView = view?.findViewById<TextView>(R.id.empty_view)
                    emptyView?.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = GoalsAdapter(sortedDocuments)
                    updateMainGoalDisplay(documents)
                }
            }
            .addOnFailureListener { e ->
                Log.e("GoalsFragment", "Error fetching goals", e)
                val emptyView = view?.findViewById<TextView>(R.id.empty_view)
                emptyView?.visibility = View.VISIBLE
                emptyView?.text = "Error loading goals"
                recyclerView.visibility = View.GONE
            }
    }

    private fun updateMainGoalDisplay(documents: QuerySnapshot) {
        val mainGoal = documents.find { it.getBoolean("isMainGoal") == true }
        mainGoal?.let {
            val goalName = it.getString("name") ?: "Main Goal"
            val currentAmount = it.getDouble("currentAmount")?.toInt() ?: 0
            val goalAmount = it.getDouble("goalAmount")?.toInt() ?: 0
            
            (activity as? ActivityHome)?.updateMainGoalDisplay(goalName, currentAmount, goalAmount)
        }
    }

    private fun showEditGoalDialog(goalId: String, currentName: String, currentAmount: Int, currentProgress: Int, isMainGoal: Boolean) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_edit_goal, null)

        val nameInput = view.findViewById<TextInputEditText>(R.id.editGoalNameInput)
        val amountInput = view.findViewById<TextInputEditText>(R.id.editGoalAmountInput)
        val progressInput = view.findViewById<TextInputEditText>(R.id.editCurrentAmountInput)
        val mainGoalSwitch = view.findViewById<SwitchMaterial>(R.id.mainGoalSwitch)
        val saveButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveGoalButton)

        // Set current values
        nameInput.setText(currentName)
        amountInput.setText(currentAmount.toString())
        progressInput.setText(currentProgress.toString())
        mainGoalSwitch.isChecked = isMainGoal

        saveButton.setOnClickListener {
            val newName = nameInput.text.toString()
            val newAmount = amountInput.text.toString().toIntOrNull() ?: currentAmount
            val newProgress = progressInput.text.toString().toIntOrNull() ?: currentProgress
            val isMain = mainGoalSwitch.isChecked

            // If setting as main goal, we need to update all other goals to not be main
            if (isMain) {
                updateMainGoalInFirestore(goalId, newName, newAmount, newProgress)
            } else {
                // Just update this goal
                updateGoalInFirestore(goalId, newName, newAmount, newProgress, isMain)
            }
            
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }
    
    private fun updateMainGoalInFirestore(goalId: String, name: String, amount: Int, progress: Int) {
        // Start a batch write
        val batch = firestore.batch()
        val userGoalsRef = firestore.collection("users").document(userId).collection("goals")
        
        // First get all goals to find any that are currently marked as main
        userGoalsRef.get().addOnSuccessListener { documents ->
            // For each goal that is not this one but is marked as main, update it to not be main
            for (doc in documents) {
                if (doc.id != goalId && doc.getBoolean("isMainGoal") == true) {
                    batch.update(doc.reference, "isMainGoal", false)
                }
            }
            
            // Update this goal to be the main goal
            val goalRef = userGoalsRef.document(goalId)
            batch.update(goalRef, mapOf(
                "name" to name,
                "goalAmount" to amount,
                "currentAmount" to progress,
                "isMainGoal" to true
            ))
            
            // Commit the batch
            batch.commit().addOnSuccessListener {
                Toast.makeText(context, "Goal updated and set as main goal", Toast.LENGTH_SHORT).show()
                fetchGoals() // Refresh the goals list
            }.addOnFailureListener { e ->
                Log.e("GoalsFragment", "Error updating goals batch", e)
                Toast.makeText(context, "Failed to update goal", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateGoalInFirestore(goalId: String, name: String, amount: Int, progress: Int, isMain: Boolean) {
        firestore.collection("users").document(userId)
            .collection("goals").document(goalId)
            .update(mapOf(
                "name" to name,
                "goalAmount" to amount,
                "currentAmount" to progress,
                "isMainGoal" to isMain
            ))
            .addOnSuccessListener {
                Toast.makeText(context, "Goal updated successfully", Toast.LENGTH_SHORT).show()
                fetchGoals() // Refresh the goals list
            }
            .addOnFailureListener { e ->
                Log.e("GoalsFragment", "Error updating goal", e)
                Toast.makeText(context, "Failed to update goal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteGoal(goalId: String) {
        firestore.collection("users").document(userId)
            .collection("goals").document(goalId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Goal deleted successfully", Toast.LENGTH_SHORT).show()
                fetchGoals() // Refresh the goals list
            }
            .addOnFailureListener { e ->
                Log.e("GoalsFragment", "Error deleting goal", e)
                Toast.makeText(context, "Failed to delete goal", Toast.LENGTH_SHORT).show()
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

    // Update the adapter to take a list of documents instead of QuerySnapshot
    inner class GoalsAdapter(private val goals: List<com.google.firebase.firestore.DocumentSnapshot>) :
        RecyclerView.Adapter<GoalsAdapter.GoalViewHolder>() {

        inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val categoryText: TextView = itemView.findViewById(R.id.goalCategory)
            val progressText: TextView = itemView.findViewById(R.id.goalProgress)
            val progressBar: ProgressBar = itemView.findViewById(R.id.goalProgressBar)
            val editButton: ImageButton = itemView.findViewById(R.id.editButton)
            val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
            return GoalViewHolder(view)
        }

        override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
            val document = goals[position]
            val goalId = document.id

            val currentAmount = document.getDouble("currentAmount")?.toInt() ?: 0
            val goalAmount = document.getDouble("goalAmount")?.toInt() ?: 0
            val category = document.getString("name") ?: "Unknown"
            val isMainGoal = document.getBoolean("isMainGoal") ?: false

            // Add star emoji for main goal
            holder.categoryText.text = if (isMainGoal) "⭐ $category" else category
            holder.progressText.text = "$currencySymbol${currentAmount} of $currencySymbol${goalAmount}"
            holder.progressBar.max = goalAmount
            holder.progressBar.progress = currentAmount

            holder.editButton.setOnClickListener {
                showEditGoalDialog(goalId, category, goalAmount, currentAmount, isMainGoal)
            }

            holder.deleteButton.setOnClickListener {
                // Show confirmation dialog before deleting
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Goal")
                    .setMessage("Are you sure you want to delete this goal?")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteGoal(goalId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        override fun getItemCount(): Int = goals.size
    }
}
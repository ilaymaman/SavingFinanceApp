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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userId: String
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transactions, container, false)
        recyclerView = view.findViewById(R.id.transactionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        firestore = FirebaseFirestore.getInstance()
        userId = arguments?.getString("USER_ID") ?: ""

        fetchTransactions()

        return view
    }

    private fun fetchTransactions() {
        if (userId.isEmpty()) return

        firestore.collection("users").document(userId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { documents ->
                // Pass the Firestore documents directly to the adapter
                recyclerView.adapter = TransactionsAdapter(documents)
            }
            .addOnFailureListener { e ->
                Log.e("TransactionsFragment", "Error fetching transactions", e)
            }
    }

    companion object {
        fun newInstance(userId: String): TransactionFragment {
            val fragment = TransactionFragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            fragment.arguments = args
            return fragment
        }
    }

    // Adapter that works directly with Firestore QueryDocumentSnapshot
    inner class TransactionsAdapter(private val transactions: QuerySnapshot) :
        RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

        inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val amountText: TextView = itemView.findViewById(R.id.transactionAmount)
            val categoryText: TextView = itemView.findViewById(R.id.transactionCategory)
            val dateText: TextView = itemView.findViewById(R.id.transactionDate)
            val descriptionText: TextView = itemView.findViewById(R.id.transactionDescription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            return TransactionViewHolder(view)
        }

        override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
            val document = transactions.documents[position]
            val simpleDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            // Extract data directly from the Firestore document
            val amount = document.getDouble("amount")?.toInt() ?: 0
            val category = document.getString("category") ?: "Unknown"
            val date = document.getTimestamp("date")?.toDate() ?: Date()
            val description = document.getString("description") ?: ""

            holder.amountText.text = "$${amount}"
            holder.categoryText.text = category
            holder.dateText.text = simpleDateFormat.format(date)
            holder.descriptionText.text = description
        }

        override fun getItemCount() = transactions.size()
    }
}
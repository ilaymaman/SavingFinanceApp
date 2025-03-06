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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    fun getCurrentLocalDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault() // Device's current timezone
        return sdf.format(Date()) // Formats current time in local timezone
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

            // Extract data directly from the Firestore document
            val amount = document.getDouble("amount")?.toInt() ?: 0
            val type = document.getString("type") ?: "Unknown"
            val timestampString = document.getString("timestamp") ?: ""
            val description = document.getString("description") ?: ""

            // Format the stored date string to your preferred display format
            val formattedDate = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(timestampString)
                val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                outputFormat.format(date)
            } catch (e: Exception) {
                "Invalid date"
            }

            holder.amountText.text = "$$amount"
            holder.categoryText.text = type
            holder.descriptionText.text = description
            holder.dateText.text = formattedDate
        }

        override fun getItemCount() = transactions.size()
    }
}
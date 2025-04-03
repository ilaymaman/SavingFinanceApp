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
import com.google.firebase.Timestamp
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

        // First try to migrate any incorrect timestamps
        migrateIncorrectTimestamps()
        
        // Then fetch transactions
        fetchTransactions()

        return view
    }

    private fun fetchTransactions() {
        if (userId.isEmpty()) return

        firestore.collection("users").document(userId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Handle empty collection
                    val emptyView = view?.findViewById<TextView>(R.id.empty_view)
                    emptyView?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    // Pass the Firestore documents directly to the adapter
                    val emptyView = view?.findViewById<TextView>(R.id.empty_view)
                    emptyView?.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = TransactionsAdapter(documents)
                }
            }
            .addOnFailureListener { e ->
                Log.e("TransactionsFragment", "Error fetching transactions", e)
                val emptyView = view?.findViewById<TextView>(R.id.empty_view)
                emptyView?.visibility = View.VISIBLE
                emptyView?.text = "Error loading transactions"
                recyclerView.visibility = View.GONE
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
            try {
                if (position >= 0 && position < transactions.documents.size) {
                    val document = transactions.documents[position]
                    if (document != null) {
                        // Extract data directly from the Firestore document
                        val amount = document.getDouble("amount")?.toInt() ?: 0
                        val type = document.getString("type") ?: "Unknown"
                        val description = document.getString("description") ?: ""
                        
                        // Handle timestamp which could be different types
                        val formattedDate = try {
                            // Check if timestamp field exists at all
                            if (document.contains("timestamp")) {
                                // Try to infer the type of the field
                                val timestampValue = document.get("timestamp")
                                
                                when (timestampValue) {
                                    // Case 1: It's a Date object
                                    is Date -> {
                                        val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                                        outputFormat.format(timestampValue)
                                    }
                                    // Case 2: It's a Timestamp object
                                    is com.google.firebase.Timestamp -> {
                                        val date = (timestampValue as com.google.firebase.Timestamp).toDate()
                                        val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                                        outputFormat.format(date)
                                    }
                                    // Case 3: It's a Long (milliseconds since epoch)
                                    is Long -> {
                                        val date = Date(timestampValue)
                                        val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                                        outputFormat.format(date)
                                    }
                                    // Case 4: It's a String
                                    is String -> {
                                        try {
                                            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                            val date = inputFormat.parse(timestampValue)
                                            val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                                            outputFormat.format(date)
                                        } catch (e: Exception) {
                                            // Just return the string if we can't parse it
                                            "Date: $timestampValue"
                                        }
                                    }
                                    // Case 5: It's some other type we don't handle
                                    else -> {
                                        Log.w("TransactionFragment", 
                                            "Timestamp field exists but in an unknown format: ${timestampValue?.javaClass}")
                                        "Unknown date format"
                                    }
                                }
                            } else {
                                // Field doesn't exist at all
                                Log.w("TransactionFragment", "No timestamp field found in document ${document.id}")
                                "No date"
                            }
                        } catch (e: Exception) {
                            Log.e("TransactionFragment", "Error handling timestamp", e)
                            "Date error"
                        }

                        holder.amountText.text = "$$amount"
                        holder.categoryText.text = type
                        holder.descriptionText.text = description
                        holder.dateText.text = formattedDate
                    } else {
                        setDefaultValues(holder)
                    }
                } else {
                    setDefaultValues(holder)
                }
            } catch (e: Exception) {
                Log.e("TransactionFragment", "Error binding transaction", e)
                setDefaultValues(holder)
            }
        }
        
        private fun setDefaultValues(holder: TransactionViewHolder) {
            holder.amountText.text = "$0"
            holder.categoryText.text = "Unknown"
            holder.descriptionText.text = ""
            holder.dateText.text = "Unknown date"
        }

        override fun getItemCount(): Int {
            return try {
                transactions.size()
            } catch (e: Exception) {
                Log.e("TransactionFragment", "Error getting item count", e)
                0
            }
        }
    }

    // Add this method to fix any incompatible timestamps
    private fun migrateIncorrectTimestamps() {
        if (userId.isEmpty()) return
        
        Log.d("TransactionFragment", "Starting timestamp migration check...")
        
        firestore.collection("users").document(userId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { documents ->
                var migratedCount = 0
                
                for (document in documents) {
                    // Only process documents that need migration
                    if (document.contains("timestamp") && !(document.get("timestamp") is Timestamp)) {
                        try {
                            val timestampValue = document.get("timestamp")
                            var newTimestamp: Timestamp? = null
                            
                            when (timestampValue) {
                                // Case 1: It's a Date object
                                is Date -> {
                                    newTimestamp = Timestamp(timestampValue)
                                }
                                // Case 2: It's a Long (milliseconds since epoch)
                                is Long -> {
                                    newTimestamp = Timestamp(Date(timestampValue))
                                }
                                // Case 3: It's a String
                                is String -> {
                                    try {
                                        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        val date = inputFormat.parse(timestampValue)
                                        if (date != null) {
                                            newTimestamp = Timestamp(date)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TransactionFragment", "Failed to parse timestamp string: $timestampValue", e)
                                    }
                                }
                            }
                            
                            // Update the document if we were able to create a valid timestamp
                            if (newTimestamp != null) {
                                document.reference.update("timestamp", newTimestamp)
                                    .addOnSuccessListener {
                                        migratedCount++
                                        Log.d("TransactionFragment", "Successfully migrated timestamp for document ${document.id}")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("TransactionFragment", "Failed to migrate timestamp for document ${document.id}", e)
                                    }
                            }
                        } catch (e: Exception) {
                            Log.e("TransactionFragment", "Error during timestamp migration for document ${document.id}", e)
                        }
                    }
                }
                
                Log.d("TransactionFragment", "Timestamp migration check completed. Migrated $migratedCount documents.")
            }
            .addOnFailureListener { e ->
                Log.e("TransactionFragment", "Failed to fetch documents for timestamp migration", e)
            }
    }
}
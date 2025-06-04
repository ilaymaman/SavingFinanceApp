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

    private var currencySymbol: String = "$"

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

        migrateIncorrectTimestamps()

        fetchPreferredCurrency()

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchPreferredCurrency()
    }

    private fun fetchPreferredCurrency() {
        if (userId.isEmpty()) {
            fetchTransactions()
            return
        }

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {

                    currencySymbol = document.getString("preferredCurrency") ?: "$"
                    Log.d("TransactionFragment", "Using currency symbol: $currencySymbol")
                }

                fetchTransactions()
            }
            .addOnFailureListener { e ->
                Log.e("TransactionFragment", "Error fetching currency preference", e)

                fetchTransactions()
            }
    }

    private fun fetchTransactions() {
        if (userId.isEmpty()) return

        firestore.collection("users").document(userId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {

                    val emptyView = view?.findViewById<TextView>(R.id.empty_view)
                    emptyView?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {

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
                        val amount = document.getDouble("amount")?.toInt() ?: 0
                        val type = document.getString("type") ?: "Unknown"
                        val description = document.getString("description") ?: ""

                        val formattedDate = try {
                            if (document.contains("timestamp")) {
                                val timestampValue = document.get("timestamp")
                                
                                when (timestampValue) {
                                    is Date -> {
                                        val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                                        outputFormat.format(timestampValue)
                                    }
                                    is com.google.firebase.Timestamp -> {
                                        val date = (timestampValue as com.google.firebase.Timestamp).toDate()
                                        val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                                        outputFormat.format(date)
                                    }
                                    is Long -> {
                                        val date = Date(timestampValue)
                                        val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                                        outputFormat.format(date)
                                    }
                                    is String -> {
                                        try {
                                            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                            val date = inputFormat.parse(timestampValue)
                                            val outputFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
                                            outputFormat.format(date)
                                        } catch (e: Exception) {
                                            "Date: $timestampValue"
                                        }
                                    }
                                    else -> {
                                        Log.w("TransactionFragment", 
                                            "Timestamp field exists but in an unknown format: ${timestampValue?.javaClass}")
                                        "Unknown date format"
                                    }
                                }
                            } else {
                                Log.w("TransactionFragment", "No timestamp field found in document ${document.id}")
                                "No date"
                            }
                        } catch (e: Exception) {
                            Log.e("TransactionFragment", "Error handling timestamp", e)
                            "Date error"
                        }

                        holder.amountText.text = "$currencySymbol$amount"
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
            holder.amountText.text = "${currencySymbol}0"
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

    private fun migrateIncorrectTimestamps() {
        if (userId.isEmpty()) return
        
        Log.d("TransactionFragment", "Starting timestamp migration check...")
        
        firestore.collection("users").document(userId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { documents ->
                var migratedCount = 0
                
                for (document in documents) {
                    if (document.contains("timestamp") && !(document.get("timestamp") is Timestamp)) {
                        try {
                            val timestampValue = document.get("timestamp")
                            var newTimestamp: Timestamp? = null
                            
                            when (timestampValue) {
                                is Date -> {
                                    newTimestamp = Timestamp(timestampValue)
                                }
                                is Long -> {
                                    newTimestamp = Timestamp(Date(timestampValue))
                                }
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
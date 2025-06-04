package com.example.savingfinance

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class CurrencyBottomSheet(private val userId: String, private val onCurrencySelected: (String, String) -> Unit) : BottomSheetDialogFragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var selectedCurrencySymbol = "$"
    private var selectedCurrencyCode = "USD"
    private var previousCurrencyCode = "USD"

    companion object {
        private const val TAG = "CurrencyBottomSheet"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    override fun getTheme(): Int {
        return R.style.TransparentBottomSheetDialog
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.bottom_sheet_edit_currency, container, false)
            setupUI(view)
            view
        } catch (e: Exception) {
            Log.e(TAG, "Error creating view", e)
            Toast.makeText(requireContext(), "Failed to load currency selector", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun setupUI(view: View) {
        val currencySpinner = view.findViewById<Spinner>(R.id.spCategory)
        val saveButton = view.findViewById<Button>(R.id.saveCurrencyButton)

        // Fetch current currency preference
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val currentCurrency = document.getString("preferredCurrency") ?: "$"
                    previousCurrencyCode = if (currentCurrency == "$") "USD" else "NIS"
                    selectedCurrencyCode = previousCurrencyCode
                    selectedCurrencySymbol = currentCurrency
                    
                    // Set spinner to current selection
                    val position = if (currentCurrency == "$") 0 else 1
                    currencySpinner.setSelection(position)
                }
            }

        val currencies = listOf("$ USD", "₪ NIS")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, currencies)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        currencySpinner.adapter = adapter

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        selectedCurrencySymbol = "$"
                        selectedCurrencyCode = "USD"
                    }
                    1 -> {
                        selectedCurrencySymbol = "₪"
                        selectedCurrencyCode = "NIS"
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        saveButton.setOnClickListener {
            if (userId.isNotEmpty()) {
                if (selectedCurrencyCode != previousCurrencyCode) {
                    convertAndUpdateCurrency()
                } else {
                    updateUserCurrency(selectedCurrencySymbol)
                }
            } else {
                Toast.makeText(requireContext(), "User ID not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertAndUpdateCurrency() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Starting currency conversion from $previousCurrencyCode to $selectedCurrencyCode")
                
                // Convert all transactions
                val transactionsRef = firestore.collection("users").document(userId).collection("transactions")
                val transactions = transactionsRef.get().await()
                Log.d(TAG, "Found ${transactions.size()} transactions to convert")
                
                val batch = firestore.batch()
                for (transaction in transactions) {
                    val amount = transaction.getDouble("amount") ?: 0.0
                    Log.d(TAG, "Original amount in Firestore: $amount")
                    Log.d(TAG, "Converting from $previousCurrencyCode to $selectedCurrencyCode")
                    val convertedAmount = CurrencyConverter.convertAmount(amount, previousCurrencyCode, selectedCurrencyCode)
                    Log.d(TAG, "Final amount to be stored: $convertedAmount")
                    batch.update(transaction.reference, "amount", convertedAmount)
                }

                // Convert all goals
                val goalsRef = firestore.collection("users").document(userId).collection("goals")
                val goals = goalsRef.get().await()
                Log.d(TAG, "Found ${goals.size()} goals to convert")
                
                for (goal in goals) {
                    val currentAmount = goal.getDouble("currentAmount") ?: 0.0
                    val goalAmount = goal.getDouble("goalAmount") ?: 0.0
                    
                    Log.d(TAG, "Original goal amounts in Firestore:")
                    Log.d(TAG, "Current amount: $currentAmount")
                    Log.d(TAG, "Goal amount: $goalAmount")
                    
                    val convertedCurrentAmount = CurrencyConverter.convertAmount(currentAmount, previousCurrencyCode, selectedCurrencyCode)
                    val convertedGoalAmount = CurrencyConverter.convertAmount(goalAmount, previousCurrencyCode, selectedCurrencyCode)
                    
                    Log.d(TAG, "Converted amounts to be stored:")
                    Log.d(TAG, "Current amount: $convertedCurrentAmount")
                    Log.d(TAG, "Goal amount: $convertedGoalAmount")
                    
                    batch.update(goal.reference, mapOf(
                        "currentAmount" to convertedCurrentAmount,
                        "goalAmount" to convertedGoalAmount
                    ))
                }

                // Update user's preferred currency
                batch.update(firestore.collection("users").document(userId), "preferredCurrency", selectedCurrencySymbol)
                
                // Commit all changes
                batch.commit().await()
                
                // Wait a moment to ensure Firestore has processed the changes
                delay(1000)
                
                // Force refresh the UI by reloading the current fragment
                val currentFragment = (activity as? ActivityHome)?.supportFragmentManager?.fragments?.firstOrNull()
                if (currentFragment is TransactionFragment) {
                    (activity as? ActivityHome)?.loadFragment(TransactionFragment.newInstance(userId))
                } else if (currentFragment is GoalsFragment) {
                    (activity as? ActivityHome)?.loadFragment(GoalsFragment.newInstance(userId))
                }
                
                Log.d(TAG, "Currency conversion completed successfully")
                Toast.makeText(requireContext(), "Currency updated successfully", Toast.LENGTH_SHORT).show()
                onCurrencySelected(selectedCurrencySymbol, selectedCurrencyCode)
                dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error converting currency", e)
                Toast.makeText(requireContext(), "Failed to update currency: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserCurrency(currency: String) {
        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users")
            .document(userId)
            .update("preferredCurrency", currency)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Currency updated successfully", Toast.LENGTH_SHORT).show()
                onCurrencySelected(selectedCurrencySymbol, selectedCurrencyCode)
                dismiss()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating currency", e)
                Toast.makeText(requireContext(), "Failed to update currency: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val behavior = (dialog as BottomSheetDialog).behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
} 
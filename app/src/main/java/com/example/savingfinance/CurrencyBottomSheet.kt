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

class CurrencyBottomSheet(private val userId: String, private val onCurrencySelected: (String, String) -> Unit) : BottomSheetDialogFragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var selectedCurrencySymbol = "$"
    private var selectedCurrencyCode = "USD"

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

        // Only include USD and NIS as options
        val currencies = listOf("$ USD", "₪ NIS")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, currencies)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        currencySpinner.adapter = adapter

        // Currency selection listener
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
                updateUserCurrency(selectedCurrencySymbol)
            } else {
                Toast.makeText(requireContext(), "User ID not available", Toast.LENGTH_SHORT).show()
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
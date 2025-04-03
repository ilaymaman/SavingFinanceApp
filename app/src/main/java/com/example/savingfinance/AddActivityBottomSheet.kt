package com.example.savingfinance

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddActivityBottomSheet : BottomSheetDialogFragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "AddActivityBottomSheet"
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.activity_add, container, false)
            setupUI(view)
            view
        } catch (e: Exception) {
            Log.e(TAG, "Error creating view", e)
            Toast.makeText(requireContext(), "Failed to load add activity screen", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun createTimestamp(): Timestamp {
        return Timestamp(Date())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupUI(view: View) {
        val categorySpinner = view.findViewById<Spinner>(R.id.spCategory)
        val goalNameEditText = view.findViewById<EditText>(R.id.etName)
        val goalAmountEditText = view.findViewById<EditText>(R.id.etGoal)
        val addButton = view.findViewById<Button>(R.id.btnAdd)
        val cancelButton = view.findViewById<Button>(R.id.btnCancel)

        val categories = listOf("Goal", "Transaction")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, categories)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        categorySpinner.adapter = adapter

        // Category selection listener
        categorySpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (categories[position]) {
                    "Transaction" -> goalAmountEditText.hint = "Transaction Amount"
                    "Goal" -> goalAmountEditText.hint = "Goal Amount"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })

        cancelButton.setOnClickListener { dismiss() }

        addButton.setOnClickListener {
            val goalName = goalNameEditText.text.toString().trim()
            val goalAmount = goalAmountEditText.text.toString().trim().toDoubleOrNull()
            val selectedCategory = categorySpinner.selectedItem.toString()

            // Validate inputs
            if (goalName.isEmpty()) {
                goalNameEditText.error = "Name cannot be empty"
                return@setOnClickListener
            }

            if (goalAmount == null || goalAmount <= 0) {
                goalAmountEditText.error = "Invalid amount"
                return@setOnClickListener
            }

            // Check user authentication
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create a proper timestamp
            val timestamp = createTimestamp()

            val data = when (selectedCategory) {
                "Transaction" -> hashMapOf(
                    "amount" to goalAmount,
                    "timestamp" to timestamp,
                    "type" to goalName,
                    "description" to ""
                )
                else -> hashMapOf(
                    "name" to goalName,
                    "goalAmount" to goalAmount,
                    "currentAmount" to 0,
                    "category" to selectedCategory
                )
            }

            // Determine collection based on category
            val collection = if (selectedCategory == "Transaction") "transactions" else "goals"

            // Add to Firestore
            firestore.collection("users")
                .document(userId)
                .collection(collection)
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "$selectedCategory added successfully", Toast.LENGTH_SHORT).show()
                    // Refresh the appropriate fragment based on the category
                    val activity = requireActivity() as ActivityHome
                    when (selectedCategory) {
                        "Transaction" -> activity.loadFragment(TransactionFragment.newInstance(userId))
                        "Goal" -> activity.loadFragment(GoalsFragment.newInstance(userId))
                    }
                    dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e("AddActivityBottomSheet", "Error adding $selectedCategory", e)
                    Toast.makeText(requireContext(), "Failed to add $selectedCategory", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val behavior = (dialog as BottomSheetDialog).behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}
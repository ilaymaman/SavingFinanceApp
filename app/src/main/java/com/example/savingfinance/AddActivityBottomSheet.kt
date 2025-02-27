package com.example.savingfinance

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddActivityBottomSheet : BottomSheetDialogFragment() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance() // Initialize Firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_add, container, false)

        val categorySpinner = view.findViewById<Spinner>(R.id.spCategory)
        val goalNameEditText = view.findViewById<EditText>(R.id.etName) // Assuming you have an EditText for goal name
        val goalAmountEditText = view.findViewById<EditText>(R.id.etGoal) // Assuming you have an EditText for amount
        val addButton = view.findViewById<Button>(R.id.btnAdd)
        val cancelButton = view.findViewById<Button>(R.id.btnCancel)

        val categories = listOf("None", "Car Budget", "Gift", "Bills", "Laptop Budget")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, categories)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        categorySpinner.adapter = adapter

        cancelButton.setOnClickListener {
            dismiss()
        }

        addButton.setOnClickListener {
            val goalName = goalNameEditText.text.toString().trim()
            val goalAmount = goalAmountEditText.text.toString().trim().toDoubleOrNull()
            val selectedCategory = categorySpinner.selectedItem.toString()

            if (goalName.isEmpty() || goalAmount == null) {
                Toast.makeText(requireContext(), "Please enter valid goal details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val goal = hashMapOf(
                "name" to goalName,
                "goalAmount" to goalAmount,
                "currentAmount" to 0,
                "category" to selectedCategory
            )


            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                firestore.collection("users") // Replace with your collection name
                    .document(userId) // Replace with the actual user ID
                    .collection("goals") // Subcollection inside the user document
                    .add(goal)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Goal added successfully", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to add goal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        return view
    }
}
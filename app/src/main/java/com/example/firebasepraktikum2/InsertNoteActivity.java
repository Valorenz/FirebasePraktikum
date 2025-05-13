package com.example.firebasepraktikum2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InsertNoteActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvEmail;
    private TextView tvUid;
    private Button btnKeluar;
    private FirebaseAuth mAuth;
    private EditText etTitle;
    private EditText etDesc;
    private Button btnSubmit;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private RecyclerView rvNotes;
    private NoteAdapter noteAdapter;
    private List<Note> noteList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_note);

        tvEmail = findViewById(R.id.tv_email);
        tvUid = findViewById(R.id.tv_uid);
        btnKeluar = findViewById(R.id.btn_keluar);
        etTitle = findViewById(R.id.et_title);
        etDesc = findViewById(R.id.et_description);
        btnSubmit = findViewById(R.id.btn_submit);


        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance("https://praktikumpam-8aeb8-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReference = firebaseDatabase.getReference();

        btnKeluar.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);

        rvNotes = findViewById(R.id.rv_notes);
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new NoteAdapter(this, noteList, new NoteAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Note note) {
                etTitle.setText(note.getTitle());
                etDesc.setText(note.getDescription());
                btnSubmit.setText("Update Note");
                btnSubmit.setOnClickListener(v -> updateNote(note));
            }
            public void onDelete(Note note) {
                deleteNote(note);
            }
        });

        rvNotes.setAdapter(noteAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadNotes();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            tvUid.setText(currentUser.getUid());
        }
    }
    private void loadNotes() {
        String userId = mAuth.getUid();
        if (userId == null) return;

        databaseReference.child("notes").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noteList.clear();
                for (DataSnapshot noteSnapshot : snapshot.getChildren()) {
                    Note note = noteSnapshot.getValue(Note.class);
                    if (note != null) {
                        note.setKey(noteSnapshot.getKey());
                        noteList.add(note);
                    }
                }
                noteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Failed to read notes", error.toException());
            }
        });
    }



    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_keluar) {
            logOut();
        } else if (id == R.id.btn_submit) {
            submitData();
        }
    }

    public void logOut() {
        mAuth.signOut();
        Intent intent = new Intent(InsertNoteActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // make sure user can't go back
        startActivity(intent);
    }

    public void submitData() {
        String title = etTitle.getText().toString();
        String desc = etDesc.getText().toString();
        Note baru = new Note(title, desc);

        if (!validateForm()) {
            return;
        } else {
            // Add these logging lines:
            Log.d("SubmitData", "Validation passed, attempting database write.");
            String userId = mAuth.getUid();
            if (userId == null) {
                Log.e("SubmitData", "User ID is null! Cannot write data.");
                Toast.makeText(InsertNoteActivity.this, "User not signed in properly.", Toast.LENGTH_SHORT).show();
                return; // Stop here if user ID is null
            }
            Log.d("SubmitData", "User ID: " + userId);
            Log.d("SubmitData", "Note object: Title='" + baru.getTitle() + "', Description='" + baru.getDescription() + "'");
            Log.d("SubmitData", "Database path: notes/" + userId + "/<push_id>"); // <push_id> is generated by push()

            databaseReference.child("notes")
                    .child(mAuth.getUid()) // Use userId variable here for clarity
                    .push()
                    .setValue(baru)
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d("SubmitData", "Data added successfully!"); // Log success too
                            Toast.makeText(InsertNoteActivity.this, "Data added", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("SubmitData", "Failed to add data", e); // Ensure this uses the SubmitData tag
                            Toast.makeText(InsertNoteActivity.this, "Failed to add data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            // Ensure the original Log.e is still there for FirebaseError tag if you want
                            Log.e("FirebaseError", "Error adding data", e);
                        }
                    });
        }
    }

    public void onFailure(@NonNull Exception e) {
        Toast.makeText(InsertNoteActivity.this, "Failed to add data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        e.printStackTrace();
    }
    private boolean validateForm() {
        boolean result = true;

        if (TextUtils.isEmpty(etTitle.getText().toString())) {
            etTitle.setError("Required");
            result = false;
        } else {
            etTitle.setError(null);
        }

        if (TextUtils.isEmpty(etDesc.getText().toString())) {
            etDesc.setError("Required");
            result = false;
        } else {
            etDesc.setError(null);
        }

        return result;
    }

    private void deleteNote(Note note) {
        String userId = mAuth.getUid();
        if (userId == null || note.getKey() == null) {
            Toast.makeText(this, "Unable to delete: Invalid user or note key", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child("notes").child(userId).child(note.getKey())
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void updateNote(Note note) {
        String userId = mAuth.getUid();
        if (userId == null) return;

        String updatedTitle = etTitle.getText().toString();
        String updatedDesc = etDesc.getText().toString();

        if (TextUtils.isEmpty(updatedTitle) || TextUtils.isEmpty(updatedDesc)) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Note updatedNote = new Note(updatedTitle, updatedDesc);
        updatedNote.setKey(note.getKey());

        databaseReference.child("notes").child(userId).child(note.getKey())
                .setValue(updatedNote)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
                    btnSubmit.setText("Save");
                    btnSubmit.setOnClickListener(this); // restore original submit behavior
                    etTitle.setText("");
                    etDesc.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}

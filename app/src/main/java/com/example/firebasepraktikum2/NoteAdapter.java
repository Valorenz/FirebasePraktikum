package com.example.firebasepraktikum2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private Context context;
    private List<Note> noteList;
    private DatabaseReference databaseReference;

    public interface OnItemActionListener {
        void onEdit(Note note);
    }

    private OnItemActionListener listener;

    public NoteAdapter(Context context, List<Note> noteList, OnItemActionListener listener) {
        this.context = context;
        this.noteList = noteList;
        this.listener = listener;
        databaseReference = FirebaseDatabase.getInstance("https://praktikumpam-8aeb8-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("notes");
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.tvTitle.setText(note.getTitle());
        holder.tvDesc.setText(note.getDescription());

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(note));

        holder.btnDelete.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getUid();
            String key = note.getKey();

            if (userId != null && key != null) {
                databaseReference.child(userId).child(key)
                        .removeValue()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show();
                            // DO NOT manually update noteList, Firebase listener in InsertNoteActivity handles it
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to delete note: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                Toast.makeText(context, "Note key or user ID is null", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc;
        Button btnEdit, btnDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvDesc = itemView.findViewById(R.id.tv_note_desc);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}

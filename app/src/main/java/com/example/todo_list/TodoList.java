package com.example.todo_list;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

public class TodoList extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton floatingActionButton;

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private String userId;
    private DatabaseReference reference;

    private ProgressDialog loader;

    private String key = "";
    private String taskTitleForUpdate;
    private String taskDescriptionForUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);

        recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        loader = new ProgressDialog(this);

        floatingActionButton = findViewById(R.id.floating_action_button);
        floatingActionButton.setOnClickListener(view -> addTask());

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        assert user != null;
        userId = user.getUid();

        reference = FirebaseDatabase.getInstance().getReference().child("Tasks").child(userId);
    }

    private void addTask(){
        AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);

        View view = inflater.inflate(R.layout.input_file, null);
        mDialog.setView(view);

        AlertDialog dialog = mDialog.create();
        dialog.setCancelable(true);
        dialog.show();

        final EditText titleEditText = view.findViewById(R.id.task_title);
        final EditText descriptionEditText = view.findViewById(R.id.task_description);
        final Button saveBtn = view.findViewById(R.id.save_task);
        final Button cancelBtn = view.findViewById(R.id.cancel_creating_task);

        cancelBtn.setOnClickListener(view1 -> dialog.dismiss());

        saveBtn.setOnClickListener(v -> {
            String taskTitle = titleEditText.getText().toString().trim();
            String taskDescription = descriptionEditText.getText().toString().trim();
            String id = reference.push().getKey();
            String date = DateFormat.getDateInstance().format(new Date());

            if(TextUtils.isEmpty(taskTitle)) {
                titleEditText.setError("Title is required!");
                return;
            }

            loader.setMessage("Adding your data");
            loader.setCanceledOnTouchOutside(false);
            loader.show();

            Task task = new Task(id, taskTitle, taskDescription, date);

            reference.child(Objects.requireNonNull(id)).setValue(task).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {
                    if(task.isSuccessful()) {
                        Toast.makeText(TodoList.this, "Task has been inserted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(this.getClass().toString(), Objects.requireNonNull(task.getException()).toString());
                        Toast.makeText(TodoList.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                    loader.dismiss();
                }
            });

            dialog.dismiss();
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Task> options = new FirebaseRecyclerOptions.Builder<Task>()
                .setQuery(reference, Task.class)
                .build();

        FirebaseRecyclerAdapter<Task, MViewHolder> adapter = new FirebaseRecyclerAdapter<Task, MViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MViewHolder holder, int position, @NonNull Task model) {
                holder.setDate(model.getDate());
                holder.setTitle(model.getTitle());
                holder.setDescription(model.getDescription());

                holder.getView().setOnClickListener(view -> {
                    key = getRef(position).getKey();
                    taskTitleForUpdate = model.getTitle();
                    taskDescriptionForUpdate = model.getDescription();

                    updateTask();
                });
            }

            @NonNull
            @Override
            public MViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_details, parent, false);
                return new MViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    public static class MViewHolder extends RecyclerView.ViewHolder {

        private View mView;

        public MViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }

        public View getView() {
            return mView;
        }

        public void setView(View view) {
            this.mView = view;
        }

        public void setTitle(String title) {
            TextView titleTextView = mView.findViewById(R.id.task_title);
            titleTextView.setText(title);
        }

        public void setDescription(String description) {
            TextView descriptionTextView = mView.findViewById(R.id.task_description);
            descriptionTextView.setText(description);
        }

        public void setDate(String date) {
            TextView dateTextView = mView.findViewById(R.id.task_created_date);
            dateTextView.setText(date);
        }
    }

    private void updateTask() {

        AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.update_task, null);
        mDialog.setView(view);

        AlertDialog dialog = mDialog.create();

        EditText titleEditText = (EditText) view.findViewById(R.id.task_title_to_update);
        EditText descriptionEditText = (EditText) view.findViewById(R.id.task_description_to_update);

        titleEditText.setText(taskTitleForUpdate);
        titleEditText.setSelection(taskTitleForUpdate.length());

        descriptionEditText.setText(taskDescriptionForUpdate);
        descriptionEditText.setSelection(taskDescriptionForUpdate.length());

        Button deleteButton = view.findViewById(R.id.delete_task_btn);
        Button updateButton = view.findViewById(R.id.update_task_btn);

        updateButton.setOnClickListener(view1 -> {
            taskTitleForUpdate = titleEditText.getText().toString().trim();
            taskDescriptionForUpdate = descriptionEditText.getText().toString().trim();

            String date = DateFormat.getDateInstance().format(new Date());

            Task task = new Task(key, taskTitleForUpdate, taskDescriptionForUpdate, date);

            reference.child(key).setValue(task).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {
                    if(task.isSuccessful()) {
                        Toast.makeText(TodoList.this, "Task has been updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(this.getClass().toString(), Objects.requireNonNull(task.getException()).toString());
                        Toast.makeText(TodoList.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialog.dismiss();

        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reference.child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {
                        if(task.isSuccessful()) {
                            Toast.makeText(TodoList.this, "Task has been deleted successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(this.getClass().toString(), Objects.requireNonNull(task.getException()).toString());
                            Toast.makeText(TodoList.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                dialog.dismiss();
            }
        });

        dialog.show();

    }


}
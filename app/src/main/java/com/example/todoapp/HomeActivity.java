package com.example.todoapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {
    EditText taskEt;  //Et = edit text
    EditText descriptionEt;
    TextView dateTv; // Tv = text view


    DateFormat fmtDate = DateFormat.getDateInstance();
    Calendar myCalendar = Calendar.getInstance();
    DatePickerDialog.OnDateSetListener d = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view,
                              int year, int monthOfYear, int dayOfMonth) {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        }
    };

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private FloatingActionButton floatingActionButton;

    private DatabaseReference reference;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private String onlineUserID;

    private ProgressDialog loader;

    private String key = "";
    private String task;
    private String description;
    private String date;
    private TaskType taskType;

    ArrayList<TaskType> taskTypeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        toolbar = findViewById(R.id.homeToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Todo List App");

        recyclerView = findViewById(R.id.recyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        loader = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        onlineUserID = mUser.getUid();
        reference = FirebaseDatabase.getInstance().getReference().child("tasks").child(onlineUserID);

        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });

        String[] taskTypeNames = getResources().getStringArray(R.array.task_types);
        taskTypeList = new ArrayList<TaskType>(Arrays.asList(
                new TaskType(taskTypeNames[0], R.drawable.black_meeting_icon),
                new TaskType(taskTypeNames[1], R.drawable.black_shopping_icon),
                new TaskType(taskTypeNames[2], R.drawable.black_office_icon),
                new TaskType(taskTypeNames[3], R.drawable.black_contact_icon),
                new TaskType(taskTypeNames[4], R.drawable.black_travelling_icon),
                new TaskType(taskTypeNames[5], R.drawable.black_relaxing_icon)
        ));
    }

    private void addTask() {
        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);

        View myView = inflater.inflate(R.layout.input_file, null);
        myDialog.setView(myView);

        final AlertDialog dialog = myDialog.create();
        dialog.setCancelable(false);
        dialog.show();

        taskEt = myView.findViewById(R.id.task);
        descriptionEt = myView.findViewById(R.id.description);
        dateTv = myView.findViewById(R.id.date);
        Button pickDate = myView.findViewById(R.id.pickDateBtn);
        Button save = myView.findViewById(R.id.saveBtn);
        Button cancel = myView.findViewById(R.id.cancelBtn);
        Spinner taskTypeDropdown = myView.findViewById(R.id.taskTypeDropdown);


        pickDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new DatePickerDialog(HomeActivity.this, d,
                        myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // cập nhật giá trị tại textview day
        updateLabel();

        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            String mTask = taskEt.getText().toString().trim();
            String mDescription = descriptionEt.getText().toString().trim();
            String id = reference.push().getKey();
            String mDate = dateTv.getText().toString().trim();
            TaskType taskType = (TaskType) taskTypeDropdown.getSelectedItem();

            if (TextUtils.isEmpty(mTask)) {
                taskEt.setError("Task is required!");
                return;
            } else if (TextUtils.isEmpty(mDescription)) {
                taskEt.setError("Description is required!");
                return;
            } else {
                loader.setMessage("Adding your task...");
                loader.setCanceledOnTouchOutside(false);
                loader.show();

                TaskModel model;
                switch (taskType.name) {
                    case "Meeting":
                        model = new MeetingTask(mTask, mDescription, id, mDate, taskType,
                                "lấy url từ edit text", "lấy location từ edit text");
                        break;
                    case "Shopping":
                        model = new ShoppingTask(mTask, mDescription, id, mDate, taskType,
                                "lấy url từ edit text", "lấy location từ edit text");
                        break;
                    case "Office":
                        model = new OfficeTask(mTask, mDescription, id, mDate, taskType,
                                "lấy filename từ edit text");
                        break;
                    case "Contact":
                        TextView phoneET = myView.findViewById(R.id.et_phoneNumber);
                        TextView emailET = myView.findViewById(R.id.et_email);
                        model = new ContactTask(mTask, mDescription, id, mDate, taskType,
                                phoneET.getText().toString(), emailET.getText().toString());
                        break;
                    case "Travelling":
                        //travelling chưa có TravellingTask :(
                        model = new ContactTask(mTask, mDescription, id, mDate, taskType,
                                "lấy phone từ edit text", "lấy email từ edit text");
                        break;
                    case "Relaxing":
                        model = new RelaxingTask(mTask, mDescription, id, mDate, taskType,
                                "lấy playlist từ edit text");
                        break;
                    default:
                        model = new MeetingTask(mTask, mDescription, id, mDate, taskType,
                                "lấy url từ edit text", "lấy location từ edit text");
                        break;
                }
                reference.child(id).setValue(model).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(HomeActivity.this, "Task has been added successfully!", Toast.LENGTH_SHORT).show();
                        loader.dismiss();
                    } else {
                        //String error = task.getException().toString();
                        Toast.makeText(HomeActivity.this, "Add task failed! Please try again!", Toast.LENGTH_SHORT).show();
                        loader.dismiss();
                    }

                });
            }

            dialog.dismiss();
        });

        taskTypeDropdown.setAdapter(new TaskTypeAdapter(this, taskTypeList));
        taskTypeDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //do stuff
                RelativeLayout taskDetail = myView.findViewById(R.id.taskDetail);
                LayoutInflater inflater = LayoutInflater.from(myView.getContext());

                //thay doi taskDetail tuong ung voi moi type
                taskDetail.removeAllViews();
                switch (i) {
                    case 0: { //Meeting
                        View meetingInputDetail = inflater.inflate(R.layout.meeting_input_detail, null);
                        taskDetail.addView(meetingInputDetail);
                        break;
                    }
                    case 1: { //Shopping
                        View shoppingInputDetail = inflater.inflate(R.layout.shopping_input_detail, null);
                        taskDetail.addView(shoppingInputDetail);
                        break;
                    }
                    case 2: { //office
                        View officeInputDetail = inflater.inflate(R.layout.office_input_detail, null);
                        taskDetail.addView(officeInputDetail);
                        break;
                    }
                    case 3: { //contact
                        View contactInputDetail = inflater.inflate(R.layout.contact_input_detail, null);
                        taskDetail.addView(contactInputDetail);
                        break;

                    }
                    case 4: { //travel
                        View travellingInputDetail = inflater.inflate(R.layout.travelling_input_detail, null);
                        taskDetail.addView(travellingInputDetail);
                        break;

                    }
                    case 5: {  //relax
                        View relaxingInputDetail = inflater.inflate(R.layout.relaxing_input_detail, null);
                        taskDetail.addView(relaxingInputDetail);
                        break;

                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        dialog.show();
    }

    private void updateLabel() {
        dateTv.setText(fmtDate.format(myCalendar.getTime()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<TaskModel> options = new FirebaseRecyclerOptions.Builder<TaskModel>().setQuery(reference, TaskModel.class).build();

        FirebaseRecyclerAdapter<TaskModel, MyViewHolder> adapter = new FirebaseRecyclerAdapter<TaskModel, MyViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") final int position, @NonNull final TaskModel model) {
                holder.setDate(model.getDate());
                holder.setTask(model.getTask());
                holder.setDescription(model.getDescription());
                holder.setTaskType(model.getTaskType());

                holder.mView.setOnClickListener(v -> {
                    Intent intent;
                    switch (model.getTaskType().name) {
                        case "Meeting":
                            intent = new Intent(HomeActivity.this, MeetingTaskDetail.class);
                            startActivity(intent);
                            break;
                        case "Shopping":
                            intent = new Intent(HomeActivity.this, ShoppingTaskDetail.class);
                            startActivity(intent);
                            break;
                        case "Office":
                            intent = new Intent(HomeActivity.this, OfficeTaskDetail.class);
                            startActivity(intent);
                            break;
                        case "Contact":
                            intent = new Intent(HomeActivity.this, ContactTaskDetail.class);
                            reference.child(getRef(position).getKey()).child(model.getId()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DataSnapshot> task) {
                                    if (!task.isSuccessful()) {
                                        Log.e("firebase", "Error getting data", task.getException());
                                    } else {
                                        ContactTask contactTask = task.getResult().getValue(ContactTask.class);
                                        intent.putExtra("task", contactTask);
                                        startActivity(intent);
                                    }
                                }
                            });


                            break;
                        case "Travelling":
                            intent = new Intent(HomeActivity.this, TravellingTaskDetail.class);
                            startActivity(intent);
                            break;
                        case "Relaxing":
                            intent = new Intent(HomeActivity.this, RelaxingTaskDetail.class);
                            startActivity(intent);
                            break;
                        default:
                            intent = new Intent(HomeActivity.this, MeetingTaskDetail.class);
                            startActivity(intent);
                            break;
                    }

                });

                Button editButton = holder.mView.findViewById(R.id.editButton);
                editButton.setOnClickListener(v -> {
                    key = getRef(position).getKey();
                    task = model.getTask();
                    description = model.getDescription();
                    taskType = model.getTaskType();
                    date = model.getDate();
                    updateTask();
                });
            }

            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.retrieved_layout, parent, false);
                return new MyViewHolder(v);
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        View mView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setTask(String task) {
            TextView taskTextView = mView.findViewById(R.id.taskTv);
            taskTextView.setText(task);
        }

        public void setDescription(String des) {
            TextView desTextView = mView.findViewById(R.id.descriptionTv);
            desTextView.setText(des);
        }

        public void setDate(String date) {
            TextView dateTextView = mView.findViewById(R.id.dateTv);
            dateTextView.setText(date);
        }

        public void setTaskType(TaskType taskType) {
            ImageView icon = mView.findViewById(R.id.taskTypeIcon);
            icon.setImageResource(taskType.iconResource);
        }
    }

    private void updateTask() {
        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.update_data, null);
        myDialog.setView(view);

        AlertDialog dialog = myDialog.create();

        EditText mTask = view.findViewById(R.id.mEditTextTask);
        EditText mDescription = view.findViewById(R.id.mEditTextDescription);
        TextView mDate = view.findViewById(R.id.mEditDate);
        Button updateDateBtn = view.findViewById(R.id.pickUpdateDateBtn);

        mTask.setText(task);
        mTask.setSelection(task.length());

        mDescription.setText(description);
        mDescription.setSelection(description.length());

        mDate.setText(date);

        DateFormat fmtDate = DateFormat.getDateInstance();
        Calendar myCalendar = Calendar.getInstance();

        DatePickerDialog.OnDateSetListener d = new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view,
                                  int year, int monthOfYear, int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mDate.setText(fmtDate.format(myCalendar.getTime()));
            }
        };

        updateDateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new DatePickerDialog(HomeActivity.this, d,
                        myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        Button delButton = view.findViewById(R.id.btnDelete);
        Button updateButton = view.findViewById(R.id.btnUpdate);
        Spinner taskTypeDropdown = view.findViewById(R.id.taskTypeDropdown);

        updateButton.setOnClickListener(view1 -> {
            task = mTask.getText().toString().trim();
            description = mDescription.getText().toString().trim();
            taskType = (TaskType) taskTypeDropdown.getSelectedItem();
            String date = mDate.getText().toString().trim();

            TaskModel model = new TaskModel(task, description, key, date, taskType);

            reference.child(key).setValue(model).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(HomeActivity.this, "Task has been updated", Toast.LENGTH_SHORT).show();
                    } else {
                        //String err = task.getException().toString();
                        Toast.makeText(HomeActivity.this, "Update task failed!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialog.dismiss();
        });

        delButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reference.child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(HomeActivity.this, "Task has been deleted successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            String err = task.getException().toString();
                            Toast.makeText(HomeActivity.this, "Delete task failed!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                dialog.dismiss();
            }
        });

        dialog.show();

        taskTypeDropdown.setAdapter(new TaskTypeAdapter(this, taskTypeList));
        int updatingTaskIndex = 0;
        for (int i = 0; i < taskTypeList.size(); i++) {
            if (taskTypeList.get(i).name.equals(taskType.name)) {
                updatingTaskIndex = i;
                break;
            }
        }
        taskTypeDropdown.setSelection(updatingTaskIndex);
        taskTypeDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //do stuff
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                mAuth.signOut();
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
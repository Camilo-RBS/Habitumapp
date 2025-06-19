package com.wayneenterprises.habitum.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wayneenterprises.habitum.model.Task
import com.wayneenterprises.habitum.model.TaskPriority
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskDialog(
    taskToEdit: Task? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Date?, Date?, TaskPriority, String) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var category by remember { mutableStateOf(taskToEdit?.category ?: "Work") }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: TaskPriority.LOW) }
    var dueDate by remember { mutableStateOf(taskToEdit?.dueDate) }
    var completionDate by remember { mutableStateOf(taskToEdit?.completionDate) }

    val categories = listOf("Work", "Entertainment", "Study", "Personal")
    val priorities = TaskPriority.values()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (taskToEdit != null) "Edit Task" else "Add New Task",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF2E2E2E)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF2E2E2E),
                        unfocusedTextColor = Color(0xFF2E2E2E),
                        focusedLabelColor = Color(0xFF5C6BC0),
                        unfocusedLabelColor = Color(0xFF757575),
                        focusedBorderColor = Color(0xFF5C6BC0),
                        unfocusedBorderColor = Color(0xFFCCCCCC)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF2E2E2E),
                        unfocusedTextColor = Color(0xFF2E2E2E),
                        focusedLabelColor = Color(0xFF5C6BC0),
                        unfocusedLabelColor = Color(0xFF757575),
                        focusedBorderColor = Color(0xFF5C6BC0),
                        unfocusedBorderColor = Color(0xFFCCCCCC)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Dropdown
                var expandedCategory by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF2E2E2E),
                            unfocusedTextColor = Color(0xFF2E2E2E),
                            focusedLabelColor = Color(0xFF5C6BC0),
                            unfocusedLabelColor = Color(0xFF757575),
                            focusedBorderColor = Color(0xFF5C6BC0),
                            unfocusedBorderColor = Color(0xFFCCCCCC)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false },
                        containerColor = Color.White
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat,
                                        color = Color(0xFF2E2E2E)
                                    )
                                },
                                onClick = {
                                    category = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Priority Dropdown
                var expandedPriority by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedPriority,
                    onExpandedChange = { expandedPriority = !expandedPriority }
                ) {
                    OutlinedTextField(
                        value = priority.displayName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPriority) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF2E2E2E),
                            unfocusedTextColor = Color(0xFF2E2E2E),
                            focusedLabelColor = Color(0xFF5C6BC0),
                            unfocusedLabelColor = Color(0xFF757575),
                            focusedBorderColor = Color(0xFF5C6BC0),
                            unfocusedBorderColor = Color(0xFFCCCCCC)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPriority,
                        onDismissRequest = { expandedPriority = false },
                        containerColor = Color.White
                    ) {
                        priorities.forEach { pri ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = pri.displayName,
                                        color = pri.color
                                    )
                                },
                                onClick = {
                                    priority = pri
                                    expandedPriority = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF5C6BC0)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5C6BC0)).brush
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title,
                                    category,
                                    description,
                                    dueDate,
                                    completionDate,
                                    priority,
                                    taskToEdit?.id ?: ""
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5C6BC0)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (taskToEdit != null) "Update" else "Save",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
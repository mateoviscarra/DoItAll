package com.mateoviscarra.doitall.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mateoviscarra.doitall.data.Category
import com.mateoviscarra.doitall.data.CategoryExercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    categories: List<Category>,
    onBack: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onAddExercise: (categoryId: String, CategoryExercise) -> Unit,
    onDeleteExercise: (categoryId: String, exerciseId: String) -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories & Exercises") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCategoryDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categories.forEach { category ->
                item {
                    CategoryCard(
                        category = category,
                        onDeleteCategory = { onDeleteCategory(category.id) },
                        onAddExercise = { exercise -> onAddExercise(category.id, exercise) },
                        onDeleteExercise = { exerciseId -> onDeleteExercise(category.id, exerciseId) }
                    )
                }
            }
        }
    }
    
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName)
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    onDeleteCategory: () -> Unit,
    onAddExercise: (CategoryExercise) -> Unit,
    onDeleteExercise: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDeleteCategory) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Category")
                }
            }
            
            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                category.exercises.forEach { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = exercise.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${exercise.defaultSets} sets × ${exercise.defaultReps} reps${if (exercise.defaultLoad.isNotBlank()) " @ ${exercise.defaultLoad}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteExercise(exercise.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                TextButton(
                    onClick = { showAddExerciseDialog = true },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Exercise")
                }
            }
        }
    }
    
    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { exercise ->
                onAddExercise(exercise)
                showAddExerciseDialog = false
            }
        )
    }
}

@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (CategoryExercise) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var defaultSets by remember { mutableStateOf("3") }
    var defaultReps by remember { mutableStateOf("10") }
    var defaultLoad by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = defaultSets,
                    onValueChange = { defaultSets = it.filter { c -> c.isDigit() } },
                    label = { Text("Default Sets") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = defaultReps,
                    onValueChange = { defaultReps = it },
                    label = { Text("Default Reps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = defaultLoad,
                    onValueChange = { defaultLoad = it },
                    label = { Text("Default Load (e.g., Bodyweight, 20 kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            CategoryExercise(
                                id = name.lowercase().replace(" ", "_"),
                                name = name,
                                defaultSets = defaultSets.toIntOrNull() ?: 3,
                                defaultReps = defaultReps,
                                defaultLoad = defaultLoad
                            )
                        )
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
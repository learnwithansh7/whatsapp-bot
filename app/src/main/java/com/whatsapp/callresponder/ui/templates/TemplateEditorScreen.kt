package com.whatsapp.callresponder.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatsapp.callresponder.data.local.AppDatabase
import com.whatsapp.callresponder.data.local.entity.TemplateEntity
import com.whatsapp.callresponder.ui.theme.WhatsAppGreen
import com.whatsapp.callresponder.util.TemplateResolver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(db: AppDatabase) {
    val coroutineScope = rememberCoroutineScope()
    val templates by db.templateDao().getAllTemplates().collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<TemplateEntity?>(null) }
    var inputTitle by remember { mutableStateOf("") }
    var inputContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Reply Messages", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTemplate = null
                    inputTitle = ""
                    inputContent = "Hey {name}, I missed your call at {time}. I will call you back shortly!"
                    showEditDialog = true
                },
                containerColor = WhatsAppGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Template")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Select which message WhatsApp sends when a call is missed. You can customize the text, add placeholders, or create new ones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(templates, key = { it.id }) { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                db.templateDao().setActiveTemplate(template.id)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (template.isActive)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (template.isActive)
                        CardDefaults.outlinedCardBorder().copy(width = 2.dp)
                    else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = template.isActive,
                                    onClick = {
                                        coroutineScope.launch {
                                            db.templateDao().setActiveTemplate(template.id)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    template.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            IconButton(onClick = {
                                editingTemplate = template
                                inputTitle = template.title
                                inputContent = template.content
                                showEditDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            template.content,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        // Live Sample Preview
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Preview: " + TemplateResolver.resolve(template.content, "John Doe"),
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit / Create Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editingTemplate == null) "New Message Template" else "Edit Template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Template Title (e.g., Busy, Driving)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputContent,
                        onValueChange = { inputContent = it },
                        label = { Text("Message Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )

                    Text("Insert dynamic placeholders:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = { inputContent += " {name}" },
                            label = { Text("+ Caller Name") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { inputContent += " {time}" },
                            label = { Text("+ Time") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val title = inputTitle.ifBlank { "Custom Reply" }
                            val current = editingTemplate
                            if (current == null) {
                                db.templateDao().insertTemplate(
                                    TemplateEntity(title = title, content = inputContent, isActive = templates.isEmpty())
                                )
                            } else {
                                db.templateDao().updateTemplate(
                                    current.copy(title = title, content = inputContent)
                                )
                            }
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

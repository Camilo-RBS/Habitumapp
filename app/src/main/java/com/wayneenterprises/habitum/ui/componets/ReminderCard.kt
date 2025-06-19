package com.wayneenterprises.habitum.ui.componets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayneenterprises.habitum.model.Reminder
import com.wayneenterprises.habitum.model.ReminderStatus
import com.wayneenterprises.habitum.model.ReminderType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReminderCard(
    reminder: Reminder,
    onComplete: (String) -> Unit,
    onOmit: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icono del tipo de recordatorio
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getReminderTypeColor(reminder.type).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getReminderTypeIcon(reminder.type),
                    contentDescription = null,
                    tint = getReminderTypeColor(reminder.type),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Título y estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.Black
                    )

                    // Badge de estado
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(getStatusColor(reminder.status))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = getStatusText(reminder.status),
                            color = getStatusTextColor(reminder.status),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Descripción
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hora y estado
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(reminder.dateTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = when (reminder.status) {
                            ReminderStatus.COMPLETED -> Icons.Default.CheckCircle
                            ReminderStatus.MISSED -> Icons.Default.Error
                            ReminderStatus.OMITTED -> Icons.Default.CancelScheduleSend
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = when (reminder.status) {
                            ReminderStatus.COMPLETED -> Color(0xFF4CAF50)
                            ReminderStatus.MISSED -> Color(0xFFF44336)
                            ReminderStatus.OMITTED -> Color(0xFFFF9800)
                            else -> Color(0xFFFF9800)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (reminder.status) {
                            ReminderStatus.COMPLETED -> "Actividad Completa"
                            ReminderStatus.MISSED -> "Perdido"
                            ReminderStatus.OMITTED -> "Omitido"
                            else -> "Pendiente"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botones de acción según el estado
                when (reminder.status) {
                    ReminderStatus.PENDING -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onComplete(reminder.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1)
                                ),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "Completar",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }

                            OutlinedButton(
                                onClick = { onOmit(reminder.id) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                border = null
                            ) {
                                Text(
                                    text = "Omitir",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            IconButton(
                                onClick = { onEdit(reminder.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDelete(reminder.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        // Para recordatorios completados, omitidos o perdidos, solo mostrar editar y eliminar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { onEdit(reminder.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDelete(reminder.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getReminderTypeIcon(type: ReminderType): ImageVector {
    return when (type) {
        ReminderType.WATER -> Icons.Default.LocalDrink
        ReminderType.EXERCISE -> Icons.Default.FitnessCenter
        ReminderType.REST -> Icons.Default.Bedtime
        ReminderType.MEDICINE -> Icons.Default.Medication
        ReminderType.GENERAL -> Icons.Default.Notifications
    }
}

private fun getReminderTypeColor(type: ReminderType): Color {
    return when (type) {
        ReminderType.WATER -> Color(0xFF2196F3)
        ReminderType.EXERCISE -> Color(0xFF4CAF50)
        ReminderType.REST -> Color(0xFF9C27B0)
        ReminderType.MEDICINE -> Color(0xFFF44336)
        ReminderType.GENERAL -> Color(0xFF607D8B)
    }
}

private fun getStatusColor(status: ReminderStatus): Color {
    return when (status) {
        ReminderStatus.PENDING -> Color(0xFFFFF3E0)
        ReminderStatus.COMPLETED -> Color(0xFFE8F5E8)
        ReminderStatus.MISSED -> Color(0xFFFFEBEE)
        ReminderStatus.OMITTED -> Color(0xFFF5F5F5)
    }
}

private fun getStatusTextColor(status: ReminderStatus): Color {
    return when (status) {
        ReminderStatus.PENDING -> Color(0xFFFF9800)
        ReminderStatus.COMPLETED -> Color(0xFF4CAF50)
        ReminderStatus.MISSED -> Color(0xFFF44336)
        ReminderStatus.OMITTED -> Color(0xFF757575)
    }
}

private fun getStatusText(status: ReminderStatus): String {
    return when (status) {
        ReminderStatus.PENDING -> "Pendiente"
        ReminderStatus.COMPLETED -> "Completado"
        ReminderStatus.MISSED -> "Perdido"
        ReminderStatus.OMITTED -> "Omitido"
    }
}
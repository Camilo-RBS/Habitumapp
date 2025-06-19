package com.wayneenterprises.habitum.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == "All",
                onClick = { onCategorySelected("All") },
                label = {
                    Text(
                        text = "All",
                        fontSize = 14.sp,
                        fontWeight = if (selectedCategory == "All") FontWeight.Medium else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF5C6BC0),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFF5F5F5),
                    labelColor = Color(0xFF666666)
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }

        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        fontSize = 14.sp,
                        fontWeight = if (selectedCategory == category) FontWeight.Medium else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF5C6BC0),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFF5F5F5),
                    labelColor = Color(0xFF666666)
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
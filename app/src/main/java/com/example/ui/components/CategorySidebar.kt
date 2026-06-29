package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A beautiful, feature-rich Navigation Sidebar for filtering IPTV channels by category.
 * Features:
 * - List of categories with dynamic icons based on name.
 * - Dynamic channel counts per category.
 * - Local search/filter text field for quick category navigation.
 * - Material 3 aesthetics with clear active/inactive states and high accessibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySidebar(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categoryCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter categories based on the local sidebar search query
    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            categories.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF07080B))
            .border(1.dp, Color(0xFF1E222D))
            .padding(16.dp)
            .testTag("category_sidebar")
    ) {
        // Sidebar Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = "Categories",
                tint = Color(0xFFFFB03A),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CATEGORIES",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            )
        }

        // Category Search / Filter Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter categories...", color = Color(0xFF9096A5), fontSize = 13.sp) },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.Search, 
                    contentDescription = "Search categories", 
                    tint = Color(0xFF9096A5),
                    modifier = Modifier.size(16.dp)
                ) 
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close, 
                            contentDescription = "Clear search", 
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF5D5FEF),
                unfocusedBorderColor = Color(0xFF1E222D),
                focusedContainerColor = Color(0xFF0F1115),
                unfocusedContainerColor = Color(0xFF0F1115)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(48.dp)
                .testTag("category_sidebar_search")
        )

        // Vertical List of Categories
        if (filteredCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No categories found",
                    color = Color(0xFF9096A5),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("category_sidebar_list")
            ) {
                items(filteredCategories, key = { it }) { category ->
                    val isSelected = category == selectedCategory
                    val count = categoryCounts[category] ?: 0
                    val icon = getCategoryIcon(category)
                    val isFav = category.equals("Favorites", ignoreCase = true)

                    val itemBg = if (isSelected) Color(0xFF5D5FEF) else Color.Transparent
                    val textColor = if (isSelected) Color.White else Color(0xFFE2E4EB)
                    val iconColor = if (isSelected) {
                        Color.White
                    } else if (isFav) {
                        Color(0xFFE53935) // Elegant soft red for favorite icon when unselected
                    } else {
                        Color(0xFFFFB03A) // Golden orange for general icons
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp) // Accessibility standard touch target
                            .clip(RoundedCornerShape(8.dp))
                            .background(itemBg)
                            .clickable { onCategorySelected(category) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("category_sidebar_item_$category"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Count Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFF1E222D))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else Color(0xFF9096A5)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns a beautiful, context-aware category icon based on keyword matching.
 */
private fun getCategoryIcon(category: String): ImageVector {
    val lower = category.lowercase()
    return when {
        lower == "all" -> Icons.Default.AllInclusive
        lower == "favorites" || lower.contains("fav") -> Icons.Default.Favorite
        lower.contains("movie") || lower.contains("cinema") || lower.contains("film") || lower.contains("vod") -> Icons.Default.Movie
        lower.contains("series") || lower.contains("show") || lower.contains("season") -> Icons.Default.VideoLibrary
        lower.contains("sport") || lower.contains("football") || lower.contains("soccer") || lower.contains("cricket") || lower.contains("espn") || lower.contains("league") -> Icons.Default.Sports
        lower.contains("news") || lower.contains("cnn") || lower.contains("bbc") || lower.contains("info") -> Icons.Default.Announcement
        lower.contains("music") || lower.contains("song") || lower.contains("mtv") -> Icons.Default.MusicNote
        lower.contains("kid") || lower.contains("cartoon") || lower.contains("disney") || lower.contains("child") -> Icons.Default.ChildCare
        lower.contains("documentary") || lower.contains("doc") || lower.contains("history") || lower.contains("nature") -> Icons.Default.MenuBook
        lower.contains("live") || lower.contains("broadcast") -> Icons.Default.LiveTv
        lower.contains("event") || lower.contains("ppv") -> Icons.Default.Event
        lower.contains("radio") -> Icons.Default.Radio
        lower.contains("hd") || lower.contains("fhd") || lower.contains("4k") -> Icons.Default.Hd
        lower.contains("adult") || lower.contains("xxx") -> Icons.Default.Lock
        else -> Icons.Default.Folder
    }
}

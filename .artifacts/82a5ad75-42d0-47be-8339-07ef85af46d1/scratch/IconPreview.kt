package com.rekluzlabs.reminera.ui.familygroups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class IconItem(val name: String, val icon: ImageVector)

@Composable
fun IconPreviewGrid() {
    val icons = listOf(
        IconItem("Elderly", Icons.Default.Elderly),
        IconItem("ElderlyWoman", Icons.Default.ElderlyWoman),
        IconItem("Chair", Icons.Default.Chair),
        IconItem("Nature (Tree)", Icons.Default.Nature),
        IconItem("Park", Icons.Default.Park),
        IconItem("MenuBook", Icons.Default.MenuBook),
        IconItem("AutoStories", Icons.Default.AutoStories),
        IconItem("Coffee", Icons.Default.Coffee),
        IconItem("History", Icons.Default.History),
        IconItem("Castle", Icons.Default.Castle),
        IconItem("Anchor", Icons.Default.Anchor),
        IconItem("VolunteerActivism", Icons.Default.VolunteerActivism),
        IconItem("SupervisedUserCircle", Icons.Default.SupervisedUserCircle)
    )

    Surface(color = MaterialTheme.colorScheme.background) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(icons) { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.name,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun PreviewIcons() {
    MaterialTheme {
        IconPreviewGrid()
    }
}

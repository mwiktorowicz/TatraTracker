package com.example.tatratracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tatratracker.navigation.Screen
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraDarkCard
import com.example.tatratracker.ui.theme.TatraGreen

private data class BottomNavigationItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val bottomNavigationItems =
    listOf(
        BottomNavigationItem(Screen.Home, "Start", Icons.Outlined.Home),
        BottomNavigationItem(Screen.Map, "Mapa", Icons.Outlined.Map),
        BottomNavigationItem(Screen.History, "Aktywności", Icons.Outlined.DirectionsWalk),
        BottomNavigationItem(Screen.Statistics, "Statystyki", Icons.Outlined.BarChart),
        BottomNavigationItem(Screen.More, "Więcej", Icons.Outlined.Menu)
    )

@Composable
fun TatraBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp),
        color = TatraDark,
        shadowElevation = 14.dp
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavigationItems.forEach { item ->
                val selected =
                    isItemSelected(
                        currentRoute = currentRoute,
                        screen = item.screen
                    )

                BottomNavigationButton(
                    item = item,
                    selected = selected,
                    onClick = { onNavigate(item.screen.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationButton(
    item: BottomNavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor =
        if (selected) {
            TatraGreen
        } else {
            Color(0xFFA7B0B2)
        }

    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .clickable(onClick = onClick)
                .padding(top = 8.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 42.dp, height = 28.dp)
                    .background(
                        color = if (selected) TatraDarkCard else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(21.dp)
            )
        }

        Text(
            text = item.label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

private fun isItemSelected(
    currentRoute: String?,
    screen: Screen
): Boolean {
    return when (screen) {
        Screen.History ->
            currentRoute == Screen.History.route ||
                    currentRoute == Screen.Import.route ||
                    currentRoute == Screen.ActivityDetails.route

        else -> currentRoute == screen.route
    }
}
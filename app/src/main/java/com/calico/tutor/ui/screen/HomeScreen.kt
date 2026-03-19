package com.calico.tutor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calico.tutor.ui.theme.*

@Composable
fun HomeScreen(
    userName: String = "Usuario",
    onLogout: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {}
) {
    var currentNavRoute by remember { mutableStateOf("home") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Header
            HomeHeader(userName = userName, onLogout = onLogout)

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Welcome Card
                WelcomeCard(userName = userName)

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Actions
                QuickActionsSection(
                    onSearchTutors = onNavigateToSearch,
                    onViewProfile = onNavigateToProfile,
                    onViewHistory = onNavigateToHistory
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Featured Section
                FeaturedTutorsSection()

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Section
                StatsSection()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Bottom Navigation
        BottomNavigationBar(
            currentRoute = currentNavRoute,
            onNavigate = { route ->
                currentNavRoute = route
                when (route) {
                    "search" -> onNavigateToSearch()
                    "profile" -> onNavigateToProfile()
                    "history" -> onNavigateToHistory()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp),
        color = WhiteBase
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "¡Hola, $userName!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Text(
                    text = "Bienvenido a Calico",
                    style = MaterialTheme.typography.bodySmall,
                    color = MediumGray
                )
            }

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Cerrar sesión",
                    tint = PrimaryOrange
                )
            }
        }
    }
}

@Composable
private fun WelcomeCard(userName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .shadow(elevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryOrange,
            contentColor = WhiteBase
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "¡Empecemos!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = WhiteBase
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Encuentra tutores especializados o comienza a enseñar",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhiteBase.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onSearchTutors: () -> Unit,
    onViewProfile: () -> Unit,
    onViewHistory: () -> Unit
) {
    Column {
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Search,
                label = "Buscar\nTutores",
                onClick = onSearchTutors,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.Person,
                label = "Mi\nPerfil",
                onClick = onViewProfile,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                icon = Icons.Default.History,
                label = "Historial",
                onClick = onViewHistory,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(elevation = 2.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant,
            contentColor = OnSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PrimaryOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FeaturedTutorsSection() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tutores Destacados",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            Text(
                text = "Ver más",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryOrange,
                fontWeight = FontWeight.SemiBold
            )
        }

        repeat(2) { index ->
            TutorCard(tutorName = "Tutor ${index + 1}")
            if (index == 0) Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TutorCard(tutorName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(elevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface,
            contentColor = OnSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tutorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Especialista en Matemáticas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = PrimaryOrange,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "(245)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGray
                    )
                }
            }

            Button(
                onClick = {},
                modifier = Modifier
                    .height(40.dp)
                    .width(80.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange,
                    contentColor = WhiteBase
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Agendar",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatsSection() {
    Column {
        Text(
            text = "Tu Actividad",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = "12",
                label = "Sesiones",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "4.8",
                label = "Rating",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "8",
                label = "Tutores",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(elevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant,
            contentColor = OnSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MediumGray
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp),
        color = WhiteBase,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItemIcon(
                icon = Icons.Default.Home,
                label = "Inicio",
                isSelected = currentRoute == "home",
                onClick = { onNavigate("home") }
            )
            NavBarItemIcon(
                icon = Icons.Default.Search,
                label = "Buscar",
                isSelected = currentRoute == "search",
                onClick = { onNavigate("search") }
            )
            NavBarItemIcon(
                icon = Icons.Default.History,
                label = "Historial",
                isSelected = currentRoute == "history",
                onClick = { onNavigate("history") }
            )
            NavBarItemIcon(
                icon = Icons.Default.Person,
                label = "Perfil",
                isSelected = currentRoute == "profile",
                onClick = { onNavigate("profile") }
            )
        }
    }
}

@Composable
private fun NavBarItemIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) PrimaryOrange else MediumGray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) PrimaryOrange else MediumGray,
            fontSize = 10.sp
        )
    }
}

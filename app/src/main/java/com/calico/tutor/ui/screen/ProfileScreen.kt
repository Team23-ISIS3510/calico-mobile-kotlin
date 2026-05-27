package com.calico.tutor.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.calico.tutor.ui.component.OfflineBanner
import com.calico.tutor.ui.theme.*
import com.calico.tutor.ui.util.rememberIsOnline
import com.calico.tutor.ui.viewmodel.ProfileState
import com.calico.tutor.ui.viewmodel.ProfileViewModel
import java.io.File

@Composable
fun ProfileScreen(
    context: Context,
    viewModel: ProfileViewModel? = null,
    onLogout: () -> Unit = {}
) {
    val profileState = viewModel?.profileState?.collectAsState()?.value ?: ProfileState.Idle
    val isOnline by rememberIsOnline(context)

    LaunchedEffect(Unit) {
        viewModel?.loadProfile()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBase)
    ) {
        when (val state = profileState) {
            ProfileState.Idle, ProfileState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            }
            is ProfileState.Success -> {
                ProfileContent(
                    state = state,
                    isOnline = isOnline,
                    onLogout = onLogout
                )
            }
            is ProfileState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileState.Success,
    isOnline: Boolean,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        if (!isOnline || state.isOffline) {
            OfflineBanner(
                message = "Viewing offline data. Check your connection.",
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "MY PROFILE",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(PrimaryOrange)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            if (!state.profileImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = state.profileImageUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                DefaultAvatar(name = state.userName)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = state.userName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = state.userEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = MediumGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileInfoItem(label = "Status", value = if (state.bio != null) "Active Tutor" else "Active")
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)
                ProfileInfoItem(label = "Rating", value = state.rating?.let { "$it" } ?: "New")
                if (state.bio != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)
                    ProfileInfoItem(label = "Bio", value = state.bio)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Logout,
                contentDescription = "Logout",
                tint = Color.White,
                modifier = Modifier.size(20.dp).padding(end = 8.dp)
            )
            Text(text = "LOGOUT", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun DefaultAvatar(name: String) {
    Text(
        text = name.firstOrNull()?.uppercase() ?: "U",
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
private fun ProfileInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MediumGray)
        Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}
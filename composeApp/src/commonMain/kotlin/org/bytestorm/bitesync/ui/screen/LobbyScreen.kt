package org.bytestorm.bitesync.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bytestorm.bitesync.model.RoomState

@Composable
fun LobbyScreen(
    roomState: RoomState?,
    isConnecting: Boolean,
    error: String?,
    serverIp: String,
    onServerIpChange: (String) -> Unit,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Unit,
    onStartSuggesting: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
    )

    Box(
        modifier = modifier.fillMaxSize().background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "BiteSync",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Swipe together. Eat together.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (roomState == null) {
                CreateOrJoinCard(
                    userName = userName,
                    onUserNameChange = { userName = it },
                    pin = pin,
                    onPinChange = { if (it.length <= 4) pin = it.filter { c -> c.isDigit() } },
                    isJoining = isJoining,
                    onToggleJoining = {
                        isJoining = !isJoining
                        onClearError()
                    },
                    isConnecting = isConnecting,
                    error = error,
                    serverIp = serverIp,
                    onServerIpChange = onServerIpChange,
                    showSettings = showSettings,
                    onToggleSettings = { showSettings = !showSettings },
                    onCreateRoom = { onCreateRoom(userName) },
                    onJoinRoom = { onJoinRoom(pin, userName) }
                )
            } else {
                WaitingRoomCard(
                    roomState = roomState,
                    onStartSuggesting = onStartSuggesting
                )
            }
        }
    }
}

@Composable
private fun SegmentedControl(
    isJoining: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F0ED))
            .padding(3.dp)
    ) {
        Row {
            listOf(false, true).forEach { joinMode ->
                val selected = isJoining == joinMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (selected) Modifier
                                .shadow(2.dp, RoundedCornerShape(10.dp))
                                .background(Color.White)
                            else Modifier
                        )
                        .clickable { onToggle(joinMode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (joinMode) "Join Room" else "Create Room",
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color(0xFFFF6B6B) else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateOrJoinCard(
    userName: String,
    onUserNameChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    isJoining: Boolean,
    onToggleJoining: () -> Unit,
    isConnecting: Boolean,
    error: String?,
    serverIp: String,
    onServerIpChange: (String) -> Unit,
    showSettings: Boolean,
    onToggleSettings: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SegmentedControl(
                isJoining = isJoining,
                onToggle = { joining ->
                    if (joining != isJoining) onToggleJoining()
                }
            )

            OutlinedTextField(
                value = userName,
                onValueChange = onUserNameChange,
                label = { Text("Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            AnimatedVisibility(isJoining) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    label = { Text("Room PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            AnimatedVisibility(showSettings) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Server Settings",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = onServerIpChange,
                        label = { Text("Server IP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        "Default is 10.0.2.2 for emulator. Use your PC's local IP for physical devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                if (!showSettings) {
                    TextButton(onClick = onToggleSettings) {
                        Text("Need to change server IP?", fontSize = 12.sp)
                    }
                }
            }

            Button(
                onClick = { if (isJoining) onJoinRoom() else onCreateRoom() },
                enabled = userName.isNotBlank() && !isConnecting && (!isJoining || pin.length == 4),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (isJoining) "Join Room" else "Create Room",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleSettings) {
                    Text(
                        if (showSettings) "Hide Settings" else "Settings",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingRoomCard(
    roomState: RoomState,
    onStartSuggesting: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Room PIN", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Text(
                roomState.pin,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFF6B6B),
                letterSpacing = 12.sp
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Players (${roomState.users.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val displayedUsers = roomState.users.take(6)
            val extraCount = roomState.users.size - displayedUsers.size
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                displayedUsers.forEach { user ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF8E53)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                user.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            user.displayName.take(8),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
                if (extraCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+$extraCount",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (roomState.users.size >= 2) {
                Button(
                    onClick = onStartSuggesting,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
                ) {
                    Text("Pick Restaurants!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Text(
                    "Waiting for more players...",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color(0xFFFF6B6B),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

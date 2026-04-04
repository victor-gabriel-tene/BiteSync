package org.bytestorm.bitesync.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bytestorm.bitesync.localization.LocalStrings
import org.bytestorm.bitesync.model.RoomState
import org.bytestorm.bitesync.ui.theme.BiteSyncTheme

@Composable
fun LobbyScreen(
    roomState: RoomState?,
    isConnecting: Boolean,
    error: String?,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Unit,
    onStartSuggesting: () -> Unit,
    onClearError: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var userName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    val gradient = BiteSyncTheme.gradients.main

    Box(
        modifier = modifier.fillMaxSize().background(gradient),
        contentAlignment = Alignment.Center
    ) {
        // Settings gear button – top end
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2699\uFE0F", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        Column(
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "BiteSync",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = strings.tagline,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
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
    val strings = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                .background(MaterialTheme.colorScheme.surface)
                            else Modifier
                        )
                        .clickable { onToggle(joinMode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (joinMode) strings.joinRoom else strings.createRoom,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                label = { Text(strings.yourName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            AnimatedVisibility(isJoining) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    label = { Text(strings.roomPin) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { if (isJoining) onJoinRoom() else onCreateRoom() },
                enabled = userName.isNotBlank() && !isConnecting && (!isJoining || pin.length == 4),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (isJoining) strings.joinRoom else strings.createRoom,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
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
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(strings.roomPin, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(
                roomState.pin,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 12.sp
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                strings.playersCount(roomState.users.size),
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
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                user.displayName.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onSecondary,
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(strings.pickRestaurants, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Text(
                    strings.waitingForPlayers,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

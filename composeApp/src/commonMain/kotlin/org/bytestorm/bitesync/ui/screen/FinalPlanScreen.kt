package org.bytestorm.bitesync.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bytestorm.bitesync.localization.LocalStrings
import org.bytestorm.bitesync.model.Attendee
import org.bytestorm.bitesync.model.Venue
import org.bytestorm.bitesync.ui.theme.BiteSyncTheme

@Composable
fun FinalPlanScreen(
    venue: Venue,
    attendees: List<Attendee>,
    onBackToLobby: () -> Unit,
    myUserId: String? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    val gradient = BiteSyncTheme.gradients.main
    val coming = attendees.filter { it.attending }
    val notComing = attendees.filter { !it.attending }
    val isMeAttending = attendees.find { it.user.id == myUserId }?.attending == true

    Box(modifier = modifier.fillMaxSize().background(gradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Text("\uD83C\uDF7D\uFE0F", fontSize = 48.sp)
            Text(
                strings.thePlan,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            // Restaurant card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        venue.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (venue.rating != null) {
                            Text(
                                "\u2B50 ${venue.rating}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8E53)
                            )
                        }
                        val priceLevel = venue.priceLevel
                        if (priceLevel != null) {
                            Text(
                                "$".repeat(priceLevel),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    if (venue.address.isNotEmpty()) {
                        Text(
                            "\uD83D\uDCCD ${venue.address}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Attendees card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (coming.isNotEmpty()) {
                        Text(
                            "\u2705 ${strings.whoComing}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        coming.forEach { attendee ->
                            AttendeeRow(
                                name = attendee.user.displayName,
                                attending = true
                            )
                        }
                    }

                    if (coming.isNotEmpty() && notComing.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }

                    if (notComing.isNotEmpty()) {
                        Text(
                            "\u274C ${strings.notComing}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        notComing.forEach { attendee ->
                            AttendeeRow(
                                name = attendee.user.displayName,
                                attending = false
                            )
                        }
                    }
                }
            }

            // Action buttons
            if (isMeAttending) {
                Button(
                    onClick = {
                        val query = (venue.name + " " + venue.address)
                            .replace(" ", "+")
                            .replace("&", "%26")
                        uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=$query")
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(
                        "\uD83D\uDDFA\uFE0F  ${strings.letsGo}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onBackToLobby,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Text(strings.backToLobby, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AttendeeRow(name: String, attending: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (attending) Color(0xFF4CAF50) else Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1).uppercase(),
                color = if (attending) Color.White else Color(0xFF757575),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (attending) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

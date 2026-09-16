package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEvent
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarMonthView(
    selectedDate: Calendar,
    events: List<CalendarEvent>,
    onDateSelected: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayMonthCal by remember(selectedDate) {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.ITALY)
    val monthTitle = monthYearFormat.format(displayMonthCal.time).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.ITALY) else it.toString()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calendar_month_view"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Month Header with Prev / Next Navigation and "Oggi" shortcut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        displayMonthCal = (displayMonthCal.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("prev_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Mese precedente",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Today shortcut button
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                val today = Calendar.getInstance()
                                displayMonthCal = (today.clone() as Calendar).apply {
                                    set(Calendar.DAY_OF_MONTH, 1)
                                }
                                onDateSelected(today)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = "Torna a oggi",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Oggi",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            displayMonthCal = (displayMonthCal.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("next_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Mese successivo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day of Week Header: Lun, Mar, Mer, Gio, Ven, Sab, Dom
            val daysOfWeek = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                daysOfWeek.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val cal = displayMonthCal.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, 1)

            // In Italy (ISO-8601), week starts on Monday (Calendar.MONDAY = 2)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val leadEmptyDays = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val totalCells = leadEmptyDays + maxDaysInMonth
            val totalRows = (totalCells + 6) / 7

            val todayCal = Calendar.getInstance()
            val todayYear = todayCal.get(Calendar.YEAR)
            val todayMonth = todayCal.get(Calendar.MONTH)
            val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

            val selYear = selectedDate.get(Calendar.YEAR)
            val selMonth = selectedDate.get(Calendar.MONTH)
            val selDay = selectedDate.get(Calendar.DAY_OF_MONTH)

            val curMonthYear = displayMonthCal.get(Calendar.YEAR)
            val curMonth = displayMonthCal.get(Calendar.MONTH)

            for (row in 0 until totalRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadEmptyDays + 1

                        if (dayNumber in 1..maxDaysInMonth) {
                            val isToday = (todayYear == curMonthYear && todayMonth == curMonth && todayDay == dayNumber)
                            val isSelected = (selYear == curMonthYear && selMonth == curMonth && selDay == dayNumber)

                            // Check if this day has events
                            val hasEvents = events.any { it.occursOnDate(curMonthYear, curMonth, dayNumber) }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable {
                                        val pickedCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, curMonthYear)
                                            set(Calendar.MONTH, curMonth)
                                            set(Calendar.DAY_OF_MONTH, dayNumber)
                                        }
                                        onDateSelected(pickedCal)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    if (hasEvents) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) AmberWarm else AmberAccent)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        } else {
                            // Empty cell
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

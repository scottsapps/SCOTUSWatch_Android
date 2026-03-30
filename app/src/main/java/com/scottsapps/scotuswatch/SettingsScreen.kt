package com.scottsapps.scotuswatch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class DocTypeRow(
    val key: String,
    val label: String,
    val description: String,
)

private val DOC_TYPES = listOf(
    DocTypeRow("orders",             "Order Lists",
        "Released on Order List Issuance Days"),
    DocTypeRow("slip_opinions",      "Slip Opinions",
        "Full text opinions as issued"),
    DocTypeRow("in_chambers",        "In-Chambers Opinions",
        "Opinions issued by individual Justices"),
    DocTypeRow("relating_to_orders", "Opinions Relating to Orders",
        "Opinions accompanying order list entries"),
    DocTypeRow("misc_order",         "Miscellaneous Orders",
        "Administrative and miscellaneous orders"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Load initial state from SharedPreferences; track locally for instant UI response.
    val enabledState = remember {
        NotificationPreferences.getAll(context)
            .mapValues { (_, v) -> mutableStateOf(v) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose which document types trigger notifications. " +
                        "Disabled types are delivered silently.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            DOC_TYPES.forEachIndexed { index, row ->
                var enabled by enabledState[row.key]!!

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = row.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { newValue ->
                            enabled = newValue
                            NotificationPreferences.setEnabled(context, row.key, newValue)
                        }
                    )
                }

                if (index < DOC_TYPES.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

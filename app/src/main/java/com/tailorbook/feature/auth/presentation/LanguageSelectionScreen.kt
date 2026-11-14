package com.tailorbook.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tailorbook.R
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.TailorBookTheme
import com.tailorbook.ui.theme.WarmIvory

/**
 * Data model for a selectable language item.
 */
data class LanguageOption(
    val code: String,
    val nativeName: String,
    val subtitle: String
)

/**
 * Language Selection Screen for TailorBook.
 * Allows tailors and boutique owners to select their preferred operational language (English or తెలుగు).
 *
 * @param currentLanguageCode Currently selected language code ('en' or 'te')
 * @param onLanguageConfirmed Callback invoked with the selected language code when "Continue" is tapped
 */
@Composable
fun LanguageSelectionScreen(
    currentLanguageCode: String = "en",
    onLanguageConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguageCode) }

    val languages = listOf(
        LanguageOption(
            code = "en",
            nativeName = "English",
            subtitle = stringResource(id = R.string.lang_english_desc)
        ),
        LanguageOption(
            code = "te",
            nativeName = "తెలుగు",
            subtitle = stringResource(id = R.string.lang_telugu_desc)
        )
    )

    Scaffold(
        containerColor = WarmIvory,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                TailorPrimaryButton(
                    text = stringResource(id = R.string.action_continue),
                    onClick = { onLanguageConfirmed(selectedLanguage) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Boutique brand emblem
            Image(
                painter = painterResource(id = R.drawable.ic_tailorbook_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.lang_select_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Charcoal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.lang_select_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalSubtle,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Language Options List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                languages.forEach { option ->
                    LanguageCard(
                        option = option,
                        isSelected = selectedLanguage == option.code,
                        onSelect = { selectedLanguage = option.code }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageCard(
    option: LanguageOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DeepTeal.copy(alpha = 0.05f) else CardWhite
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) DeepTeal else LightBeige
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.nativeName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) DeepTeal else Charcoal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalSubtle
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) DeepTeal else MutedSage,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageSelectionScreenPreview() {
    TailorBookTheme {
        LanguageSelectionScreen(onLanguageConfirmed = {})
    }
}

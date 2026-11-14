package com.tailorbook.feature.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tailorbook.R
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.components.TailorTextButton
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.TailorBookTheme
import com.tailorbook.ui.theme.WarmIvory
import kotlinx.coroutines.launch

/**
 * Model representing an onboarding carousel slide.
 */
data class OnboardingSlide(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector
)

val onboardingSlides = listOf(
    OnboardingSlide(
        titleRes = R.string.onboarding_title_1,
        descRes = R.string.onboarding_desc_1,
        icon = Icons.Default.People
    ),
    OnboardingSlide(
        titleRes = R.string.onboarding_title_2,
        descRes = R.string.onboarding_desc_2,
        icon = Icons.Default.Straighten
    ),
    OnboardingSlide(
        titleRes = R.string.onboarding_title_3,
        descRes = R.string.onboarding_desc_3,
        icon = Icons.Default.CalendarMonth
    ),
    OnboardingSlide(
        titleRes = R.string.onboarding_title_4,
        descRes = R.string.onboarding_desc_4,
        icon = Icons.Default.CloudDone
    )
)

/**
 * Onboarding Welcome Carousel Screen for TailorBook.
 * Features a swipeable pager with boutique indicators, feature explanations,
 * and seamless navigation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { onboardingSlides.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingSlides.size - 1

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip button hidden on the final slide
                AnimatedVisibility(
                    visible = !isLastPage,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TailorTextButton(
                        text = stringResource(id = R.string.onboarding_skip),
                        onClick = onOnboardingComplete
                    )
                }
            }
        },
        containerColor = WarmIvory,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pager Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(onboardingSlides.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) Modifier
                                        .size(width = 24.dp, height = 8.dp)
                                        .background(DeepTeal)
                                    else Modifier
                                        .size(8.dp)
                                        .background(LightBeige)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action button (Next vs Get Started)
                TailorPrimaryButton(
                    text = if (isLastPage) {
                        stringResource(id = R.string.onboarding_get_started)
                    } else {
                        stringResource(id = R.string.action_next)
                    },
                    onClick = {
                        if (isLastPage) {
                            onOnboardingComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { pageIndex ->
            val slide = onboardingSlides[pageIndex]
            OnboardingSlideContent(slide = slide)
        }
    }
}

@Composable
private fun OnboardingSlideContent(
    slide: OnboardingSlide,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Feature icon card
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(DeepTeal),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = slide.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SoftGold
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = stringResource(id = slide.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Charcoal,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = slide.descRes),
            style = MaterialTheme.typography.bodyLarge,
            color = CharcoalSubtle,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    TailorBookTheme {
        OnboardingScreen(onOnboardingComplete = {})
    }
}

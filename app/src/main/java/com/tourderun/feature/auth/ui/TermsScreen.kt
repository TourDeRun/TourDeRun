package com.tourderun.feature.auth.ui

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tourderun.R
import com.tourderun.core.designsystem.component.TdrButton
import com.tourderun.core.designsystem.component.TdrIcons
import com.tourderun.core.designsystem.component.TdrTopBar
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/** 필수 약관 3가지. */
enum class TermsItem(@StringRes val labelRes: Int) {
    SERVICE(R.string.terms_service),
    PRIVACY(R.string.terms_privacy),
    LOCATION(R.string.terms_location),
}

/** S01-1 약관 동의 화면 상태. */
data class TermsUiState(
    val agreed: Set<TermsItem> = emptySet(),
) {
    val allAgreed: Boolean get() = agreed.size == TermsItem.entries.size
}

/** S01-1 약관 동의 (신규 사용자만). 필수 3개를 모두 켜야 다음으로 간다. */
@Composable
fun TermsScreen(
    uiState: TermsUiState,
    onBack: () -> Unit,
    onToggleAll: () -> Unit,
    onToggle: (TermsItem) -> Unit,
    onViewTerms: (TermsItem) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    val type = TdrTheme.typography
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = TdrSpacing.s5, end = TdrSpacing.s5, top = TdrSpacing.s3, bottom = TdrSpacing.s8),
    ) {
        TdrTopBar(onBack = onBack) {
            Text(stringResource(R.string.terms_step), style = type.caption, color = colors.inkMuted)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = TdrSpacing.s5),
            verticalArrangement = Arrangement.spacedBy(TdrSpacing.s5),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.terms_title), style = type.titleLg)
                Text(stringResource(R.string.terms_subtitle), style = type.bodySm, color = colors.inkMuted)
            }

            // 전체 동의
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(TdrShapes.md)
                    .background(if (uiState.allAgreed) colors.brandSoft else colors.bgSubtle)
                    .toggleable(value = uiState.allAgreed, role = Role.Checkbox, onValueChange = { onToggleAll() })
                    .padding(horizontal = TdrSpacing.s4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
            ) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (uiState.allAgreed) colors.brand else colors.borderControl),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(TdrIcons.CheckBold, contentDescription = null, tint = colors.inkOnBrand, modifier = Modifier.size(14.dp))
                }
                Text(stringResource(R.string.terms_all), style = type.subtitle.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TermsItem.entries.forEach { item ->
                    TermsRow(
                        item = item,
                        checked = item in uiState.agreed,
                        onToggle = { onToggle(item) },
                        onView = { onViewTerms(item) },
                    )
                }
            }

            PrivacySummary()
        }
        Spacer(Modifier.height(TdrSpacing.s4))
        TdrButton(
            text = stringResource(if (uiState.allAgreed) R.string.terms_continue else R.string.terms_need_all),
            onClick = onContinue,
            enabled = uiState.allAgreed,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TermsRow(item: TermsItem, checked: Boolean, onToggle: () -> Unit, onView: () -> Unit) {
    val colors = TdrTheme.colors
    val label = stringResource(item.labelRes)
    val required = stringResource(R.string.terms_required)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3),
        ) {
            Icon(
                TdrIcons.Check,
                contentDescription = null,
                tint = if (checked) colors.brand else colors.borderControl,
                modifier = Modifier.size(22.dp),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.brand, fontWeight = FontWeight.Bold)) {
                        append(required)
                    }
                    append(" ")
                    append(label)
                },
                style = TdrTheme.typography.body,
            )
        }
        Text(
            stringResource(R.string.terms_view),
            style = TdrTheme.typography.label.copy(fontWeight = FontWeight.Normal, textDecoration = TextDecoration.Underline),
            color = colors.inkMuted,
            modifier = Modifier
                .clip(TdrShapes.sm)
                .clickable(onClickLabel = stringResource(R.string.terms_view_desc, label), onClick = onView)
                .padding(horizontal = TdrSpacing.s2, vertical = 14.dp),
        )
    }
}

@Composable
private fun PrivacySummary() {
    val colors = TdrTheme.colors
    val rows = listOf(
        R.string.terms_summary_items_label to R.string.terms_summary_items,
        R.string.terms_summary_purpose_label to R.string.terms_summary_purpose,
        R.string.terms_summary_location_label to R.string.terms_summary_location,
        R.string.terms_summary_retention_label to R.string.terms_summary_retention,
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(TdrShapes.lg)
            .background(colors.bgSubtle)
            .padding(TdrSpacing.s4),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.terms_summary_title), style = TdrTheme.typography.label.copy(fontWeight = FontWeight.Bold))
        val small = TdrTheme.typography.caption.copy(lineHeight = 18.sp)
        rows.forEach { (label, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(TdrSpacing.s3)) {
                Text(stringResource(label), style = small, color = colors.inkMuted, modifier = Modifier.width(56.dp))
                Text(stringResource(value), style = small, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TermsScreenPreview() {
    TdrTheme {
        TermsScreen(TermsUiState(agreed = setOf(TermsItem.SERVICE)), {}, {}, {}, {}, {})
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TermsScreenAllPreview() {
    TdrTheme {
        TermsScreen(TermsUiState(agreed = TermsItem.entries.toSet()), {}, {}, {}, {}, {})
    }
}

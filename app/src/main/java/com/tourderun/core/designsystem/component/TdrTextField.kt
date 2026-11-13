package com.tourderun.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tourderun.core.designsystem.theme.TdrShapes
import com.tourderun.core.designsystem.theme.TdrSpacing
import com.tourderun.core.designsystem.theme.TdrTheme

/**
 * 라벨 + 입력창 + 도움말. [maxLength]를 넘는 입력은 받지 않는다.
 *
 * @param focusColor 포커스 테두리 색. 초대 코드 입력은 [com.tourderun.core.designsystem.theme.TdrColors.actionJoin].
 */
@Composable
fun TdrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    maxLength: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
    minHeight: Dp = 48.dp,
    textStyle: TextStyle = TdrTheme.typography.body,
    textAlign: TextAlign = TextAlign.Start,
    focusColor: Color = TdrTheme.colors.brand,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = TdrTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        error != null -> colors.danger
        focused -> focusColor
        else -> colors.borderControl
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(label, style = TdrTheme.typography.label)
        }
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
            textStyle = textStyle.copy(color = colors.ink, textAlign = textAlign),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            interactionSource = interaction,
            cursorBrush = SolidColor(focusColor),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight)
                        .border(if (focused || error != null) 2.dp else 1.dp, borderColor, TdrShapes.md)
                        .padding(horizontal = TdrSpacing.s4, vertical = TdrSpacing.s3),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            placeholder,
                            style = textStyle.copy(textAlign = textAlign),
                            color = colors.inkSubtle,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    inner()
                }
            },
        )
        val help = error ?: helper
        if (help != null) {
            Text(
                help,
                style = TdrTheme.typography.caption,
                color = if (error != null) colors.danger else colors.inkMuted,
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun TdrTextFieldPreview() {
    TdrTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TdrTextField("민지", {}, label = "닉네임", helper = "2~10자, 한글·영문·숫자만 쓸 수 있어요 (공백 불가)")
            TdrTextField("", {}, label = "팀 이름", placeholder = "뛰뛰크루")
            TdrTextField("민 지", {}, label = "닉네임", error = "공백은 쓸 수 없어요")
        }
    }
}

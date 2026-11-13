package com.tourderun.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tourderun.R
import com.tourderun.core.designsystem.theme.TdrSize
import com.tourderun.core.designsystem.theme.TdrTheme

/**
 * 44dp 상단바: 왼쪽 뒤로 · 가운데 제목 · 오른쪽 동작.
 * 화면 좌우 여백(20dp) 안에 놓는다. 뒤로 버튼은 터치 영역만큼 왼쪽으로 내민다.
 */
@Composable
fun TdrTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    titleStyle: TextStyle = TdrTheme.typography.topBar,
    titleColor: Color = TdrTheme.colors.ink,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(modifier.fillMaxWidth().height(TdrSize.touch)) {
        if (onBack != null) {
            TdrIconButton(
                icon = TdrIcons.Back,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).offset(x = (-12).dp),
            )
        }
        if (title != null) {
            Text(
                text = title,
                style = titleStyle,
                color = titleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = TdrSize.touch)
                    .semantics { heading() },
            )
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** 상단바 오른쪽 글자 버튼 ("수정", "저장"). */
@Composable
fun TdrTopBarTextAction(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.offset(x = 12.dp)) {
        Text(
            text,
            style = TdrTheme.typography.subtitle,
            color = if (enabled) TdrTheme.colors.brand else TdrTheme.colors.inkSubtle,
        )
    }
}

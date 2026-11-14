package com.tourderun.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tourderun.R
import com.tourderun.core.designsystem.theme.TdrTheme

/** 하단 탭 4칸. 가운데 [Run]은 떠 있는 둥근 버튼(FAB)으로 그린다. */
enum class TdrTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    Home(R.string.tab_home, TdrIcons.TabHome),
    Road(R.string.tab_road, TdrIcons.TabRoad),
    Run(R.string.tab_run, TdrIcons.Runner),
    Profile(R.string.tab_profile, TdrIcons.TabProfile),
}

/**
 * 하단 탭바. 화면 연결(NavHost)은 AppNavHost 담당이 이 컴포저블을 Scaffold bottomBar에 넣는다.
 * [TdrTab.Run]은 화면 전환이 아니라 러닝 시작 동작으로 처리해도 된다.
 */
@Composable
fun TdrTabBar(
    selected: TdrTab,
    onSelect: (TdrTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TdrTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.bgSurface)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = colors.border)
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TdrTab.entries.forEach { tab ->
                val on = tab == selected
                val label = stringResource(tab.labelRes)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(tab) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (tab == TdrTab.Run) {
                        Box(
                            Modifier
                                .offset(y = (-10).dp)
                                .size(52.dp)
                                .shadow(8.dp, CircleShape, ambientColor = colors.brand, spotColor = colors.brand)
                                .background(colors.brand, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(tab.icon, contentDescription = null, tint = colors.inkOnBrand, modifier = Modifier.size(24.dp))
                        }
                        Text(
                            label,
                            style = TdrTheme.typography.label,
                            color = if (on) colors.brand else colors.inkMuted,
                            // 버튼(52dp)이 칸 높이(64dp)를 거의 다 써서, 남은 높이에 맞추면 글자 아래가 잘린다.
                            modifier = Modifier
                                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                                .offset(y = (-6).dp),
                        )
                    } else {
                        Icon(
                            tab.icon,
                            contentDescription = null,
                            tint = if (on) colors.brand else colors.inkSubtle,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(label, style = TdrTheme.typography.label, color = if (on) colors.brand else colors.inkMuted)
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 390)
@Composable
private fun TdrTabBarPreview() {
    TdrTheme {
        TdrTabBar(selected = TdrTab.Home, onSelect = {})
    }
}

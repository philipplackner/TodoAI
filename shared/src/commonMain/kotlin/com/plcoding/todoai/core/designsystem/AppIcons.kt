package com.plcoding.todoai.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import todoai.shared.generated.resources.Res
import todoai.shared.generated.resources.ic_add
import todoai.shared.generated.resources.ic_arrow_back
import todoai.shared.generated.resources.ic_build
import todoai.shared.generated.resources.ic_calendar
import todoai.shared.generated.resources.ic_chat
import todoai.shared.generated.resources.ic_check
import todoai.shared.generated.resources.ic_close
import todoai.shared.generated.resources.ic_delete
import todoai.shared.generated.resources.ic_download
import todoai.shared.generated.resources.ic_refresh
import todoai.shared.generated.resources.ic_send
import todoai.shared.generated.resources.ic_sparkle

object AppIcons {
    val Add: Painter @Composable get() = painterResource(Res.drawable.ic_add)
    val ArrowBack: Painter @Composable get() = painterResource(Res.drawable.ic_arrow_back)
    val Build: Painter @Composable get() = painterResource(Res.drawable.ic_build)
    val Calendar: Painter @Composable get() = painterResource(Res.drawable.ic_calendar)
    val Chat: Painter @Composable get() = painterResource(Res.drawable.ic_chat)
    val Check: Painter @Composable get() = painterResource(Res.drawable.ic_check)
    val Close: Painter @Composable get() = painterResource(Res.drawable.ic_close)
    val Delete: Painter @Composable get() = painterResource(Res.drawable.ic_delete)
    val Download: Painter @Composable get() = painterResource(Res.drawable.ic_download)
    val Refresh: Painter @Composable get() = painterResource(Res.drawable.ic_refresh)
    val Send: Painter @Composable get() = painterResource(Res.drawable.ic_send)
    val Sparkle: Painter @Composable get() = painterResource(Res.drawable.ic_sparkle)
}

package com.plcoding.todoai.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import todoai.shared.generated.resources.Res
import todoai.shared.generated.resources.ic_add
import todoai.shared.generated.resources.ic_arrow_back
import todoai.shared.generated.resources.ic_chat
import todoai.shared.generated.resources.ic_check
import todoai.shared.generated.resources.ic_delete
import todoai.shared.generated.resources.ic_send
import todoai.shared.generated.resources.ic_sparkle

object AppIcons {
    val Add: Painter @Composable get() = painterResource(Res.drawable.ic_add)
    val ArrowBack: Painter @Composable get() = painterResource(Res.drawable.ic_arrow_back)
    val Chat: Painter @Composable get() = painterResource(Res.drawable.ic_chat)
    val Check: Painter @Composable get() = painterResource(Res.drawable.ic_check)
    val Delete: Painter @Composable get() = painterResource(Res.drawable.ic_delete)
    val Send: Painter @Composable get() = painterResource(Res.drawable.ic_send)
    val Sparkle: Painter @Composable get() = painterResource(Res.drawable.ic_sparkle)
}

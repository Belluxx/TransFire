package com.belluxx.transfire.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.belluxx.transfire.data.Message
import com.belluxx.transfire.data.MessageAuthor
import com.belluxx.transfire.mdOptions
import com.halilibo.richtext.commonmark.CommonmarkAstNodeParser
import com.halilibo.richtext.markdown.BasicMarkdown
import com.halilibo.richtext.ui.material3.RichText

@Composable
fun ChatBubble(message: Message) {
    if (message.name == MessageAuthor.SYSTEM) return
    val isUser = message.name == MessageAuthor.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.padding(start = if (isUser) 48.dp else 0.dp, end = if (isUser) 0.dp else 48.dp),
            shape = RoundedCornerShape(16.dp, 16.dp, if (isUser) 16.dp else 4.dp, if (isUser) 4.dp else 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(12.dp)) {
                if (!isUser) {
                    Text(
                        "Assistant",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (isUser) {
                    Text(
                        message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        RichText {
                            val parser = remember(mdOptions) { CommonmarkAstNodeParser(mdOptions) }
                            BasicMarkdown(remember(parser) { parser.parse(message.content) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Card(
            modifier = Modifier.padding(end = 48.dp),
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "Assistant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    repeat(3) { i ->
                        val alpha by rememberInfiniteTransition(label = "dot").animateFloat(
                            0.25f, 0.85f,
                            infiniteRepeatable(
                                tween(800, i * 120, FastOutSlowInEasing),
                                RepeatMode.Reverse
                            ), label = "alpha"
                        )
                        Box(
                            Modifier
                                .size(5.dp)
                                .graphicsLayer { this.alpha = alpha }
                                .background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
    }
}
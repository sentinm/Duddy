package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FilePillBackgroundDark
import com.example.ui.theme.FilePillBackgroundLight
import com.example.ui.theme.FilePillBorderDark
import com.example.ui.theme.FilePillBorderLight
import com.example.ui.theme.FilePillTextDark
import com.example.ui.theme.FilePillTextLight
import com.example.util.FileHelpers

/**
 * File name container displayed on top of the progress bar in a different color
 * rectangular shape with very curved edges (RoundedCornerShape 24dp).
 */
@Composable
fun FileNameBadge(
    fileName: String,
    totalBytes: Long? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) FilePillBackgroundDark else FilePillBackgroundLight
    val textColor = if (isDark) FilePillTextDark else FilePillTextLight
    val borderColor = if (isDark) FilePillBorderDark else FilePillBorderLight

    // Rectangular shape with very curved edges
    val pillShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(pillShape)
            .background(bgColor)
            .border(1.5.dp, borderColor, pillShape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("file_name_badge"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon representing the file type
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = textColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = FileIconHelper.getIconForFileName(fileName),
                        contentDescription = "File Type",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // File Name in high contrast text
            Text(
                text = fileName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = textColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag("file_name_text")
            )

            // Optional Size Badge if known
            if (totalBytes != null && totalBytes > 0) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = textColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = FileHelpers.formatBytes(totalBytes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

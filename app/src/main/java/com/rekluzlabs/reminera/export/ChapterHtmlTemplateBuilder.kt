package com.rekluzlabs.reminera.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.rekluzlabs.reminera.data.ChapterExportEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChapterHtmlTemplateBuilder {

    data class ChapterInput(
        val chapter: ChapterExportEntity,
        val member: FamilyMemberEntity,
        val mediaEntries: List<MemoryEntryEntity>
    )

    fun buildHtml(input: ChapterInput): String {
        val photos = input.mediaEntries.filter { it.type == "PHOTO" }
        val audioVideo = input.mediaEntries.filter { it.type == "VIDEO" || it.type == "AUDIO" }

        val avatarBase64 = input.member.photoUri?.let { loadAndEncodeImage(it) }
        val photoSections = photos.map { encodePhotoSection(it) }
        val mediaSections = audioVideo.map { encodeMediaPlaceholder(it) }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<style>
:root {
    --page-margin: 48px;
    --heading-font: Georgia, "Times New Roman", serif;
    --body-font: -apple-system, "Segoe UI", Helvetica, Arial, sans-serif;
    --accent-color: #6b5b4d;
    --text-color: #2a2a2a;
    --muted-color: #7a7a7a;
    --card-bg: #f8f6f3;
    --card-border: #e0dcd7;
}

@page {
    margin: var(--page-margin);
    size: A4;
}

* { box-sizing: border-box; margin: 0; padding: 0; }

body {
    font-family: var(--body-font);
    color: var(--text-color);
    line-height: 1.6;
    font-size: 14px;
}

.chapter-header {
    text-align: center;
    padding: 32px 0 24px;
    border-bottom: 2px solid var(--accent-color);
    margin-bottom: 24px;
    break-after: avoid;
}

.chapter-header .avatar {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    object-fit: cover;
    margin-bottom: 12px;
    border: 3px solid var(--accent-color);
}

.chapter-header h1 {
    font-family: var(--heading-font);
    font-size: 28px;
    font-weight: bold;
    color: var(--text-color);
    margin-bottom: 4px;
}

.chapter-header .subtitle {
    font-size: 14px;
    color: var(--muted-color);
}

.biography-section {
    margin-bottom: 28px;
}

.biography-section p {
    margin-bottom: 12px;
    text-indent: 1.5em;
}

.biography-section p:first-child {
    text-indent: 0;
}

.section-title {
    font-family: var(--heading-font);
    font-size: 18px;
    color: var(--accent-color);
    margin-bottom: 12px;
    padding-bottom: 4px;
    border-bottom: 1px solid var(--card-border);
    break-after: avoid;
}

.photo-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin-bottom: 28px;
}

.photo-tile {
    break-inside: avoid;
    page-break-inside: avoid;
}

.photo-tile img {
    width: 100%;
    aspect-ratio: 4/3;
    object-fit: cover;
    border-radius: 4px;
    display: block;
}

.photo-tile .caption {
    font-size: 11px;
    color: var(--muted-color);
    margin-top: 4px;
    text-align: center;
}

.media-placeholder-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin-bottom: 28px;
}

.media-placeholder {
    background: var(--card-bg);
    border: 1px solid var(--card-border);
    border-radius: 8px;
    padding: 16px;
    text-align: center;
    break-inside: avoid;
    page-break-inside: avoid;
}

.media-placeholder .type-badge {
    display: inline-block;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--accent-color);
    background: white;
    padding: 2px 8px;
    border-radius: 4px;
    margin-bottom: 8px;
    border: 1px solid var(--card-border);
}

.media-placeholder .title {
    font-size: 13px;
    font-weight: 500;
    margin-bottom: 4px;
}

.media-placeholder .date {
    font-size: 11px;
    color: var(--muted-color);
    margin-bottom: 12px;
}

.qr-placeholder {
    width: 100px;
    height: 100px;
    margin: 0 auto 8px;
    border: 2px dashed var(--card-border);
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    color: var(--muted-color);
}

.chapter-footer {
    margin-top: 40px;
    padding-top: 16px;
    border-top: 1px solid var(--card-border);
    text-align: center;
    font-size: 11px;
    color: var(--muted-color);
}

.empty-state {
    text-align: center;
    padding: 40px 0;
    color: var(--muted-color);
    font-style: italic;
}
</style>
</head>
<body>

<div class="chapter-header">
    ${avatarBase64?.let { """<img class="avatar" src="data:image/jpeg;base64,$it" alt="${escapeHtml(input.member.name)}"/>""" } ?: ""}
    <h1>${escapeHtml(input.member.name)}</h1>
    <div class="subtitle">${escapeHtml(input.member.role)}${formatBirthDate(input.member.birthDate)}</div>
</div>

${buildBiographySection(input.chapter.generatedBioText)}

${if (photoSections.isNotEmpty()) """
<div class="section-title">Photographs</div>
<div class="photo-grid">
${photoSections.joinToString("\n")}
</div>
""" else ""}

${if (mediaSections.isNotEmpty()) """
<div class="section-title">Recordings</div>
<div class="media-placeholder-grid">
${mediaSections.joinToString("\n")}
</div>
""" else ""}

<div class="chapter-footer">
    Reminera &mdash; Preserve your family's history
</div>

</body>
</html>
""".trimIndent()
    }

    private fun buildBiographySection(bioText: String): String {
        val paragraphs = bioText
            .split(Regex("\n\\s*\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (paragraphs.isEmpty()) return ""

        return """
<div class="section-title">Biography</div>
<div class="biography-section">
${paragraphs.joinToString("\n") { "<p>${escapeHtml(it)}</p>" }}
</div>
""".trimIndent()
    }

    private fun encodePhotoSection(media: MemoryEntryEntity): String {
        val base64 = loadAndEncodeImage(media.localFilePath) ?: return ""
        val caption = formatDate(media.dateCaptured)
        return """
<div class="photo-tile">
    <img src="data:image/jpeg;base64,$base64" alt="${escapeHtml(media.title)}"/>
    <div class="caption">${escapeHtml(media.title)} &mdash; $caption</div>
</div>
""".trimIndent()
    }

    private fun encodeMediaPlaceholder(media: MemoryEntryEntity): String {
        val typeLabel = if (media.type == "VIDEO") "Video" else "Audio"
        val date = formatDate(media.dateCaptured)
        val duration = media.durationMillis?.let { formatDuration(it) } ?: ""

        return """
<div class="media-placeholder">
    <div class="type-badge">$typeLabel</div>
    <div class="title">${escapeHtml(media.title)}</div>
    <div class="date">$date${if (duration.isNotEmpty()) " &bull; $duration" else ""}</div>
    <div class="qr-placeholder">
        <span>QR code<br/>coming in a<br/>later step</span>
    </div>
</div>
""".trimIndent()
    }

    fun loadAndEncodeImage(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var inSampleSize = 1
            val maxDim = 1024
            if (options.outHeight > maxDim || options.outWidth > maxDim) {
                inSampleSize = maxOf(options.outHeight, options.outWidth) / maxDim
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null

            val outputStream = ByteArrayOutputStream()
            val format = if (file.extension.equals("png", ignoreCase = true)) {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }
            bitmap.compress(format, 85, outputStream)
            bitmap.recycle()

            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun formatBirthDate(timestamp: Long?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("yyyy", Locale.US)
        return " &bull; Born ${sdf.format(Date(timestamp))}"
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
        return sdf.format(Date(timestamp))
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

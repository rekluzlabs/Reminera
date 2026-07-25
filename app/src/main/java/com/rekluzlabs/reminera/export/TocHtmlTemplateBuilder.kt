package com.rekluzlabs.reminera.export

import com.rekluzlabs.reminera.data.FamilyMemberEntity

object TocHtmlTemplateBuilder {

    data class TocEntry(
        val memberName: String,
        val role: String,
        val startPage: Int
    )

    fun buildHtml(bookTitle: String, entries: List<TocEntry>): String {
        val rows = entries.joinToString("\n") { entry ->
            """
            <tr>
                <td class="entry-name">${escapeHtml(entry.memberName)}</td>
                <td class="entry-role">${escapeHtml(entry.role)}</td>
                <td class="entry-page">${entry.startPage}</td>
            </tr>
            """.trimIndent()
        }

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

.toc-header {
    text-align: center;
    padding: 32px 0 24px;
    border-bottom: 2px solid var(--accent-color);
    margin-bottom: 24px;
}

.toc-header h1 {
    font-family: var(--heading-font);
    font-size: 28px;
    font-weight: bold;
    color: var(--text-color);
    margin-bottom: 4px;
}

.toc-header .subtitle {
    font-size: 14px;
    color: var(--muted-color);
}

.toc-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 28px;
}

.toc-table tr {
    border-bottom: 1px solid var(--card-border);
}

.toc-table td {
    padding: 10px 8px;
    vertical-align: baseline;
}

.toc-table .entry-name {
    font-weight: 500;
    font-size: 15px;
}

.toc-table .entry-role {
    font-size: 13px;
    color: var(--muted-color);
    padding-left: 12px;
}

.toc-table .entry-page {
    text-align: right;
    font-size: 13px;
    color: var(--muted-color);
    width: 60px;
    font-variant-numeric: tabular-nums;
}

.toc-footer {
    margin-top: 40px;
    padding-top: 16px;
    border-top: 1px solid var(--card-border);
    text-align: center;
    font-size: 11px;
    color: var(--muted-color);
}
</style>
</head>
<body>

<div class="toc-header">
    <h1>${escapeHtml(bookTitle)}</h1>
    <div class="subtitle">Table of Contents</div>
</div>

<table class="toc-table">
${rows}
</table>

<div class="toc-footer">
    Reminera &mdash; Preserve your family's history
</div>

</body>
</html>
""".trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

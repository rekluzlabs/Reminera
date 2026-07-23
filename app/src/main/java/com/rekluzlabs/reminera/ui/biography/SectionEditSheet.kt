package com.rekluzlabs.reminera.ui.biography

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SECTION_FIELD_DEFINITIONS: Map<String, List<FieldDef>> = mapOf(
    "origins" to listOf(
        FieldDef("birthplace", "Birthplace"),
        FieldDef("heritage", "Heritage & Ethnicity"),
        FieldDef("familyBackground", "Family Background"),
        FieldDef("childhood", "Childhood")
    ),
    "milestones" to listOf(
        FieldDef("education", "Education"),
        FieldDef("career", "Career & Work"),
        FieldDef("marriage", "Marriage & Partnership"),
        FieldDef("achievements", "Major Achievements")
    ),
    "personality" to listOf(
        FieldDef("traits", "Personality Traits"),
        FieldDef("hobbies", "Hobbies & Interests"),
        FieldDef("values", "Values & Beliefs"),
        FieldDef("quirks", "Quirks & Habits")
    ),
    "legacy" to listOf(
        FieldDef("wisdom", "Life Wisdom"),
        FieldDef("impact", "Impact on Others"),
        FieldDef("traditions", "Family Traditions"),
        FieldDef("message", "Message to Future Generations")
    )
)

private data class FieldDef(val key: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionEditSheet(
    sectionType: String,
    sectionLabel: String,
    initialFields: Map<String, String>,
    onSave: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val fields = remember(initialFields) { mutableStateMapOf<String, String>().apply { putAll(initialFields) } }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fieldDefs = SECTION_FIELD_DEFINITIONS[sectionType] ?: emptyList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = sectionLabel,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            fieldDefs.forEach { fieldDef ->
                Text(
                    text = fieldDef.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = fields[fieldDef.key] ?: "",
                    onValueChange = { fields[fieldDef.key] = it },
                    placeholder = { Text("Enter ${fieldDef.label.lowercase()}...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSave(fields.toMap()) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

package com.rekluzlabs.reminera.ui.home

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.rekluzlabs.reminera.BuildConfig
import com.rekluzlabs.reminera.data.BiographyDao
import com.rekluzlabs.reminera.data.BiographySectionDao
import com.rekluzlabs.reminera.data.ChapterExportDao
import com.rekluzlabs.reminera.data.FamilyGroupEntity
import com.rekluzlabs.reminera.data.FamilyMemberDao
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.MemoryEntryDao
import com.rekluzlabs.reminera.data.RemineraDatabase
import com.rekluzlabs.reminera.data.StoryEntryDao
import com.rekluzlabs.reminera.data.repository.ChapterExportRepository
import com.rekluzlabs.reminera.export.ChapterHtmlTemplateBuilder
import com.rekluzlabs.reminera.export.ChapterPdfRenderer
import com.rekluzlabs.reminera.ui.editor.ImageEditorScreen
import com.rekluzlabs.reminera.util.copyUriToInternal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMemberListScreen(
    groupId: Long,
    viewModel: RemineraViewModel,
    onBack: () -> Unit,
    onMemberClick: (FamilyMemberEntity) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { RemineraDatabase.getInstance(context) }
    val groupsState = db.familyGroupDao().getAllOrderedBySortOrder()
        .collectAsState(initial = emptyList())
    val allGroups = groupsState.value
    val currentGroup = allGroups.find { it.id == groupId }
    val membersState = viewModel.getMembersByGroupId(groupId).collectAsState(initial = emptyList())
    val members = membersState.value

    var showAddMemberSheet by remember { mutableStateOf(false) }
    var showEditMemberSheet by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    var showDeleteMemberDialog by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    var fullScreenPhotoUri by remember { mutableStateOf<String?>(null) }
    var editingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var memberBeingEdited by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to groups",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = currentGroup?.name ?: "Family Group",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Text(
                            text = "Choose a family member",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (members.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No family members yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to add your first family member.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(members, key = { it.id }) { member ->
                        MemberItemCard(
                            member = member,
                            onClick = { onMemberClick(member) },
                            onPhotoClick = { member.photoUri?.let { fullScreenPhotoUri = it } },
                            onEditMember = { showEditMemberSheet = member },
                            onDeleteMember = { showDeleteMemberDialog = member }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddMemberSheet = true },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add family member")
        }
    }

    if (showAddMemberSheet && currentGroup != null) {
        AddFamilyMemberSheet(
            groupName = currentGroup.name,
            groupType = currentGroup.groupType,
            onSave = { name, role, birthDate, photoUri ->
                scope.launch {
                    val persistentUri = if (photoUri != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                copyUriToInternal(context, Uri.parse(photoUri), "jpg")
                            } catch (_: Exception) { photoUri }
                        }
                    } else null
                    viewModel.addMember(groupId, name, role, "", birthDate, persistentUri)
                }
                showAddMemberSheet = false
            },
            onDismiss = { showAddMemberSheet = false }
        )
    }

    if (fullScreenPhotoUri != null) {
        androidx.activity.compose.BackHandler { fullScreenPhotoUri = null }
        FullScreenPhotoViewer(
            photoUri = fullScreenPhotoUri!!,
            onBack = { fullScreenPhotoUri = null },
            onEdit = {
                // Find the member for this photo
                memberBeingEdited = members.find { it.photoUri == fullScreenPhotoUri }
                editingPhotoUri = Uri.parse(fullScreenPhotoUri!!)
                fullScreenPhotoUri = null
            }
        )
    }

    if (editingPhotoUri != null && memberBeingEdited != null) {
        ImageEditorScreen(
            imageUri = editingPhotoUri!!,
            onSave = { editedUri ->
                scope.launch {
                    val persistentUri = withContext(Dispatchers.IO) {
                        try {
                            copyUriToInternal(context, editedUri, "jpg")
                        } catch (_: Exception) { editedUri.toString() }
                    }
                    viewModel.updateMemberPhoto(memberBeingEdited!!.id, persistentUri)
                }
                editingPhotoUri = null
                memberBeingEdited = null
            },
            onDismiss = {
                editingPhotoUri = null
                memberBeingEdited = null
            }
        )
    }

    showEditMemberSheet?.let { member ->
        EditFamilyMemberSheet(
            member = member,
            groupName = currentGroup?.name ?: "",
            groupType = currentGroup?.groupType ?: "",
            onSave = { name, role, birthDate, photoUri ->
                scope.launch {
                    val persistentUri = if (photoUri != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                copyUriToInternal(context, Uri.parse(photoUri), "jpg")
                            } catch (_: Exception) { photoUri }
                        }
                    } else null
                    viewModel.updateMemberDetails(member.id, name, role, birthDate, persistentUri)
                }
                showEditMemberSheet = null
            },
            onDismiss = { showEditMemberSheet = null }
        )
    }

    showDeleteMemberDialog?.let { member ->
        AlertDialog(
            onDismissRequest = { showDeleteMemberDialog = null },
            title = { Text("Delete ${member.name}?") },
            text = {
                Text("This will remove ${member.name} from this family group. Memories tagged with their name will be kept but will no longer be associated with this profile.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMember(member.id)
                        showDeleteMemberDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMemberDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MemberItemCard(
    member: FamilyMemberEntity,
    onClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onEditMember: () -> Unit,
    onDeleteMember: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }
    val memberPhoto = remember(member.photoUri) {
        member.photoUri?.let { uriStr ->
            try {
                val file = File(uriStr)
                if (file.exists()) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                } else {
                    val uri = Uri.parse(uriStr)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
            } catch (_: Exception) { null }
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (memberPhoto != null) {
                Image(
                    bitmap = memberPhoto.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = true, onClick = onPhotoClick),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(enabled = true, onClick = onPhotoClick)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (member.biography.isNotBlank()) {
                    Text(
                        text = member.biography,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            IconButton(onClick = onEditMember) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit member",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // DEBUG-ONLY: remove or gate behind a proper settings flag once export UI exists
            if (BuildConfig.DEBUG) {
                IconButton(
                    onClick = {
                        if (!isGeneratingPdf) {
                            isGeneratingPdf = true
                            scope.launch {
                                try {
                                    val db = RemineraDatabase.getInstance(context)
                                    val chapterRepo = ChapterExportRepository(
                                        chapterDao = db.chapterExportDao(),
                                        memberDao = db.familyMemberDao(),
                                        biographyDao = db.biographyDao(),
                                        sectionDao = db.biographySectionDao(),
                                        storyDao = db.storyEntryDao(),
                                        memoryDao = db.memoryEntryDao()
                                    )
                                    val chapter = withContext(Dispatchers.IO) {
                                        chapterRepo.getOrGenerateChapter(member.id)
                                    }
                                    val mediaEntries = withContext(Dispatchers.IO) {
                                        db.memoryEntryDao()
                                            .getEntriesByGroupIdAndPersonTagList(
                                                member.groupId, member.name
                                            )
                                    }
                                    val html = ChapterHtmlTemplateBuilder.buildHtml(
                                        ChapterHtmlTemplateBuilder.ChapterInput(
                                            chapter = chapter,
                                            member = member,
                                            mediaEntries = mediaEntries
                                        )
                                    )
                                    val result = ChapterPdfRenderer.renderChapter(
                                        context = context,
                                        memberId = member.id,
                                        html = html,
                                        chapterTitle = "Chapter: ${member.name}"
                                    )
                                    if (result is ChapterPdfRenderer.RenderResult.Success) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "com.rekluzlabs.reminera.fileprovider",
                                            result.outputFile
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    }
                                } catch (_: Exception) { }
                                isGeneratingPdf = false
                            }
                        }
                    },
                    enabled = !isGeneratingPdf
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Preview chapter PDF (debug)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            IconButton(onClick = onDeleteMember) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete member",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}



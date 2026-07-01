package com.klicmobile.app.feature.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.klicmobile.app.data.Attachment
import com.klicmobile.app.data.Conversation
import com.klicmobile.app.data.Message
import com.klicmobile.app.feature.KlicViewModel
import com.klicmobile.app.ui.components.AvatarView
import com.klicmobile.app.ui.components.MessageTicks
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: KlicViewModel,
    conversation: Conversation,
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    onOpenProfile: () -> Unit = {},
) {
    val messages by vm.messages.collectAsState()
    val me by vm.currentUser.collectAsState()
    val presenceMap by vm.presence.collectAsState()
    var draft by remember { mutableStateOf("") }
    val peer = conversation.members.firstOrNull()
    val isDirect = conversation.type == "DIRECT"
    val title = when {
        !conversation.title.isNullOrBlank() -> conversation.title
        isDirect -> peer?.displayName ?: "Chat"
        else -> conversation.members.joinToString(", ") { it.displayName }.ifBlank { "Group" }
    }
    val headerSubtitle = when {
        isDirect -> peer?.id?.let { presenceSubtitle(presenceMap[it]) }
        conversation.members.isNotEmpty() -> "${conversation.members.size + 1} members"
        else -> null
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current
    var showAttachSheet by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMedia by remember(conversation.id) { mutableStateOf<List<PendingMediaDraft>>(emptyList()) }
    var captureMode by remember(conversation.id) { mutableStateOf(CaptureMode.AUDIO) }
    var uploading by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val stickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val stickers by vm.stickers.collectAsState()

    val typingMap by vm.typing.collectAsState()
    val replyingTo by vm.replyingTo.collectAsState()
    val clipboard = LocalClipboardManager.current
    var menuTarget by remember { mutableStateOf<Message?>(null) }
    var deleteTarget by remember { mutableStateOf<Message?>(null) }
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    val peerTyping = isDirect && typingMap[conversation.id]?.let { System.currentTimeMillis() - it < 6000L } == true
    val displaySubtitle = if (peerTyping) "typing…" else headerSubtitle

    // Pagination
    val isLoadingOlder by vm.isLoadingOlderMessages.collectAsState()
    val hasMore by vm.hasMoreMessages.collectAsState()

    val recorder = remember { VoiceRecorder(context) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && !recorder.start()) vm.error.value = "Couldn't start recording."
    }

    // Multi-select photos + videos together, matching iOS's PhotosPicker(.any(of: [.images, .videos])).
    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val drafts = uris.mapNotNull { loadMediaDraft(context, it) }
                if (drafts.isEmpty()) {
                    vm.error.value = "Couldn't read selected media."
                } else {
                    pendingMedia = pendingMedia + drafts
                }
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = tempCameraUri
        tempCameraUri = null
        if (success && uri != null) {
            scope.launch {
                loadImageDraft(context, uri)?.let { draft ->
                    pendingMedia = pendingMedia + draft
                } ?: run {
                    vm.error.value = "Couldn't read captured photo."
                }
            }
        }
    }
    // Video capture from the composer's hold-to-record button (captureMode == VIDEO).
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        val uri = tempVideoUri
        tempVideoUri = null
        if (success && uri != null) {
            scope.launch {
                loadVideoDraft(context, uri)?.let { draft ->
                    pendingMedia = pendingMedia + draft
                } ?: run {
                    vm.error.value = "Couldn't read captured video."
                }
            }
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { docUri ->
            scope.launch {
                uploading = true
                val attachment = loadFileAttachment(context, docUri)
                if (attachment != null) {
                    vm.sendAttachments(conversation.id, null, listOf(attachment)).join()
                } else {
                    vm.error.value = "Couldn't read selected file."
                }
                uploading = false
            }
        }
    }

    LaunchedEffect(conversation.id) {
        vm.openChat(conversation.id)
        vm.markRead(conversation.id)
    }

    // Initial open: instant scroll to bottom (no animation).
    // Subsequent new messages: animated scroll.
    // Older messages prepended: no scroll-to-bottom (lastMessageId doesn't change).
    val lastMessageId = messages.lastOrNull()?.id
    var initialScrollDone by remember(conversation.id) { mutableStateOf(false) }

    LaunchedEffect(lastMessageId, peerTyping) {
        val target = messages.size - 1 + if (peerTyping) 1 else 0
        if (target >= 0) {
            if (!initialScrollDone) {
                listState.scrollToItem(target)
                initialScrollDone = true
            } else {
                scope.launch { listState.animateScrollToItem(target) }
            }
        }
    }

    // Restore scroll position after older messages are prepended.
    LaunchedEffect(Unit) {
        vm.prependedCount.collect { count ->
            if (count > 0) {
                val newIndex = (listState.firstVisibleItemIndex + count)
                    .coerceAtMost(messages.size - 1)
                listState.scrollToItem(newIndex, listState.firstVisibleItemScrollOffset)
            }
        }
    }

    // Trigger pagination when the user reaches the top.
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset < 10
        }
    }
    LaunchedEffect(isAtTop) {
        if (isAtTop && hasMore && !isLoadingOlder && initialScrollDone) {
            vm.loadOlderMessages()
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (isDirect) Modifier.clickable(onClick = onOpenProfile) else Modifier,
                    ) {
                        AvatarView(url = peer?.avatarUrl, name = title, size = 34.dp)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            if (displaySubtitle != null) {
                                Text(
                                    displaySubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (peerTyping || presenceMap[peer?.id]?.online == true)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isDirect) {
                        IconButton(onClick = { vm.startCall(conversation.id, "AUDIO", title); onCall("AUDIO") }) {
                            Icon(Icons.Filled.Call, contentDescription = "Voice call", modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = { vm.startCall(conversation.id, "VIDEO", title); onCall("VIDEO") }) {
                            Icon(Icons.Filled.Videocam, contentDescription = "Video call", modifier = Modifier.size(24.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
        Column(
            Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(messages.indices.toList()) { idx ->
                    val msg = messages[idx]
                    val isMine = msg.senderId == me?.id
                    val isFirst = idx == 0 || messages[idx - 1].senderId != msg.senderId
                    val isLast  = idx == messages.size - 1 || messages[idx + 1].senderId != msg.senderId

                    if (idx == 0 || !sameDay(messages[idx - 1].createdAt, msg.createdAt)) {
                        DateSeparator(msg.createdAt)
                    }
                    MessageBubble(
                        message = msg,
                        isMine  = isMine,
                        isFirst = isFirst,
                        isLast  = isLast,
                        replyAuthorName = msg.replyTo?.let { if (it.senderId == me?.id) "You" else title } ?: "",
                        onCallBack = { kind -> vm.startCall(conversation.id, kind, title); onCall(kind) },
                        onLongPress = { menuTarget = msg },
                        onReactionTap = { emoji -> vm.react(conversation.id, msg.id, emoji) },
                        onImageClick = { url -> viewerUrl = url },
                    )
                }
                if (peerTyping) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.Start) {
                            TypingBubble()
                        }
                    }
                }
            }

            // Loading indicator overlay at the top while fetching older messages.
            if (isLoadingOlder) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Scroll-to-latest button: shown only when the newest item is off-screen.
            val isAtBottom by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                    info.totalItemsCount == 0 || lastVisible >= info.totalItemsCount - 1
                }
            }
            if (!isAtBottom) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 8.dp)
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clickable {
                            scope.launch {
                                val target = maxOf(0, listState.layoutInfo.totalItemsCount - 1)
                                listState.animateScrollToItem(target)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Scroll to latest",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            } // Box (messages)

            if (recorder.isRecording) {
                RecordingBar(
                    elapsed  = recorder.elapsed,
                    onCancel = { recorder.cancel() },
                    onSend   = {
                        recorder.stop()?.let { (bytes, durationMs, waveform) ->
                            vm.sendVoice(conversation.id, bytes, durationMs, waveform)
                        }
                    },
                )
            } else {
                if (pendingMedia.isNotEmpty()) {
                    PendingMediaBar(
                        items = pendingMedia,
                        onRemove = { id -> pendingMedia = pendingMedia.filterNot { it.id == id } },
                    )
                }
                replyingTo?.let { target ->
                    ReplyComposerBar(
                        authorName = if (target.senderId == me?.id) "yourself" else title,
                        preview = messagePreview(target),
                        onCancel = { vm.setReplyTo(null) },
                    )
                }
                ComposerBar(
                    draft    = draft,
                    onChange = { draft = it; vm.setTyping(conversation.id, it.isNotBlank()) },
                    onSend   = {
                        if (pendingMedia.isNotEmpty()) {
                            val toSend = pendingMedia
                            val caption = draft.trim().takeIf { it.isNotBlank() }
                            pendingMedia = emptyList()
                            draft = ""
                            scope.launch {
                                uploading = true
                                vm.sendAttachments(conversation.id, caption, toSend.map { it.attachment }).join()
                                uploading = false
                            }
                        } else if (draft.isNotBlank()) {
                            vm.send(conversation.id, draft.trim()); draft = ""
                        }
                    },
                    onAttach = { showAttachSheet = true },
                    onStickers = { focusManager.clearFocus(); vm.loadStickers(); showStickerSheet = true },
                    hasPendingAttachments = pendingMedia.isNotEmpty(),
                    uploading = uploading,
                    captureMode = captureMode,
                    onToggleCaptureMode = {
                        captureMode = if (captureMode == CaptureMode.AUDIO) CaptureMode.VIDEO else CaptureMode.AUDIO
                    },
                    onHoldStart = {
                        when (captureMode) {
                            CaptureMode.AUDIO -> {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                    == PackageManager.PERMISSION_GRANTED
                                ) {
                                    if (!recorder.start()) vm.error.value = "Couldn't start recording."
                                } else {
                                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            CaptureMode.VIDEO -> {
                                val file = File.createTempFile("klic_vid_", ".mp4", context.cacheDir)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                tempVideoUri = uri
                                videoLauncher.launch(uri)
                            }
                        }
                    },
                    onHoldEnd = {
                        if (captureMode == CaptureMode.AUDIO && recorder.isRecording) {
                            recorder.stop()?.let { (bytes, durationMs, waveform) ->
                                vm.sendVoice(conversation.id, bytes, durationMs, waveform)
                            }
                        }
                    },
                )
            }
        }
        } // Box
    }

    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            AttachSheet(
                onPhotos = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAttachSheet = false }
                    mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onCamera = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAttachSheet = false }
                    val file = File.createTempFile("klic_", ".jpg", context.cacheDir)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                onFile = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAttachSheet = false }
                    fileLauncher.launch(arrayOf("*/*"))
                },
            )
        }
    }

    if (showStickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStickerSheet = false },
            sheetState = stickerSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            StickerPickerSheet(stickers = stickers) { id ->
                vm.sendSticker(conversation.id, id)
                scope.launch { stickerSheetState.hide() }.invokeOnCompletion { showStickerSheet = false }
            }
        }
    }

    // Long-press action menu (reactions + reply/copy/delete).
    menuTarget?.let { target ->
        MessageActionsOverlay(
            message = target,
            isMine = target.senderId == me?.id,
            onReact = { emoji -> vm.react(conversation.id, target.id, emoji); menuTarget = null },
            onReply = { vm.setReplyTo(target); menuTarget = null },
            onCopy = { clipboard.setText(AnnotatedString(target.body)) },
            onDelete = { deleteTarget = target },
            onDismiss = { menuTarget = null },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null; menuTarget = null },
            title = { Text("Delete message") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                Column {
                    if (target.senderId == me?.id) {
                        TextButton(onClick = {
                            vm.deleteForEveryone(conversation.id, target.id); deleteTarget = null; menuTarget = null
                        }) { Text("Delete for everyone", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = {
                        vm.deleteForMe(target); deleteTarget = null; menuTarget = null
                    }) { Text("Delete for me", color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null; menuTarget = null }) { Text("Cancel") }
            },
        )
    }

    viewerUrl?.let { url ->
        ImageViewerOverlay(url = url, onDismiss = { viewerUrl = null })
    }
    } // Box
}

// MARK: - Message bubble

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    replyAuthorName: String = "",
    onCallBack: (String) -> Unit = {},
    onLongPress: () -> Unit = {},
    onReactionTap: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
) {
    if (message.isDeleted) { DeletedBubble(isMine); return }
    if (message.isCallEvent && message.call != null) {
        CallEventBubble(message.call, outgoing = isMine, time = shortTime(message.createdAt), onCallBack = onCallBack)
        return
    }
    if (message.isSticker) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 1.dp)
                .combinedClickable(onClick = {}, onLongClick = onLongPress),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        ) {
            StickerBubble(message, isMine = isMine, time = if (isLast) shortTime(message.createdAt) else null)
            if (message.reactions.isNotEmpty()) ReactionPillsRow(message.reactions, onReactionTap)
        }
        return
    }

    val tailRadius = 4.dp
    val fullRadius = 18.dp
    val shape = RoundedCornerShape(
        topStart     = if (!isMine && isFirst) fullRadius else if (!isMine) 4.dp else fullRadius,
        topEnd       = if (isMine  && isFirst) fullRadius else if (isMine)  4.dp else fullRadius,
        bottomEnd    = if (isMine  && isLast)  tailRadius else fullRadius,
        bottomStart  = if (!isMine && isLast)  tailRadius else fullRadius,
    )

    val voiceAtt = message.attachments.firstOrNull { it.kind == "VOICE" }
    val imageAtt = message.attachments.firstOrNull { it.kind == "IMAGE" }
    val videoAtt = message.attachments.firstOrNull { it.kind == "VIDEO" }

    val time = shortTime(message.createdAt)
    val status = if (isMine) message.status else null

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        when {
            voiceAtt != null ->
                Box(Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)) {
                    VoiceAttachmentView(
                        att = voiceAtt,
                        isMine = isMine,
                        time = time,
                        status = status,
                    )
                }

            imageAtt != null ->
                Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                    message.replyTo?.let { ReplyQuote(it, replyAuthorName) }
                    Box(
                        Modifier
                            .widthIn(max = 240.dp)
                            .heightIn(max = 320.dp)
                            .aspectRatio(imageAspect(imageAtt))
                            .clip(RoundedCornerShape(16.dp))
                            .combinedClickable(
                                onClick = { onImageClick(imageAtt.url) },
                                onLongClick = onLongPress,
                            ),
                    ) {
                        AsyncImage(
                            model = imageAtt.url,
                            contentDescription = "Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Time + ticks pill overlay — bottom-right.
                        MediaTimePill(
                            time = time,
                            status = status,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        )
                    }
                }

            videoAtt != null ->
                Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                    message.replyTo?.let { ReplyQuote(it, replyAuthorName) }
                    Box(
                        Modifier
                            .widthIn(max = 240.dp)
                            .heightIn(max = 320.dp)
                            .aspectRatio(imageAspect(videoAtt))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A1A1A))
                            .combinedClickable(onClick = {}, onLongClick = onLongPress),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play video",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(48.dp).align(Alignment.Center),
                        )
                        // Duration pill — bottom-left.
                        if (videoAtt.durationMs != null) {
                            MediaTimePill(
                                text = durationText(videoAtt.durationMs),
                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                            )
                        }
                        // Time + ticks pill — bottom-right.
                        MediaTimePill(
                            time = time,
                            status = status,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        )
                    }
                }

            else ->
                Box(
                    Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape,
                        )
                        .combinedClickable(onClick = {}, onLongClick = onLongPress)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    val timeColor = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                    Column {
                        message.replyTo?.let { ReplyQuote(it, replyAuthorName, onPrimary = isMine) }
                        // Message body + inline time + ticks, aligned to bottom of last text line.
                        Row(verticalAlignment = Alignment.Bottom) {
                            if (message.body.isNotBlank()) {
                                Text(
                                    message.body,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = timeColor,
                                )
                                if (status != null) {
                                    MessageTicks(status = status, onPrimary = isMine)
                                }
                            }
                        }
                    }
                }
        }

        if (message.reactions.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            ReactionPillsRow(message.reactions, onReactionTap)
        }
    }
}

// Semi-transparent dark pill used as overlay on image/video.
@Composable
private fun MediaTimePill(
    modifier: Modifier = Modifier,
    time: String = "",
    text: String = "",           // for the duration pill on video (no ticks)
    status: String? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val label = text.ifEmpty { time }
        if (label.isNotEmpty()) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
        if (status != null && text.isEmpty()) {
            MessageTicks(status = status, onMedia = true)
        }
    }
}

// Aspect ratio for an inline image/video, clamped so extreme shapes stay reasonable.
private fun imageAspect(att: Attachment): Float {
    val w = att.width; val h = att.height
    return if (w != null && h != null && w > 0 && h > 0) (w.toFloat() / h.toFloat()).coerceIn(0.6f, 1.6f) else 1f
}

// Compact preview text for the composer's reply bar.
private fun messagePreview(m: Message): String = when {
    m.body.isNotBlank() -> m.body
    m.isSticker -> "Sticker"
    m.attachments.firstOrNull()?.kind == "IMAGE" -> "Photo"
    m.attachments.firstOrNull()?.kind == "VOICE" -> "Voice message"
    m.attachments.firstOrNull()?.kind == "VIDEO" -> "Video"
    m.attachments.isNotEmpty() -> "File"
    else -> "Message"
}

// Presence subtitle for the chat header: "Online" or "last seen …".
private fun presenceSubtitle(presence: com.klicmobile.app.realtime.SocketService.Presence?): String? {
    if (presence == null) return null
    if (presence.online) return "Online"
    val ms = presence.lastSeenMs ?: return null
    val date = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val time = DateTimeFormatter.ofPattern("HH:mm").format(date)
    return when (date.toLocalDate()) {
        LocalDate.now() -> "last seen $time"
        LocalDate.now().minusDays(1) -> "last seen yesterday"
        else -> "last seen ${DateTimeFormatter.ofPattern("MMM d").format(date)}"
    }
}

// MARK: - Date separator

@Composable
private fun DateSeparator(isoDate: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                dateLabel(isoDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

// MARK: - Helpers

private fun sameDay(a: String, b: String): Boolean = a.take(10) == b.take(10)

private fun shortTime(iso: String): String = runCatching {
    val instant = Instant.parse(iso)
    DateTimeFormatter.ofPattern("h:mm a").format(instant.atZone(ZoneId.systemDefault()))
}.getOrDefault("")

private fun dateLabel(iso: String): String = runCatching {
    val date = Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    when (date) {
        today            -> "Today"
        today.minusDays(1) -> "Yesterday"
        else             -> DateTimeFormatter.ofPattern("MMMM d").format(date)
    }
}.getOrDefault("")

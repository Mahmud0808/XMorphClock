package com.drdisagree.xmorphclock.ui.home

import android.graphics.RectF
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.editor.ItemGroups
import com.drdisagree.xmorphclock.domain.model.EditScope
import com.drdisagree.xmorphclock.domain.model.ElementKind
import com.drdisagree.xmorphclock.domain.model.FaceGrid
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.domain.model.ItemState
import com.drdisagree.xmorphclock.domain.model.ModuleStatus
import com.drdisagree.xmorphclock.engine.FaceColors
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.BackdropScreen
import com.drdisagree.xmorphclock.ui.components.ClockFacePreview
import com.drdisagree.xmorphclock.ui.components.FitText
import com.drdisagree.xmorphclock.ui.components.GlassContainer
import com.drdisagree.xmorphclock.ui.components.GlassDialog
import com.drdisagree.xmorphclock.ui.components.GlassIconButton
import com.drdisagree.xmorphclock.ui.components.GlassMenu
import com.drdisagree.xmorphclock.ui.components.GlassMenuItem
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.LocalClockGeometry
import com.drdisagree.xmorphclock.ui.components.LocalFaceData
import com.drdisagree.xmorphclock.ui.components.LocalSampleFaceData
import com.drdisagree.xmorphclock.ui.components.LockscreenPreview
import com.drdisagree.xmorphclock.ui.components.StatusDot
import com.drdisagree.xmorphclock.ui.components.StatusPill
import com.drdisagree.xmorphclock.ui.components.StatusTone
import com.drdisagree.xmorphclock.ui.components.fadingEdges
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTab
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTabs
import com.drdisagree.xmorphclock.ui.editor.ColorPickerDialog
import com.drdisagree.xmorphclock.ui.editor.ColorTarget
import com.drdisagree.xmorphclock.ui.editor.EditorCanvas
import com.drdisagree.xmorphclock.ui.editor.EditorPanel
import com.drdisagree.xmorphclock.ui.editor.EditorScopeTabs
import com.drdisagree.xmorphclock.ui.editor.EditorTopBar
import com.drdisagree.xmorphclock.ui.editor.EditorUiState
import com.drdisagree.xmorphclock.ui.editor.EditorViewModel
import com.drdisagree.xmorphclock.ui.editor.FontPickerDialog
import com.drdisagree.xmorphclock.ui.editor.GroupingBar
import com.drdisagree.xmorphclock.ui.editor.IconPickerDialog
import com.drdisagree.xmorphclock.ui.editor.elementLabel
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.DarkPalette
import com.drdisagree.xmorphclock.ui.theme.LocalPalette
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val SheetShape = RoundedCornerShape(32.dp)
private val THUMB_WIDTH = 84.dp
private val SHEET_MARGIN = 12.dp
private val PREVIEW_SIDE_GAP = 16.dp
private val SHEET_PADDING = 20.dp
private val LABEL_GAP = 8.dp
private val HEADER_HEIGHT = 36.dp
private val GRABBER_WIDTH = 36.dp
private val GRABBER_HEIGHT = 5.dp
private const val GRABBER_ALPHA = 0.3f
private const val JUMP_ITEMS = 10
private val HEADER_NUDGE = 6.dp
private const val LEAD_ITEMS = 4
private val GRABBER_TOP = 12.dp
private val GRABBER_AREA = 28.dp
private val LABEL_LINE = 16.sp
private const val EDITOR_FRACTION = 0.45f
private const val MORPH_MS = 560
private val FULL_BOTTOM = 84.dp
private val DRAG_THRESHOLD = 32.dp
private val GRID_FADE = 22.dp
private val CHIPS_HEIGHT = 32.dp
private val CHIPS_GAP = 14.dp
private const val HEADER_KEY = "header:"
private const val FACES_EXPANDED_FRACTION = 0.72f
private val TABS_INSET = 8.dp
private val THUMB_RADIUS = 16.dp
private val BLOCK_PADDING = 8.dp
private val CLOCK_PADDING = 10.dp
private val THUMB_RING = 2.dp
internal val ThumbInk = Color(0xFF1F2227)
private val TABS_BOTTOM = 18.dp
private val BADGE_TOP = 10.dp
private val MENU_TOP = 60.dp
private val MENU_END = 12.dp
private val BADGE_INSET = 10.dp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    editor: EditorViewModel,
    onOpenStatus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val defaultText = stringResource(R.string.default_text)
    val elementNames = ElementKind.entries.associateWith { stringResource(elementLabel(it)) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editorState by editor.uiState.collectAsStateWithLifecycle()
    val styleStatus by viewModel.styleStatus.collectAsStateWithLifecycle()
    val editing = state.editingStyle != null && editorState.face != null && editorState.style == state.editingStyle
    val aspect = LocalClockGeometry.current.aspect
    val screenHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val density = LocalDensity.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val labelHeight = with(density) { LABEL_LINE.toDp() }
    val thumbHeight = THUMB_WIDTH / aspect
    val rowHeight = thumbHeight + LABEL_GAP + labelHeight
    val facesHeight = GRABBER_AREA + TITLE_HEIGHT + HEADER_HEIGHT + TABS_GAP + CHIPS_GAP + CHIPS_HEIGHT + GRID_FADE * 2 + rowHeight
    val editorHeight = screenHeight * EDITOR_FRACTION - navBottom - SHEET_MARGIN
    val morph by animateFloatAsState(
        if (editing) 1f else 0f,
        tween(MORPH_MS, easing = Emphasized),
        label = "morph"
    )
    var facesExpanded by rememberSaveable { mutableStateOf(false) }
    val expand by animateFloatAsState(
        if (facesExpanded && !editing) 1f else 0f,
        tween(MORPH_MS, easing = Emphasized),
        label = "expand"
    )
    val facesExpandedHeight = screenHeight * FACES_EXPANDED_FRACTION - navBottom - SHEET_MARGIN
    val imeBottom = WindowInsets.ime.getBottom(density)
    val lift = with(density) { (imeBottom - navBottom.roundToPx()).coerceAtLeast(0).toDp() }
    val keyboard by animateFloatAsState(if (imeBottom > 0 && editing) 1f else 0f, tween(KEYBOARD_MS, easing = Emphasized), label = "keyboard")
    val editSheet = lerp(editorHeight, minOf(editorHeight, KEYBOARD_SHEET), keyboard)
    val sheetHeight = lerp(lerp(facesHeight, facesExpandedHeight, expand), editSheet, morph)
    val reveal by animateFloatAsState(if (state.preview == null) 0f else 1f, tween(320), label = "reveal")
    var playToken by remember { mutableIntStateOf(0) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var guides by rememberSaveable { mutableStateOf(true) }
    var liveInfo by rememberSaveable { mutableStateOf(false) }
    var pickingColor by rememberSaveable { mutableStateOf<ColorTarget?>(null) }
    var colorTarget by rememberSaveable { mutableStateOf(ColorTarget.FILL) }
    var pickingIcon by rememberSaveable { mutableStateOf(false) }
    var pickingFont by rememberSaveable { mutableStateOf(false) }
    val userFonts by editor.userFonts.collectAsStateWithLifecycle()
    val fontFailed = stringResource(R.string.font_add_failed)
    val fontContext = LocalContext.current
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            pickingFont = false
            editor.addFont(it.toString()) { Toast.makeText(fontContext, fontFailed, Toast.LENGTH_SHORT).show() }
        }
    }
    var addingElement by rememberSaveable { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onAddImage(it.toString(), elementNames.getValue(ElementKind.IMAGE)) }
    }
    var actionsFor by remember { mutableStateOf<FacePreset?>(null) }
    var naming by remember { mutableStateOf<FacePreset?>(null) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var savingStyle by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val liveWallpaper by viewModel.liveWallpaper.collectAsStateWithLifecycle()
    val full by animateFloatAsState(
        if (fullscreen) 1f else 0f,
        tween(MORPH_MS, easing = Emphasized),
        label = "full"
    )

    val currentState by rememberUpdatedState(state)

    fun openEditor() {
        currentState.clockPreset?.let { preset ->
            editor.open(viewModel.editTarget())
            viewModel.onEdit(preset.style)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.editEvents.collect {
            fullscreen = false
            openEditor()
        }
    }

    fun closeEditor() {
        fullscreen = false
        viewModel.onEditDone()
    }

    BackHandler(enabled = editing) {
        if (fullscreen) fullscreen = false else closeEditor()
    }

    BackHandler(enabled = facesExpanded && !editing) { facesExpanded = false }

    BackHandler(enabled = fullscreen && !editing) { fullscreen = false }

    var previewHeight by remember { mutableStateOf(0.dp) }
    var previewWidth by remember { mutableStateOf(0.dp) }
    val fitScale = if (previewHeight > 0.dp && previewWidth > 0.dp) ((previewWidth - PREVIEW_SIDE_GAP * 2) / (previewHeight * aspect)).coerceIn(0.2f, 1f) else 1f
    val reserved = FULL_BOTTOM - 16.dp
    val homeScale = if (previewHeight > 0.dp) ((previewHeight - (SHEET_MARGIN + facesHeight - reserved)) / previewHeight).coerceIn(0.2f, 1f) else 1f
    val editScale = if (previewHeight > 0.dp) ((previewHeight - (SHEET_MARGIN + editSheet + lift - reserved)) / previewHeight).coerceIn(0.2f, 1f) else 1f
    val previewScale = {
        val sheetScale = homeScale + (editScale - homeScale) * morph
        minOf(sheetScale + (1f - sheetScale) * full, fitScale)
    }

    BackdropScreen(
        content = {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 68.dp, bottom = navBottom + FULL_BOTTOM)
                    .graphicsLayer { alpha = reveal },
                contentAlignment = Alignment.TopCenter
            ) {
                val available = maxHeight
                val availableWidth = maxWidth
                SideEffect {
                    previewHeight = available
                    previewWidth = availableWidth
                }
                Crossfade(
                    targetState = editing,
                    animationSpec = tween(320),
                    modifier = Modifier
                        .requiredSize(available * aspect * fitScale, available * fitScale)
                        .graphicsLayer {
                            val scale = previewScale() / fitScale
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        },
                    label = "preview"
                ) { edit ->
                    if (edit) {
                        EditPreview(editorState, editor, playToken, guides)
                    } else {
                        HomePreview(state, viewModel)
                    }
                }
            }
        },
        overlay = {
            AnimatedContent(
                targetState = Pair(editorState.grouping != null, editing),
                transitionSpec = {
                    (fadeIn(tween(260, delayMillis = 80)) + slideInVertically(tween(MORPH_MS, easing = Emphasized)) { -it / 2 }) togetherWith
                            (fadeOut(tween(160)) + slideOutVertically(tween(MORPH_MS, easing = Emphasized)) { -it / 2 })
                },
                label = "topBar"
            ) { (_, edit) ->
                if (edit && editorState.grouping != null) {
                    val groupName = stringResource(R.string.group_name)
                    GroupingBar(
                        count = editorState.grouping?.size ?: 0,
                        onCancel = editor::cancelGrouping,
                        onGroup = { editor.confirmGrouping(groupName) }
                    )
                } else if (edit) {
                    EditorTopBar(
                        canUndo = editorState.canUndo,
                        canRedo = editorState.canRedo,
                        fullscreen = fullscreen,
                        guides = guides,
                        onCancel = ::closeEditor,
                        onUndo = editor::undo,
                        onRedo = editor::redo,
                        onSave = { editor.save(::closeEditor) },
                        onToggleFullscreen = { fullscreen = !fullscreen },
                        onToggleGuides = { guides = !guides }
                    )
                } else {
                    TopBar(state.moduleStatus, fullscreen, styleStatus.changed, { fullscreen = !fullscreen }, onOpenStatus) { menuOpen = true }
                }
            }
            if (morph < 1f && previewHeight > 0.dp) {
                CompositionLocalProvider(LocalPalette provides DarkPalette) {
                    PreviewModeTabs(
                        mode = state.previewMode,
                        onModeChange = viewModel::onPreviewModeChange,
                        height = lerp(MODE_TABS_HEIGHT, MODE_TABS_FULL_HEIGHT, full),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 68.dp)
                            .layout { measurable, _ ->
                                val scale = previewScale()
                                val height = previewHeight.toPx() * scale
                                val width = (height * aspect - TABS_INSET.toPx() * 2f).roundToInt().coerceIn(0, TABS_MAX_WIDTH.roundToPx())
                                val placeable = measurable.measure(Constraints.fixedWidth(width))
                                val y = (height - TABS_BOTTOM.toPx() - placeable.height).roundToInt().coerceAtLeast(0)
                                layout(placeable.width, y + placeable.height) { placeable.place(0, y) }
                            }
                            .graphicsLayer { alpha = reveal * (1f - morph) * (1f - expand) }
                    )
                }
            }
            AnimatedVisibility(
                visible = liveWallpaper && !editing && !facesExpanded && previewHeight > 0.dp,
                enter = fadeIn(tween(260)) + scaleIn(tween(320, easing = Emphasized), initialScale = 0.85f),
                exit = fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 68.dp + BADGE_TOP)
                    .offset {
                        val half = previewHeight.toPx() * aspect * previewScale() / 2f
                        IntOffset((half - BADGE_INSET.toPx() - LIVE_BADGE_SIZE.toPx() / 2f).roundToInt(), 0)
                    }
            ) {
                CompositionLocalProvider(LocalPalette provides DarkPalette) {
                    LiveWallpaperBadge(onClick = { liveInfo = true })
                }
            }
            GlassContainer(
                shape = SheetShape,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = SHEET_MARGIN)
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .offset { IntOffset(0, -lift.roundToPx()) }
                    .graphicsLayer {
                        alpha = reveal * (1f - full)
                        translationY = full * (sheetHeight + navBottom + SHEET_MARGIN).toPx()
                    }
            ) {
                AnimatedContent(
                    targetState = editing,
                    transitionSpec = {
                        fadeIn(tween(260, delayMillis = 160)) togetherWith fadeOut(tween(140))
                    },
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                    label = "sheetContent"
                ) { edit ->
                    if (edit) {
                        EditorPanel(
                            state = editorState,
                            viewModel = editor,
                            onPlayChange = { playToken++ },
                            onPickColor = {
                                colorTarget = it
                                pickingColor = it
                            },
                            onPickIcon = { pickingIcon = true },
                            onPickFont = { pickingFont = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        FacesContent(
                            state = state,
                            aspect = aspect,
                            expanded = facesExpanded,
                            onExpandedChange = { facesExpanded = it },
                            onTabChange = viewModel::onTabChange,
                            onFaceSelected = viewModel::onFaceSelected,
                            onAdd = { addingElement = true },
                            onLongPress = { actionsFor = it },
                            onFullscreen = { fullscreen = true },
                            onEdit = ::openEditor
                        )
                    }
                }
            }
            if (full > 0f && editing) {
                EditorScopeTabs(
                    scope = editorState.scope,
                    onScopeChange = editor::setScope,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = full
                            translationY = (1f - full) * 48.dp.toPx()
                        }
                )
            }
            AddElementDialog(
                visible = addingElement && !editing,
                onAdd = { kind ->
                    addingElement = false
                    if (kind == ElementKind.IMAGE) {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        viewModel.onAddElement(kind, elementNames.getValue(kind), defaultText)
                    }
                },
                onDismiss = { addingElement = false }
            )
            val iconLayer = editorState.primary?.let { id -> editorState.face?.layers?.firstOrNull { it.id == id } }
            IconPickerDialog(
                visible = pickingIcon && editing && iconLayer != null,
                font = iconLayer?.font.orEmpty(),
                selected = iconLayer?.format,
                onPick = { glyph ->
                    iconLayer?.let { layer -> editor.updateLayer(layer.id, "icon") { it.copy(format = glyph) } }
                    pickingIcon = false
                },
                onDismiss = { pickingIcon = false }
            )
            FontPickerDialog(
                visible = pickingFont && editing,
                selected = editorState.primary?.let { id -> editorState.face?.layers?.firstOrNull { it.id == id } }?.font,
                userFonts = userFonts,
                onPick = { family ->
                    editor.setFont(family)
                    pickingFont = false
                },
                onAdd = { fontPicker.launch(FONT_TYPES) },
                onDismiss = { pickingFont = false }
            )
            ColorPickerDialog(
                visible = pickingColor != null && editing,
                initial = if (colorTarget == ColorTarget.BORDER) editor.borderColor() else editor.color(),
                resolve = { FaceColors.resolve(context, it) },
                onApply = { hex ->
                    if (colorTarget == ColorTarget.BORDER) editor.setBorder(hex) else editor.setColor(hex)
                    pickingColor = null
                },
                onDismiss = { pickingColor = null },
                allowGradient = colorTarget != ColorTarget.BORDER
            )
            FaceActionsDialog(
                preset = actionsFor,
                onDuplicate = { naming = it },
                onDelete = viewModel::onDelete,
                onDismiss = { actionsFor = null }
            )
            SavePresetDialog(
                initialName = naming?.let { stringResource(R.string.face_copy_name, it.face.name) },
                onSave = { name -> naming?.let { viewModel.onDuplicate(it, name) } },
                onDismiss = { naming = null }
            )
            val styleName = state.clockPreset?.face?.name ?: stringResource(R.string.menu_style_name)
            GlassMenu(visible = menuOpen && !editing, onDismiss = { menuOpen = false }, top = MENU_TOP, end = MENU_END) {
                val updatable = styleStatus.presetStyle
                if (styleStatus.changed && updatable != null) {
                    GlassMenuItem(
                        icon = R.drawable.ic_save,
                        label = stringResource(R.string.menu_update_preset),
                        badge = true,
                        onClick = {
                            menuOpen = false
                            viewModel.updatePreset(updatable)
                        }
                    )
                }
                if (styleStatus.changed) {
                    GlassMenuItem(
                        icon = R.drawable.ic_plus,
                        label = stringResource(R.string.menu_save_preset),
                        badge = updatable == null,
                        onClick = {
                            menuOpen = false
                            savingStyle = styleName
                        }
                    )
                }
                GlassMenuItem(R.drawable.ic_settings, stringResource(R.string.tab_settings), {
                    menuOpen = false
                    onOpenSettings()
                })
                GlassMenuItem(R.drawable.ic_info, stringResource(R.string.action_about), {
                    menuOpen = false
                    onOpenAbout()
                })
            }
            SavePresetDialog(
                initialName = savingStyle,
                onSave = { name -> viewModel.saveAsPreset(name) },
                onDismiss = { savingStyle = null }
            )
            GlassDialog(
                visible = liveInfo,
                title = stringResource(R.string.live_wallpaper_title),
                message = stringResource(R.string.live_wallpaper_message),
                confirmLabel = stringResource(R.string.action_got_it),
                onDismiss = { liveInfo = false }
            )
        }
    )
}

@Composable
private fun HomePreview(state: HomeUiState, viewModel: HomeViewModel) {
    val geometry = LocalClockGeometry.current
    val spec = tween<Float>(durationMillis = 700, easing = Emphasized)
    var aodLarge by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(state.previewMode) {
        if (state.previewMode != PreviewMode.AOD) aodLarge = state.previewMode == PreviewMode.LARGE
    }
    val showLarge = if (state.previewMode == PreviewMode.AOD) aodLarge else state.previewMode == PreviewMode.LARGE
    val itemState = when {
        state.previewMode == PreviewMode.AOD && showLarge -> ItemState.AOD_LARGE
        state.previewMode == PreviewMode.AOD -> ItemState.AOD_SMALL
        showLarge -> ItemState.LARGE
        else -> ItemState.SMALL
    }
    val large by animateFloatAsState(if (showLarge) 1f else 0f, spec, label = "large")
    val sleep by animateFloatAsState(if (state.previewMode == PreviewMode.AOD) 1f else 0f, spec, label = "sleep")
    var extents by remember { mutableStateOf(emptyMap<String, RectF>()) }
    LockscreenPreview(
        face = state.preview,
        modifier = Modifier.fillMaxSize(),
        largeProgress = { large },
        sleepProgress = { sleep },
        onExtents = { extents = it },
        overlay = {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val frame = geometry.frame(large)
                ItemOverlay(
                    extents = extents,
                    composition = state.composition,
                    selected = state.selectedItem,
                    focused = state.focusedLayer,
                    unit = constraints.maxWidth * frame.width,
                    frameTop = constraints.maxHeight * frame.top,
                    onSelect = viewModel::onItemSelected,
                    onFocus = viewModel::onLayerFocused,
                    frameLeft = constraints.maxWidth * frame.left,
                    height = constraints.maxHeight.toFloat(),
                    onMeasured = { bounds, maxY -> viewModel.onItemsMeasured(itemState, bounds, maxY) },
                    singles = state.composition.items.filter { item ->
                        !item.clock && state.faceOf(item.style)?.let { it.category == CustomFaces.ELEMENT_CATEGORY && it.layers.size == 1 } == true
                    }.mapTo(HashSet()) { it.id }
                )
            }
        }
    )
}

@Composable
private fun PreviewModeTabs(mode: PreviewMode, onModeChange: (PreviewMode) -> Unit, height: Dp, modifier: Modifier = Modifier) {
    LiquidTabs(
        selectedIndex = mode.ordinal,
        onSelected = { onModeChange(PreviewMode.entries[it]) },
        backdrop = LocalBackdrop.current,
        tabsCount = PreviewMode.entries.size,
        height = height,
        adaptive = true,
        modifier = modifier
    ) {
        PreviewMode.entries.forEach { entry ->
            LiquidTab(onClick = { onModeChange(entry) }) {
                FitText(
                    stringResource(entry.label),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EditPreview(state: EditorUiState, editor: EditorViewModel, playToken: Int, guides: Boolean) {
    val face = state.face ?: return
    val spec = tween<Float>(durationMillis = 700, easing = Emphasized)
    val showLarge = if (state.scope == EditScope.AOD) state.aodLarge else state.scope == EditScope.LARGE
    val large by animateFloatAsState(if (showLarge) 1f else 0f, spec, label = "large")
    val aod by animateFloatAsState(if (state.scope == EditScope.AOD) 1f else 0f, spec, label = "aod")
    EditorCanvas(
        face = face,
        selectedId = state.selectedId,
        large = large,
        sleep = aod,
        playToken = playToken,
        onSelect = editor::select,
        onMove = editor::move,
        onResizeStart = editor::axes,
        onResize = editor::resize,
        onTransformStart = editor::transformBase,
        onTransform = editor::transform,
        onGestureEnd = editor::endGesture,
        modifier = Modifier.fillMaxSize(),
        showGuides = guides,
        grouping = state.grouping?.mapTo(HashSet()) { ItemGroups.keyOfItem(it, false) },
        onToggleGrouping = editor::toggleGrouping,
        canRemove = editor::canRemoveTarget,
        onRemove = editor::removeTarget,
        alignOf = editor::alignOf,
        onAlign = editor::alignTarget
    )
}

@Composable
private fun TopBar(
    status: ModuleStatus,
    fullscreen: Boolean,
    unsaved: Boolean,
    onToggleFullscreen: () -> Unit,
    onOpenStatus: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Row(
        Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = OnInk,
            modifier = Modifier.weight(1f)
        )
        val active = status == ModuleStatus.ACTIVE
        StatusPill(
            text = stringResource(if (active) R.string.module_active else R.string.module_inactive),
            tone = if (active) StatusTone.OK else StatusTone.WARNING,
            modifier = Modifier.clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onOpenStatus)
        )
        AnimatedVisibility(
            visible = fullscreen,
            enter = fadeIn(tween(220)) + scaleIn(tween(260, easing = Emphasized), initialScale = 0.8f),
            exit = fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.8f)
        ) {
            GlassIconButton(
                R.drawable.ic_fullscreen_exit,
                onClick = onToggleFullscreen,
                description = stringResource(R.string.action_exit_fullscreen)
            )
        }
        val dot by animateFloatAsState(if (unsaved) 1f else 0f, tween(260, easing = Emphasized), label = "unsavedDot")
        Box {
            GlassIconButton(R.drawable.ic_more, onOpenMenu, description = stringResource(R.string.action_more))
            StatusDot(
                Accent,
                size = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .graphicsLayer {
                        alpha = dot
                        scaleX = dot
                        scaleY = dot
                    }
            )
        }
    }
}

@Composable
private fun FacesContent(
    state: HomeUiState,
    aspect: Float,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTabChange: (FaceTab) -> Unit,
    onFaceSelected: (Int) -> Unit,
    onAdd: () -> Unit,
    onLongPress: (FacePreset) -> Unit,
    onEdit: () -> Unit,
    onFullscreen: () -> Unit
) {
    val density = LocalDensity.current
    val threshold = with(density) { DRAG_THRESHOLD.toPx() }
    val canEdit = state.clockPreset != null
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .pointerInput(expanded) {
                    var travel = 0f
                    detectVerticalDragGestures(
                        onDragStart = { travel = 0f },
                        onDragEnd = {
                            if (travel < -threshold) onExpandedChange(true)
                            if (travel > threshold) onExpandedChange(false)
                        }
                    ) { change, amount ->
                        change.consume()
                        travel += amount
                    }
                }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(GRABBER_AREA)
                    .clickable(interactionSource = null, indication = null, role = Role.Button) { onExpandedChange(!expanded) },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    Modifier
                        .padding(top = GRABBER_TOP)
                        .size(GRABBER_WIDTH, GRABBER_HEIGHT)
                        .clip(CircleShape)
                        .background(OnInk.copy(alpha = GRABBER_ALPHA))
                )
            }
            Row(
                Modifier
                    .padding(start = 20.dp, end = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = state.tab,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.height(TITLE_HEIGHT),
                        label = "tabTitle"
                    ) { tab ->
                        Text(
                            stringResource(tabLabel(tab)),
                            style = MaterialTheme.typography.titleSmall,
                            color = OnInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FaceTabs(
                        selected = state.tab,
                        onSelect = onTabChange,
                        height = HEADER_HEIGHT,
                        modifier = Modifier
                            .padding(top = TABS_GAP)
                            .offset(x = (-4).dp)
                            .width(KIND_TABS_WIDTH)
                    )
                }
                PreviewActions(
                    height = HEADER_HEIGHT,
                    canEdit = canEdit,
                    onAdd = onAdd,
                    onFullscreen = onFullscreen,
                    onEdit = onEdit
                )
            }
        }
        AnimatedContent(
            targetState = state.tab,
            transitionSpec = { fadeIn(tween(240, delayMillis = 60)) togetherWith fadeOut(tween(140)) },
            modifier = Modifier.weight(1f),
            label = "tabGrid"
        ) { tab ->
            FaceGrid(
                tab = tab,
                faces = state.facesOf(tab).filterNot { it.face.category == CustomFaces.ELEMENT_CATEGORY },
                checked = state::isChecked,
                onSelect = onFaceSelected,
                onLongPress = onLongPress.takeIf { tab != FaceTab.WIDGETS }
            )
        }
    }
}

@Composable
private fun FaceGrid(
    tab: FaceTab,
    faces: List<FacePreset>,
    checked: (FacePreset) -> Boolean,
    onSelect: (Int) -> Unit,
    onLongPress: ((FacePreset) -> Unit)?
) {
    CompositionLocalProvider(LocalFaceData provides LocalSampleFaceData.current) {
        val density = LocalDensity.current
        val block = tab == FaceTab.WIDGETS
        val sectioned = tab != FaceTab.MINE
        val grid = rememberLazyGridState()
        val scope = rememberCoroutineScope()
        val sections = remember(faces) { sectionsOf(faces) }
        val currentSections by rememberUpdatedState(sections)
        var scrolled by remember { mutableStateOf(false) }
        val appliedIndex = sections.firstNotNullOfOrNull { section ->
            section.faces.indexOfFirst(checked)
                .takeIf { it >= 0 }
                ?.let { section.headerIndex + 1 + it }
        } ?: -1
        LaunchedEffect(appliedIndex) {
            if (!scrolled && appliedIndex >= 0) {
                grid.scrollToItem(appliedIndex)
                scrolled = true
            }
        }
        val activeSection by remember {
            derivedStateOf {
                val sections = currentSections
                val coverage = FloatArray(sections.size)
                val rows = HashSet<Long>()
                val info = grid.layoutInfo
                val start = info.viewportStartOffset
                val end = info.viewportEndOffset
                info.visibleItemsInfo.forEach { item ->
                    val section = sections.indexOfLast { it.headerIndex <= item.index }
                    if (section < 0 || !rows.add(section.toLong() shl ROW_SHIFT or item.offset.y.toLong().and(ROW_MASK))) return@forEach
                    val top = item.offset.y.coerceAtLeast(start)
                    val bottom = (item.offset.y + item.size.height).coerceAtMost(end)
                    if (bottom > top) coverage[section] += (bottom - top).toFloat()
                }
                coverage.indices.maxByOrNull { coverage[it] }?.takeIf { coverage[it] > 0f } ?: 0
            }
        }
        var pendingSection by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(grid) {
            grid.interactionSource.interactions.collect { interaction ->
                if (interaction is DragInteraction.Start) pendingSection = null
            }
        }
        if (faces.isEmpty()) {
            MineEmptyState()
            return@CompositionLocalProvider
        }
        Column(Modifier.fillMaxSize()) {
            if (sectioned) {
                val chipSections = sections.indices.toList()
                CategoryChips(
                    labels = chipSections.map { categoryLabel(sections[it].category) },
                    selected = chipSections.indexOf(pendingSection ?: activeSection),
                    onSelect = { chip ->
                        val index = chipSections.getOrNull(chip) ?: return@CategoryChips
                        val section = sections.getOrNull(index) ?: return@CategoryChips
                        pendingSection = index
                        val target = section.headerIndex
                        scope.launch {
                            val first = grid.firstVisibleItemIndex
                            if (abs(target - first) > JUMP_ITEMS) {
                                grid.scrollToItem(if (target > first) target - LEAD_ITEMS else target + LEAD_ITEMS)
                            }
                            grid.animateScrollToItem(target, with(density) { HEADER_NUDGE.roundToPx() })
                        }
                    },
                    modifier = Modifier
                        .padding(top = CHIPS_GAP)
                        .fillMaxWidth()
                        .height(CHIPS_HEIGHT)
                )
            }
            LazyVerticalGrid(
                columns = if (block) GridCells.Fixed(FaceGrid.ROW_CELLS) else GridCells.Adaptive(THUMB_WIDTH),
                state = grid,
                modifier = Modifier
                    .weight(1f)
                    .fadingEdges(grid, Orientation.Vertical, GRID_FADE),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 20.dp, top = GRID_FADE, end = 20.dp, bottom = GRID_FADE)
            ) {
                sections.forEach { section ->
                    if (sectioned) item(key = HEADER_KEY + section.category, span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            categoryLabel(section.category),
                            style = MaterialTheme.typography.labelLarge,
                            color = OnInkMuted,
                            modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                        )
                    }
                    items(
                        section.faces,
                        key = { it.style },
                        span = { preset -> GridItemSpan(if (block) preset.face.span.coerceIn(1, maxLineSpan) else 1) }
                    ) { preset ->
                        FaceThumb(
                            preset = preset,
                            block = block,
                            selected = checked(preset),
                            onClick = { onSelect(preset.style) },
                            onLongClick = onLongPress?.let { { it(preset) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FaceThumb(
    preset: FacePreset,
    block: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val labelColor by animateColorAsState(if (selected) OnInk else OnInkMuted, label = "label")
    val ring by animateColorAsState(if (selected) Accent else Color.Transparent, label = "thumbRing")
    val shape = RoundedCornerShape(THUMB_RADIUS)
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = null,
                indication = null,
                role = Role.RadioButton,
                onLongClick = onLongClick,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LABEL_GAP)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(ThumbInk)
                .border(THUMB_RING, ring, shape)
                .padding(if (block) BLOCK_PADDING else CLOCK_PADDING),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalPalette provides DarkPalette) {
                if (block) {
                    BoxWithConstraints(Modifier.fillMaxWidth().clipToBounds(), contentAlignment = Alignment.TopCenter) {
                        ClockFacePreview(
                            face = preset.face,
                            modifier = Modifier.requiredWidth(maxWidth / FaceGrid.width(preset.face.span.coerceIn(1, FaceGrid.ROW_CELLS))),
                            fit = false
                        )
                    }
                } else {
                    ClockFacePreview(face = preset.face, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        if (!block) {
            Text(
                preset.face.name,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SelectedBadge(visible: Boolean, modifier: Modifier = Modifier) {
    val accent = Accent
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(260, easing = Emphasized), initialScale = 0.4f),
        exit = fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.4f),
        modifier = modifier
    ) {
        Box(
            Modifier
                .size(20.dp)
                .drawBehind { drawCircle(accent) },
            contentAlignment = Alignment.Center
        ) {
            AppIcon(R.drawable.ic_check, tint = Color.White, size = 12.dp)
        }
    }
}
private val KEYBOARD_SHEET = 280.dp
private const val KEYBOARD_MS = 280
private val MODE_TABS_HEIGHT = 32.dp
private val MODE_TABS_FULL_HEIGHT = 44.dp
private val TABS_GAP = 6.dp
private val TITLE_HEIGHT = 24.dp
private val KIND_TABS_WIDTH = 156.dp
private const val ROW_SHIFT = 32
private const val ROW_MASK = 0xFFFFFFFFL
private val TABS_MAX_WIDTH = 260.dp

private val FONT_TYPES = arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "application/font-sfnt", "application/vnd.ms-opentype", "application/octet-stream")

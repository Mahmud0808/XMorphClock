package com.drdisagree.xmorphclock.ui.editor

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drdisagree.xmorphclock.di.AppContainer

fun editorViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { EditorViewModel(container.getCompositionDraft, container.saveComposition, container.importImage, container.importFont, container.observeUserFonts, container.groupItems, container.ungroupItem, container.removeFromGroup) }
}

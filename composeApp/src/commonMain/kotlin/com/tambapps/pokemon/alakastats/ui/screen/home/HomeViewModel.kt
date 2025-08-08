package com.tambapps.pokemon.alakastats.ui.screen.home

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics

class HomeViewModel : ScreenModel {
    
    val teamlyticsList: SnapshotStateList<Teamlytics> = mutableStateListOf()

}
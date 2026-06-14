package com.mdp.badmintonadmin.ui.player_mgmt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mdp.badmintonadmin.data.local.AppDatabase
import com.mdp.badmintonadmin.data.local.entity.PlayerEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RosterUiState(
    val players: List<PlayerEntity> = emptyList(),
    val searchQuery: String = ""
)

class RosterViewModel(application: Application) : AndroidViewModel(application) {
    private val playerDao = AppDatabase.getDatabase(application).playerDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val uiState: StateFlow<RosterUiState> = combine(
        playerDao.getAllPermanentPlayers(),
        _searchQuery
    ) { players, query ->
        val filteredPlayers = if (query.isBlank()) {
            players
        } else {
            players.filter { it.name.contains(query, ignoreCase = true) }
        }
        RosterUiState(players = filteredPlayers, searchQuery = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RosterUiState())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addPlayer(name: String, tier: String, gender: String) {
        viewModelScope.launch {
            val newPlayer = PlayerEntity(
                name = name.trim(),
                baseTier = tier.uppercase(),
                gender = gender.uppercase()
            )
            playerDao.insertPlayer(newPlayer)
        }
    }

    fun updatePlayer(player: PlayerEntity, name: String, tier: String, gender: String) {
        viewModelScope.launch {
            val updatedPlayer = player.copy(
                name = name.trim(),
                baseTier = tier.uppercase(),
                gender = gender.uppercase()
            )
            playerDao.updatePlayer(updatedPlayer)
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            playerDao.deletePlayer(player)
        }
    }
}

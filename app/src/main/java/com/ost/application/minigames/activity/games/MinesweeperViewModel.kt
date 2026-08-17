package com.ost.application.minigames.activity.games
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.random.Random
data class Cell(
    val x: Int,
    val y: Int,
    val isMine: Boolean = false,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val hasQuestion: Boolean = false,
    val adjacentMines: Int = 0,
    val isExploding: Boolean = false
)
enum class GameStatus {
    IDLE, PLAYING, LOST_ANIMATING, WON, LOST
}
enum class InteractionMode {
    TOUCH, FLAG, QUESTION
}
class MinesweeperViewModel : ViewModel() {
    private val width = 10
    private val height = 10
    private val totalMines = 15
    var grid by mutableStateOf<List<List<Cell>>>(emptyList())
        private set
    var gameStatus by mutableStateOf(GameStatus.IDLE)
        private set
    var minesLeft by mutableStateOf(totalMines)
        private set
    var timeElapsed by mutableStateOf(0)
        private set
    var interactionMode by mutableStateOf(InteractionMode.TOUCH)
    private var timerJob: Job? = null
    init {
        resetGame()
    }
    fun resetGame() {
        stopTimer()
        gameStatus = GameStatus.IDLE
        minesLeft = totalMines
        timeElapsed = 0
        interactionMode = InteractionMode.TOUCH
        grid = List(height) { y -> List(width) { x -> Cell(x, y) } }
    }
    private fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                timeElapsed++
            }
        }
    }
    private fun stopTimer() {
        timerJob?.cancel()
    }
    private fun generateGrid(firstClickX: Int, firstClickY: Int) {
        var newGrid = grid.map { row -> row.toList() }.toMutableList()
        var minesPlaced = 0
        while (minesPlaced < totalMines) {
            val rx = Random.nextInt(width)
            val ry = Random.nextInt(height)
            if (!newGrid[ry][rx].isMine && (rx != firstClickX || ry != firstClickY)) {
                newGrid[ry] = newGrid[ry].toMutableList().apply {
                    this[rx] = this[rx].copy(isMine = true)
                }
                minesPlaced++
            }
        }
        newGrid = newGrid.mapIndexed { y, row ->
            row.mapIndexed { x, cell ->
                if (cell.isMine) cell else cell.copy(adjacentMines = countAdjacentMines(newGrid, x, y))
            }.toMutableList()
        }.toMutableList()
        grid = newGrid
    }
    fun onCellClicked(x: Int, y: Int) {
        if (gameStatus == GameStatus.WON || gameStatus == GameStatus.LOST || gameStatus == GameStatus.LOST_ANIMATING) return
        if (gameStatus == GameStatus.IDLE) {
            gameStatus = GameStatus.PLAYING
            generateGrid(firstClickX = x, firstClickY = y)
            startTimer()
        }
        val cell = grid[y][x]
        if (cell.isRevealed) return
        when (interactionMode) {
            InteractionMode.TOUCH -> {
                if (cell.isFlagged || cell.hasQuestion) return
                if (cell.isMine) {
                    triggerExplosionWave(x, y)
                } else {
                    startFloodFillWave(x, y)
                }
            }
            InteractionMode.FLAG -> {
                if (cell.hasQuestion) return
                val isCurrentlyFlagged = cell.isFlagged
                grid = updateCell(grid, x, y) { it.copy(isFlagged = !isCurrentlyFlagged) }
                minesLeft += if (isCurrentlyFlagged) 1 else -1
            }
            InteractionMode.QUESTION -> {
                if (cell.isFlagged) return
                val hasCurrentlyQuestion = cell.hasQuestion
                grid = updateCell(grid, x, y) { it.copy(hasQuestion = !hasCurrentlyQuestion) }
            }
        }
    }
    private fun triggerExplosionWave(startX: Int, startY: Int) {
        viewModelScope.launch {
            stopTimer()
            gameStatus = GameStatus.LOST_ANIMATING
            grid = updateCell(grid, startX, startY) { it.copy(isRevealed = true, isExploding = true) }
            delay(400)
            val otherMines = grid.flatten().filter { it.isMine && !it.isRevealed }
            val groupedByDistance = otherMines.groupBy { mine ->
                hypot((mine.x - startX).toDouble(), (mine.y - startY).toDouble()).toInt()
            }.toSortedMap()
            for ((_, minesInWave) in groupedByDistance) {
                var currentGrid = grid
                for (mine in minesInWave) {
                    currentGrid = updateCell(currentGrid, mine.x, mine.y) {
                        it.copy(isRevealed = true, isExploding = true)
                    }
                }
                grid = currentGrid
                delay(120)
            }
            delay(600)
            gameStatus = GameStatus.LOST
        }
    }
    private fun startFloodFillWave(startX: Int, startY: Int) {
        viewModelScope.launch {
            val visited = mutableSetOf<Pair<Int, Int>>()
            var currentLevel = listOf(Pair(startX, startY))
            while (currentLevel.isNotEmpty()) {
                var currentGrid = grid
                val nextLevel = mutableSetOf<Pair<Int, Int>>()
                var hasUpdates = false
                for ((x, y) in currentLevel) {
                    if (!visited.add(Pair(x, y))) continue
                    val cell = currentGrid[y][x]
                    if (cell.isRevealed || cell.isFlagged || cell.hasQuestion) continue
                    currentGrid = updateCell(currentGrid, x, y) { it.copy(isRevealed = true) }
                    hasUpdates = true
                    if (cell.adjacentMines == 0 && !cell.isMine) {
                        for (dx in -1..1) {
                            for (dy in -1..1) {
                                val nx = x + dx
                                val ny = y + dy
                                if (nx in 0 until width && ny in 0 until height) {
                                    val neighbor = currentGrid[ny][nx]
                                    if (!neighbor.isRevealed && !neighbor.isFlagged && !neighbor.hasQuestion) {
                                        nextLevel.add(Pair(nx, ny))
                                    }
                                }
                            }
                        }
                    }
                }
                if (hasUpdates) {
                    grid = currentGrid
                    delay(40)
                }
                currentLevel = nextLevel.toList()
            }
            checkWinCondition()
        }
    }
    private fun checkWinCondition() {
        val won = grid.flatten().all { it.isRevealed || it.isMine }
        if (won) {
            stopTimer()
            gameStatus = GameStatus.WON
        }
    }
    private fun countAdjacentMines(grid: List<List<Cell>>, x: Int, y: Int): Int {
        var count = 0
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height && grid[ny][nx].isMine) {
                    count++
                }
            }
        }
        return count
    }
    private fun updateCell(currentGrid: List<List<Cell>>, x: Int, y: Int, update: (Cell) -> Cell): List<List<Cell>> {
        val newGrid = currentGrid.toMutableList()
        val newRow = newGrid[y].toMutableList()
        newRow[x] = update(newRow[x])
        newGrid[y] = newRow
        return newGrid
    }
}

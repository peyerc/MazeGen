package ch.reason.mazegen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.reason.mazegen.ui.theme.MazeGenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlin.math.absoluteValue


@Composable
fun Maze(width: Int, height: Int, directions: List<Direction> = emptyList(), goalReached: () -> Unit = {}) {
    var tileSize by remember { mutableStateOf(Size.Zero) }
    val maze = remember { mutableStateMapOf<Coordinates, Cell>() }
    val path = remember { mutableStateListOf<Cell>() }
    var start by remember { mutableStateOf<Coordinates?>(null) }
    var goal by remember { mutableStateOf<Coordinates?>(null) }
    var currentCoordinates by remember { mutableStateOf<Coordinates?>(null) }

    var isGenerating by remember { mutableStateOf(false) }
    var isGameRunning by remember { mutableStateOf(false) }
    var isAutoSolving by remember { mutableStateOf(false) }
    var autoSolverStart by remember { mutableStateOf<Coordinates?>(null) }

    val resetGame = {
        isGenerating = false
        isGameRunning = false
        start = null
        autoSolverStart = null
        isAutoSolving = false
        currentCoordinates = null
    }

    fun onCellClicked(cell: Cell) = run {
        if (!isGameRunning && !isAutoSolving) {
            start = cell.coordinates
        } else {
            isAutoSolving = true
            autoSolverStart = cell.coordinates
            modifyCells(maze) { it.copy(visited = false) }
        }
    }

    LaunchedEffect(isAutoSolving, isGameRunning) {
        if (isAutoSolving && isGameRunning) {
            autoSolverStart?.let { autoSolverStart ->
                currentCoordinates = autoSolverStart
                goal?.let { goal ->
                    val manhattenDistance = { coordinates: Coordinates ->
                        (coordinates.x - goal.x).absoluteValue + (coordinates.y - goal.y).absoluteValue
                    }
                    val solverFlow = findPathWithAStar(maze, autoSolverStart, goal, h = manhattenDistance)
                    val solverPath = mutableListOf<Coordinates>()
                    solverFlow
                        .flowOn(Dispatchers.IO)
                        .onEach { (calculating, path) ->
                            modifyCells(maze) { it.copy(calculating = it.coordinates in calculating) }

                            solverPath.addAll(path)
                            delay(15)
                        }.onCompletion { error ->
                            modifyCells(maze) { it.copy(calculating = false) }
                            for (coordinates in solverPath) {
                                maze[coordinates]?.let { cell ->
                                    maze[cell.coordinates] = cell.copy(visited = true)
                                }
                                currentCoordinates = coordinates
                                delay(25)
                            }
                            isAutoSolving = false
                        }
                        .launchIn(this)
                }
            }
        }
    }

    LaunchedEffect(start) {
        maze.clear()
        maze.putAll(generateMaze(height, width, tileSize))

        if (start == null) return@LaunchedEffect
        isGenerating = true

        // mark start, set walls and add to stack
        maze[start]?.let {
            maze[it.coordinates] = it.copy(
                start = true,
                visited = true,
                walls = Direction.entries,
            )
            path.add(it)
        }

        while (path.isNotEmpty()) {

            val currentCell = path.removeLast()
            currentCoordinates = currentCell.coordinates

            // get next cell
            val nextCell = currentCell.findNext(maze)

            currentCoordinates?.let { curCoordinates ->
                nextCell?.let {
                    path.add(currentCell)

                    // set walls
                    maze[nextCell.coordinates]?.let {
                        maze[it.coordinates] = it.copy(walls = Direction.entries)
                    }

                    // remove walls of both cells
                    val currentWallToRemove = curCoordinates.findWallToRemove(nextCell.coordinates)
                    maze[currentCell.coordinates]?.let { current ->
                        maze[current.coordinates] = current.copy(
                            walls = current.walls.filter { it != currentWallToRemove },
                        )
                    }
                    val nextWallToRemove = nextCell.coordinates.findWallToRemove(curCoordinates)
                    maze[nextCell.coordinates]?.let { next ->
                        maze[next.coordinates] = next.copy(
                            walls = next.walls.filter { it != nextWallToRemove },
                        )
                    }

                    //set distance to start
                    maze[nextCell.coordinates]?.let { next ->
                        maze[next.coordinates] = next.copy(
                            distanceToStart = path.size,
                        )
                    }

                    // mark visited
                    maze[nextCell.coordinates]?.let {
                        maze[it.coordinates] = it.copy(visited = true)
                    }
                    // the current cell would lag behind, so we need to update it here
                    currentCoordinates = nextCell.coordinates

                    // move on
                    path.add(nextCell)

                    delay(1)
                }
            }
        }

        // mark goal
        maze.values.maxByOrNull { it.distanceToStart }?.coordinates?.let { furthestCellCoordinates ->
            maze[furthestCellCoordinates]?.let {
                maze[it.coordinates] = it.copy(goal = true)
                 it.coordinates
            }
            goal = furthestCellCoordinates
        }

        // return to start
        currentCoordinates = start

        modifyCells(maze) { it.copy(visited = false) }

        isGameRunning = true
    }

    LaunchedEffect(isGameRunning, directions, isAutoSolving) {

        // input disabled

//        while (isGameRunning) {
//            for (direction in directions) {
//                maze[currentCoordinates]?.let { currentCell ->
//                    val nextCell = currentCell.move(maze, direction)
//                    currentCoordinates = nextCell.coordinates
//                }
//            }
//            delay(100)
//        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val tileWidth = (it.size.width / width.toFloat())
                val tileHeight = (it.size.height / height.toFloat())
                tileSize = Size(tileWidth, tileHeight)
            }
    ) {
        val wallWidth = 8.dp
        Column {
            maze.values.sortedBy { it.coordinates.y }.groupBy { it.coordinates.y }.forEach { (_, row) ->
                Row {
                    row.sortedBy { it.coordinates.x }.forEach { cell ->
                        if (cell.coordinates == currentCoordinates && cell.goal) {
                            goalReached()
                        }
                        Box(
                            modifier = Modifier
                                .clickable(
                                    onClick = { onCellClicked(cell) }
                                )
                                .size(tileSize.width.pxToDp(), tileSize.height.pxToDp())
                                .background(
                                    if (cell.start) Color.LightGray
                                    else if (cell.goal) Color.Black
                                    else if (cell.calculating) Color.Red
                                    else if (cell.visited) Color(0xFF4C00FF)
                                    else Color(0xFFA23DDC)
                                )
                                .border(
                                    width = .1.dp,
                                    color = Color.Black.copy(alpha = 0.1f),
                                )
                                .drawBehind {

                                    if (cell.coordinates == currentCoordinates) {
                                        drawCircle(
                                            color = Color(0xFF2FFF00),
                                            radius = tileSize.width / 4,
                                            center = Offset(
                                                tileSize.width / 2,
                                                tileSize.height / 2
                                            ),
                                        )
                                    }

                                    val wallWidthPx = wallWidth.toPx()
                                    val color = Color(0xFFF5BF00)
                                    for (wall in cell.walls) {
                                        when (wall) {
                                            Direction.North ->
                                                drawLine(
                                                    color = color,
                                                    start = Offset(0f, 0f),
                                                    end = Offset(size.width, 0f),
                                                    strokeWidth = wallWidthPx,
                                                )

                                            Direction.East ->
                                                drawLine(
                                                    color = color,
                                                    start = Offset(size.width, 0f),
                                                    end = Offset(size.width, size.height),
                                                    strokeWidth = wallWidthPx,
                                                )

                                            Direction.South ->
                                                drawLine(
                                                    color = color,
                                                    start = Offset(size.width, size.height),
                                                    end = Offset(0f, size.height),
                                                    strokeWidth = wallWidthPx,
                                                )

                                            Direction.West ->
                                                drawLine(
                                                    color = color,
                                                    start = Offset(0f, size.height),
                                                    end = Offset(0f, 0f),
                                                    strokeWidth = wallWidthPx,
                                                )
                                        }
                                    }
                                }
                        ) {
//                            Text(
//                                text = cell.distanceToStart.toString(),
//                                fontSize = 12.sp,
//                                modifier = Modifier.align(Alignment.Center),
//                                color = if (cell.goal) Color.White else Color.Black,
//                            )
                        }
                    }
                }
            }
        }
        if (!isGenerating && !isGameRunning) {
            Text(
                text = "Click anywhere to choose your starting tile",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(4.dp)
                    .background(Color.White.copy(alpha = 0.65f))
            )
        } else {
            ResetButton(resetGame)
        }

        Text(
            text = "Maze size ${width}x$height",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .padding(
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
                .background(Color.White.copy(alpha = 0.65f))
        )
    }
}

private fun modifyCells(maze: SnapshotStateMap<Coordinates, Cell>, transform: (Cell) -> Cell) {
    for (cell in maze.values) {
        maze[cell.coordinates] = transform(cell)
    }
}

@Composable
private fun BoxScope.ResetButton(resetGame:  () -> Unit) {
    IconButton(
        onClick = resetGame,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(8.dp)
            .padding(
                bottom = WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding()
            )
    ) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = "Restart",
            tint = Color.Black,
            modifier = Modifier
                .background(Color.White.copy(0.5f))
                .size(48.dp)
        )
    }
}

@Composable
fun Float.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

fun generateMaze(
    height: Int,
    width: Int,
    tileSize: Size,
) = (0 until height).map { y ->
    (0 until width).map { x ->
        val currentCoordinates = Coordinates(x, y)
        currentCoordinates to Cell(
            coordinates = currentCoordinates,
            size = tileSize,
        )
    }
}.flatten().toMap()

enum class Direction(val coordinates: Coordinates) {
    North(Coordinates(0, -1)),
    East(Coordinates(1, 0)),
    South(Coordinates(0, 1)),
    West(Coordinates(-1, 0));
}

@Preview(showBackground = true)
@Composable
fun MazePreview() {
    MazeGenTheme {
        Maze(5, 10)
    }
}
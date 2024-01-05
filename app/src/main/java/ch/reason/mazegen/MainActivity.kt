package ch.reason.mazegen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MazeGenTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val sensorValue by rememberAccelerometerSensorValueAsState(
//                        samplingPeriodUs = SENSOR_DELAY_GAME,
                    )
                    val directions by remember {
                        derivedStateOf {
                            val (x, y, _) = sensorValue.value
                            val threshold = 1
                            val dirX = when {
                                x > threshold -> Direction.West
                                x < -threshold -> Direction.East
                                else -> null
                            }
                            val dirY = when {
                                y > threshold -> Direction.South
                                y < -threshold -> Direction.North
                                else -> null
                            }
                            listOfNotNull(dirX, dirY)
                        }
                    }

                    Maze(
                        width = 15,
                        height = 30,
                        directions = directions,
                        goalReached = {
                            println("Goal reached!!")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Maze(width: Int, height: Int, directions: List<Direction> = emptyList(), goalReached: () -> Unit = {}) {
    var tileSize by remember { mutableStateOf(Size.Zero) }
    val maze = remember { mutableStateMapOf<Coordinates, Cell>() }
    val path = remember { mutableStateListOf<Cell>() }
    var start by remember { mutableStateOf<Coordinates?>(null) }
    var currentCoordinates by remember { mutableStateOf<Coordinates?>(null) }

    var isGenerating by remember { mutableStateOf(false) }
    var isGameRunning by remember { mutableStateOf(false) }

    val resetGame = {
        isGenerating = false
        isGameRunning = false
        start = null
        currentCoordinates = null
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
                walls = Wall.entries,
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
                        maze[nextCell.coordinates] = it.copy(walls = Wall.entries)
                    }

                    // remove walls of both cells
                    val currentWallToRemove = findWallToRemove(curCoordinates, nextCell.coordinates)
                    maze[currentCell.coordinates]?.let { current ->
                        maze[current.coordinates] = current.copy(
                            walls = current.walls.filter { it != currentWallToRemove },
                        )
                    }
                    val nextWallToRemove = findWallToRemove(nextCell.coordinates, curCoordinates)
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
                        maze[nextCell.coordinates] = it.copy(visited = true)
                    }
                    // the current cell would lag behind, so we need to update it here
                    currentCoordinates = nextCell.coordinates

                    // move on
                    path.add(nextCell)

                    delay(5)
                }
            }
        }

        // mark goal
        val goal = maze.values.maxByOrNull { it.distanceToStart }?.coordinates ?: Coordinates(0, 0)
        maze[goal]?.let {
            maze[goal] = it.copy(goal = true)
        }

        // return to start
        currentCoordinates = start

        isGameRunning = true
    }

    LaunchedEffect(isGameRunning, directions) {
        while (isGameRunning) {
            for (direction in directions) {
                maze[currentCoordinates]?.let { currentCell ->
                    val nextCell = currentCell.move(maze, direction)
                    currentCoordinates = nextCell.coordinates
                }
            }
            delay(100)
        }
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
                            isGameRunning = false
                        }
                        Box(
                            modifier = Modifier
                                .clickable(
                                    onClick = {
                                        if (!isGameRunning) {
                                            start = cell.coordinates
                                        }
                                    }
                                )
                                .size(tileSize.width.pxToDp(), tileSize.height.pxToDp())
                                .background(
                                    if (cell.start) Color.LightGray
                                    else if (cell.goal) Color.Black
                                    else if (cell.visited) Color(0xFFA23DDC)
                                    else Color.White
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
                                            center = Offset(tileSize.width / 2, tileSize.height / 2),
                                        )
                                    }

                                    val wallWidthPx = wallWidth.toPx()
                                    val color = Color(0xFFF5BF00)
                                    for (wall in cell.walls) {
                                        when (wall) {
                                            Wall.North ->
                                                drawLine(
                                                    color = color,
                                                    start = Offset(0f, 0f),
                                                    end = Offset(size.width, 0f),
                                                    strokeWidth = wallWidthPx,
                                                )

                                            Wall.East ->
                                                drawLine(
                                                    color = color,
                                                    start = Offset(size.width, 0f),
                                                    end = Offset(size.width, size.height),
                                                    strokeWidth = wallWidthPx,
                                                )

                                            Wall.South ->
                                                drawLine(
                                                    color = color,
                                                    start = Offset(size.width, size.height),
                                                    end = Offset(0f, size.height),
                                                    strokeWidth = wallWidthPx,
                                                )

                                            Wall.West ->
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

@Composable
private fun BoxScope.ResetButton(resetGame:  () -> Unit) {
    IconButton(
        onClick = resetGame,
        modifier = Modifier.Companion
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

@Preview(showBackground = true)
@Composable
fun MazePreview() {
    MazeGenTheme {
        Maze(5, 10)
    }
}
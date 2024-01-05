package ch.reason.mazegen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
                        width = 20,
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
    val path = remember { mutableStateListOf<Coordinates>() }
    var start by remember { mutableStateOf<Coordinates?>(null) }
    var currentCoordinates by remember { mutableStateOf<Coordinates?>(null) }

    var isGameRunning by remember { mutableStateOf(false) }

    LaunchedEffect(start) {
        maze.clear()
        maze.putAll(generateMaze(height, width, tileSize))

        if (start == null) return@LaunchedEffect
        currentCoordinates = start

        // mark start
        maze[currentCoordinates]?.let {
            maze[it.coordinates] = it.copy(start = true)
        }

        while (true) {
            // visit current cell
            val currentCell = maze[currentCoordinates] ?: throw IllegalStateException("Current cell is null")
            maze[currentCell.coordinates]?.let {
                maze[currentCell.coordinates] = it.copy(visited = true)
            }

            // get next cell
            val nextCell = currentCell.findNext(maze)

            currentCoordinates?.let { curCoordinates ->
                nextCell?.let {
                    // save current cell to stack
                    path.add(curCoordinates)

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

                    // move on
                    currentCoordinates = nextCell.coordinates
                    delay(25)
                } ?: run {
                    // backtrack
                    val previousCell = path.removeLast()
                    currentCoordinates = previousCell
                    delay(5)
                }
            }
            if (path.isEmpty()) break
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

//    val textMeasurer = rememberTextMeasurer()

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
//                                .padding(wallWidth/2)
//                                .border(1.dp, Color.Red)
                                .size(tileSize.width.pxToDp(), tileSize.height.pxToDp())
                                .background(
                                    if (cell.coordinates == currentCoordinates) Color.Green
                                    else if (cell.start) Color.White
                                    else if (cell.goal) Color.Black
                                    else if (cell.visited) Color(0xFFA23DDC)
                                    else Color.White
                                )
                                .drawBehind {
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
fun Float.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

@Preview(showBackground = true)
@Composable
fun MazePreview() {
    MazeGenTheme {
        Maze(10, 20)
    }
}
package ch.reason.mazegen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.reason.mazegen.ui.theme.MazeGenTheme
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
                    Maze(20, 40)
                }
            }
        }
    }
}

@Composable
fun Maze(width: Int, height: Int, start: Coordinates = Coordinates(0, 0)) {
    var tileSize by remember { mutableStateOf(Size.Zero) }
    val maze = remember { mutableStateMapOf<Coordinates, Cell>() }
    val path = remember { mutableStateListOf<Coordinates>() }
    var currentCoordinates by remember { mutableStateOf(start) }

    LaunchedEffect(Unit) {
        maze.putAll(generateMaze(height, width, tileSize))

        while (maze.values.find { !it.visited } != null) {
            delay(25)

            // visit current cell
            val currentCell = maze[currentCoordinates] ?: break
            maze[currentCell.coordinates]?.let {
                maze[currentCell.coordinates] = it.copy(visited = true)
            }

            // get next cell
            val nextCell = currentCell.findNext(maze)

            nextCell?.let {
                // save current cell to stack
                path.add(currentCoordinates)

                // remove walls of both cells
                val currentWallToRemove = findWallToRemove(currentCoordinates, nextCell.coordinates)
                maze[currentCell.coordinates]?.let { current ->
                    maze[current.coordinates] = current.copy(
                        walls = current.walls.filter { it != currentWallToRemove },
                    )
                }
                val nextWallToRemove = findWallToRemove(nextCell.coordinates, currentCoordinates)
                maze[nextCell.coordinates]?.let { next ->
                    maze[next.coordinates] = next.copy(
                        walls = next.walls.filter { it != nextWallToRemove },
                    )
                }

                // move on
                currentCoordinates = nextCell.coordinates
            } ?: run {
                // backtrack
                val previousCell = path.removeLast()
                currentCoordinates = previousCell
            }

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
            .drawBehind {
                for (cell in maze.values) {
                    cell.draw(
                        scope = this,
                        highlighted = cell.coordinates == currentCoordinates,
                    )
                }
            }
    ) {
        Text(
            text = "Maze size ${width}x$height",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .background(Color.White.copy(alpha = 0.65f))
        )
    }
}

private fun generateMaze(
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

@Preview(showBackground = true)
@Composable
fun MazePreview() {
    MazeGenTheme {
        Maze(10, 20)
    }
}
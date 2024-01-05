package ch.reason.mazegen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.reason.mazegen.ui.theme.MazeGenTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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

        // TODO: check if the find is expensive
        while (maze.values.find { !it.visited } != null) {
            delay(25)

            // visit current cell
            val currentCell = maze[currentCoordinates] ?: break
            maze[currentCell.coordinates]?.let {
                maze[currentCell.coordinates] = it.copy(visited = true)
            }

            // get next cell
            val nextCell = currentCell.getNext(maze)

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
                println("backtrack: $currentCoordinates")
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

private fun findWallToRemove(
    currentCoordinates: Coordinates,
    nextCoordinates: Coordinates,
): Wall? = when {
    currentCoordinates.x < nextCoordinates.x -> Wall.Right
    currentCoordinates.x > nextCoordinates.x -> Wall.Left
    currentCoordinates.y < nextCoordinates.y -> Wall.Bottom
    currentCoordinates.y > nextCoordinates.y -> Wall.Top
    else -> null
}

private fun List<Coordinates>.wallsToRemove(coordinates: Coordinates): List<Wall> {
    val posInStack = this.indexOf(coordinates)
    return if (posInStack > -1) {
        val frontWall = this.getOrNull(posInStack + 1)?.let {
            wallToRemove(coordinates, it)
        }
        val backWall = this.getOrNull(posInStack - 1)?.let {
            wallToRemove(coordinates, it)
        }
        listOfNotNull(frontWall, backWall)
    } else emptyList()
}

private fun wallToRemove(a: Coordinates, b: Coordinates): Wall? = when {
    a.x < b.x -> Wall.Right
    a.x > b.x -> Wall.Left
    a.y < b.y -> Wall.Bottom
    a.y > b.y -> Wall.Top
    else -> null
}

private fun Cell.getNext(maze: Map<Coordinates, Cell>): Cell? {
    val neighbours = listOf(
        Coordinates(coordinates.x + 1, coordinates.y),
        Coordinates(coordinates.x, coordinates.y + 1),
        Coordinates(coordinates.x - 1, coordinates.y),
        Coordinates(coordinates.x, coordinates.y - 1),
    ).mapNotNull { maze[it] }.filter { !it.visited }
    return if (neighbours.isNotEmpty()) neighbours.random() else null
}

data class Cell(
    val coordinates: Coordinates,
    val size: Size,
    val walls: List<Wall> = listOf(Wall.Top, Wall.Left, Wall.Bottom, Wall.Right),
    val visited: Boolean = false,
) {

    fun draw(scope: DrawScope, highlighted: Boolean = false) {
        val color = Color(0xFF000D50)
        val strokeWidth = 4f
        val x = coordinates.x * size.width
        val y = coordinates.y * size.height
        for (wall in walls) {
            when (wall) {
                Wall.Top ->
                    scope.drawLine(
                        color = color,
                        start = Offset(x, y),
                        end = Offset(x + size.width, y),
                        strokeWidth = strokeWidth,
                    )
                Wall.Right ->
                    scope.drawLine(
                        color = color,
                        start = Offset(x + size.width, y),
                        end = Offset(x + size.width, y + size.height),
                        strokeWidth = strokeWidth,
                    )
                Wall.Bottom ->
                    scope.drawLine(
                        color = color,
                        start = Offset(x + size.width, y + size.height),
                        end = Offset(x, y + size.height),
                        strokeWidth = strokeWidth,
                    )
                Wall.Left ->
                    scope.drawLine(
                        color = color,
                        start = Offset(x, y + size.height),
                        end = Offset(x, y),
                        strokeWidth = strokeWidth,
                    )
            }
        }

        if (highlighted) {
            scope.drawRect(
                color = Color(0xB921D147),
                topLeft = Offset(x, y),
                size = size,
            )
        } else if (visited) {
            scope.drawRect(
                color = Color(0xA1B300FF),
                topLeft = Offset(x, y),
                size = size,
            )
        }
    }

}

data class Coordinates(val x: Int, val y: Int)

enum class Wall {
    Top, Right, Bottom, Left
}

@Preview(showBackground = true)
@Composable
fun MazePreview() {
    MazeGenTheme {
        Maze(10, 20)
    }
}
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
                    Maze(10, 20)
                }
            }
        }
    }
}

@Composable
fun Maze(width: Int, height: Int) {
    var tileSize by remember { mutableStateOf(Size.Zero) }

    var visited by remember { mutableStateOf(setOf<Coordinates>()) }

    var current by remember { mutableStateOf<Coordinates?>(null) }

    LaunchedEffect(current) {
        current?.let { visited += visited + it }
    }

    val maze by remember {
        derivedStateOf {
            (0 until height).map { y ->
                (0 until width).map { x ->
                    val coordinates = Coordinates(x, y)
                    coordinates to Cell(
                        coordinates = coordinates,
                        size = tileSize,
                        visited = coordinates in visited,
                        current = coordinates == current,
                    )
                }
            }.flatten().toMap()
        }
    }

    println("xxx Maze: ${maze[Coordinates(3, 4)]}")

    LaunchedEffect(Unit) {
        delay(1000)
        current = Coordinates(0, 0)

        while (true) {
            delay(200)
            current = current?.getNext(maze) ?: break
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
                    cell.draw(this)
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

private fun Coordinates.getNext(maze: Map<Coordinates, Cell>): Coordinates? {
    val neighbours = listOf(
        Coordinates(x + 1, y),
        Coordinates(x, y + 1),
        Coordinates(x - 1, y),
        Coordinates(x, y - 1),
    ).filter { it in maze.keys && maze[it]?.visited == false }
    return if (!neighbours.isEmpty()) neighbours.random() else null
}

data class Cell(
    val coordinates: Coordinates,
    val size: Size,
    val walls: List<Wall> = listOf(Wall.Top, Wall.Left, Wall.Bottom, Wall.Right),
    val visited: Boolean,
    val current: Boolean,
) {

    fun draw(scope: DrawScope) {
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
        if (current) {
            scope.drawRect(
                color = Color(0xA152CF44),
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
fun GreetingPreview() {
    MazeGenTheme {
        Maze(10, 20)
    }
}
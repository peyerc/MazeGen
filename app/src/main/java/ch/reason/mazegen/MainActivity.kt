package ch.reason.mazegen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ch.reason.mazegen.ui.theme.MazeGenTheme

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
    var tileSize by remember {
        mutableStateOf(Size.Zero)
    }

    val maze by remember {
        derivedStateOf {
            (0 until height).map { y ->
                (0 until width).map { x ->
                    val coordinates = Coordinates(x, y)
                    coordinates to Cell(coordinates, tileSize, emptyList())
                }
            }.flatten().toMap()
        }
    }

    println("xxx Maze: ${maze[Coordinates(3, 4)]}")

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
            text = "Hello, this maze will be of size ${width}x$height!",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

data class Cell(val coordinates: Coordinates, val size: Size, val walls: List<Wall>) {
    fun draw(scope: DrawScope) {

        val color = Color.Red
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
package ch.reason.mazegen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

fun Cell.findNext(maze: Map<Coordinates, Cell>): Cell? {
    val neighbours = listOf(
        Coordinates(coordinates.x + 1, coordinates.y),
        Coordinates(coordinates.x, coordinates.y + 1),
        Coordinates(coordinates.x - 1, coordinates.y),
        Coordinates(coordinates.x, coordinates.y - 1),
    ).mapNotNull { maze[it] }.filter { !it.visited }
    return if (neighbours.isNotEmpty()) neighbours.random() else null
}

fun findWallToRemove(
    currentCoordinates: Coordinates,
    nextCoordinates: Coordinates,
): Wall? = when {
    currentCoordinates.x < nextCoordinates.x -> Wall.Right
    currentCoordinates.x > nextCoordinates.x -> Wall.Left
    currentCoordinates.y < nextCoordinates.y -> Wall.Bottom
    currentCoordinates.y > nextCoordinates.y -> Wall.Top
    else -> null
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

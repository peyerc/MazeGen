package ch.reason.mazegen

import androidx.compose.ui.geometry.Size

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

enum class Direction {
    North,
    East,
    South,
    West
}
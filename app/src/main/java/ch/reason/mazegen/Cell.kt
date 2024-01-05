package ch.reason.mazegen

import androidx.compose.ui.geometry.Size

fun Cell.findNext(maze: Map<Coordinates, Cell>): Cell? {
    val neighbours = listOf(
        Coordinates(coordinates.x + 1, coordinates.y),
        Coordinates(coordinates.x, coordinates.y + 1),
        Coordinates(coordinates.x - 1, coordinates.y),
        Coordinates(coordinates.x, coordinates.y - 1),
    ).mapNotNull { maze[it] }.filter { !it.visited }
    return if (neighbours.isNotEmpty()) neighbours.random() else null
}

fun Cell.move(maze: Map<Coordinates, Cell>, direction: Direction): Cell {
    val nextCoordinates = when (direction) {
        Direction.North ->
            if (Wall.North in walls) Coordinates(coordinates.x, coordinates.y)
            else Coordinates(coordinates.x, coordinates.y - 1)
        Direction.East ->
            if (Wall.East in walls) Coordinates(coordinates.x, coordinates.y)
            else Coordinates(coordinates.x + 1, coordinates.y)
        Direction.South ->
            if (Wall.South in walls) Coordinates(coordinates.x, coordinates.y)
            else Coordinates(coordinates.x, coordinates.y + 1)
        Direction.West ->
            if (Wall.West in walls) Coordinates(coordinates.x, coordinates.y)
            else Coordinates(coordinates.x - 1, coordinates.y)
    }
    return maze[nextCoordinates] ?: this
}

fun findWallToRemove(
    currentCoordinates: Coordinates,
    nextCoordinates: Coordinates,
): Wall? = when {
    currentCoordinates.x < nextCoordinates.x -> Wall.East
    currentCoordinates.x > nextCoordinates.x -> Wall.West
    currentCoordinates.y < nextCoordinates.y -> Wall.South
    currentCoordinates.y > nextCoordinates.y -> Wall.North
    else -> null
}

data class Cell(
    val coordinates: Coordinates,
    val size: Size,
    val walls: List<Wall> = listOf(Wall.North, Wall.West, Wall.South, Wall.East),
    val visited: Boolean = false,
    val distanceToStart: Int = 0,
    val start: Boolean = false,
    val target: Boolean = false,
)

data class Coordinates(val x: Int, val y: Int)

enum class Wall {
    North, East, South, West
}

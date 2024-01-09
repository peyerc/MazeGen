package ch.reason.mazegen

data class Cell(
    val coordinates: Coordinates,
    val walls: List<Direction> = emptyList(),
    val visited: Boolean = false,
    val distanceToStart: Int = 0,
    val start: Boolean = false,
    val goal: Boolean = false,
    val calculating: Boolean = false,
) {
    fun findNext(maze: Map<Coordinates, Cell>): Cell? {
        val neighbours = listOf(
            coordinates.plus(Direction.North.coordinates),
            coordinates.plus(Direction.East.coordinates),
            coordinates.plus(Direction.South.coordinates),
            coordinates.plus(Direction.West.coordinates),
        ).mapNotNull { maze[it] }.filter { !it.visited }
        return if (neighbours.isNotEmpty()) neighbours.random() else null
    }

    val possibleDirections = Direction.entries.filter { it !in walls }

    fun isBlocked(direction: Direction): Boolean = direction in walls

    fun moveCost(direction: Direction, blockingCost: Int) = if (isBlocked(direction)) blockingCost else 1
    fun move(direction: Direction): Coordinates =
        if (isBlocked(direction)) coordinates else coordinates.plus(direction.coordinates)

}

data class Coordinates(val x: Int, val y: Int) {
    fun plus(coordinates: Coordinates): Coordinates = Coordinates(
        x = x + coordinates.x,
        y = y + coordinates.y,
    )

    fun findWallToRemove(
        nextCoordinates: Coordinates,
    ): Direction? = when {
        x < nextCoordinates.x -> Direction.East
        x > nextCoordinates.x -> Direction.West
        y < nextCoordinates.y -> Direction.South
        y > nextCoordinates.y -> Direction.North
        else -> null
    }

}

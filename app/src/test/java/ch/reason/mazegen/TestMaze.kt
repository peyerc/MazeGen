package ch.reason.mazegen

import androidx.compose.ui.geometry.Size
import org.junit.Test

import org.junit.Assert.*

class TestMaze {

    @Test
    fun `When walls North, East, West are set in a cell, possibleDirections should return South`() {
        val cell = Cell(
            coordinates = Coordinates(x = 1, y = 1),
            size = Size(1f, 1f),
            walls = listOf(Direction.North, Direction.East, Direction.West),
        )
        assertEquals(listOf(Direction.South), cell.possibleDirections)
    }

    @Test
    fun `When walls North, West are set in a cell, possibleDirections should return East, South`() {
        val cell = Cell(
            coordinates = Coordinates(x = 1, y = 1),
            size = Size(1f, 1f),
            walls = listOf(Direction.North, Direction.West),
        )
        assertEquals(setOf(Direction.South, Direction.East), cell.possibleDirections.toSet())
    }

    @Test
    fun `When no walls are set, possibleDirections should return all directions`() {
        val cell = Cell(
            coordinates = Coordinates(x = 1, y = 1),
            size = Size(1f, 1f),
        )
        assertEquals(Direction.entries.toSet(), cell.possibleDirections.toSet())
    }

    @Test
    fun `When all walls are set, possibleDirections should return an empty list`() {
        val cell = Cell(
            coordinates = Coordinates(x = 1, y = 1),
            size = Size(1f, 1f),
            walls = Direction.entries,
        )
        assertEquals(emptyList<Direction>(), cell.possibleDirections)
    }
}
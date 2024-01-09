package ch.reason.mazegen

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Algorithm from https://en.wikipedia.org/wiki/A*_search_algorithm
//
// A* finds a path from start to goal.
// h is the heuristic function. h(n) estimates the cost to reach goal from node n.
fun findPathWithAStar(maze: Map<Coordinates, Cell>, start: Coordinates, goal: Coordinates, h: (Coordinates) -> Int): Flow<Pair<Set<Coordinates>,List<Coordinates>>> = flow {

    val startTime = System.currentTimeMillis()

    val maxCost = 9999

    // The set of discovered nodes that may need to be (re-)expanded.
    // Initially, only the start node is known.
    // This is usually implemented as a min-heap or priority queue rather than a hash-set.
    val open = mutableSetOf(start)

    // For node n, cameFrom[n] is the node immediately preceding it on the cheapest path from the start
    // to n currently known.
    val cameFrom = mutableMapOf<Coordinates, Coordinates>()

    // For node n, gScore[n] is the cost of the cheapest path from start to n currently known.
    val gScore = mutableMapOf<Coordinates, Int>().withDefault { maxCost }
    gScore[start] = 0

    // For node n, fScore[n] := gScore[n] + h(n). fScore[n] represents our current best guess as to
    // how cheap a path could be from start to finish if it goes through n.
    val fScore = mutableMapOf<Coordinates, Int>().withDefault { maxCost }
    fScore[start] = h(start)

    while (open.isNotEmpty()) {
        // This operation can occur in O(Log(N)) time if openSet is a min-heap or a priority queue
        val current =
            open.minBy { fScore.getValue(it) } //the node in openSet having the lowest fScore[] value

        if (current == goal) {
            emit(open to reconstructPath(cameFrom, current))
            println("xxx Found path in ${System.currentTimeMillis() - startTime}ms")
            return@flow
        } else {
            emit(open to emptyList())
        }

        open.remove(current)

        for (direction in Direction.entries) {

            val neighbor = current.plus(direction.coordinates)

            val currentCell = maze.getValue(current)

            // d(current,neighbor) is the weight of the edge from current to neighbor
            // tentative_gScore is the distance from start to the neighbor through current
            val tentativeGScore = gScore.getValue(current) + (currentCell.moveCost(direction, maxCost))
            if (tentativeGScore < gScore.getValue(neighbor)) {
                // This path to neighbor is better than any previous one. Record it!
                cameFrom[neighbor] = current
                gScore[neighbor] = tentativeGScore
                fScore[neighbor] = tentativeGScore + h(neighbor)
                if (neighbor !in open) {
                    open.add(neighbor)
                }
            }
        }

    }

    // Open set is empty but goal was never reached
    throw Exception("Didn't find a path... :(")
}

private fun reconstructPath(cameFrom: Map<Coordinates, Coordinates>, end: Coordinates): List<Coordinates> {
    val totalPath = mutableListOf(end)
    var current = end
    while (cameFrom.containsKey(current)) {
        current = cameFrom.getValue(current)
        totalPath.add(0, current)
    }
    return totalPath
}

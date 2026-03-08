package com.Bobr.mill.domain.engine

object BoardDefinitions {

    // Defines which points are connected by lines.
    // Format: Point ID -> List of connected Point IDs
    val adjacencyMap: Map<Int, List<Int>> = mapOf(
        // Outer Ring
        0 to listOf(1, 7),        // Top-Left corner
        1 to listOf(0, 2, 9),     // Top-Middle (connects to middle ring)
        2 to listOf(1, 3),        // Top-Right corner
        3 to listOf(2, 4, 11),    // Right-Middle (connects to middle ring)
        4 to listOf(3, 5),        // Bottom-Right corner
        5 to listOf(4, 6, 13),    // Bottom-Middle (connects to middle ring)
        6 to listOf(5, 7),        // Bottom-Left corner
        7 to listOf(0, 6, 15),    // Left-Middle (connects to middle ring)

        // Middle Ring
        8 to listOf(9, 15),
        9 to listOf(1, 8, 10, 17), // Connects outer and inner rings
        10 to listOf(9, 11),
        11 to listOf(3, 10, 12, 19),
        12 to listOf(11, 13),
        13 to listOf(5, 12, 14, 21),
        14 to listOf(13, 15),
        15 to listOf(7, 8, 14, 23),

        // Inner Ring
        16 to listOf(17, 23),
        17 to listOf(9, 16, 18),
        18 to listOf(17, 19),
        19 to listOf(11, 18, 20),
        20 to listOf(19, 21),
        21 to listOf(13, 20, 22),
        22 to listOf(21, 23),
        23 to listOf(15, 16, 22)
    )

    // The 16 possible ways to get 3-in-a-row on a Mill board
    val millCombinations: List<List<Int>> = listOf(
        // Outer Ring Mills
        listOf(0, 1, 2), listOf(2, 3, 4), listOf(4, 5, 6), listOf(6, 7, 0),

        // Middle Ring Mills
        listOf(8, 9, 10), listOf(10, 11, 12), listOf(12, 13, 14), listOf(14, 15, 8),

        // Inner Ring Mills
        listOf(16, 17, 18), listOf(18, 19, 20), listOf(20, 21, 22), listOf(22, 23, 16),

        // Cross Intersections (connecting the three rings)
        listOf(1, 9, 17),  // Top vertical
        listOf(3, 11, 19), // Right horizontal
        listOf(5, 13, 21), // Bottom vertical
        listOf(7, 15, 23)  // Left horizontal
    )
}
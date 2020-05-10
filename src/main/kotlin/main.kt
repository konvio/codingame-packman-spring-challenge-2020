import java.util.*
import kotlin.Comparator


class Log {
    companion object {
        fun info(message: String) {
            System.err.println("[INFO] $message")
        }
        fun warn(message: String) {
            System.err.println("[WARN] $message")
        }
    }
}

class SimpleStrategy(val game: Game) {

    fun getNextMove(packman: Packman): Cell {
        val valueComparator: Comparator<Cell> = Comparator.comparingInt { cell -> cell?.pellet?.value ?: -1 }
        val distComparator: Comparator<Cell> = Comparator.comparingInt { cell ->
            game.getCell(packman.x, packman.y).distanceTo[cell] ?: -1
        }
        val comparator = valueComparator.then(distComparator)
        val queue = PriorityQueue(distComparator)
        for (pelletCell in game.getPelletCells()) {
            queue.add(pelletCell)
        }
        Log.info("${queue.size} was added to priority queue")
        val next = queue.poll() ?: game.getCell(packman.x, packman.y)
        Log.info("next move is $next ${next.pellet} ${next.distanceTo}")
        return next
    }
}

class Game(private val width: Int, private val height: Int, rows: List<String>) {

    private val board: Array<Array<Cell>> // cell(x, y) = board[y][x]
    private val packmans = mutableListOf<Packman>()
    private val pellets = mutableListOf<Pellet>()

    private val simpleStrategy = SimpleStrategy(this)

    init {
        board = Array(height) { row ->
            Array(width) { col ->
                if (rows[row][col] == '#') Cell(col, row, CellType.WALL) else Cell(col, row, CellType.FLOOR)
            }
        }
        initCells()
        initCellDistances()
    }

    fun onNextTurnStart() {
        clearPackmans()
        clearPellets()
    }

    fun onNextTurnEnd() {
        getMyPackmans().forEach { move(it, simpleStrategy.getNextMove(it)) }
    }

    fun addPackman(packman: Packman) {
        packmans.add(packman)
        getCell(packman.x, packman.y).packman = packman
    }

    fun addPellet(pellet: Pellet) {
        pellets.add(pellet)
        val cell = getCell(pellet.x, pellet.y)
        if (cell.isFloor().not()) {
            Log.warn("Cell $cell for pellet $pellet is wall")
        }
        getCell(pellet.x, pellet.y).pellet = pellet
    }

    fun getCell(x: Int, y: Int): Cell {
        return board[y][x]
    }

    fun getPelletCells(): List<Cell> {
        return pellets.map { getCell(it.x, it.y) }
    }

    private fun getMyPackmans(): List<Packman> {
        return packmans.filter { it.isMine }
    }

    private fun move(packman: Packman, cell: Cell) {
        println("MOVE ${packman.packId} ${cell.x} ${cell.y}")
    }

    private fun clearPackmans() {
        for (p in packmans) {
            getCell(p.x, p.y).packman = null
        }
        packmans.clear()
    }

    private fun clearPellets() {
        for (p in pellets) {
            getCell(p.x, p.y).pellet = null
        }
        pellets.clear()
    }

    private fun initCells() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val cell = board[y][x]
                if (cell.type == CellType.WALL) continue

                val leftCell = board[y][(x + 1) % width]
                if (leftCell.type == CellType.FLOOR) {
                    cell.left = leftCell
                    leftCell.right = cell
                }

                val bottomCell = board[(y + 1) % height][x]
                if (bottomCell.type == CellType.FLOOR) {
                    cell.bottom = bottomCell
                    bottomCell.top = cell
                }
            }
        }
    }

    private fun initCellDistances() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                val cell = board[y][x]
                if (cell.isFloor()) {
                    initCellDistance(cell)
                }
            }
        }
    }

    private fun initCellDistance(cell: Cell) {
        var queue = LinkedList<Cell>()
        var curDist = 0
        queue.add(cell)
        while (queue.isNotEmpty()) {
            val nextQueue = LinkedList<Cell>()
            for (curCell in queue) {
                if (cell.distanceTo.contains(curCell).not() && cell.isFloor()) {
                    cell.distanceTo[curCell] = curDist
                    nextQueue.addAll(curCell.neighbors)
                }
            }
            queue = nextQueue
            curDist++
        }
    }
}

data class Cell(val x: Int, val y: Int, val type: CellType) {

    val distanceTo = mutableMapOf<Cell, Int>()

    var packman: Packman? = null
    var pellet: Pellet? = null

    var left: Cell? = null
    var right: Cell? = null
    var top: Cell? = null
    var bottom: Cell? = null

    val neighbors: List<Cell>
        get() = listOfNotNull(left, right, top, bottom)

    fun isFloor(): Boolean = type == CellType.FLOOR
}

enum class CellType {
    WALL, FLOOR
}

data class Pellet(val x: Int, val y: Int, val value: Int)

data class Packman(val packId: Int, val isMine: Boolean, val x: Int, val y: Int)

/**
 * Grab the pellets as fast as you can!
 **/
fun main() {
    val input = Scanner(System.`in`)

    val width = input.nextInt() // size of the grid
    val height = input.nextInt() // top left corner is (x=0, y=0)
    if (input.hasNextLine()) {
        input.nextLine()
    }

    val rows = mutableListOf<String>()
    for (i in 0 until height) {
        val row = input.nextLine() // one line of the grid: space " " is floor, pound "#" is wall
        rows.add(row)
    }

    val game = Game(width, height, rows)

    // game loop
    while (true) {
        game.onNextTurnStart()

        val myScore = input.nextInt()
        val opponentScore = input.nextInt()

        val visiblePacCount = input.nextInt() // all your pacs and enemy pacs in sight
        for (i in 0 until visiblePacCount) {
            val pacId = input.nextInt() // pac number (unique within a team)
            val mine = input.nextInt() != 0 // true if this pac is yours
            val x = input.nextInt() // position in the grid
            val y = input.nextInt() // position in the grid
            val typeId = input.next() // unused in wood leagues
            val speedTurnsLeft = input.nextInt() // unused in wood leagues
            val abilityCooldown = input.nextInt() // unused in wood leagues

            val packman = Packman(packId = pacId, isMine = mine, x = x, y = y)
            game.addPackman(packman)
        }

        val visiblePelletCount = input.nextInt() // all pellets in sight
        Log.info("visiblePelletCount: $visiblePelletCount")
        for (i in 0 until visiblePelletCount) {
            val x = input.nextInt()
            val y = input.nextInt()
            val value = input.nextInt() // amount of points this pellet is worth

            val pellet = Pellet(x, y, value)
            game.addPellet(pellet)
        }

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");
        game.onNextTurnEnd()
    }
}
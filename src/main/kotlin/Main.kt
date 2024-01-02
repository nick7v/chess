package chess
import kotlin.math.abs
const val ROWS = 8
const val COLUMNS = 8

class Square(
    var location: Pair<Int, Int> = Pair(0, 0),
    var firstStep: Boolean = false,
    var winSquare: Boolean = false,
    var ownerOfFigure: Player? = null
) {
    override fun toString(): String = when(ownerOfFigure?.color) {
        "Black" -> "B"
        "White" -> "W"
        else -> " "
    }
}

class Player(val name: String?, val color: String) {
    val listOfEnPassant = mutableListOf<Square>()
    var win = false
    val listOfFigures = mutableSetOf<Square>()

}

class ChessBoard(private val rows: Int, private val columns: Int,
                 private val playerWhite: Player, private val playerBlack: Player) {
    private val chessBoard = Array(rows) { row -> Array(columns) { column ->  Square(Pair(row, column))} }
    init {
        chessBoard[ROWS-2].forEach { it.ownerOfFigure = playerBlack; it.firstStep = true; playerBlack.listOfFigures.add(it) }
        chessBoard[1].forEach { it.ownerOfFigure = playerWhite; it.firstStep = true; playerWhite.listOfFigures.add(it)  }
        chessBoard[0].forEach { it.winSquare = true  }
        chessBoard[ROWS-1].forEach { it.winSquare = true  }
    }

    operator fun get(pair: Pair<Int, Int>) = runCatching { chessBoard[pair.second-1][pair.first-1] }.getOrNull()

    fun printChessBoard() {
        val rowsDelimeter = "  ${"+---".repeat(columns)}+"
        println(rowsDelimeter)
        for (row in (rows-1)downTo 0) {
            print("${row+1} |")
            for (column in chessBoard[row].indices) {
                print(" ${chessBoard[row][column]} |")
            }
            print("\n")
            println(rowsDelimeter)
        }
        println("    a   b   c   d   e   f   g   h")
    }

    fun turnFigure (startSquare: Square, finishSquare: Square, player: Player) {
        if (finishSquare.winSquare) player.win = true
        chessBoard[startSquare.location.first][startSquare.location.second] = finishSquare
        chessBoard[finishSquare.location.first][finishSquare.location.second] = startSquare

        val tempLocation = startSquare.location
        startSquare.apply {
            location = finishSquare.location
            firstStep = false
        }
        finishSquare.apply {
            ownerOfFigure = null
            location = tempLocation
            firstStep = false
        }
    }

}

class Game(private val chessBoard: ChessBoard) {
    fun startGame(listOfPlayers: MutableList<Player>) {
        while (true) { // цикл поочередного хода 2х игроков
            chessBoard.printChessBoard()
            val player = listOfPlayers[0]
            val rival = listOfPlayers[1]
            while (true) { //цикл одного игрока, повторяется пока не будет корректный ввод
                player.listOfEnPassant.clear()
                println("${player.name}'s turn:")
                val playersInput = readln().lowercase()
                when {
                    playersInput == "exit" -> println("Bye!").also { return }
                    playersInput.matches(Regex("([a-h][1-8]){2}")) ->
                        if(checkAndTurn(playersInput, player, rival)) break
                    else -> println("Invalid Input")
                }
            }
            if(player.win || rival.listOfFigures.size == 0) {
                chessBoard.printChessBoard()
                println("${player.color} Wins!\nBye!").also { return }
            }
            else if(!areThereMoves(rival, player)) {
                chessBoard.printChessBoard()
                println("Stalemate!\nBye!").also { return }
            }
            else listOfPlayers.reverse()
        }
    }

    private fun checkAndTurn(input: String, player: Player, rival: Player): Boolean {
        val (startCoordinate, finishCoordinate) = inputToCoordinates(input)
        val horizontalStep = abs((startCoordinate.first - finishCoordinate.first))
        var verticalStep = finishCoordinate.second - startCoordinate.second
        val enPassantSquare: Square
        val startSquare = chessBoard[startCoordinate]!!
        val finishSquare = chessBoard[finishCoordinate]!!

        //если нет пешки игрока в стартовой клетке
        if (player != chessBoard[startCoordinate]?.ownerOfFigure) {
            println("No ${player.color} pawn at ${input.substring(0, 2)}").also { return false }
        }

        // проверка не ходит ли игрок назад или на то же место (тот же ряд)
        if (verticalStep > 0 && player.color == "White") {
            enPassantSquare = chessBoard[Pair(finishCoordinate.first, finishCoordinate.second - 1)]!!
        }
        else if (verticalStep < 0 && player.color == "Black") {
            verticalStep = abs(verticalStep)
            enPassantSquare = chessBoard[Pair(finishCoordinate.first, finishCoordinate.second + 1)]!!
        }
        else println("Invalid Input").also { return false }

        //описание хода прямо
        if (horizontalStep == 0 && finishSquare.ownerOfFigure == null) {
            if (verticalStep == 1) {
                chessBoard.turnFigure(startSquare, finishSquare, player)
                return true
            }
            else if (verticalStep == 2 && startSquare.firstStep) { //НЕТ ПРОВЕРКИ что между клетками стоит пешка противника
                chessBoard.turnFigure(startSquare, finishSquare, player)
                player.listOfEnPassant.add(enPassantSquare)
                return true
            }
            else println("Invalid Input").also { return false }
        }

        // описание съесть пешку врага
        else if (horizontalStep == 1 && verticalStep == 1 && (finishSquare.ownerOfFigure == rival
                    || finishSquare in rival.listOfEnPassant)) {
            if (finishSquare in rival.listOfEnPassant) { // если съедаем пешку на проходе
                enPassantSquare.ownerOfFigure = null
                rival.listOfFigures.remove(enPassantSquare)
            }
            else rival.listOfFigures.remove(finishSquare) // если просто съедаем пешку
            chessBoard.turnFigure(startSquare, finishSquare, player)
            return true
        }
        else println("Invalid Input").also { return false}
    }

    //перевод координат игры с буквами в координаты массива с цифрами
    private fun inputToCoordinates(input: String): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        val listOfLetters = ".abcdefgh"
        return Pair(
            Pair(listOfLetters.indexOf(input[0]), input[1].digitToInt()),
            Pair(listOfLetters.indexOf(input[2]), input[3].digitToInt())
        )
    }

    private fun areThereMoves(player: Player, rival: Player): Boolean {
        val verticalMove = if (player.color == "White") 1 else -1
        for (square in player.listOfFigures) {
            val vertical = square.location.first + 1
            val horizontal = square.location.second + 1
            if ((chessBoard[Pair(horizontal, vertical + verticalMove)]?.ownerOfFigure) == null) return true
            else if (chessBoard[Pair(horizontal - 1, vertical + verticalMove)]?.ownerOfFigure == rival ||
                chessBoard[Pair(horizontal + 1, vertical + verticalMove)]?.ownerOfFigure == rival
            ) return true
            else if (chessBoard[Pair(horizontal - 1, vertical + verticalMove)] in rival.listOfEnPassant ||
                chessBoard[Pair(horizontal + 1, vertical + verticalMove)] in rival.listOfEnPassant
            ) return true
        }
        return false
    }

}

fun main() {
    println("Pawns-Only Chess")
    println("First Player's name:")
    val playerWhite = Player(readlnOrNull(), "White")
    println("Second Player's name:")
    val playerBlack = Player(readlnOrNull(), "Black")
    val listOfPlayers = mutableListOf(playerWhite, playerBlack)
    val chessBoard = ChessBoard(ROWS, COLUMNS, playerWhite, playerBlack)
    Game(chessBoard).startGame(listOfPlayers)
}
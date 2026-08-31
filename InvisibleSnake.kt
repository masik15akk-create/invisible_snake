// InvisibleSnake.kt
import com.google.gson.GsonBuilder
import java.io.File
import kotlin.concurrent.thread
import kotlin.random.Random

class InvisibleSnake {
    companion object {
        const val WIDTH = 20
        const val HEIGHT = 15
        const val FOOD_CHAR = "🍎"
        const val HEAD_CHAR = "🐍"
    }

    data class Point(val x: Int, val y: Int)

    private val snake = mutableListOf<Point>()
    private var direction = "right"
    private var nextDirection = "right"
    private lateinit var food: Point
    private var score = 0
    private var highScore = 0
    private var level = 1
    private var speed = 150 // ms
    private var gameOver = false
    private var paused = false
    private var running = true
    private val inputQueue = mutableListOf<String>()
    private val lock = Any()

    init {
        resetGame()
        highScore = loadHighScore()
        startInputThread()
    }

    private fun resetGame() {
        snake.clear()
        val startX = WIDTH / 2
        val startY = HEIGHT / 2
        snake.add(Point(startX, startY))
        direction = "right"
        nextDirection = "right"
        food = spawnFood()
        score = 0
        level = 1
        speed = 150
        gameOver = false
        paused = false
    }

    private fun loadHighScore(): Int {
        return try {
            val json = File("snake_score.json").readText()
            val map = GsonBuilder().create().fromJson(json, Map::class.java)
            (map["high_score"] as? Number)?.toInt() ?: 0
        } catch (e: Exception) { 0 }
    }

    private fun saveHighScore() {
        val json = GsonBuilder().setPrettyPrinting().create().toJson(mapOf("high_score" to highScore))
        File("snake_score.json").writeText(json)
    }

    private fun spawnFood(): Point {
        while (true) {
            val x = Random.nextInt(WIDTH)
            val y = Random.nextInt(HEIGHT)
            if (snake.none { it.x == x && it.y == y }) {
                return Point(x, y)
            }
        }
    }

    private fun setDirection(dir: String) {
        val opposites = mapOf("up" to "down", "down" to "up", "left" to "right", "right" to "left")
        if (dir != opposites[direction]) {
            nextDirection = dir
        }
    }

    private fun update() {
        if (gameOver || paused) return

        direction = nextDirection
        val head = snake.first()
        val newHead = when (direction) {
            "up" -> Point(head.x, head.y - 1)
            "down" -> Point(head.x, head.y + 1)
            "left" -> Point(head.x - 1, head.y)
            else -> Point(head.x + 1, head.y)
        }

        if (newHead.x !in 0 until WIDTH || newHead.y !in 0 until HEIGHT) {
            gameOver = true
            updateHighScore()
            return
        }

        if (snake.any { it.x == newHead.x && it.y == newHead.y }) {
            gameOver = true
            updateHighScore()
            return
        }

        snake.add(0, newHead)

        if (newHead.x == food.x && newHead.y == food.y) {
            score += 10
            if (score > highScore) { highScore = score; saveHighScore() }
            food = spawnFood()
            if (score % 50 == 0) {
                speed = maxOf(50, speed - 10)
                level++
            }
        } else {
            snake.removeAt(snake.size - 1)
        }
    }

    private fun updateHighScore() {
        if (score > highScore) { highScore = score; saveHighScore() }
    }

    private fun draw() {
        print("\u001B[2J\u001B[1;1H")
        println("┌" + "─".repeat(WIDTH * 2 + 1) + "┐")
        for (y in 0 until HEIGHT) {
            print("│")
            for (x in 0 until WIDTH) {
                val isHead = snake.isNotEmpty() && snake[0].x == x && snake[0].y == y
                val isFood = food.x == x && food.y == y
                val isBody = snake.drop(1).any { it.x == x && it.y == y }
                when {
                    isHead -> print("\u001B[32m$HEAD_CHAR\u001B[0m")
                    isFood -> print("\u001B[31m$FOOD_CHAR\u001B[0m")
                    isBody -> print(" ") // НЕВИДИМО!
                    else -> print(" ")
                }
                print(" ")
            }
            println("│")
        }
        println("└" + "─".repeat(WIDTH * 2 + 1) + "┘")

        var status = "Счёт: $score  Рекорд: $highScore  Уровень: $level"
        when {
            gameOver -> status += "\u001B[31m  💀 ИГРА ОКОНЧЕНА! Нажмите R для рестарта\u001B[0m"
            paused -> status += "\u001B[33m  ⏸ ПАУЗА\u001B[0m"
            else -> status += "  Длина: ${snake.size} (невидимая)"
        }
        println(status)
    }

    private fun startInputThread() {
        thread {
            while (running) {
                try {
                    if (System.`in`.available() > 0) {
                        val ch = System.`in`.read()
                        synchronized(lock) {
                            inputQueue.add(ch.toChar().toString())
                        }
                    }
                    Thread.sleep(20)
                } catch (_: Exception) {}
            }
        }
    }

    fun run() {
        println("\u001B[36m🐍 НЕВИДИМАЯ ЗМЕЙКА\u001B[0m")
        println("Управление: WASD/Стрелки, P - пауза, Q - выход, R - рестарт")
        println("ВНИМАНИЕ: Тело змейки НЕВИДИМО! Запоминайте своё положение!")
        println("Нажмите Enter для начала...")
        readLine()

        var lastTime = System.currentTimeMillis()
        while (running) {
            var input: String? = null
            synchronized(lock) {
                if (inputQueue.isNotEmpty()) {
                    input = inputQueue.removeAt(0)
                }
            }
            if (input != null) {
                when (input) {
                    "q" -> { running = false; break }
                    "p" -> { paused = !paused }
                    "r" -> if (gameOver) { resetGame() }
                    else -> {
                        if (!paused && !gameOver) {
                            when (input) {
                                "w" -> setDirection("up")
                                "s" -> setDirection("down")
                                "a" -> setDirection("left")
                                "d" -> setDirection("right")
                            }
                        }
                    }
                }
            }

            val now = System.currentTimeMillis()
            if (now - lastTime >= speed) {
                lastTime = now
                update()
            }
            draw()
            Thread.sleep(20)
        }
        println("\u001B[33mИгра завершена. Финальный счёт: $score\u001B[0m")
    }
}

fun main() {
    InvisibleSnake().run()
}

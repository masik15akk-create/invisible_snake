// InvisibleSnake.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

class InvisibleSnake
{
    const int WIDTH = 20;
    const int HEIGHT = 15;
    const string FOOD_CHAR = "🍎";
    const string HEAD_CHAR = "🐍";

    class Point
    {
        public int X, Y;
        public Point(int x, int y) { X = x; Y = y; }
    }

    private List<Point> snake;
    private string direction = "right";
    private string nextDirection = "right";
    private Point food;
    private int score = 0;
    private int highScore = 0;
    private int level = 1;
    private int speed = 150;
    private bool gameOver = false;
    private bool paused = false;
    private bool running = true;
    private Queue<string> inputQueue = new Queue<string>();
    private object queueLock = new object();

    public InvisibleSnake()
    {
        resetGame();
        highScore = loadHighScore();
        startInputThread();
    }

    private void resetGame()
    {
        snake = new List<Point>();
        int startX = WIDTH / 2;
        int startY = HEIGHT / 2;
        snake.Add(new Point(startX, startY));
        direction = "right";
        nextDirection = "right";
        food = spawnFood();
        score = 0;
        level = 1;
        speed = 150;
        gameOver = false;
        paused = false;
    }

    private int loadHighScore()
    {
        try
        {
            string json = File.ReadAllText("snake_score.json");
            var doc = JsonDocument.Parse(json);
            return doc.RootElement.GetProperty("high_score").GetInt32();
        }
        catch { return 0; }
    }

    private void saveHighScore()
    {
        var json = JsonSerializer.Serialize(new { high_score = highScore });
        File.WriteAllText("snake_score.json", json);
    }

    private Point spawnFood()
    {
        var rand = new Random();
        while (true)
        {
            int x = rand.Next(WIDTH);
            int y = rand.Next(HEIGHT);
            if (!snake.Any(p => p.X == x && p.Y == y))
                return new Point(x, y);
        }
    }

    private void setDirection(string dir)
    {
        var opposites = new Dictionary<string, string> {
            ["up"] = "down", ["down"] = "up",
            ["left"] = "right", ["right"] = "left"
        };
        if (dir != opposites.GetValueOrDefault(direction))
            nextDirection = dir;
    }

    private void update()
    {
        if (gameOver || paused) return;

        direction = nextDirection;
        Point head = snake[0];
        Point newHead = new Point(head.X, head.Y);
        switch (direction)
        {
            case "up": newHead.Y--; break;
            case "down": newHead.Y++; break;
            case "left": newHead.X--; break;
            case "right": newHead.X++; break;
        }

        if (newHead.X < 0 || newHead.X >= WIDTH || newHead.Y < 0 || newHead.Y >= HEIGHT)
        {
            gameOver = true;
            updateHighScore();
            return;
        }

        if (snake.Any(p => p.X == newHead.X && p.Y == newHead.Y))
        {
            gameOver = true;
            updateHighScore();
            return;
        }

        snake.Insert(0, newHead);

        if (newHead.X == food.X && newHead.Y == food.Y)
        {
            score += 10;
            if (score > highScore) { highScore = score; saveHighScore(); }
            food = spawnFood();
            if (score % 50 == 0)
            {
                speed = Math.Max(50, speed - 10);
                level++;
            }
        }
        else
        {
            snake.RemoveAt(snake.Count - 1);
        }
    }

    private void updateHighScore()
    {
        if (score > highScore) { highScore = score; saveHighScore(); }
    }

    private void draw()
    {
        Console.Clear();
        Console.WriteLine("┌" + new string('─', WIDTH * 2 + 1) + "┐");
        for (int y = 0; y < HEIGHT; y++)
        {
            Console.Write("│");
            for (int x = 0; x < WIDTH; x++)
            {
                bool isHead = snake.Count > 0 && snake[0].X == x && snake[0].Y == y;
                bool isFood = food.X == x && food.Y == y;
                bool isBody = snake.Skip(1).Any(p => p.X == x && p.Y == y);
                if (isHead)
                    Console.Write("\u001B[32m" + HEAD_CHAR + "\u001B[0m");
                else if (isFood)
                    Console.Write("\u001B[31m" + FOOD_CHAR + "\u001B[0m");
                else if (isBody)
                    Console.Write(" "); // НЕВИДИМО!
                else
                    Console.Write(" ");
                Console.Write(" ");
            }
            Console.WriteLine("│");
        }
        Console.WriteLine("└" + new string('─', WIDTH * 2 + 1) + "┘");

        string status = $"Счёт: {score}  Рекорд: {highScore}  Уровень: {level}";
        if (gameOver)
            status += "\u001B[31m  💀 ИГРА ОКОНЧЕНА! Нажмите R для рестарта\u001B[0m";
        else if (paused)
            status += "\u001B[33m  ⏸ ПАУЗА\u001B[0m";
        else
            status += $"  Длина: {snake.Count} (невидимая)";
        Console.WriteLine(status);
    }

    private void startInputThread()
    {
        Task.Run(() =>
        {
            while (running)
            {
                if (Console.KeyAvailable)
                {
                    var key = Console.ReadKey(true).Key;
                    string input = key.ToString().ToLower();
                    lock (queueLock)
                    {
                        inputQueue.Enqueue(input);
                    }
                }
                Thread.Sleep(20);
            }
        });
    }

    public void Run()
    {
        Console.WriteLine("\u001B[36m🐍 НЕВИДИМАЯ ЗМЕЙКА\u001B[0m");
        Console.WriteLine("Управление: WASD/Стрелки, P - пауза, Q - выход, R - рестарт");
        Console.WriteLine("ВНИМАНИЕ: Тело змейки НЕВИДИМО! Запоминайте своё положение!");
        Console.WriteLine("Нажмите Enter для начала...");
        Console.ReadLine();

        long lastTime = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;
        while (running)
        {
            string input = null;
            lock (queueLock)
            {
                if (inputQueue.Count > 0)
                    input = inputQueue.Dequeue();
            }
            if (input != null)
            {
                if (input == "q") { running = false; break; }
                if (input == "p") { paused = !paused; }
                if (input == "r" && gameOver) { resetGame(); }
                if (!paused && !gameOver)
                {
                    switch (input)
                    {
                        case "w": setDirection("up"); break;
                        case "s": setDirection("down"); break;
                        case "a": setDirection("left"); break;
                        case "d": setDirection("right"); break;
                    }
                }
            }

            long now = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;
            if (now - lastTime >= speed)
            {
                lastTime = now;
                update();
            }
            draw();
            Thread.Sleep(20);
        }
        Console.WriteLine($"\u001B[33mИгра завершена. Финальный счёт: {score}\u001B[0m");
    }

    public static void Main()
    {
        new InvisibleSnake().Run();
    }
}

// InvisibleSnake.java
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class InvisibleSnake {
    private static final int WIDTH = 20;
    private static final int HEIGHT = 15;
    private static final String FOOD_CHAR = "🍎";
    private static final String HEAD_CHAR = "🐍";

    static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        boolean equals(Point p) { return x == p.x && y == p.y; }
    }

    private List<Point> snake;
    private String direction = "right";
    private String nextDirection = "right";
    private Point food;
    private int score = 0;
    private int highScore = 0;
    private int level = 1;
    private int speed = 150;
    private boolean gameOver = false;
    private boolean paused = false;
    private boolean running = true;
    private BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

    public InvisibleSnake() {
        resetGame();
        highScore = loadHighScore();
        startInputThread();
    }

    private void resetGame() {
        snake = new ArrayList<>();
        int startX = WIDTH / 2;
        int startY = HEIGHT / 2;
        snake.add(new Point(startX, startY));
        direction = "right";
        nextDirection = "right";
        food = spawnFood();
        score = 0;
        level = 1;
        speed = 150;
        gameOver = false;
        paused = false;
    }

    private int loadHighScore() {
        try {
            String json = new String(Files.readAllBytes(Paths.get("snake_score.json")));
            Map<String, Object> map = new Gson().fromJson(json, Map.class);
            return ((Number) map.getOrDefault("high_score", 0)).intValue();
        } catch (Exception e) { return 0; }
    }

    private void saveHighScore() {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("high_score", highScore);
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(map);
            Files.write(Paths.get("snake_score.json"), json.getBytes());
        } catch (Exception e) {}
    }

    private Point spawnFood() {
        Random rand = new Random();
        while (true) {
            int x = rand.nextInt(WIDTH);
            int y = rand.nextInt(HEIGHT);
            boolean found = false;
            for (Point p : snake) {
                if (p.x == x && p.y == y) { found = true; break; }
            }
            if (!found) return new Point(x, y);
        }
    }

    private void setDirection(String dir) {
        Map<String, String> opposites = new HashMap<>();
        opposites.put("up", "down"); opposites.put("down", "up");
        opposites.put("left", "right"); opposites.put("right", "left");
        if (!dir.equals(opposites.get(direction))) {
            nextDirection = dir;
        }
    }

    private void update() {
        if (gameOver || paused) return;

        direction = nextDirection;
        Point head = snake.get(0);
        Point newHead = new Point(head.x, head.y);
        switch (direction) {
            case "up": newHead.y--; break;
            case "down": newHead.y++; break;
            case "left": newHead.x--; break;
            case "right": newHead.x++; break;
        }

        if (newHead.x < 0 || newHead.x >= WIDTH || newHead.y < 0 || newHead.y >= HEIGHT) {
            gameOver = true;
            updateHighScore();
            return;
        }

        for (Point p : snake) {
            if (p.x == newHead.x && p.y == newHead.y) {
                gameOver = true;
                updateHighScore();
                return;
            }
        }

        snake.add(0, newHead);

        if (newHead.x == food.x && newHead.y == food.y) {
            score += 10;
            if (score > highScore) { highScore = score; saveHighScore(); }
            food = spawnFood();
            if (score % 50 == 0) {
                speed = Math.max(50, speed - 10);
                level++;
            }
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private void updateHighScore() {
        if (score > highScore) { highScore = score; saveHighScore(); }
    }

    private void draw() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        System.out.println("┌" + "─".repeat(WIDTH * 2 + 1) + "┐");
        for (int y = 0; y < HEIGHT; y++) {
            System.out.print("│");
            for (int x = 0; x < WIDTH; x++) {
                boolean isHead = snake.size() > 0 && snake.get(0).x == x && snake.get(0).y == y;
                boolean isFood = food.x == x && food.y == y;
                boolean isBody = false;
                for (int i = 1; i < snake.size(); i++) {
                    if (snake.get(i).x == x && snake.get(i).y == y) { isBody = true; break; }
                }
                if (isHead) {
                    System.out.print("\u001B[32m" + HEAD_CHAR + "\u001B[0m");
                } else if (isFood) {
                    System.out.print("\u001B[31m" + FOOD_CHAR + "\u001B[0m");
                } else if (isBody) {
                    System.out.print(" "); // НЕВИДИМО!
                } else {
                    System.out.print(" ");
                }
                System.out.print(" ");
            }
            System.out.println("│");
        }
        System.out.println("└" + "─".repeat(WIDTH * 2 + 1) + "┘");

        String status = "Счёт: " + score + "  Рекорд: " + highScore + "  Уровень: " + level;
        if (gameOver) {
            status += "\u001B[31m  💀 ИГРА ОКОНЧЕНА! Нажмите R для рестарта\u001B[0m";
        } else if (paused) {
            status += "\u001B[33m  ⏸ ПАУЗА\u001B[0m";
        } else {
            status += "  Длина: " + snake.size() + " (невидимая)";
        }
        System.out.println(status);
    }

    private void startInputThread() {
        new Thread(() -> {
            while (running) {
                try {
                    if (System.in.available() > 0) {
                        char ch = (char) System.in.read();
                        inputQueue.offer(String.valueOf(ch));
                    }
                    Thread.sleep(20);
                } catch (Exception e) {}
            }
        }).start();
    }

    public void run() {
        System.out.println("\u001B[36m🐍 НЕВИДИМАЯ ЗМЕЙКА\u001B[0m");
        System.out.println("Управление: WASD/Стрелки, P - пауза, Q - выход, R - рестарт");
        System.out.println("ВНИМАНИЕ: Тело змейки НЕВИДИМО! Запоминайте своё положение!");
        System.out.println("Нажмите Enter для начала...");
        new Scanner(System.in).nextLine();

        long lastTime = System.currentTimeMillis();
        while (running) {
            String input = inputQueue.poll();
            if (input != null) {
                char key = input.charAt(0);
                if (key == 'q' || key == 'Q') { running = false; break; }
                if (key == 'p' || key == 'P') { paused = !paused; }
                if ((key == 'r' || key == 'R') && gameOver) { resetGame(); }
                if (!paused && !gameOver) {
                    switch (key) {
                        case 'w': setDirection("up"); break;
                        case 's': setDirection("down"); break;
                        case 'a': setDirection("left"); break;
                        case 'd': setDirection("right"); break;
                    }
                }
            }

            long now = System.currentTimeMillis();
            if (now - lastTime >= speed) {
                lastTime = now;
                update();
            }
            draw();
            try { Thread.sleep(20); } catch (InterruptedException e) {}
        }
        System.out.println("\u001B[33mИгра завершена. Финальный счёт: " + score + "\u001B[0m");
    }

    public static void main(String[] args) {
        new InvisibleSnake().run();
    }
}

// invisible_snake.cpp
#include <iostream>
#include <vector>
#include <deque>
#include <string>
#include <cstdlib>
#include <ctime>
#include <thread>
#include <chrono>
#include <fstream>
#include <json/json.h> // using jsoncpp
#include <termios.h>
#include <unistd.h>
#include <fcntl.h>

using namespace std;

const int WIDTH = 20;
const int HEIGHT = 15;

struct Point {
    int x, y;
    bool operator==(const Point& other) const {
        return x == other.x && y == other.y;
    }
};

class InvisibleSnake {
private:
    deque<Point> snake;
    string direction, nextDirection;
    Point food;
    int score, highScore, level, speed;
    bool gameOver, paused, running;
    string inputBuffer;

public:
    InvisibleSnake() : score(0), highScore(0), level(1), speed(150),
                       gameOver(false), paused(false), running(true) {
        srand(time(nullptr));
        highScore = loadHighScore();
        resetGame();
        setupTerminal();
    }

    ~InvisibleSnake() {
        restoreTerminal();
    }

    void resetGame() {
        snake.clear();
        int startX = WIDTH / 2;
        int startY = HEIGHT / 2;
        snake.push_back({startX, startY});
        direction = "right";
        nextDirection = "right";
        food = spawnFood();
        score = 0;
        level = 1;
        speed = 150;
        gameOver = false;
        paused = false;
    }

    int loadHighScore() {
        ifstream ifs("snake_score.json");
        if (!ifs) return 0;
        Json::Value root;
        ifs >> root;
        return root.get("high_score", 0).asInt();
    }

    void saveHighScore() {
        Json::Value root;
        root["high_score"] = highScore;
        ofstream ofs("snake_score.json");
        ofs << root.toStyledString();
    }

    Point spawnFood() {
        while (true) {
            int x = rand() % WIDTH;
            int y = rand() % HEIGHT;
            bool found = false;
            for (auto& p : snake) {
                if (p.x == x && p.y == y) { found = true; break; }
            }
            if (!found) return {x, y};
        }
    }

    void setDirection(const string& dir) {
        if ((direction == "up" && dir != "down") ||
            (direction == "down" && dir != "up") ||
            (direction == "left" && dir != "right") ||
            (direction == "right" && dir != "left")) {
            nextDirection = dir;
        }
    }

    void update() {
        if (gameOver || paused) return;

        direction = nextDirection;
        Point head = snake.front();
        Point newHead = head;
        if (direction == "up") newHead.y--;
        else if (direction == "down") newHead.y++;
        else if (direction == "left") newHead.x--;
        else if (direction == "right") newHead.x++;

        if (newHead.x < 0 || newHead.x >= WIDTH || newHead.y < 0 || newHead.y >= HEIGHT) {
            gameOver = true;
            updateHighScore();
            return;
        }

        for (auto& p : snake) {
            if (p.x == newHead.x && p.y == newHead.y) {
                gameOver = true;
                updateHighScore();
                return;
            }
        }

        snake.push_front(newHead);

        if (newHead.x == food.x && newHead.y == food.y) {
            score += 10;
            if (score > highScore) { highScore = score; saveHighScore(); }
            food = spawnFood();
            if (score % 50 == 0) {
                speed = max(50, speed - 10);
                level++;
            }
        } else {
            snake.pop_back();
        }
    }

    void updateHighScore() {
        if (score > highScore) { highScore = score; saveHighScore(); }
    }

    void draw() {
        system("clear");
        cout << "┌" << string(WIDTH * 2 + 1, '─') << "┐" << endl;
        for (int y = 0; y < HEIGHT; y++) {
            cout << "│";
            for (int x = 0; x < WIDTH; x++) {
                bool isHead = !snake.empty() && snake.front().x == x && snake.front().y == y;
                bool isFood = food.x == x && food.y == y;
                bool isBody = false;
                for (int i = 1; i < (int)snake.size(); i++) {
                    if (snake[i].x == x && snake[i].y == y) { isBody = true; break; }
                }
                if (isHead) {
                    cout << "\033[32m🐍\033[0m";
                } else if (isFood) {
                    cout << "\033[31m🍎\033[0m";
                } else if (isBody) {
                    cout << " "; // НЕВИДИМО!
                } else {
                    cout << " ";
                }
                cout << " ";
            }
            cout << "│" << endl;
        }
        cout << "└" << string(WIDTH * 2 + 1, '─') << "┘" << endl;

        string status = "Счёт: " + to_string(score) + "  Рекорд: " + to_string(highScore) + "  Уровень: " + to_string(level);
        if (gameOver) {
            status += "\033[31m  💀 ИГРА ОКОНЧЕНА! Нажмите R для рестарта\033[0m";
        } else if (paused) {
            status += "\033[33m  ⏸ ПАУЗА\033[0m";
        } else {
            status += "  Длина: " + to_string(snake.size()) + " (невидимая)";
        }
        cout << status << endl;
    }

    void setupTerminal() {
        struct termios term;
        tcgetattr(STDIN_FILENO, &term);
        term.c_lflag &= ~(ICANON | ECHO);
        tcsetattr(STDIN_FILENO, TCSANOW, &term);
        fcntl(STDIN_FILENO, F_SETFL, O_NONBLOCK);
    }

    void restoreTerminal() {
        struct termios term;
        tcgetattr(STDIN_FILENO, &term);
        term.c_lflag |= (ICANON | ECHO);
        tcsetattr(STDIN_FILENO, TCSANOW, &term);
        fcntl(STDIN_FILENO, F_SETFL, 0);
    }

    char getChar() {
        char ch;
        if (read(STDIN_FILENO, &ch, 1) > 0) {
            return ch;
        }
        return 0;
    }

    void run() {
        cout << "\033[36m🐍 НЕВИДИМАЯ ЗМЕЙКА\033[0m" << endl;
        cout << "Управление: WASD/Стрелки, P - пауза, Q - выход, R - рестарт" << endl;
        cout << "ВНИМАНИЕ: Тело змейки НЕВИДИМО! Запоминайте своё положение!" << endl;
        cout << "Нажмите Enter для начала..." << endl;
        cin.get();

        auto lastTime = chrono::steady_clock::now();
        while (running) {
            char ch = getChar();
            if (ch) {
                if (ch == 'q' || ch == 'Q') { running = false; break; }
                if (ch == 'p' || ch == 'P') { paused = !paused; }
                if ((ch == 'r' || ch == 'R') && gameOver) { resetGame(); }
                if (!paused && !gameOver) {
                    switch (ch) {
                        case 'w': setDirection("up"); break;
                        case 's': setDirection("down"); break;
                        case 'a': setDirection("left"); break;
                        case 'd': setDirection("right"); break;
                    }
                }
            }

            auto now = chrono::steady_clock::now();
            if (chrono::duration_cast<chrono::milliseconds>(now - lastTime).count() >= speed) {
                lastTime = now;
                update();
            }
            draw();
            this_thread::sleep_for(chrono::milliseconds(20));
        }
        cout << "\033[33mИгра завершена. Финальный счёт: " << score << "\033[0m" << endl;
    }
};

int main() {
    InvisibleSnake game;
    game.run();
    return 0;
}

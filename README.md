# Змейка (невидимая)

Многоязычная консольная игра «Змейка» с невидимой змейкой.  
Игрок видит только голову змейки и еду, а тело остаётся невидимым, что делает игру экстремально сложной и требует отличной памяти и пространственного мышления.

## Особенности
- Невидимое тело змейки (видна только голова и еда).
- Движение по полю с помощью клавиш WASD или стрелок.
- Автоматическое увеличение длины при поедании еды (невидимой).
- Столкновение со стенами или собственным телом приводит к поражению.
- Три уровня сложности: **Easy** (медленная скорость), **Medium**, **Hard**.
- Счётчик очков и рекорд (сохраняется в файл).
- Подсказка о положении тела (звуковая или цветовая, в зависимости от языка).
- Возможность паузы и перезапуска игры.
- Цветной вывод в терминале (где поддерживается).
- Кроссплатформенность (Windows, Linux, macOS).

## Установка и запуск
Для каждого языка требуются соответствующие инструменты и зависимости (указаны ниже).

### Запуск на разных языках

1. **Python**  
   Установка: `pip install colorama keyboard` (или `pynput`).  
   Запуск: `python invisible_snake.py`

2. **JavaScript (Node.js)**  
   Установка: `npm install chalk readline`  
   Запуск: `node invisible_snake.js`

3. **Go**  
   Запуск: `go run invisible_snake.go`

4. **Rust**  
   Сборка: `cargo build --release`  
   Запуск: `cargo run --release`

5. **Java**  
   Сборка: `javac InvisibleSnake.java`  
   Запуск: `java InvisibleSnake`

6. **C# (.NET Core)**  
   Запуск: `dotnet run`

7. **C++ (Linux)**  
   Сборка: `g++ -std=c++11 -o invisible_snake invisible_snake.cpp -lpthread`  
   Запуск: `./invisible_snake`

8. **Kotlin (JVM)**  
   Сборка: `kotlinc InvisibleSnake.kt -include-runtime -d snake.jar`  
   Запуск: `java -jar snake.jar`

## Управление
- `W` / `↑` – движение вверх
- `A` / `←` – движение влево
- `S` / `↓` – движение вниз
- `D` / `→` – движение вправо
- `P` – пауза
- `R` – перезапуск после окончания игры
- `Q` – выход

## Структура репозитория
/
├── README.md
├── invisible_snake.py
├── invisible_snake.js
├── invisible_snake.go
├── invisible_snake.rs
├── InvisibleSnake.java
├── InvisibleSnake.cs
├── invisible_snake.cpp
└── InvisibleSnake.kt

text

## Лицензия
MIT

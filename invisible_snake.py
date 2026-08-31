#!/usr/bin/env python3
# invisible_snake.py
import random
import os
import sys
import time
import json
import threading
from collections import deque
from colorama import init, Fore, Back, Style

init(autoreset=True)

WIDTH = 20
HEIGHT = 15
FOOD_CHAR = '🍎'
HEAD_CHAR = '🐍'
BODY_CHAR = ' '  # невидимое тело!

class InvisibleSnake:
    def __init__(self):
        self.width = WIDTH
        self.height = HEIGHT
        self.reset_game()
        self.high_score = self.load_high_score()
        self.running = True
        self.input_queue = deque()

    def reset_game(self):
        self.snake = deque()
        # Начальная позиция: центр
        start_x = self.width // 2
        start_y = self.height // 2
        self.snake.append((start_x, start_y))
        self.direction = 'right'
        self.next_direction = 'right'
        self.food = self.spawn_food()
        self.score = 0
        self.game_over = False
        self.paused = False
        self.speed = 0.15  # секунды между ходами
        self.level = 1

    def load_high_score(self):
        try:
            with open('snake_score.json', 'r') as f:
                return json.load(f).get('high_score', 0)
        except:
            return 0

    def save_high_score(self):
        with open('snake_score.json', 'w') as f:
            json.dump({'high_score': self.high_score}, f)

    def spawn_food(self):
        while True:
            x = random.randint(0, self.width - 1)
            y = random.randint(0, self.height - 1)
            if (x, y) not in self.snake:
                return (x, y)

    def set_direction(self, direction):
        opposites = {'up': 'down', 'down': 'up', 'left': 'right', 'right': 'left'}
        if direction != opposites.get(self.direction, ''):
            self.next_direction = direction

    def update(self):
        if self.game_over or self.paused:
            return

        self.direction = self.next_direction
        head_x, head_y = self.snake[0]
        if self.direction == 'up':
            head_y -= 1
        elif self.direction == 'down':
            head_y += 1
        elif self.direction == 'left':
            head_x -= 1
        elif self.direction == 'right':
            head_x += 1

        # Столкновение со стенами
        if head_x < 0 or head_x >= self.width or head_y < 0 or head_y >= self.height:
            self.game_over = True
            self.update_high_score()
            return

        new_head = (head_x, head_y)

        # Проверка столкновения с телом (невидимым)
        if new_head in self.snake:
            self.game_over = True
            self.update_high_score()
            return

        self.snake.appendleft(new_head)

        # Проверка еды
        if new_head == self.food:
            self.score += 10
            if self.score > self.high_score:
                self.high_score = self.score
                self.save_high_score()
            self.food = self.spawn_food()
            # Увеличение скорости
            if self.score % 50 == 0:
                self.speed = max(0.05, self.speed - 0.01)
                self.level += 1
        else:
            # Убираем хвост (невидимый)
            self.snake.pop()

    def update_high_score(self):
        if self.score > self.high_score:
            self.high_score = self.score
            self.save_high_score()

    def draw(self):
        os.system('cls' if os.name == 'nt' else 'clear')
        # Верхняя граница
        print('┌' + '─' * (self.width * 2 + 1) + '┐')

        for y in range(self.height):
            line = '│'
            for x in range(self.width):
                pos = (x, y)
                if pos == self.snake[0]:
                    # Голова видна
                    line += Fore.GREEN + HEAD_CHAR + Style.RESET_ALL
                elif pos == self.food:
                    line += Fore.RED + FOOD_CHAR + Style.RESET_ALL
                elif pos in self.snake:
                    # Тело НЕВИДИМО! (используем пробел, но с фоном, чтобы занимать место)
                    line += ' '  # абсолютно невидимо
                else:
                    line += ' '
                line += ' '
            line += '│'
            print(line)

        print('└' + '─' * (self.width * 2 + 1) + '┘')

        # Информация
        status = f"Счёт: {self.score}  Рекорд: {self.high_score}  Уровень: {self.level}"
        if self.game_over:
            status += Fore.RED + "  💀 ИГРА ОКОНЧЕНА! Нажмите R для рестарта" + Style.RESET_ALL
        elif self.paused:
            status += Fore.YELLOW + "  ⏸ ПАУЗА" + Style.RESET_ALL
        else:
            # Подсказка: отображаем длину змейки (визуализация невидимости)
            status += f"  Длина: {len(self.snake)} (невидимая)"
        print(status)

    def handle_input(self):
        # Неблокирующий ввод через отдельный поток
        try:
            import keyboard
            while self.running:
                event = keyboard.read_event()
                if event.event_type == 'down':
                    self.input_queue.append(event.name)
        except ImportError:
            # fallback
            import msvcrt if os.name == 'nt' else select, sys, tty, termios
            if os.name == 'nt':
                while self.running:
                    if msvcrt.kbhit():
                        ch = msvcrt.getch().decode('utf-8', errors='ignore')
                        self.input_queue.append(ch)
                    time.sleep(0.05)
            else:
                fd = sys.stdin.fileno()
                old = termios.tcgetattr(fd)
                try:
                    tty.setraw(fd)
                    while self.running:
                        if select.select([sys.stdin], [], [], 0.05)[0]:
                            ch = sys.stdin.read(1)
                            self.input_queue.append(ch)
                finally:
                    termios.tcsetattr(fd, termios.TCSADRAIN, old)

    def run(self):
        print(Fore.CYAN + "🐍 НЕВИДИМАЯ ЗМЕЙКА" + Style.RESET_ALL)
        print("Управление: WASD/Стрелки, P - пауза, Q - выход, R - рестарт")
        print("ВНИМАНИЕ: Тело змейки НЕВИДИМО! Запоминайте своё положение!")
        print("Нажмите Enter для начала...")
        input()

        # Запускаем поток ввода
        threading.Thread(target=self.handle_input, daemon=True).start()

        last_time = time.time()
        while self.running:
            # Обработка ввода
            while self.input_queue:
                key = self.input_queue.popleft()
                if key == 'q' or key == 'Q':
                    self.running = False
                elif key == 'p' or key == 'P':
                    self.paused = not self.paused
                elif key == 'r' or key == 'R' and self.game_over:
                    self.reset_game()
                elif not self.paused and not self.game_over:
                    if key in ['w', 'up']:
                        self.set_direction('up')
                    elif key in ['s', 'down']:
                        self.set_direction('down')
                    elif key in ['a', 'left']:
                        self.set_direction('left')
                    elif key in ['d', 'right']:
                        self.set_direction('right')

            # Обновление игры
            current_time = time.time()
            if current_time - last_time >= self.speed:
                last_time = current_time
                self.update()

            self.draw()
            time.sleep(0.02)

        print(Fore.YELLOW + f"Игра завершена. Финальный счёт: {self.score}" + Style.RESET_ALL)

if __name__ == "__main__":
    game = InvisibleSnake()
    game.run()

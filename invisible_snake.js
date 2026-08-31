#!/usr/bin/env node
// invisible_snake.js
const readline = require('readline');
const fs = require('fs');
const chalk = require('chalk');

const WIDTH = 20;
const HEIGHT = 15;
const FOOD_CHAR = '🍎';
const HEAD_CHAR = '🐍';

class InvisibleSnake {
    constructor() {
        this.width = WIDTH;
        this.height = HEIGHT;
        this.resetGame();
        this.highScore = this.loadHighScore();
        this.running = true;
        this.inputQueue = [];
        this.rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });
        readline.emitKeypressEvents(process.stdin);
        process.stdin.setRawMode(true);
        this.setupInput();
    }

    resetGame() {
        this.snake = [];
        const startX = Math.floor(this.width / 2);
        const startY = Math.floor(this.height / 2);
        this.snake.push([startX, startY]);
        this.direction = 'right';
        this.nextDirection = 'right';
        this.food = this.spawnFood();
        this.score = 0;
        this.gameOver = false;
        this.paused = false;
        this.speed = 150; // мс
        this.level = 1;
    }

    loadHighScore() {
        try {
            const data = JSON.parse(fs.readFileSync('snake_score.json'));
            return data.high_score || 0;
        } catch { return 0; }
    }

    saveHighScore() {
        fs.writeFileSync('snake_score.json', JSON.stringify({ high_score: this.highScore }));
    }

    spawnFood() {
        while (true) {
            const x = Math.floor(Math.random() * this.width);
            const y = Math.floor(Math.random() * this.height);
            if (!this.snake.some(pos => pos[0] === x && pos[1] === y)) {
                return [x, y];
            }
        }
    }

    setDirection(dir) {
        const opposites = { up: 'down', down: 'up', left: 'right', right: 'left' };
        if (dir !== opposites[this.direction]) {
            this.nextDirection = dir;
        }
    }

    update() {
        if (this.gameOver || this.paused) return;

        this.direction = this.nextDirection;
        const head = this.snake[0];
        let newHead = [...head];
        switch (this.direction) {
            case 'up': newHead[1]--; break;
            case 'down': newHead[1]++; break;
            case 'left': newHead[0]--; break;
            case 'right': newHead[0]++; break;
        }

        // Стены
        if (newHead[0] < 0 || newHead[0] >= this.width || newHead[1] < 0 || newHead[1] >= this.height) {
            this.gameOver = true;
            this.updateHighScore();
            return;
        }

        // Столкновение с телом
        if (this.snake.some(pos => pos[0] === newHead[0] && pos[1] === newHead[1])) {
            this.gameOver = true;
            this.updateHighScore();
            return;
        }

        this.snake.unshift(newHead);

        // Еда
        if (newHead[0] === this.food[0] && newHead[1] === this.food[1]) {
            this.score += 10;
            if (this.score > this.highScore) {
                this.highScore = this.score;
                this.saveHighScore();
            }
            this.food = this.spawnFood();
            if (this.score % 50 === 0) {
                this.speed = Math.max(50, this.speed - 10);
                this.level++;
            }
        } else {
            this.snake.pop();
        }
    }

    updateHighScore() {
        if (this.score > this.highScore) {
            this.highScore = this.score;
            this.saveHighScore();
        }
    }

    draw() {
        console.clear();
        console.log('┌' + '─'.repeat(this.width * 2 + 1) + '┐');
        for (let y = 0; y < this.height; y++) {
            let line = '│';
            for (let x = 0; x < this.width; x++) {
                const pos = [x, y];
                const isHead = this.snake.length > 0 && pos[0] === this.snake[0][0] && pos[1] === this.snake[0][1];
                const isFood = pos[0] === this.food[0] && pos[1] === this.food[1];
                const isBody = this.snake.some(p => p[0] === pos[0] && p[1] === pos[1]);

                if (isHead) {
                    line += chalk.green(HEAD_CHAR);
                } else if (isFood) {
                    line += chalk.red(FOOD_CHAR);
                } else if (isBody) {
                    line += ' '; // НЕВИДИМО!
                } else {
                    line += ' ';
                }
                line += ' ';
            }
            line += '│';
            console.log(line);
        }
        console.log('└' + '─'.repeat(this.width * 2 + 1) + '┘');

        let status = `Счёт: ${this.score}  Рекорд: ${this.highScore}  Уровень: ${this.level}`;
        if (this.gameOver) {
            status += chalk.red('  💀 ИГРА ОКОНЧЕНА! Нажмите R для рестарта');
        } else if (this.paused) {
            status += chalk.yellow('  ⏸ ПАУЗА');
        } else {
            status += `  Длина: ${this.snake.length} (невидимая)`;
        }
        console.log(status);
    }

    setupInput() {
        process.stdin.on('keypress', (str, key) => {
            if (!key) return;
            const name = key.name;
            if (name === 'q') { this.running = false; process.exit(0); }
            if (name === 'p') { this.paused = !this.paused; }
            if (name === 'r' && this.gameOver) { this.resetGame(); }
            if (!this.paused && !this.gameOver) {
                if (name === 'up' || name === 'w') this.setDirection('up');
                else if (name === 'down' || name === 's') this.setDirection('down');
                else if (name === 'left' || name === 'a') this.setDirection('left');
                else if (name === 'right' || name === 'd') this.setDirection('right');
            }
        });
    }

    run() {
        console.log(chalk.cyan('🐍 НЕВИДИМАЯ ЗМЕЙКА'));
        console.log('Управление: WASD/Стрелки, P - пауза, Q - выход, R - рестарт');
        console.log('ВНИМАНИЕ: Тело змейки НЕВИДИМО! Запоминайте своё положение!');
        console.log('Нажмите Enter для начала...');
        process.stdin.once('keypress', () => {
            let lastTime = Date.now();
            const loop = () => {
                if (!this.running) return;
                const now = Date.now();
                if (now - lastTime >= this.speed) {
                    lastTime = now;
                    this.update();
                }
                this.draw();
                setTimeout(loop, 20);
            };
            loop();
        });
    }
}

const game = new InvisibleSnake();
game.run();

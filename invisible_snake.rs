// invisible_snake.rs
use rand::Rng;
use std::collections::VecDeque;
use std::fs;
use std::io::{self, Write, Read};
use std::time::{Duration, Instant};
use termion::raw::IntoRawMode;
use termion::input::TermRead;
use colored::*;

const WIDTH: usize = 20;
const HEIGHT: usize = 15;

#[derive(Clone, Copy, PartialEq)]
struct Point {
    x: usize,
    y: usize,
}

struct Snake {
    body: VecDeque<Point>,
    direction: Direction,
    next_direction: Direction,
    food: Point,
    score: u32,
    high_score: u32,
    level: u32,
    speed: u64, // milliseconds
    game_over: bool,
    paused: bool,
    running: bool,
    width: usize,
    height: usize,
}

#[derive(Clone, Copy, PartialEq)]
enum Direction {
    Up, Down, Left, Right,
}

impl Snake {
    fn new() -> Self {
        let mut snake = Snake {
            width: WIDTH,
            height: HEIGHT,
            speed: 150,
            running: true,
            high_score: 0,
            game_over: false,
            paused: false,
            level: 1,
            score: 0,
            direction: Direction::Right,
            next_direction: Direction::Right,
            body: VecDeque::new(),
            food: Point { x: 0, y: 0 },
        };
        snake.reset_game();
        snake.high_score = snake.load_high_score();
        snake
    }

    fn reset_game(&mut self) {
        let start_x = self.width / 2;
        let start_y = self.height / 2;
        self.body.clear();
        self.body.push_back(Point { x: start_x, y: start_y });
        self.direction = Direction::Right;
        self.next_direction = Direction::Right;
        self.food = self.spawn_food();
        self.score = 0;
        self.game_over = false;
        self.paused = false;
        self.level = 1;
        self.speed = 150;
    }

    fn load_high_score(&self) -> u32 {
        if let Ok(data) = fs::read_to_string("snake_score.json") {
            if let Ok(json) = serde_json::from_str::<serde_json::Value>(&data) {
                if let Some(score) = json.get("high_score").and_then(|v| v.as_u64()) {
                    return score as u32;
                }
            }
        }
        0
    }

    fn save_high_score(&self) {
        let json = serde_json::json!({ "high_score": self.high_score });
        let _ = fs::write("snake_score.json", serde_json::to_string_pretty(&json).unwrap());
    }

    fn spawn_food(&self) -> Point {
        let mut rng = rand::thread_rng();
        loop {
            let x = rng.gen_range(0..self.width);
            let y = rng.gen_range(0..self.height);
            if !self.body.iter().any(|p| p.x == x && p.y == y) {
                return Point { x, y };
            }
        }
    }

    fn set_direction(&mut self, dir: Direction) {
        let opposites = [
            (Direction::Up, Direction::Down),
            (Direction::Down, Direction::Up),
            (Direction::Left, Direction::Right),
            (Direction::Right, Direction::Left),
        ];
        for (a, b) in opposites {
            if self.direction == a && dir == b {
                return;
            }
        }
        self.next_direction = dir;
    }

    fn update(&mut self) {
        if self.game_over || self.paused {
            return;
        }

        self.direction = self.next_direction;
        let head = self.body.front().unwrap();
        let mut new_head = *head;
        match self.direction {
            Direction::Up => new_head.y -= 1,
            Direction::Down => new_head.y += 1,
            Direction::Left => new_head.x -= 1,
            Direction::Right => new_head.x += 1,
        }

        if new_head.x >= self.width || new_head.y >= self.height {
            self.game_over = true;
            self.update_high_score();
            return;
        }

        for &p in &self.body {
            if p.x == new_head.x && p.y == new_head.y {
                self.game_over = true;
                self.update_high_score();
                return;
            }
        }

        self.body.push_front(new_head);

        if new_head.x == self.food.x && new_head.y == self.food.y {
            self.score += 10;
            if self.score > self.high_score {
                self.high_score = self.score;
                self.save_high_score();
            }
            self.food = self.spawn_food();
            if self.score % 50 == 0 {
                if self.speed > 50 {
                    self.speed -= 10;
                }
                self.level += 1;
            }
        } else {
            self.body.pop_back();
        }
    }

    fn update_high_score(&mut self) {
        if self.score > self.high_score {
            self.high_score = self.score;
            self.save_high_score();
        }
    }

    fn draw(&self, stdout: &mut termion::raw::RawTerminal<std::io::Stdout>) {
        write!(stdout, "{}", termion::clear::All).unwrap();
        write!(stdout, "{}", termion::cursor::Goto(1, 1)).unwrap();
        println!("┌{}┐", "─".repeat(self.width * 2 + 1));
        for y in 0..self.height {
            print!("│");
            for x in 0..self.width {
                let is_head = self.body.front().map(|p| p.x == x && p.y == y).unwrap_or(false);
                let is_food = self.food.x == x && self.food.y == y;
                let is_body = self.body.iter().any(|p| p.x == x && p.y == y);
                if is_head {
                    print!("{}", "🐍".green());
                } else if is_food {
                    print!("{}", "🍎".red());
                } else if is_body {
                    print!(" "); // НЕВИДИМО!
                } else {
                    print!(" ");
                }
                print!(" ");
            }
            println!("│");
        }
        println!("└{}┘", "─".repeat(self.width * 2 + 1));

        let mut status = format!("Счёт: {}  Рекорд: {}  Уровень: {}", self.score, self.high_score, self.level);
        if self.game_over {
            status += &format!("{}", "  💀 ИГРА ОКОНЧЕНА! Нажмите R для рестарта".red());
        } else if self.paused {
            status += &format!("{}", "  ⏸ ПАУЗА".yellow());
        } else {
            status += &format!("  Длина: {} (невидимая)", self.body.len());
        }
        println!("{}", status);
        stdout.flush().unwrap();
    }

    fn run(&mut self) {
        let mut stdout = io::stdout().into_raw_mode().unwrap();
        let stdin = io::stdin();
        let mut keys = stdin.keys();

        write!(stdout, "{}", termion::clear::All).unwrap();
        write!(stdout, "{}", termion::cursor::Goto(1, 1)).unwrap();
        println!("{}", "🐍 НЕВИДИМАЯ ЗМЕЙКА".cyan());
        println!("Управление: WASD/Стрелки, P - пауза, Q - выход, R - рестарт");
        println!("ВНИМАНИЕ: Тело змейки НЕВИДИМО! Запоминайте своё положение!");
        println!("Нажмите любую клавишу для начала...");
        stdout.flush().unwrap();
        keys.next();

        let mut last_update = Instant::now();
        while self.running {
            if let Some(Ok(key)) = keys.next() {
                match key {
                    termion::event::Key::Char('q') | termion::event::Key::Char('Q') => self.running = false,
                    termion::event::Key::Char('p') | termion::event::Key::Char('P') => self.paused = !self.paused,
                    termion::event::Key::Char('r') | termion::event::Key::Char('R') if self.game_over => {
                        self.reset_game();
                    }
                    termion::event::Key::Up | termion::event::Key::Char('w') => self.set_direction(Direction::Up),
                    termion::event::Key::Down | termion::event::Key::Char('s') => self.set_direction(Direction::Down),
                    termion::event::Key::Left | termion::event::Key::Char('a') => self.set_direction(Direction::Left),
                    termion::event::Key::Right | termion::event::Key::Char('d') => self.set_direction(Direction::Right),
                    _ => {}
                }
            }

            let now = Instant::now();
            if now.duration_since(last_update).as_millis() >= self.speed as u128 {
                last_update = now;
                self.update();
            }
            self.draw(&mut stdout);
            std::thread::sleep(Duration::from_millis(20));
        }
        println!("{}", format!("Игра завершена. Финальный счёт: {}", self.score).yellow());
    }
}

fn main() {
    let mut snake = Snake::new();
    snake.run();
}

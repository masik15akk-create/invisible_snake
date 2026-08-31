// invisible_snake.go
package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"math/rand"
	"os"
	"os/exec"
	"runtime"
	"strconv"
	"strings"
	"time"
)

const WIDTH = 20
const HEIGHT = 15
const FOOD_CHAR = "🍎"
const HEAD_CHAR = "🐍"

type Point struct {
	X, Y int
}

type Snake struct {
	Body          []Point
	Direction     string
	NextDirection string
	Food          Point
	Score         int
	HighScore     int
	Level         int
	Speed         int // milliseconds
	GameOver      bool
	Paused        bool
	Running       bool
	Width         int
	Height        int
	inputChan     chan string
}

func NewSnake() *Snake {
	s := &Snake{
		Width:      WIDTH,
		Height:     HEIGHT,
		Speed:      150,
		Running:    true,
		inputChan:  make(chan string, 10),
		HighScore:  0,
	}
	s.resetGame()
	s.HighScore = s.loadHighScore()
	return s
}

func (s *Snake) resetGame() {
	startX := s.Width / 2
	startY := s.Height / 2
	s.Body = []Point{{startX, startY}}
	s.Direction = "right"
	s.NextDirection = "right"
	s.Food = s.spawnFood()
	s.Score = 0
	s.GameOver = false
	s.Paused = false
	s.Level = 1
	s.Speed = 150
}

func (s *Snake) loadHighScore() int {
	data, err := os.ReadFile("snake_score.json")
	if err != nil {
		return 0
	}
	var score map[string]int
	if err := json.Unmarshal(data, &score); err != nil {
		return 0
	}
	return score["high_score"]
}

func (s *Snake) saveHighScore() {
	data, _ := json.Marshal(map[string]int{"high_score": s.HighScore})
	os.WriteFile("snake_score.json", data, 0644)
}

func (s *Snake) spawnFood() Point {
	for {
		x := rand.Intn(s.Width)
		y := rand.Intn(s.Height)
		found := false
		for _, p := range s.Body {
			if p.X == x && p.Y == y {
				found = true
				break
			}
		}
		if !found {
			return Point{x, y}
		}
	}
}

func (s *Snake) setDirection(dir string) {
	opposites := map[string]string{"up": "down", "down": "up", "left": "right", "right": "left"}
	if dir != opposites[s.Direction] {
		s.NextDirection = dir
	}
}

func (s *Snake) update() {
	if s.GameOver || s.Paused {
		return
	}

	s.Direction = s.NextDirection
	head := s.Body[0]
	newHead := head
	switch s.Direction {
	case "up":
		newHead.Y--
	case "down":
		newHead.Y++
	case "left":
		newHead.X--
	case "right":
		newHead.X++
	}

	if newHead.X < 0 || newHead.X >= s.Width || newHead.Y < 0 || newHead.Y >= s.Height {
		s.GameOver = true
		s.updateHighScore()
		return
	}

	for _, p := range s.Body {
		if p.X == newHead.X && p.Y == newHead.Y {
			s.GameOver = true
			s.updateHighScore()
			return
		}
	}

	s.Body = append([]Point{newHead}, s.Body...)

	if newHead.X == s.Food.X && newHead.Y == s.Food.Y {
		s.Score += 10
		if s.Score > s.HighScore {
			s.HighScore = s.Score
			s.saveHighScore()
		}
		s.Food = s.spawnFood()
		if s.Score%50 == 0 {
			if s.Speed > 50 {
				s.Speed -= 10
			}
			s.Level++
		}
	} else {
		s.Body = s.Body[:len(s.Body)-1]
	}
}

func (s *Snake) updateHighScore() {
	if s.Score > s.HighScore {
		s.HighScore = s.Score
		s.saveHighScore()
	}
}

func (s *Snake) draw() {
	clearScreen()
	fmt.Println("┌" + strings.Repeat("─", s.Width*2+1) + "┐")
	for y := 0; y < s.Height; y++ {
		fmt.Print("│")
		for x := 0; x < s.Width; x++ {
			pos := Point{x, y}
			isHead := len(s.Body) > 0 && s.Body[0].X == x && s.Body[0].Y == y
			isFood := s.Food.X == x && s.Food.Y == y
			isBody := false
			for _, p := range s.Body {
				if p.X == x && p.Y == y {
					isBody = true
					break
				}
			}
			if isHead {
				fmt.Print("\033[32m" + HEAD_CHAR + "\033[0m")
			} else if isFood {
				fmt.Print("\033[31m" + FOOD_CHAR + "\033[0m")
			} else if isBody {
				fmt.Print(" ") // НЕВИДИМО!
			} else {
				fmt.Print(" ")
			}
			fmt.Print(" ")
		}
		fmt.Println("│")
	}
	fmt.Println("└" + strings.Repeat("─", s.Width*2+1) + "┘")

	status := fmt.Sprintf("Счёт: %d  Рекорд: %d  Уровень: %d", s.Score, s.HighScore, s.Level)
	if s.GameOver {
		status += "\033[31m  💀 ИГРА ОКОНЧЕНА! Нажмите R для рестарта\033[0m"
	} else if s.Paused {
		status += "\033[33m  ⏸ ПАУЗА\033[0m"
	} else {
		status += fmt.Sprintf("  Длина: %d (невидимая)", len(s.Body))
	}
	fmt.Println(status)
}

func clearScreen() {
	cmd := exec.Command("clear")
	if runtime.GOOS == "windows" {
		cmd = exec.Command("cmd", "/c", "cls")
	}
	cmd.Stdout = os.Stdout
	cmd.Run()
}

func (s *Snake) inputLoop() {
	reader := bufio.NewReader(os.Stdin)
	for s.Running {
		ch, err := reader.ReadByte()
		if err != nil {
			continue
		}
		s.inputChan <- string(ch)
	}
}

func (s *Snake) run() {
	fmt.Println("\033[36m🐍 НЕВИДИМАЯ ЗМЕЙКА\033[0m")
	fmt.Println("Управление: WASD/Стрелки, P - пауза, Q - выход, R - рестарт")
	fmt.Println("ВНИМАНИЕ: Тело змейки НЕВИДИМО! Запоминайте своё положение!")
	fmt.Println("Нажмите Enter для начала...")
	fmt.Scanln()

	go s.inputLoop()

	lastTime := time.Now()
	for s.Running {
		select {
		case key := <-s.inputChan:
			switch key {
			case "q", "Q":
				s.Running = false
			case "p", "P":
				s.Paused = !s.Paused
			case "r", "R":
				if s.GameOver {
					s.resetGame()
				}
			default:
				if !s.Paused && !s.GameOver {
					switch key {
					case 'w', 'W':
						s.setDirection("up")
					case 's', 'S':
						s.setDirection("down")
					case 'a', 'A':
						s.setDirection("left")
					case 'd', 'D':
						s.setDirection("right")
					}
				}
			}
		default:
		}

		now := time.Now()
		if int(now.Sub(lastTime).Milliseconds()) >= s.Speed {
			lastTime = now
			s.update()
		}
		s.draw()
		time.Sleep(20 * time.Millisecond)
	}
	fmt.Printf("\033[33mИгра завершена. Финальный счёт: %d\033[0m\n", s.Score)
}

func main() {
	rand.Seed(time.Now().UnixNano())
	snake := NewSnake()
	snake.run()
}

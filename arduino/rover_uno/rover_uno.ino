/*
 * rover_uno.ino — Sketch Unificado (1x Arduino UNO)
 *
 * Pinos:
 *   0 (RX) ← HC-05 TXD      (Hardware Serial — desconecte antes de gravar!)
 *   1 (TX) → HC-05 RXD
 *   2      → L298N IN1       (motor esquerdo dir.)
 *   3  PWM → L298N ENA       (motor esquerdo vel.)  ← remover jumper
 *   4      → L298N IN2
 *   5  PWM → L298N ENB       (motor direito vel.)   ← remover jumper
 *   6      → L298N IN3       (motor direito dir.)
 *   7      → L298N IN4
 *   8      → Servo A1 Base
 *   9      → Servo A2 Ombro
 *  10      → Servo A3 Cotovelo
 *  11      → Servo A4 Pulso pitch
 *  12      → Servo A5 Pulso roll
 *  A0      → Servo A6 Garra
 *
 * Alimentação:
 *   Bateria 11.1V → L298N VMS + buck 5V
 *   L298N 5V out  → Arduino Vin
 *   Arduino 5V    → HC-05 VCC
 *   Buck 5V out   → todos os servos VCC
 *   GND comum     → L298N GND, HC-05 GND, servos GND, Arduino GND
 */

#include <Servo.h>

// ── L298N ──────────────────────────────────────────────────────────────────────
const uint8_t IN1 = 2;
const uint8_t ENA = 3;   // PWM (Timer2 — não conflita com Servo/Timer1)
const uint8_t IN2 = 4;
const uint8_t ENB = 5;   // PWM (Timer0)
const uint8_t IN3 = 6;
const uint8_t IN4 = 7;

// ── Servos ─────────────────────────────────────────────────────────────────────
static const uint8_t NUM_SERVOS = 6;
static const uint8_t SERVO_PINS[NUM_SERVOS]  = { 8, 9, 10, 11, 12, A0 };
static const uint8_t SERVO_MIN[NUM_SERVOS]   = {  0,  30,  20,   0,   0,   0 };
static const uint8_t SERVO_MAX[NUM_SERVOS]   = { 180, 150, 160, 180, 180, 180 };
static const uint8_t SERVO_HOME[NUM_SERVOS]  = {  90,  90,  90,  90,  90,  90 };

Servo   servo[NUM_SERVOS];
uint8_t servoPos[NUM_SERVOS];

// ── Gravação de movimentos ─────────────────────────────────────────────────────
static const uint8_t  MAX_FRAMES  = 50;
static const uint16_t FRAME_DELAY = 500;

struct Frame {
  uint8_t  pos[NUM_SERVOS];
  uint16_t delayMs;
};

Frame    recording[MAX_FRAMES];
uint8_t  recordCount = 0;
uint8_t  playRepeat  = 1;
uint16_t frameDelay  = FRAME_DELAY;
bool     isRecording = false;
bool     isPlaying   = false;

// ── Estado dos motores ─────────────────────────────────────────────────────────
int motorSpeed = 180;   // 0–255

// ══════════════════════════════════════════════════════════════════════════════
void setup() {
  pinMode(IN1, OUTPUT); pinMode(IN2, OUTPUT); pinMode(ENA, OUTPUT);
  pinMode(IN3, OUTPUT); pinMode(IN4, OUTPUT); pinMode(ENB, OUTPUT);
  stopMotors();

  for (uint8_t i = 0; i < NUM_SERVOS; i++) {
    servo[i].attach(SERVO_PINS[i]);
    servoPos[i] = SERVO_HOME[i];
    servo[i].write(servoPos[i]);
  }

  Serial.begin(9600);
}

void loop() {
  if (Serial.available()) {
    String cmd = Serial.readStringUntil('\n');
    cmd.trim();
    if (cmd.length() > 0) processCommand(cmd);
  }

  if (isPlaying) runPlayback();
}

// ── Roteador principal ─────────────────────────────────────────────────────────
void processCommand(const String& cmd) {
  if      (cmd.startsWith("MOV:")) handleMovement(cmd.substring(4));
  else if (cmd.startsWith("SPD:")) motorSpeed = constrain(cmd.substring(4).toInt(), 0, 255);
  else if (cmd.startsWith("SRV:")) handleServo(cmd.substring(4));
  else if (cmd.startsWith("ALL:")) handleAll(cmd.substring(4));
  else if (cmd.startsWith("BTN:")) handleButton(cmd.substring(4));
  else if (cmd.startsWith("REC:")) handleRecord(cmd.substring(4));
  else if (cmd.startsWith("SET:")) handleSettings(cmd.substring(4));
}

// ── Motores ────────────────────────────────────────────────────────────────────
void handleMovement(const String& action) {
  if      (action == "F")  { setLeftMotor(motorSpeed, true);  setRightMotor(motorSpeed, true);  }
  else if (action == "B")  { setLeftMotor(motorSpeed, false); setRightMotor(motorSpeed, false); }
  else if (action == "L")  { setLeftMotor(motorSpeed, false); setRightMotor(motorSpeed, true);  }
  else if (action == "R")  { setLeftMotor(motorSpeed, true);  setRightMotor(motorSpeed, false); }
  else if (action == "FL") { setLeftMotor(motorSpeed / 2, true);  setRightMotor(motorSpeed, true);  }
  else if (action == "FR") { setLeftMotor(motorSpeed, true);  setRightMotor(motorSpeed / 2, true);  }
  else if (action == "BL") { setLeftMotor(motorSpeed / 2, false); setRightMotor(motorSpeed, false); }
  else if (action == "BR") { setLeftMotor(motorSpeed, false); setRightMotor(motorSpeed / 2, false); }
  else if (action == "S")  stopMotors();
  else if (action.startsWith("JOY:")) handleJoystick(action.substring(4));
}

// Formato enviado pelo app: MOV:JOY:left:right  (valores -255..255)
void handleJoystick(const String& params) {
  int sep = params.indexOf(':');
  if (sep < 0) return;
  int leftVal  = params.substring(0, sep).toInt();
  int rightVal = params.substring(sep + 1).toInt();
  setLeftMotor(abs(leftVal),  leftVal  >= 0);
  setRightMotor(abs(rightVal), rightVal >= 0);
}

void stopMotors() {
  setLeftMotor(0, true);
  setRightMotor(0, true);
}

void setLeftMotor(int spd, bool fwd) {
  digitalWrite(IN1, fwd ? HIGH : LOW);
  digitalWrite(IN2, fwd ? LOW  : HIGH);
  analogWrite(ENA, constrain(spd, 0, 255));
}

void setRightMotor(int spd, bool fwd) {
  digitalWrite(IN3, fwd ? HIGH : LOW);
  digitalWrite(IN4, fwd ? LOW  : HIGH);
  analogWrite(ENB, constrain(spd, 0, 255));
}

// ── Servos ─────────────────────────────────────────────────────────────────────
// Formato: SRV:1:90  (servo 1–6, ângulo 0–180)
void handleServo(const String& params) {
  int col = params.indexOf(':');
  if (col < 0) return;
  int idx   = params.substring(0, col).toInt() - 1;
  int angle = params.substring(col + 1).toInt();
  moveServo(idx, angle);
  if (isRecording && recordCount < MAX_FRAMES) captureFrame();
}

// Formato: ALL:90,90,90,90,90,90
void handleAll(const String& params) {
  String  p   = params;
  uint8_t idx = 0;
  while (p.length() > 0 && idx < NUM_SERVOS) {
    int comma = p.indexOf(',');
    int angle = (comma >= 0) ? p.substring(0, comma).toInt() : p.toInt();
    moveServo(idx++, angle);
    if (comma < 0) break;
    p = p.substring(comma + 1);
  }
  if (isRecording && recordCount < MAX_FRAMES) captureFrame();
}

void handleButton(const String& btn) {
  if      (btn == "B")               moveServo(5, 180);  // garra abre
  else if (btn == "b")               moveServo(5, 0);    // garra fecha
  else if (btn == "C" || btn == "c") goHome();
  else if (btn == "D")               goPreset(0);
  else if (btn == "d")               goPreset(1);
  else if (btn == "E")               goPreset(2);
  else if (btn == "e")               goPreset(3);
}

void moveServo(uint8_t idx, int angle) {
  if (idx >= NUM_SERVOS) return;
  angle = constrain(angle, SERVO_MIN[idx], SERVO_MAX[idx]);
  servo[idx].write(angle);
  servoPos[idx] = (uint8_t)angle;
}

void goHome() {
  for (uint8_t i = 0; i < NUM_SERVOS; i++) {
    moveServo(i, SERVO_HOME[i]);
    delay(15);
  }
}

// Posições pré-definidas — ajuste conforme a mecânica do seu braço
const uint8_t PRESETS[][NUM_SERVOS] = {
  {  90,  60,  90, 90, 90,   0 },   // D: braço estendido, garra fechada
  {  90, 120,  90, 90, 90, 180 },   // d: braço recolhido, garra aberta
  {  45,  90, 120, 90, 90,  90 },   // E: lateral esquerda
  { 135,  90, 120, 90, 90,  90 },   // e: lateral direita
};

void goPreset(uint8_t idx) {
  if (idx >= 4) return;
  for (uint8_t i = 0; i < NUM_SERVOS; i++) {
    moveServo(i, PRESETS[idx][i]);
    delay(20);
  }
}

// ── Gravação / Playback ────────────────────────────────────────────────────────
void handleRecord(const String& action) {
  if (action == "START") {
    recordCount = 0;
    isRecording = true;
    isPlaying   = false;
    Serial.println("REC:STARTED");
  } else if (action == "STOP") {
    isRecording = false;
    Serial.print("REC:SAVED:");
    Serial.println(recordCount);
  } else if (action == "PLAY") {
    if (recordCount > 0) { isPlaying = true; isRecording = false; }
  } else if (action == "PAUSE") {
    isPlaying = false;
  } else if (action == "CLEAR") {
    recordCount = 0;
    isRecording = false;
    isPlaying   = false;
  }
}

void handleSettings(const String& params) {
  int col = params.indexOf(':');
  if (col < 0) return;
  String key = params.substring(0, col);
  int    val = params.substring(col + 1).toInt();
  if      (key == "DELAY")  frameDelay = constrain(val, 50, 5000);
  else if (key == "REPEAT") playRepeat = constrain(val, 1, 255);
}

void captureFrame() {
  for (uint8_t i = 0; i < NUM_SERVOS; i++) {
    recording[recordCount].pos[i] = servoPos[i];
  }
  recording[recordCount].delayMs = frameDelay;
  recordCount++;
}

void runPlayback() {
  for (uint8_t rep = 0; rep < playRepeat && isPlaying; rep++) {
    for (uint8_t f = 0; f < recordCount && isPlaying; f++) {
      for (uint8_t i = 0; i < NUM_SERVOS; i++) {
        moveServo(i, recording[f].pos[i]);
      }
      uint32_t t = millis();
      while (millis() - t < recording[f].delayMs) {
        if (Serial.available()) {
          String cmd = Serial.readStringUntil('\n');
          cmd.trim();
          if (cmd == "REC:PAUSE" || cmd == "REC:STOP") {
            isPlaying = false;
            return;
          }
        }
      }
    }
  }
  isPlaying = false;
  Serial.println("REC:DONE");
}

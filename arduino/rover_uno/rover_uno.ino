/*
 * rover_uno.ino — Sketch Unificado (1x Arduino UNO) - Simplificado
 *
 * Pinos:
 *   0 (RX) ← HC-05 TXD      (Hardware Serial — desconecte antes de gravar!)
 *   1 (TX) → HC-05 RXD
 *   2      → L298N IN1       (motor esquerdo dir.)
 *   3      → L298N IN2       (motor esquerdo dir.)
 *   4      → L298N IN3       (motor direito dir.)
 *   5      → L298N IN4       (motor direito dir.)
 *   6      → Servo A1 Base
 *   7      → Servo A2 Ombro
 *   8      → Servo A3 Cotovelo
 *   9      → Servo A4 Pulso pitch
 *  10      → Servo A5 Pulso roll
 *  11      → Servo A6 Garra
 *
 * NOTA: ENA e ENB do L298N devem ter jumpers conectados (velocidade constante)
 *
 * Alimentação:
 *   Bateria 11.1V → L298N VMS + buck 5V
 *   L298N 5V out  → Arduino Vin
 *   Arduino 5V    → HC-05 VCC
 *   Buck 5V out   → todos os servos VCC
 *   GND comum     → L298N GND, HC-05 GND, servos GND, Arduino GND
 */

#include <Servo.h>

// ── L298N (Simplificado - sem PWM) ────────────────────────────────────────────
const uint8_t IN1 = 2;
const uint8_t IN2 = 3;
const uint8_t IN3 = 4;
const uint8_t IN4 = 5;

// ── Servos ─────────────────────────────────────────────────────────────────────
static const uint8_t NUM_SERVOS = 6;
static const uint8_t SERVO_PINS[NUM_SERVOS]  = { 6, 7, 8, 9, 10, 11 };
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

// ══════════════════════════════════════════════════════════════════════════════
void setup() {
  pinMode(IN1, OUTPUT); pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT); pinMode(IN4, OUTPUT);
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
  else if (cmd.startsWith("SRV:")) handleServo(cmd.substring(4));
  else if (cmd.startsWith("ALL:")) handleAll(cmd.substring(4));
  else if (cmd.startsWith("BTN:")) handleButton(cmd.substring(4));
  else if (cmd.startsWith("REC:")) handleRecord(cmd.substring(4));
  else if (cmd.startsWith("SET:")) handleSettings(cmd.substring(4));
}

// ── Motores (Simplificado - velocidade constante) ─────────────────────────────
void handleMovement(const String& action) {
  if      (action == "F")  driveForward();
  else if (action == "B")  driveBackward();
  else if (action == "L")  pivotLeft();
  else if (action == "R")  pivotRight();
  else if (action == "FL") curveLeft();
  else if (action == "FR") curveRight();
  else if (action == "BL") curveBackLeft();
  else if (action == "BR") curveBackRight();
  else if (action == "S")  stopMotors();
  else if (action.startsWith("JOY:")) handleJoystick(action.substring(4));
}

// Formato enviado pelo app: MOV:JOY:left:right  (valores -255..255)
void handleJoystick(const String& params) {
  int sep = params.indexOf(':');
  if (sep < 0) return;
  int leftVal  = params.substring(0, sep).toInt();
  int rightVal = params.substring(sep + 1).toInt();
  
  // Direção baseada no sinal (velocidade constante quando ativo)
  bool leftFwd  = leftVal >= 0;
  bool rightFwd = rightVal >= 0;
  
  // Só move se o valor absoluto for significativo (>30 para evitar ruído)
  if (abs(leftVal) > 30)  setLeftMotor(true, leftFwd);
  else                    setLeftMotor(false, true);
  
  if (abs(rightVal) > 30) setRightMotor(true, rightFwd);
  else                    setRightMotor(false, true);
}

void driveForward()  { setLeftMotor(true, true);  setRightMotor(true, true);  }
void driveBackward() { setLeftMotor(true, false); setRightMotor(true, false); }
void pivotLeft()     { setLeftMotor(true, false); setRightMotor(true, true);  }
void pivotRight()    { setLeftMotor(true, true);  setRightMotor(true, false); }
void curveLeft()     { setLeftMotor(false, true); setRightMotor(true, true);  }
void curveRight()    { setLeftMotor(true, true);  setRightMotor(false, true); }
void curveBackLeft() { setLeftMotor(false, false); setRightMotor(true, false); }
void curveBackRight(){ setLeftMotor(true, false); setRightMotor(false, false); }

void stopMotors() {
  setLeftMotor(false, true);
  setRightMotor(false, true);
}

// enable: true = motor ligado, false = motor desligado
// fwd: true = frente, false = ré
void setLeftMotor(bool enable, bool fwd) {
  digitalWrite(IN1, enable ? (fwd ? HIGH : LOW) : LOW);
  digitalWrite(IN2, enable ? (fwd ? LOW  : HIGH) : LOW);
}

void setRightMotor(bool enable, bool fwd) {
  digitalWrite(IN3, enable ? (fwd ? HIGH : LOW) : LOW);
  digitalWrite(IN4, enable ? (fwd ? LOW  : HIGH) : LOW);
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

/*
 * rover_arm.ino — Arduino Arm (Slave)
 *
 * Responsabilidades:
 *   - Controlar os 6 servos do braço robótico
 *   - Gravar e reproduzir sequências de movimento (playback)
 *   - Receber comandos do Arduino Master via Serial Hardware (57600 baud)
 *
 * Mapeamento dos servos:
 *   A1 (pino 3)  — Base: rotação horizontal (MG996R)
 *   A2 (pino 5)  — Ombro: elevação principal (MG996R)
 *   A3 (pino 6)  — Cotovelo (MG90S)
 *   A4 (pino 9)  — Pulso pitch (MG90S)
 *   A5 (pino 10) — Pulso roll / rotação (MG90S)
 *   A6 (pino 11) — Garra / gripper (MG90S)
 *
 * Posição HOME (neutro): todos em 90°
 * Garra: 0° = fechada, 180° = aberta
 */

#include <Servo.h>

// ── Constantes ────────────────────────────────────────────────────────────────
static const uint8_t NUM_SERVOS   = 6;
static const uint8_t SERVO_PINS[NUM_SERVOS] = { 3, 5, 6, 9, 10, 11 };

// Limites físicos de cada servo (ajuste conforme a mecânica do seu braço)
static const uint8_t SERVO_MIN[NUM_SERVOS]  = {  0,  30,  20,  0,   0,   0  };
static const uint8_t SERVO_MAX[NUM_SERVOS]  = { 180, 150, 160, 180, 180, 180 };
static const uint8_t SERVO_HOME[NUM_SERVOS] = {  90,  90,  90,  90,  90,  90 };

// ── Gravação de movimentos ────────────────────────────────────────────────────
static const uint8_t  MAX_FRAMES   = 50;   // máximo de posições gravadas
static const uint16_t FRAME_DELAY  = 500;  // ms entre frames no playback

struct Frame {
  uint8_t pos[NUM_SERVOS];
  uint16_t delayMs;  // pausa antes do próximo frame
};

Frame    recording[MAX_FRAMES];
uint8_t  recordCount = 0;
uint8_t  playRepeat  = 1;
uint16_t frameDelay  = FRAME_DELAY;

bool isRecording = false;
bool isPlaying   = false;

// ── Objetos servo ─────────────────────────────────────────────────────────────
Servo servo[NUM_SERVOS];
uint8_t servoPos[NUM_SERVOS];

// ── Setup ─────────────────────────────────────────────────────────────────────
void setup() {
  Serial.begin(57600);

  for (uint8_t i = 0; i < NUM_SERVOS; i++) {
    servo[i].attach(SERVO_PINS[i]);
    servoPos[i] = SERVO_HOME[i];
    servo[i].write(servoPos[i]);
  }
}

// ── Loop principal ────────────────────────────────────────────────────────────
void loop() {
  if (Serial.available()) {
    String cmd = Serial.readStringUntil('\n');
    cmd.trim();
    if (cmd.length() > 0) processCommand(cmd);
  }

  if (isPlaying) runPlayback();
}

// ── Roteamento de comandos ────────────────────────────────────────────────────
void processCommand(const String& cmd) {
  if      (cmd.startsWith("SRV:")) handleServo(cmd.substring(4));
  else if (cmd.startsWith("ALL:")) handleAll(cmd.substring(4));
  else if (cmd.startsWith("BTN:")) handleButton(cmd.substring(4));
  else if (cmd.startsWith("REC:")) handleRecord(cmd.substring(4));
  else if (cmd.startsWith("SET:")) handleSettings(cmd.substring(4));
}

// ── Controle individual de servo ──────────────────────────────────────────────
// Formato: SRV:1:90  (servo 1-6, ângulo 0-180)
void handleServo(const String& params) {
  int col = params.indexOf(':');
  if (col < 0) return;

  int idx   = params.substring(0, col).toInt() - 1;  // 0-indexed
  int angle = params.substring(col + 1).toInt();
  moveServo(idx, angle);

  // Se estiver gravando, captura o frame atual
  if (isRecording && recordCount < MAX_FRAMES) {
    captureFrame();
  }
}

// ── Todos os servos de uma vez ────────────────────────────────────────────────
// Formato: ALL:90,90,90,90,90,90
void handleAll(const String& params) {
  String p   = params;
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

// ── Botões ────────────────────────────────────────────────────────────────────
void handleButton(const String& btn) {
  if      (btn == "B")  moveServo(5, 180);   // B maiúsculo = garra abre
  else if (btn == "b")  moveServo(5, 0);     // b minúsculo = garra fecha
  else if (btn == "C" || btn == "c")  goHome();
  else if (btn == "D")  goPreset(0);         // posição pré-definida 1
  else if (btn == "d")  goPreset(1);         // posição pré-definida 2
  else if (btn == "E")  goPreset(2);
  else if (btn == "e")  goPreset(3);
}

// ── Gravação / Playback ───────────────────────────────────────────────────────
void handleRecord(const String& action) {
  if (action == "START") {
    recordCount  = 0;
    isRecording  = true;
    isPlaying    = false;
    Serial.println("REC:STARTED");
  }
  else if (action == "STOP") {
    isRecording = false;
    Serial.print("REC:SAVED:");
    Serial.println(recordCount);
  }
  else if (action == "PLAY") {
    if (recordCount > 0) {
      isPlaying   = true;
      isRecording = false;
    }
  }
  else if (action == "PAUSE") {
    isPlaying = false;
  }
  else if (action == "CLEAR") {
    recordCount = 0;
    isRecording = false;
    isPlaying   = false;
  }
}

// ── Configurações (delay, repetições) ────────────────────────────────────────
// Formato: SET:DELAY:500 ou SET:REPEAT:3
void handleSettings(const String& params) {
  int col = params.indexOf(':');
  if (col < 0) return;
  String key = params.substring(0, col);
  int    val = params.substring(col + 1).toInt();
  if      (key == "DELAY")  frameDelay = constrain(val, 50, 5000);
  else if (key == "REPEAT") playRepeat = constrain(val, 1, 255);
}

// ── Execução do playback ──────────────────────────────────────────────────────
void runPlayback() {
  for (uint8_t rep = 0; rep < playRepeat && isPlaying; rep++) {
    for (uint8_t f = 0; f < recordCount && isPlaying; f++) {
      for (uint8_t i = 0; i < NUM_SERVOS; i++) {
        moveServo(i, recording[f].pos[i]);
      }
      uint16_t wait = recording[f].delayMs;
      uint32_t t    = millis();
      while (millis() - t < wait) {
        // checar por comando PAUSE durante o delay
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

// ── Capturar posição atual como frame ─────────────────────────────────────────
void captureFrame() {
  for (uint8_t i = 0; i < NUM_SERVOS; i++) {
    recording[recordCount].pos[i] = servoPos[i];
  }
  recording[recordCount].delayMs = frameDelay;
  recordCount++;
}

// ── Mover servo com limites aplicados ─────────────────────────────────────────
void moveServo(uint8_t idx, int angle) {
  if (idx >= NUM_SERVOS) return;
  angle = constrain(angle, SERVO_MIN[idx], SERVO_MAX[idx]);
  servo[idx].write(angle);
  servoPos[idx] = (uint8_t)angle;
}

// ── Ir para posição HOME ──────────────────────────────────────────────────────
void goHome() {
  for (uint8_t i = 0; i < NUM_SERVOS; i++) {
    moveServo(i, SERVO_HOME[i]);
    delay(15);
  }
}

// ── Posições pré-definidas ────────────────────────────────────────────────────
// Defina aqui posições úteis para sua aplicação específica
const uint8_t PRESETS[][NUM_SERVOS] = {
  { 90, 60,  90,  90, 90,   0 },   // 0: braço estendido, garra fechada
  { 90, 120, 90,  90, 90, 180 },   // 1: braço recolhido, garra aberta
  { 45, 90,  120, 90, 90,  90 },   // 2: posição lateral esquerda
  { 135, 90, 120, 90, 90, 90 },    // 3: posição lateral direita
};

void goPreset(uint8_t idx) {
  if (idx >= 4) return;
  for (uint8_t i = 0; i < NUM_SERVOS; i++) {
    moveServo(i, PRESETS[idx][i]);
    delay(20);
  }
}

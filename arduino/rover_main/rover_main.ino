/*
 * rover_main.ino — Arduino Master
 *
 * Responsabilidades:
 *   - Receber comandos do app via Bluetooth (HC-05 na SoftwareSerial)
 *   - Controlar os 4 motores DC via L298N
 *   - Repassar comandos de braço/servo para o Arduino Arm via Serial Hardware
 *
 * Pinos HC-05:
 *   BT_RX (pino 10) → HC-05 TX
 *   BT_TX (pino 11) → HC-05 RX (use divisor de tensão 5V→3.3V)
 *
 * Pinos L298N:
 *   Motor Esquerdo: IN1=2, IN2=3, ENA=9 (PWM)
 *   Motor Direito:  IN3=4, IN4=5, ENB=6 (PWM)
 *
 * Comunicação com Arduino Arm:
 *   Serial Hardware (pinos 0/1) a 57600 baud
 *   ATENÇÃO: desconecte os fios do Serial antes de gravar via USB.
 */

#include <SoftwareSerial.h>

// ── Bluetooth ────────────────────────────────────────────────────────────────
SoftwareSerial btSerial(10, 11);  // RX, TX

// ── L298N — Motor Esquerdo (motores dianteiro + traseiro esquerdo em paralelo)
const uint8_t IN1 = 2;
const uint8_t IN2 = 3;
const uint8_t ENA = 9;   // PWM

// ── L298N — Motor Direito
const uint8_t IN3 = 4;
const uint8_t IN4 = 5;
const uint8_t ENB = 6;   // PWM

// ── Estado global ─────────────────────────────────────────────────────────────
int  motorSpeed   = 180;   // 0–255
bool movingForward = false;

// ── Setup ─────────────────────────────────────────────────────────────────────
void setup() {
  pinMode(IN1, OUTPUT); pinMode(IN2, OUTPUT); pinMode(ENA, OUTPUT);
  pinMode(IN3, OUTPUT); pinMode(IN4, OUTPUT); pinMode(ENB, OUTPUT);
  stopMotors();

  Serial.begin(57600);    // Para Arduino Arm
  btSerial.begin(9600);   // HC-05 padrão é 9600
}

// ── Loop principal ────────────────────────────────────────────────────────────
void loop() {
  // Comandos recebidos do app via Bluetooth
  if (btSerial.available()) {
    String cmd = btSerial.readStringUntil('\n');
    cmd.trim();
    if (cmd.length() > 0) processCommand(cmd);
  }

  // Feedback do Arduino Arm de volta para o app
  if (Serial.available()) {
    String feedback = Serial.readStringUntil('\n');
    btSerial.println(feedback);
  }
}

// ── Roteamento de comandos ────────────────────────────────────────────────────
void processCommand(const String& cmd) {
  if      (cmd.startsWith("MOV:")) handleMovement(cmd.substring(4));
  else if (cmd.startsWith("SPD:")) motorSpeed = constrain(cmd.substring(4).toInt(), 0, 255);
  else if (cmd.startsWith("SRV:")) Serial.println(cmd);  // repassa para Arm
  else if (cmd.startsWith("ALL:")) Serial.println(cmd);
  else if (cmd.startsWith("BTN:")) Serial.println(cmd);
  else if (cmd.startsWith("REC:")) Serial.println(cmd);
}

// ── Lógica de movimento ───────────────────────────────────────────────────────
void handleMovement(const String& action) {
  if      (action == "F")  driveForward();
  else if (action == "B")  driveBackward();
  else if (action == "L")  pivotLeft();
  else if (action == "R")  pivotRight();
  else if (action == "FL") curveLeft(motorSpeed,   motorSpeed / 2);
  else if (action == "FR") curveLeft(motorSpeed / 2, motorSpeed);
  else if (action == "BL") curveback(motorSpeed,   motorSpeed / 2);
  else if (action == "BR") curveback(motorSpeed / 2, motorSpeed);
  else if (action == "S")  stopMotors();
  // Joystick analógico: "JOY:leftSpeed:rightSpeed" (-255 a 255)
  else if (action.startsWith("JOY:")) handleJoystick(action.substring(4));
}

// Joystick envia velocidades individuais para cada lado
void handleJoystick(const String& params) {
  int sep   = params.indexOf(':');
  if (sep < 0) return;
  int leftVal  = params.substring(0, sep).toInt();
  int rightVal = params.substring(sep + 1).toInt();
  setLeftMotor(abs(leftVal),  leftVal  >= 0);
  setRightMotor(abs(rightVal), rightVal >= 0);
}

void driveForward()  { setLeftMotor(motorSpeed, true);  setRightMotor(motorSpeed, true);  }
void driveBackward() { setLeftMotor(motorSpeed, false); setRightMotor(motorSpeed, false); }
void pivotLeft()     { setLeftMotor(motorSpeed, false); setRightMotor(motorSpeed, true);  }
void pivotRight()    { setLeftMotor(motorSpeed, true);  setRightMotor(motorSpeed, false); }

void curveLeft(int leftSpd, int rightSpd) {
  setLeftMotor(leftSpd,  true);
  setRightMotor(rightSpd, true);
}

void curveback(int leftSpd, int rightSpd) {
  setLeftMotor(leftSpd,  false);
  setRightMotor(rightSpd, false);
}

void stopMotors() {
  setLeftMotor(0, true);
  setRightMotor(0, true);
}

// ── Controle baixo nível dos motores ─────────────────────────────────────────
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

# Pinagem e Conexões do Rover

## Arduino 1 — Master (motores + bluetooth)

| Pino Arduino | Conectado em   | Função                      |
| ------------ | -------------- | --------------------------- |
| 0 (RX)       | Arduino Arm TX | Recebe dados do Arm         |
| 1 (TX)       | Arduino Arm RX | Envia dados para o Arm      |
| 2            | L298N IN1      | Motor esquerdo direção A    |
| 3            | L298N IN2      | Motor esquerdo direção B    |
| 4            | L298N IN3      | Motor direito direção A     |
| 5            | L298N IN4      | Motor direito direção B     |
| 6 (PWM)      | L298N ENB      | Velocidade motor direito    |
| 9 (PWM)      | L298N ENA      | Velocidade motor esquerdo   |
| 10           | HC-05 TX       | BT RX (entrada de comandos) |
| 11           | HC-05 RX       | BT TX (envia feedback)      |
| 5V           | HC-05 VCC      | Alimentação Bluetooth       |
| GND          | HC-05 GND      | Terra comum                 |

> ⚠️ O HC-05 usa 3.3V no pino RX. Use um divisor de tensão:
> Arduino pino 11 → R1 (1kΩ) → HC-05 RX → R2 (2kΩ) → GND

## Arduino 2 — Arm (servos)

| Pino Arduino | Servo             | Junta           |
| ------------ | ----------------- | --------------- |
| 3 (PWM)      | A1                | Base (rotação)  |
| 5 (PWM)      | A2                | Ombro           |
| 6 (PWM)      | A3                | Cotovelo        |
| 9 (PWM)      | A4                | Pulso pitch     |
| 10 (PWM)     | A5                | Pulso roll      |
| 11 (PWM)     | A6                | Garra (gripper) |
| 0 (RX)       | Arduino Master TX | Recebe comandos |
| 1 (TX)       | Arduino Master RX | Envia feedback  |

> ⚠️ Servos MG996R consomem muita corrente. Alimente pelo VIN/5V externo,
> NUNCA pelo pino 5V do Arduino. Use um regulador 7806 ou BEC de 5V/3A.

## L298N — Motor Driver

| Pino L298N | Conectado em                    |
| ---------- | ------------------------------- |
| IN1        | Arduino Master pino 2           |
| IN2        | Arduino Master pino 3           |
| ENA        | Arduino Master pino 9           |
| IN3        | Arduino Master pino 4           |
| IN4        | Arduino Master pino 5           |
| ENB        | Arduino Master pino 6           |
| OUT1+OUT2  | Motores esquerdos (em paralelo) |
| OUT3+OUT4  | Motores direitos (em paralelo)  |
| 12V        | Bateria 18650 (+)               |
| GND        | Bateria 18650 (-)               |
| 5V (saída) | Arduino Master VIN              |

## Alimentação

```
Baterias 18650 (3S = ~11.1V)
    │
    ├── L298N (12V) → alimenta motores DC
    │       └── Saída 5V do L298N → Arduino Master VIN
    │
    └── Regulador 5V/3A (ex: LM2596) → alimenta servos + Arduino Arm
```

## Comunicação entre Arduinos

```
Arduino Master pino 1 (TX) ──────→ Arduino Arm pino 0 (RX)
Arduino Master pino 0 (RX) ←────── Arduino Arm pino 1 (TX)
GND ────────────────────────────── GND  (OBRIGATÓRIO compartilhar GND)
```

## Protocolo de Comandos (Bluetooth → App)

```
Movimento:    MOV:F | MOV:B | MOV:L | MOV:R | MOV:FL | MOV:FR | MOV:S
Joystick:     MOV:JOY:leftVal:rightVal  (valores -255 a 255)
Velocidade:   SPD:200
Servo único:  SRV:1:90   (servo 1–6, ângulo 0–180)
Todos servos: ALL:90,90,90,90,90,90
Botões:       BTN:B | BTN:b | BTN:C | BTN:c | BTN:D | BTN:d | BTN:E | BTN:e
Gravação:     REC:START | REC:STOP | REC:PLAY | REC:PAUSE | REC:CLEAR
Config:       SET:DELAY:500 | SET:REPEAT:3
```

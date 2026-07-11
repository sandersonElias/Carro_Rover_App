# Pinagem e Conexões do Rover (1x Arduino UNO)

## Arduino UNO (rover_uno.ino)

| Pino Arduino | Conectado em   | Função                      |
| ------------ | -------------- | --------------------------- |
| 0 (RX)       | HC-05 TX       | BT RX (entrada de comandos) |
| 1 (TX)       | HC-05 RX       | BT TX (envia feedback)      |
| 2            | L298N IN1      | Motor esquerdo direção A    |
| 3            | L298N IN2      | Motor esquerdo direção B    |
| 4            | L298N IN3      | Motor direito direção A     |
| 5            | L298N IN4      | Motor direito direção B     |
| 6 (PWM)      | Servo A1       | Base (rotação)              |
| 7 (PWM)      | Servo A2       | Ombro                       |
| 8 (PWM)      | Servo A3       | Cotovelo                    |
| 9 (PWM)      | Servo A4       | Pulso pitch                 |
| 10 (PWM)     | Servo A5       | Pulso roll                  |
| 11 (PWM)     | Servo A6       | Garra (gripper)             |
| 5V           | HC-05 VCC      | Alimentação Bluetooth       |
| GND          | HC-05 GND      | Terra comum                 |

> ⚠️ O HC-05 usa 3.3V no pino RX. Use um divisor de tensão:
> Arduino pino 1 → R1 (1kΩ) → HC-05 RX → R2 (2kΩ) → GND

> ℹ️ ENA e ENB do L298N devem ter jumpers conectados (velocidade constante)

## L298N — Motor Driver (Simplificado)

| Pino L298N | Conectado em                    |
| ---------- | ------------------------------- |
| IN1        | Arduino pino 2                  |
| IN2        | Arduino pino 3                  |
| ENA        | Jumper conectado (velocidade fixa) |
| IN3        | Arduino pino 4                  |
| IN4        | Arduino pino 5                  |
| ENB        | Jumper conectado (velocidade fixa) |
| OUT1+OUT2  | Motores esquerdos (em paralelo) |
| OUT3+OUT4  | Motores direitos (em paralelo)  |
| 12V        | Bateria 18650 (+)               |
| GND        | Bateria 18650 (-)               |
| 5V (saída) | Arduino VIN                     |

> ℹ️ Com ENA/ENB com jumpers, os motores rodam em velocidade máxima quando ligados.
> Controle de velocidade é feito apenas por direção (ligado/desligado).

## Servos

| Pino Arduino | Servo             | Junta           |
| ------------ | ----------------- | --------------- |
| 6 (PWM)      | A1                | Base (rotação)  |
| 7 (PWM)      | A2                | Ombro           |
| 8 (PWM)      | A3                | Cotovelo        |
| 9 (PWM)      | A4                | Pulso pitch     |
| 10 (PWM)     | A5                | Pulso roll      |
| 11 (PWM)     | A6                | Garra (gripper) |

> ⚠️ Servos MG996R consomem muita corrente. Alimente pelo VIN/5V externo,
> NUNCA pelo pino 5V do Arduino. Use um regulador 7806 ou BEC de 5V/3A.

## Alimentação

```
Baterias 18650 (3S = ~11.1V)
    │
    ├── L298N (12V) → alimenta motores DC
    │       └── Saída 5V do L298N → Arduino VIN
    │
    └── Regulador 5V/3A (ex: LM2596) → alimenta servos
```

## Protocolo de Comandos (App → Bluetooth)

```
Movimento:    MOV:F | MOV:B | MOV:L | MOV:R | MOV:FL | MOV:FR | MOV:S
Joystick:     MOV:JOY:leftVal:rightVal  (valores -255 a 255)
Servo único:  SRV:1:90   (servo 1–6, ângulo 0–180)
Todos servos: ALL:90,90,90,90,90,90
Botões:       BTN:B | BTN:b | BTN:C | BTN:c | BTN:D | BTN:d | BTN:E | BTN:e
Gravação:     REC:START | REC:STOP | REC:PLAY | REC:PAUSE | REC:CLEAR
Config:       SET:DELAY:500 | SET:REPEAT:3
```

> ℹ️ O comando SPD (velocidade) foi removido. A velocidade é constante.

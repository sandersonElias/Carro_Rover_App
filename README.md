# Rover Control

![Android](https://img.shields.io/badge/Android-6.0%2B-green?style=flat&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple?style=flat&logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat)
![API](https://img.shields.io/badge/API-23%2B-brightgreen?style=flat)

Aplicativo Android para controle remoto de rover robótico via Bluetooth Classic (HC-05).

## Sumário

- [Visão Geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Pré-requisitos](#pré-requisitos)
- [Instalação](#instalação)
- [Uso](#uso)
- [Arquitetura do Sistema](#arquitetura-do-sistema)
- [Protocolo de Comunicação](#protocolo-de-comunicação)
- [Pinagem e Conexões](#pinagem-e-conexões)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [License](#license)

## Visão Geral

O **Rover Control** é um aplicativo Android nativo desenvolvido em Kotlin para controlar um rover robótico equipado com:

- 2 motores DC para movimentação (via driver L298N)
- 6 servos MG996R para braço robótico
- Módulo Bluetooth HC-05 para comunicação sem fio

O app permite controle preciso do rover através de joystick analógico, D-pad digital, e controles deslizantes para os servos.

## Funcionalidades

### Modo Direção
- **Joystick Analógico**: Controle intuitivo com tank steering (diferencial)
- **D-Pad Digital**: Botões para movimentos discretos (frente, ré, esquerda, direita)
- **Parada de Emergência**: Botão de stop imediato

### Modo Braço Robótico
- **6 Sliders Individuais**: Controle preciso de cada servo (Base, Ombro, Cotovelo, Pulso Pitch, Pulso Roll, Garra)
- **Controles Rápidos**: Botões para abrir/fechar garra, posicionar home
- **Presets**: 4 posições pré-definidas (P1-P4)
- **Gravação de Sequência**: Grave e reproduza movimentos com delay e repetição configuráveis

### Modo Escavadeira
- **Dual Joystick**: Dois joysticks para controle simultâneo de 4 servos
- **Controles de Garra**: Botões dedicados para abrir/fechar

### Conexão Bluetooth
- **Detecção Automática**: Lista dispositivos pareados
- **Indicador de Status**: Visualização do estado da conexão
- **Reconexão**: Reconexão automática em caso de perda

## Pré-requisitos

### Hardware
- Arduino UNO
- Módulo Bluetooth HC-05
- Driver de motor L298N
- 2x Motores DC (para rodas)
- 6x Servos MG996R
- Baterias 18650 (3S = ~11.1V)
- Regulador de tensão 5V/3A (LM2596 ou BEC)

### Software
- Android Studio Arctic Fox ou superior
- SDK Android 34 (Android 14)
- Dispositivo Android com Bluetooth (mínimo Android 6.0)

## Instalação

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/App_carro_rover.git
cd App_carro_rover
```

### 2. Abra no Android Studio

1. Abra o Android Studio
2. Selecione **File → Open**
3. Navegue até a pasta do projeto
4. Aguarde o Gradle sincronizar

### 3. Compile e instale

```bash
./gradlew assembleDebug
```

Ou use o Android Studio: **Run → Run 'app'**

### 4. Configure o Arduino

1. Abra o arquivo `arduino/rover_uno/rover_uno.ino` no Arduino IDE
2. Conecte o Arduino UNO via USB
3. Faça o upload do sketch

## Uso

### 1. Pareie o HC-05

1. Abra as configurações do Android
2. Vá em **Bluetooth**
3. Procure por "HC-05"
4. Pareie (senha padrão: `1234` ou `0000`)

### 2. Conecte o Rover

1. Abra o app **Rover Control**
2. Toque no ícone de Bluetooth na toolbar
3. Selecione o HC-05 na lista
4. Aguarde a conexão

### 3. Controle o Rover

- **Modo Direção**: Use o joystick ou D-pad para mover
- **Modo Braço**: Use os sliders para posicionar os servos
- **Modo Escavadeira**: Acesse pelo modo braço para controle dual

## Arquitetura do Sistema

### Diagrama de Blocos

```
┌─────────────────────────────────────────────────────────┐
│                    APP ANDROID                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │  Drive   │  │   Arm    │  │Excavator │              │
│  │ Fragment │  │ Fragment │  │ Activity │              │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘              │
│       │              │              │                    │
│       └──────────────┼──────────────┘                    │
│                      │                                  │
│              ┌───────▼───────┐                          │
│              │ BluetoothService│                         │
│              │   (Singleton)  │                          │
│              └───────┬───────┘                          │
└──────────────────────┼──────────────────────────────────┘
                       │ RFCOMM/SPP
                       │ (9600 baud)
┌──────────────────────┼──────────────────────────────────┐
│              ┌───────▼───────┐                          │
│              │   HC-05       │                          │
│              │  Bluetooth    │                          │
│              └───────┬───────┘                          │
│                      │ Serial                           │
│              ┌───────▼───────┐                          │
│              │  Arduino UNO  │                          │
│              │               │                          │
│              │  ┌─────────┐  │                          │
│              │  │rover_uno│  │                          │
│              │  │  .ino   │  │                          │
│              │  └────┬────┘  │                          │
│              └───────┼───────┘                          │
│                      │                                  │
│       ┌──────────────┼──────────────┐                   │
│       │              │              │                   │
│  ┌────▼────┐   ┌────▼────┐   ┌────▼────┐              │
│  │ L298N   │   │ Servos  │   │ Power   │              │
│  │ Motors  │   │ MG996R  │   │ 18650   │              │
│  └─────────┘   └─────────┘   └─────────┘              │
└─────────────────────────────────────────────────────────┘
```

### Fluxo de Controle

1. **Usuário** interage com a interface (joystick, sliders, botões)
2. **App Android** converte ações em comandos de texto
3. **BluetoothService** envia comandos via RFCOMM
4. **HC-05** transmite via Bluetooth Classic
5. **Arduino UNO** recebe e processa comandos
6. **Motores/Servos** executam as ações

## Protocolo de Comunicação

### Formato dos Comandos

Todos os comandos são strings terminadas com `\n` (newline).

| Comando | Descrição | Exemplo |
|---------|-----------|---------|
| `MOV:F` | Mover para frente | `MOV:F\n` |
| `MOV:B` | Mover para trás | `MOV:B\n` |
| `MOV:L` | Virar à esquerda | `MOV:L\n` |
| `MOV:R` | Virar à direita | `MOV:R\n` |
| `MOV:S` | Parar motores | `MOV:S\n` |
| `MOV:JOY:L:R` | Joystick analógico | `MOV:JOY:128:-64\n` |
| `SRV:N:ANG` | Posicionar servo | `SRV:1:90\n` |
| `ALL:A,B,C,D,E,F` | Todos os servos | `ALL:90,90,90,90,90,90\n` |
| `BTN:X` | Botão preset | `BTN:D\n` |
| `REC:START` | Iniciar gravação | `REC:START\n` |
| `REC:STOP` | Parar gravação | `REC:STOP\n` |
| `REC:PLAY` | Reproduzir sequência | `REC:PLAY\n` |
| `SET:DELAY:MS` | Configurar delay | `SET:DELAY:500\n` |
| `SET:REPEAT:N` | Configurar repetições | `SET:REPEAT:3\n` |

### Valores do Joystick

- **Left**: -255 a 255 (motor esquerdo)
- **Right**: -255 a 255 (motor direito)
- Conversão: `forward = -y`, `turn = x`
- Tank steering: `left = forward - turn`, `right = forward + turn`

## Pinagem e Conexões

### Arduino UNO

| Pino | Conexão | Função |
|------|---------|--------|
| 0 (RX) | HC-05 TX | Entrada de comandos |
| 1 (TX) | HC-05 RX | Saída de dados |
| 2 | L298N IN1 | Motor esquerdo dir. A |
| 3 | L298N IN2 | Motor esquerdo dir. B |
| 4 | L298N IN3 | Motor direito dir. A |
| 5 | L298N IN4 | Motor direito dir. B |
| 6 (PWM) | Servo A1 | Base |
| 7 (PWM) | Servo A2 | Ombro |
| 8 (PWM) | Servo A3 | Cotovelo |
| 9 (PWM) | Servo A4 | Pulso pitch |
| 10 (PWM) | Servo A5 | Pulso roll |
| 11 (PWM) | Servo A6 | Garra |

> ⚠️ **Atenção**: Use divisor de tensão no pino RX do HC-05 (1kΩ + 2kΩ)

### Diagrama de Alimentação

```
Baterias 18650 (3S ~11.1V)
    │
    ├── L298N (12V)
    │   ├── Motores DC
    │   └── Saída 5V → Arduino VIN
    │
    └── Regulador 5V/3A (LM2596)
        └── Servos MG996R (6x)
```

## Estrutura do Projeto

```
App_carro_rover/
├── app/
│   ├── src/main/
│   │   ├── java/com/rover/control/
│   │   │   ├── MainActivity.kt
│   │   │   ├── bluetooth/
│   │   │   │   └── BluetoothService.kt
│   │   │   └── ui/
│   │   │       ├── connect/
│   │   │       │   ├── ConnectActivity.kt
│   │   │       │   └── DeviceAdapter.kt
│   │   │       ├── drive/
│   │   │       │   ├── DriveFragment.kt
│   │   │       │   ├── DriveViewModel.kt
│   │   │       │   └── JoystickView.kt
│   │   │       ├── arm/
│   │   │       │   ├── ArmFragment.kt
│   │   │       │   └── ArmViewModel.kt
│   │   │       └── excavator/
│   │   │           └── ExcavatorActivity.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── drawable/
│   │   │   ├── values/
│   │   │   └── navigation/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── arduino/
│   ├── rover_uno/
│   │   └── rover_uno.ino
│   └── PINAGEM.md
├── build.gradle
├── settings.gradle
└── README.md
```

## Tecnologias Utilizadas

### Android App

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| Kotlin | 1.9.22 | Linguagem de programação |
| Android SDK | 34 | SDK do Android |
| Material Components | 1.11.0 | Componentes de UI |
| Navigation Component | 2.7.7 | Navegação entre telas |
| ViewBinding | - | Acesso tipado às views |
| Coroutines | 1.7.3 | Assincronismo |
| ViewModel | 2.7.0 | Gerenciamento de estado |

### Arduino

| Componente | Especificação |
|------------|---------------|
| Placa | Arduino UNO |
| Linguagem | C++ (Arduino) |
| Comunicação | Serial (9600 baud) |
| Biblioteca | Servo.h |

## Contribuindo

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Faça commit das suas mudanças (`git commit -m 'Adicionar nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## License

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

## Contato

- **Autor**: Sande
- **GitHub**: [seu-usuario](https://github.com/seu-usuario)

---

Desovido com ❤️ para controle de robôs

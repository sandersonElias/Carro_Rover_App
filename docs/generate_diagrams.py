from PIL import Image, ImageDraw, ImageFont
import os

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "images")
os.makedirs(OUTPUT_DIR, exist_ok=True)

W, H = 1200, 800
BG = "#FFFFFF"
BOX_FILL = "#E8F0FE"
BOX_BORDER = "#1A73E8"
SECTION_FILL = "#F1F3F4"
TEXT_COLOR = "#202124"
SUBTEXT_COLOR = "#5F6368"
ACCENT = "#EA4335"
GREEN = "#34A853"
YELLOW = "#FBBC04"
ORANGE = "#E37400"

try:
    FONT_BOLD = ImageFont.truetype("arialbd.ttf", 20)
    FONT = ImageFont.truetype("arial.ttf", 16)
    FONT_SMALL = ImageFont.truetype("arial.ttf", 13)
    FONT_TITLE = ImageFont.truetype("arialbd.ttf", 24)
except:
    try:
        FONT_BOLD = ImageFont.truetype("DejaVuSans-Bold.ttf", 20)
        FONT = ImageFont.truetype("DejaVuSans.ttf", 16)
        FONT_SMALL = ImageFont.truetype("DejaVuSans.ttf", 13)
        FONT_TITLE = ImageFont.truetype("DejaVuSans-Bold.ttf", 24)
    except:
        FONT_BOLD = ImageFont.load_default()
        FONT = ImageFont.load_default()
        FONT_SMALL = ImageFont.load_default()
        FONT_TITLE = ImageFont.load_default()


def draw_rounded_rect(draw, xy, radius, fill, outline, width=2):
    x0, y0, x1, y1 = xy
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def draw_arrow_down(draw, x, y1, y2, color="#202124", width=2):
    draw.line([(x, y1), (x, y2)], fill=color, width=width)
    draw.polygon([(x - 6, y2 - 8), (x + 6, y2 - 8), (x, y2)], fill=color)


def draw_arrow_right(draw, x1, x2, y, color="#202124", width=2):
    draw.line([(x1, y), (x2, y)], fill=color, width=width)
    draw.polygon([(x2 - 8, y - 6), (x2 - 8, y + 6), (x2, y)], fill=color)


def center_text(draw, text, y, font, fill=TEXT_COLOR, img_w=W):
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    draw.text(((img_w - tw) // 2, y), text, fill=fill, font=font)


# ============================================================
# FIGURA 1 - Diagrama de blocos do sistema
# ============================================================
def gen_fig1():
    img = Image.new("RGB", (W, 820), BG)
    draw = ImageDraw.Draw(img)

    center_text(draw, "Figura 1 - Diagrama de Blocos do Sistema", 10, FONT_TITLE)

    # App Android box
    draw_rounded_rect(draw, (200, 70, 1000, 260), 12, "#E8F0FE", BOX_BORDER, 2)
    draw.text((450, 80), "APP ANDROID", fill=BOX_BORDER, font=FONT_BOLD)

    # Sub boxes inside App
    for i, (label, color) in enumerate([
        ("DriveFragment", GREEN), ("ArmFragment", ACCENT), ("ExcavatorActivity", ORANGE)
    ]):
        x = 240 + i * 260
        draw_rounded_rect(draw, (x, 120, x + 220, 175), 8, color + "22", color, 2)
        bbox = draw.textbbox((0, 0), label, font=FONT)
        tw = bbox[2] - bbox[0]
        draw.text((x + (220 - tw) // 2, 135), label, fill=color, font=FONT)

    # BluetoothService
    draw_rounded_rect(draw, (380, 200, 820, 250), 8, "#FFF3E0", ORANGE, 2)
    bbox = draw.textbbox((0, 0), "BluetoothService (Singleton)", font=FONT)
    tw = bbox[2] - bbox[0]
    draw.text(((1200 - tw) // 2, 212), "BluetoothService (Singleton)", fill=ORANGE, font=FONT)

    # Arrows from fragments to service
    for x in [350, 610, 870]:
        draw.line([(x, 175), (x, 200)], fill=BOX_BORDER, width=2)

    # RFCOMM arrow
    draw_arrow_down(draw, 600, 260, 310, color=ACCENT, width=3)
    draw.text((640, 275), "RFCOMM/SPP", fill=ACCENT, font=FONT_SMALL)
    draw.text((640, 292), "(9600 baud)", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # HC-05
    draw_rounded_rect(draw, (430, 320, 770, 380), 10, "#FCE4EC", ACCENT, 2)
    bbox = draw.textbbox((0, 0), "HC-05 Bluetooth", font=FONT_BOLD)
    tw = bbox[2] - bbox[0]
    draw.text(((1200 - tw) // 2, 338), "HC-05 Bluetooth", fill=ACCENT, font=FONT_BOLD)

    # Serial arrow
    draw_arrow_down(draw, 600, 380, 430, color=ACCENT, width=3)
    draw.text((640, 395), "Serial", fill=ACCENT, font=FONT_SMALL)

    # Arduino UNO
    draw_rounded_rect(draw, (400, 440, 800, 510), 10, "#E8F5E9", GREEN, 2)
    bbox = draw.textbbox((0, 0), "Arduino UNO (ATmega328P)", font=FONT_BOLD)
    tw = bbox[2] - bbox[0]
    draw.text(((1200 - tw) // 2, 458), "Arduino UNO (ATmega328P)", fill=GREEN, font=FONT_BOLD)

    # Arrows from Arduino to hardware
    for i, (label, color) in enumerate([
        ("L298N\nMotores DC", GREEN), ("Servos MG996R\n(6x)", ORANGE), ("Alimentacao\n18650 / LM2596", ACCENT)
    ]):
        x = 220 + i * 260
        draw_arrow_down(draw, x + 90, 510, 560, color=color, width=2)
        draw_rounded_rect(draw, (x, 565, x + 180, 660), 8, color + "15", color, 2)
        lines = label.split("\n")
        for j, line in enumerate(lines):
            bbox = draw.textbbox((0, 0), line, font=FONT)
            tw = bbox[2] - bbox[0]
            draw.text((x + (180 - tw) // 2, 578 + j * 22), line, fill=color, font=FONT)

    img.save(os.path.join(OUTPUT_DIR, "fig1-diagrama-blocos.png"), dpi=(150, 150))
    print("Figura 1 OK")


# ============================================================
# FIGURA 2 - Arquitetura MVVM
# ============================================================
def gen_fig2():
    img = Image.new("RGB", (W, 800), BG)
    draw = ImageDraw.Draw(img)

    center_text(draw, "Figura 2 - Arquitetura do Aplicativo Android", 10, FONT_TITLE)

    # View Layer
    draw_rounded_rect(draw, (50, 60, 1150, 300), 12, "#E3F2FD", "#1565C0", 2)
    draw.text((70, 70), "VIEW LAYER", fill="#1565C0", font=FONT_BOLD)

    view_boxes = [
        ("DriveFragment", 120), ("ArmFragment", 450), ("ExcavatorActivity", 780)
    ]
    for label, x in view_boxes:
        draw_rounded_rect(draw, (x, 110, x + 260, 175), 8, "#BBDEFB", "#1565C0", 2)
        bbox = draw.textbbox((0, 0), label, font=FONT)
        tw = bbox[2] - bbox[0]
        draw.text((x + (260 - tw) // 2, 130), label, fill="#1565C0", font=FONT)

    draw.text((70, 190), "ViewBinding", fill=SUBTEXT_COLOR, font=FONT_SMALL)
    draw.text((70, 210), "XML Layouts | Fragments | Activities", fill=SUBTEXT_COLOR, font=FONT_SMALL)
    draw.text((70, 230), "JoystickView (Canvas) | SeekBars | D-Pad", fill=SUBTEXT_COLOR, font=FONT_SMALL)
    draw.text((70, 250), "StateFlow observers | Navigation Component", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Arrow View -> ViewModel
    draw_arrow_down(draw, 600, 300, 340, color="#1565C0", width=3)
    draw.text((620, 310), "observe", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # ViewModel Layer
    draw_rounded_rect(draw, (50, 350, 1150, 490), 12, "#FFF3E0", ORANGE, 2)
    draw.text((70, 360), "VIEWMODEL LAYER", fill=ORANGE, font=FONT_BOLD)

    draw_rounded_rect(draw, (200, 395, 1000, 470), 8, "#FFE0B2", ORANGE, 2)
    vm_lines = [
        "ArmViewModel",
        "  - servoPositions: IntArray(6)     - recordingState: State",
        "  - playbackState: State            - delay: Int, repeat: Int"
    ]
    for i, line in enumerate(vm_lines):
        font = FONT_BOLD if i == 0 else FONT_SMALL
        color = ORANGE if i == 0 else TEXT_COLOR
        draw.text((220, 402 + i * 20), line, fill=color, font=font)

    # Arrow ViewModel -> Model
    draw_arrow_down(draw, 600, 490, 530, color=ORANGE, width=3)
    draw.text((620, 500), "calls", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Model Layer
    draw_rounded_rect(draw, (50, 540, 1150, 700), 12, "#E8F5E9", GREEN, 2)
    draw.text((70, 550), "MODEL LAYER", fill=GREEN, font=FONT_BOLD)

    draw_rounded_rect(draw, (150, 590, 1050, 680), 8, "#C8E6C9", GREEN, 2)
    model_lines = [
        "BluetoothService (object / Singleton)",
        "  - connect(device)   - disconnect()   - send(cmd: String)",
        "  - StateFlow<State>  - Mutex socket access  - Coroutines IO"
    ]
    for i, line in enumerate(model_lines):
        font = FONT_BOLD if i == 0 else FONT_SMALL
        color = GREEN if i == 0 else TEXT_COLOR
        draw.text((170, 598 + i * 22), line, fill=color, font=font)

    img.save(os.path.join(OUTPUT_DIR, "fig2-arquitetura-mvvm.png"), dpi=(150, 150))
    print("Figura 2 OK")


# ============================================================
# FIGURA 3 - Fluxo comunicação Bluetooth
# ============================================================
def gen_fig3():
    img = Image.new("RGB", (W, 800), BG)
    draw = ImageDraw.Draw(img)

    center_text(draw, "Figura 3 - Fluxo de Comunicacao Bluetooth", 10, FONT_TITLE)

    # App box
    draw_rounded_rect(draw, (80, 80, 400, 200), 12, "#E3F2FD", "#1565C0", 2)
    draw.text((130, 100), "APP ANDROID", fill="#1565C0", font=FONT_BOLD)
    draw.text((130, 130), "Envia comando textual", fill=SUBTEXT_COLOR, font=FONT)
    draw.text((130, 155), "ex: MOV:F\\n", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Arrow App -> BT
    draw_arrow_right(draw, 400, 480, 140, color=ACCENT, width=3)
    draw.text((420, 115), "Bluetooth", fill=ACCENT, font=FONT_SMALL)
    draw.text((420, 135), "Classic", fill=ACCENT, font=FONT_SMALL)

    # BT Module box
    draw_rounded_rect(draw, (480, 80, 720, 200), 12, "#FCE4EC", ACCENT, 2)
    draw.text((510, 100), "HC-05", fill=ACCENT, font=FONT_BOLD)
    draw.text((510, 130), "Transmissao RF", fill=SUBTEXT_COLOR, font=FONT)
    draw.text((510, 155), "2.4 GHz SPP", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Arrow BT -> Arduino
    draw_arrow_right(draw, 720, 800, 140, color=ACCENT, width=3)
    draw.text((730, 115), "Serial", fill=ACCENT, font=FONT_SMALL)
    draw.text((730, 135), "9600 baud", fill=ACCENT, font=FONT_SMALL)

    # Arduino box
    draw_rounded_rect(draw, (800, 80, 1120, 200), 12, "#E8F5E9", GREEN, 2)
    draw.text((840, 100), "ARDUINO UNO", fill=GREEN, font=FONT_BOLD)
    draw.text((840, 130), "Parse + Processa", fill=SUBTEXT_COLOR, font=FONT)
    draw.text((840, 155), "rover_uno.ino", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Arrow down from Arduino
    draw_arrow_down(draw, 960, 200, 280, color=GREEN, width=3)
    draw.text((975, 230), "Aciona", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Output boxes
    outputs = [
        ("Motores DC", 180, GREEN, ["IN1-IN4 via L298N", "Frente/Re/Curva"]),
        ("Servos MG996R", 480, ORANGE, ["Base/Ombro/Cotovelo", "Pulso/Garra"]),
        ("Gravacao", 780, ACCENT, ["REC:START/STOP/PLAY", "Armazena frames"])
    ]
    for label, x, color, lines in outputs:
        draw_rounded_rect(draw, (x, 290, x + 260, 410), 10, color + "15", color, 2)
        bbox = draw.textbbox((0, 0), label, font=FONT_BOLD)
        tw = bbox[2] - bbox[0]
        draw.text((x + (260 - tw) // 2, 300), label, fill=color, font=FONT_BOLD)
        for i, line in enumerate(lines):
            bbox = draw.textbbox((0, 0), line, font=FONT_SMALL)
            tw = bbox[2] - bbox[0]
            draw.text((x + (260 - tw) // 2, 330 + i * 20), line, fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Legend at bottom
    draw.text((80, 460), "Legenda:", fill=TEXT_COLOR, font=FONT_BOLD)
    draw.text((80, 485), "Barras de progresso: Conexao (2.3s) | Comando MOV (15ms) | Comando SRV (18ms) | Joystick 25Hz (40ms)", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Timing bars
    bars = [
        ("Conexao BT", 2.3, GREEN, 200),
        ("Comando MOV", 0.015, ACCENT, 150),
        ("Comando SRV", 0.018, ORANGE, 170),
        ("Joystick 25Hz", 0.04, "#1565C0", 220),
    ]
    for i, (label, val, color, bar_w) in enumerate(bars):
        y = 520 + i * 55
        draw.text((80, y), label, fill=TEXT_COLOR, font=FONT)
        draw.rounded_rectangle((280, y, 280 + bar_w, y + 30), radius=5, fill=color + "44", outline=color, width=1)
        draw.text((290, y + 5), f"{label}: {val}s" if val > 0.1 else f"{label}: {int(val*1000)}ms", fill=color, font=FONT_SMALL)

    img.save(os.path.join(OUTPUT_DIR, "fig3-fluxo-bluetooth.png"), dpi=(150, 150))
    print("Figura 3 OK")


# ============================================================
# FIGURA 4 - Circuito de alimentacao
# ============================================================
def gen_fig4():
    img = Image.new("RGB", (W, 800), BG)
    draw = ImageDraw.Draw(img)

    center_text(draw, "Figura 4 - Circuito de Alimentacao", 10, FONT_TITLE)

    # Battery
    draw_rounded_rect(draw, (450, 60, 750, 140), 10, "#FFF9C4", YELLOW, 2)
    draw.text((480, 75), "Baterias 18650 (3S)", fill=TEXT_COLOR, font=FONT_BOLD)
    draw.text((480, 100), "~11.1V | 2000mAh", fill=SUBTEXT_COLOR, font=FONT)

    # Arrow down
    draw_arrow_down(draw, 600, 140, 190, color=TEXT_COLOR, width=3)

    # Split into two paths
    # Left path - L298N
    draw.line([(600, 190), (350, 190), (350, 220)], fill=GREEN, width=3)
    draw_arrow_down(draw, 350, 210, 250, color=GREEN, width=3)
    draw.text((380, 195), "12V IN", fill=GREEN, font=FONT_SMALL)

    draw_rounded_rect(draw, (200, 255, 500, 350), 10, "#E8F5E9", GREEN, 2)
    draw.text((230, 265), "L298N", fill=GREEN, font=FONT_BOLD)
    draw.text((230, 290), "Motor Driver", fill=SUBTEXT_COLOR, font=FONT)
    draw.text((230, 315), "H-Bridge Duplo", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # L298N outputs
    draw_arrow_down(draw, 280, 350, 400, color=GREEN, width=2)
    draw_arrow_down(draw, 420, 350, 400, color=GREEN, width=2)

    draw_rounded_rect(draw, (130, 405, 320, 490), 8, GREEN + "22", GREEN, 2)
    draw.text((155, 415), "Motores DC", fill=GREEN, font=FONT_BOLD)
    draw.text((155, 440), "Esquerdo + Direito", fill=SUBTEXT_COLOR, font=FONT_SMALL)
    draw.text((155, 460), "via OUT1-OUT4", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # 5V out from L298N
    draw_arrow_down(draw, 420, 350, 400, color=GREEN, width=2)

    draw_rounded_rect(draw, (340, 405, 520, 490), 8, "#E3F2FD", "#1565C0", 2)
    draw.text((355, 415), "5V OUT", fill="#1565C0", font=FONT_BOLD)
    draw_arrow_down(draw, 430, 490, 530, color="#1565C0", width=2)
    draw_rounded_rect(draw, (350, 535, 510, 600), 8, "#BBDEFB", "#1565C0", 2)
    draw.text((365, 545), "Arduino VIN", fill="#1565C0", font=FONT_BOLD)
    draw.text((365, 570), "Alimentacao", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Right path - LM2596
    draw.line([(600, 190), (850, 190), (850, 220)], fill=ORANGE, width=3)
    draw_arrow_down(draw, 850, 210, 250, color=ORANGE, width=3)
    draw.text((700, 195), "IN", fill=ORANGE, font=FONT_SMALL)

    draw_rounded_rect(draw, (700, 255, 1000, 350), 10, "#FFF3E0", ORANGE, 2)
    draw.text((730, 265), "LM2596", fill=ORANGE, font=FONT_BOLD)
    draw.text((730, 290), "Buck Converter", fill=SUBTEXT_COLOR, font=FONT)
    draw.text((730, 315), "11.1V -> 5V/3A", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    draw_arrow_down(draw, 850, 350, 405, color=ORANGE, width=2)

    draw_rounded_rect(draw, (720, 410, 980, 510), 10, ORANGE + "22", ORANGE, 2)
    draw.text((745, 420), "Servos MG996R", fill=ORANGE, font=FONT_BOLD)
    draw.text((745, 445), "6x servos", fill=SUBTEXT_COLOR, font=FONT)
    draw.text((745, 470), "4.8-6V | ~1A cada", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Divider
    draw.text((80, 650), "Nota:", fill=ACCENT, font=FONT_BOLD)
    draw.text((80, 675), "Divisor de tensao no RX do HC-05: R1(1k) + R2(2k) para 3.3V", fill=SUBTEXT_COLOR, font=FONT_SMALL)
    draw.text((80, 695), "ENA/ENB do L298N com jumpers (velocidade constante)", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    img.save(os.path.join(OUTPUT_DIR, "fig4-alimentacao.png"), dpi=(150, 150))
    print("Figura 4 OK")


# ============================================================
# FIGURA 5 - Fluxograma do protocolo
# ============================================================
def gen_fig5():
    img = Image.new("RGB", (W, 900), BG)
    draw = ImageDraw.Draw(img)

    center_text(draw, "Figura 5 - Fluxograma do Protocolo de Comunicacao", 10, FONT_TITLE)

    # Start
    cx = 600
    draw_rounded_rect(draw, (cx - 100, 60, cx + 100, 105), 20, GREEN, GREEN, 2)
    draw.text((cx - 30, 72), "Inicio", fill="#FFFFFF", font=FONT_BOLD)

    draw_arrow_down(draw, cx, 105, 140, width=2)

    # Receive
    draw_rounded_rect(draw, (cx - 150, 145, cx + 150, 195), 10, "#E3F2FD", "#1565C0", 2)
    draw.text((cx - 130, 157), "Receber comando serial", fill="#1565C0", font=FONT)

    draw_arrow_down(draw, cx, 195, 230, width=2)

    # Parse
    draw_rounded_rect(draw, (cx - 150, 235, cx + 150, 285), 10, "#FFF3E0", ORANGE, 2)
    draw.text((cx - 100, 247), "Parse do prefixo", fill=ORANGE, font=FONT)

    # Branch lines
    prefixes = [
        ("MOV:", cx - 380, GREEN, "Motores DC"),
        ("SRV:", cx - 140, ORANGE, "Servo individual"),
        ("ALL:", cx + 100, "#1565C0", "Todos servos"),
        ("BTN:", cx + 280, ACCENT, "Preset"),
    ]

    # Horizontal line from parse box
    draw.line([(cx - 150, 285), (cx - 150, 310), (cx + 350, 310), (cx + 350, 335)], fill=TEXT_COLOR, width=2)
    draw.line([(cx - 150, 285), (cx - 150, 310), (cx - 380, 310), (cx - 380, 335)], fill=TEXT_COLOR, width=2)
    draw.line([(cx - 150, 285), (cx + 100, 285), (cx + 100, 310), (cx + 100, 335)], fill=TEXT_COLOR, width=2)
    draw.line([(cx - 150, 285), (cx + 280, 285), (cx + 280, 310), (cx + 280, 335)], fill=TEXT_COLOR, width=2)

    for prefix, x, color, desc in prefixes:
        # Prefix label
        draw.text((x - 15, 315), prefix, fill=color, font=FONT_BOLD)

        draw_arrow_down(draw, x, 335, 375, color=color, width=2)

        # Action box
        draw_rounded_rect(draw, (x - 80, 380, x + 80, 430), 8, color + "22", color, 2)
        bbox = draw.textbbox((0, 0), desc, font=FONT_SMALL)
        tw = bbox[2] - bbox[0]
        draw.text((x - tw // 2, 395), desc, fill=color, font=FONT)

        draw_arrow_down(draw, x, 430, 470, color=color, width=2)

        # Result box
        draw_rounded_rect(draw, (x - 80, 475, x + 80, 525), 8, color + "15", color, 1)
        if prefix == "MOV:":
            text = "L298N -> Motores"
        elif prefix == "SRV:":
            text = "Index + Angulo"
        elif prefix == "ALL:":
            text = "6 angulos"
        else:
            text = "Posicao fixa"
        bbox = draw.textbbox((0, 0), text, font=FONT_SMALL)
        tw = bbox[2] - bbox[0]
        draw.text((x - tw // 2, 490), text, fill=color, font=FONT_SMALL)

    # REC and SET branches
    draw.line([(cx - 150, 285), (cx - 150, 310), (cx - 500, 310), (cx - 500, 335)], fill=TEXT_COLOR, width=2)
    draw.text((cx - 530, 315), "REC:", fill=ACCENT, font=FONT_BOLD)
    draw_arrow_down(draw, cx - 500, 335, 375, color=ACCENT, width=2)
    draw_rounded_rect(draw, (cx - 580, 380, cx - 420, 430), 8, ACCENT + "22", ACCENT, 2)
    draw.text((cx - 565, 395), "Gravacao", fill=ACCENT, font=FONT)
    draw_arrow_down(draw, cx - 500, 430, 470, color=ACCENT, width=2)
    draw_rounded_rect(draw, (cx - 580, 475, cx - 420, 525), 8, ACCENT + "15", ACCENT, 1)
    draw.text((cx - 565, 490), "START/STOP/PLAY", fill=ACCENT, font=FONT_SMALL)

    draw.line([(cx + 100, 285), (cx + 100, 310), (cx + 460, 310), (cx + 460, 335)], fill=TEXT_COLOR, width=2)
    draw.text((cx + 430, 315), "SET:", fill=SUBTEXT_COLOR, font=FONT_BOLD)
    draw_arrow_down(draw, cx + 460, 335, 375, color=SUBTEXT_COLOR, width=2)
    draw_rounded_rect(draw, (cx + 380, 380, cx + 540, 430), 8, SUBTEXT_COLOR + "22", SUBTEXT_COLOR, 2)
    draw.text((cx + 395, 395), "Config", fill=SUBTEXT_COLOR, font=FONT)
    draw_arrow_down(draw, cx + 460, 430, 470, color=SUBTEXT_COLOR, width=2)
    draw_rounded_rect(draw, (cx + 370, 475, cx + 550, 525), 8, SUBTEXT_COLOR + "15", SUBTEXT_COLOR, 1)
    draw.text((cx + 385, 490), "DELAY / REPEAT", fill=SUBTEXT_COLOR, font=FONT_SMALL)

    # Invalid
    draw.line([(cx - 150, 285), (cx - 150, 600), (cx + 350, 600), (cx + 350, 625)], fill="#9E9E9E", width=1)
    draw_rounded_rect(draw, (cx + 270, 630, cx + 430, 670), 8, "#F5F5F5", "#9E9E9E", 1)
    draw.text((cx + 285, 642), "Comando invalido", fill="#9E9E9E", font=FONT_SMALL)

    # End
    draw_rounded_rect(draw, (cx - 80, 570, cx + 80, 610), 15, GREEN, GREEN, 2)
    draw.text((cx - 25, 580), "Fim", fill="#FFFFFF", font=FONT_BOLD)

    # Connect all results to end
    for x in [cx - 500, cx - 380, cx - 140, cx + 100, cx + 280, cx + 460]:
        draw.line([(x, 525), (x, 555), (cx, 555), (cx, 570)], fill="#BDBDBD", width=1)

    img.save(os.path.join(OUTPUT_DIR, "fig5-fluxo-protocolo.png"), dpi=(150, 150))
    print("Figura 5 OK")


# ============================================================
if __name__ == "__main__":
    gen_fig1()
    gen_fig2()
    gen_fig3()
    gen_fig4()
    gen_fig5()
    print("\nTodas as figuras geradas em docs/images/")

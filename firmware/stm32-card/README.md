# stm32-card — ISO 7816 Contact Smart-Card Emulator

## Purpose

This firmware turns a STM32 Nucleo-L432KC into an ISO/IEC 7816-3 contact smart
card. A host PC, running the Iso8583Studio simulator, drives APDUs over a
USB-CDC virtual serial port. The firmware bridges those APDUs onto the physical
ISO 7816 contacts via the XCRFID 4-in-1 SIM/smart-card pinboard, so any external
smart-card terminal connected to the pinboard sees the firmware as a card. The
MCU acts in *card* (slave) role: it answers reset (ATR), responds to PPS, and
exchanges T=0 / T=1 frames driven by the terminal's clock and reset lines.

## Hardware bill of materials

| Qty | Part | Notes |
|-----|------|-------|
| 1   | ST Nucleo-L432KC | STM32L432KC, USART2 supports SmartCard mode |
| 1   | XCRFID 4-in-1 SIM/smart-card pinboard | breaks out C1..C8 contacts |
| 1   | Optional 3V3 ↔ 5V level shifter (e.g. TXS0108E) | only if the terminal drives Class A (5 V) cards |
| 1   | USB micro-B cable | for USB-CDC + ST-Link power |

## Wiring: Nucleo-L432KC → ISO 7816 contacts (via XCRFID)

| ISO 7816 contact | Function | Nucleo pin | MCU function | Notes |
|------------------|----------|------------|--------------|-------|
| C1 | VCC      | 3V3 (or 5V via shifter) | power | Class B (3 V) is safest. Use a level shifter for Class A 5 V terminals — the L432 I/O is not 5 V tolerant on USART pads. |
| C2 | RST      | PA0        | GPIO input (EXTI) | Terminal drives RST; firmware samples it to start ATR. |
| C3 | CLK      | PA8        | USART2_CK or TIM1_CH1 PWM | When firmware *generates* the card clock for self-test, output ~3.5712 MHz. In normal operation, the *terminal* supplies CLK and the MCU only consumes it for USART2 SmartCard mode. Wire as input in that case. |
| C5 | GND      | GND        | ground       | Common reference. |
| C7 | I/O      | PA2        | USART2_TX (half-duplex, open-drain, pull-up) | Bidirectional; ISO 7816 line is open-drain with 20k pull-up. |
| C4/C6/C8 | RFU/SPU/SWP | not connected | — | Unused for contact ICC. |

Notes:
- 3V3 vs 5V tradeoff: 3V3 keeps wiring simple and matches the L432's I/O domain
  but only works with Class B/C-capable terminals. A level shifter on C7 (and
  optionally on C2/C3) is required for Class A terminals.
- Add a 20 kΩ pull-up from C7 to VCC on the pinboard side; ISO 7816 I/O is
  open-drain.

## Quick start

```sh
cd firmware/stm32-card
pio run                    # compile
pio run -t upload          # flash via on-board ST-Link
pio device monitor         # 115200 baud (USB-CDC echoes ping/error frames)
```

## USB-CDC framing protocol

Each USB-CDC packet exchanged between PC and firmware is a single frame:

```
+------+--------+--------+----------------------------+----------+----------+
| 0xA5 | len_lo | len_hi |  payload[0..len-1]         | crc16_lo | crc16_hi |
+------+--------+--------+----------------------------+----------+----------+
```

- `0xA5` — start-of-frame sentinel.
- `len`  — little-endian payload length (excludes header and CRC).
- `crc16` — CRC-16/CCITT-FALSE over the payload bytes only.
  Polynomial `0x1021`, init `0xFFFF`, no reflection, no final XOR.
- Direction is implicit by which USB endpoint carries the frame
  (OUT = PC → firmware, IN = firmware → PC).

Payload byte 0 is a *type* tag; the remaining payload bytes are type-specific:

| Type | Direction        | Meaning                                  |
|------|------------------|------------------------------------------|
| 0x01 | PC → firmware    | C-APDU command to forward to the wire    |
| 0x02 | firmware → PC    | R-APDU response received from the wire   |
| 0x10 | firmware → PC    | ATR notification (raw ATR bytes)         |
| 0x20 | firmware → PC    | Card event: insertion/removal/reset detected |
| 0xFE | firmware → PC    | Error report (status code + message)     |
| 0xFF | both             | Ping/keep-alive                          |

Because the firmware acts as the *card*, the data flow is:

```
PC --(C-APDU type=0x01)--> firmware --(T=0/T=1)--> terminal
PC <--(R-APDU type=0x02)-- firmware <--(T=0/T=1)-- terminal
```

A terminal connected to the pinboard sees the firmware as a contact card.

## What works

Implemented and meant to compile + run idle on hardware today:

- `SystemClock_Config()` — HSI16 → PLL → 80 MHz SYSCLK / HCLK / APB1 / APB2,
  HSI48 enabled with CRS auto-trim off USB SOF.
- `MX_GPIO_Init()` — RCC enables for GPIOA/B/C, PA0 RST as EXTI input, PA2/PA3
  USART2 TX/RX in AF7 open-drain, PA8 TIM1_CH1 PWM, on-board LED on PB3.
- `MX_TIM1_Init()` — TIM1_CH1 PWM on PA8 generating ~3.478 MHz (within ISO
  7816's 1–5 MHz card-clock band; exact 3.5712 MHz needs an HSE crystal).
- `MX_USART2_SmartCard_Init()` — USART2 in SmartCard mode, 9600 baud at the
  3.5712 MHz card clock, 9-bit word, even parity, 1.5 stop bits, NACK on.
- USB-CDC plumbing — `cdc_write()` wraps `CDC_Transmit_FS`; `cdc_receive()`
  pushes bytes into the apdu_bridge RX ring buffer. The Cube-generated stack
  symbols are declared weak so the skeleton links even before the generated
  `usb_device.c` / `usbd_cdc_if.c` files are added.
- Frame parser end-to-end — full CRC-16/CCITT-FALSE encode + decode, single
  inbox slot drained by the super-loop, error-frame emission on CRC mismatch.
- Super-loop dispatch — type 0xFF (PING) replies with type 0x10 (ATR
  notification) carrying the test ATR `3B 65 00 00 20 63 CB 68 00`; type 0x01
  (C-APDU) is forwarded to `iso7816_link_send_apdu_to_card()`; PB3 LED toggles
  once per loop iteration as a heartbeat.

## Behavioural pieces — TODO

The following are intentionally stubbed in code:

- ATR generation (`iso7816_link_send_atr`) — historical bytes, TA1/TB1/TC1/TD1
  protocol-indicator bytes, TCK calculation.
- T=0 link layer (ISO 7816-3 §10): procedure byte (ACK / NULL 0x60 / SW1),
  byte-by-byte data phase, work waiting time (WWT).
- T=1 link layer (ISO 7816-3 §11): I/R/S blocks, NAD/PCB/LEN, LRC/CRC EDC,
  block waiting time (BWT), waiting-time extension (WTX, S-block).
- PPS exchange — accept/reject PTS request, switch protocol & Fi/Di.
- ATR-on-reset over the contact lines — currently the test ATR is only
  surfaced to the PC over USB-CDC; it is not yet driven onto the C7 I/O line
  in response to a C2 RST edge.
- Card profile load — no mechanism yet to ingest a profile (ATR + filesystem
  + APDU script) from the PC and pin it into the link layer.

Search the source for `TODO` to find each anchor.

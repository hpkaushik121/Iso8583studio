/*
 * main.h — shared declarations for the stm32-card firmware.
 *
 * Pulls in the STM32L4 HAL (when the framework headers are visible) and
 * publishes the few extern peripheral handles owned by main.c.
 *
 * Architecture (after USART rewire):
 *   - USART2 (PA2/PA3, plain UART 115200 8N1) bridges to the on-board ST-Link
 *     virtual COM port → host laptop sees it as /dev/cu.usbmodemXXXX.
 *   - USART1 (PA9/PA10, SmartCard mode) drives the C7 I/O line on the pinboard
 *     to act as a contact ICC for an external reader / POS.
 *   - PA0  is C2 RST (input + EXTI), PA8 is C3 CLK (TIM1_CH1 PWM out), PB3 LED.
 */

#ifndef MAIN_H
#define MAIN_H

#include <stdint.h>
#include <stddef.h>

#if __has_include("stm32l4xx_hal.h")
#  include "stm32l4xx_hal.h"
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* ----- Peripheral handles owned by main.c ------------------------------- */

#if __has_include("stm32l4xx_hal.h")
extern UART_HandleTypeDef       huart2;        /* USART2 = host bridge (ST-Link VCP) */
extern SMARTCARD_HandleTypeDef  hsmartcard1;   /* USART1 = ISO 7816 SmartCard line   */
extern TIM_HandleTypeDef        htim1;         /* PA8 = TIM1_CH1 card clock          */
#endif

/* ----- GPIO mapping (Nucleo-L432KC) ------------------------------------- */

#define LED_GPIO_PORT      GPIOB
#define LED_PIN            GPIO_PIN_3      /* on-board green LED on PB3 */

#define ISO_RST_GPIO_PORT  GPIOA
#define ISO_RST_PIN        GPIO_PIN_0      /* C2 RST line (input/EXTI)   */

#define ISO_CLK_GPIO_PORT  GPIOA
#define ISO_CLK_PIN        GPIO_PIN_8      /* C3 CLK = TIM1_CH1 PWM out  */

/* USART2 = host bridge — Nucleo-L432KC routes the on-board ST-Link VCP to:
 *   VCP_TX (host -> MCU) -> PA15 (USART2_RX, AF3)
 *   VCP_RX (MCU -> host) -> PA2  (USART2_TX, AF7)
 * See ST UM2105 §6.6.6. PA3 is NOT connected to ST-Link on this board — it
 * lives on the Arduino A2 header. Note the asymmetric AF (TX=AF7, RX=AF3). */
#define HOST_UART_TX_PORT  GPIOA
#define HOST_UART_TX_PIN   GPIO_PIN_2
#define HOST_UART_RX_PORT  GPIOA
#define HOST_UART_RX_PIN   GPIO_PIN_15

/* USART1 = ISO 7816 SmartCard line on the C7 wire (PA9 TX, PA10 RX, AF7).
 * Half-duplex open-drain with internal pull-up to drive a card I/O line. */
#define ISO_IO_TX_PORT     GPIOA
#define ISO_IO_TX_PIN      GPIO_PIN_9
#define ISO_IO_RX_PORT     GPIOA
#define ISO_IO_RX_PIN      GPIO_PIN_10

/* ----- Host bridge wrapper API (replaces the old cdc_* shims) ---------- */

/* Push a chunk of bytes to the host over the USART2 ST-Link VCP bridge.
 * Returns 0 on success, non-zero on transmit error or busy. */
int  bridge_uart_write(const uint8_t *buf, uint16_t len);

/* Optional fatal-error trap — drops into a tight loop with the LED pulsing. */
void Error_Handler(void);

#ifdef __cplusplus
}
#endif

#endif /* MAIN_H */

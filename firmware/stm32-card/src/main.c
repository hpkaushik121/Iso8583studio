/*
 * main.c — STM32L432KC ISO 7816 smart-card emulator entry point.
 *
 * Role: act as a contact ICC (slave). The PC drives APDUs through the on-board
 * ST-Link's virtual COM port (which routes to USART2 on the L432KC), the
 * firmware bridges them to a real terminal over the ISO 7816 contacts on
 * USART1 in SmartCard mode.
 *
 * USART layout (Nucleo-L432KC PCB constraint):
 *   USART2 (PA2/PA3) — wired through SB16/SB17 to the ST-Link UART.
 *                      The laptop sees /dev/cu.usbmodemXXXX.
 *                      We use it as a plain 115200 8N1 UART.
 *   USART1 (PA9/PA10) — free, used in SmartCard mode for the C7 I/O wire.
 *
 * Clock tree:
 *   HSI16 -> PLL -> 80 MHz SYSCLK / HCLK / APB1 / APB2.
 *   ISO 7816 CLK (~3.5712 MHz) is generated on PA8 by TIM1_CH1 PWM.
 */

#include <stdint.h>
#include <string.h>

#include "main.h"
#include "iso7816_link.h"
#include "apdu_bridge.h"

/* ------------------------------------------------------------------------- */
/* Peripheral handles                                                        */
/* ------------------------------------------------------------------------- */

#if __has_include("stm32l4xx_hal.h")
UART_HandleTypeDef      huart2;        /* host bridge: USART2 ↔ ST-Link VCP */
SMARTCARD_HandleTypeDef hsmartcard1;   /* card line:  USART1 ISO 7816       */
TIM_HandleTypeDef       htim1;         /* card clock: TIM1_CH1 on PA8       */

/* Single-byte RX buffer for USART2 — driven by HAL_UART_Receive_IT.
 * Each completed receive enqueues the byte into apdu_bridge and re-arms. */
static volatile uint8_t s_uart_rx_byte;

/* Diagnostic echo flag set by the RX ISR; drained from the super-loop. */
extern volatile uint8_t s_echo_pending;
extern volatile uint8_t s_echo_byte;
#endif

/* ------------------------------------------------------------------------- */
/* Forward declarations                                                      */
/* ------------------------------------------------------------------------- */

static void SystemClock_Config(void);
static void MX_GPIO_Init(void);
static void MX_USART2_UART_Init(void);
static void MX_USART1_SmartCard_Init(void);
static void MX_TIM1_Init(void);

/* ------------------------------------------------------------------------- */
/* main                                                                      */
/* ------------------------------------------------------------------------- */

/* Boot-trace LED helper — N quick blinks at ~6 Hz, then a long pause.
 * Used to mark which init step we successfully passed. If the LED settles
 * solid (Error_Handler fast-toggle) we know the *next* step failed. */
#if __has_include("stm32l4xx_hal.h")
static void boot_blink_led_minimal(void)
{
    /* Init just the LED so we can blink before MX_GPIO_Init runs. */
    __HAL_RCC_GPIOB_CLK_ENABLE();
    GPIO_InitTypeDef gp = {0};
    gp.Pin   = LED_PIN;
    gp.Mode  = GPIO_MODE_OUTPUT_PP;
    gp.Pull  = GPIO_NOPULL;
    gp.Speed = GPIO_SPEED_FREQ_LOW;
    HAL_GPIO_Init(LED_GPIO_PORT, &gp);
}

static void boot_pulse(uint8_t n)
{
    for (uint8_t i = 0; i < n; ++i) {
        HAL_GPIO_WritePin(LED_GPIO_PORT, LED_PIN, GPIO_PIN_SET);
        HAL_Delay(80);
        HAL_GPIO_WritePin(LED_GPIO_PORT, LED_PIN, GPIO_PIN_RESET);
        HAL_Delay(120);
    }
    HAL_Delay(700);   /* long gap so the blink count is unambiguous */
}
#endif

int main(void)
{
#if __has_include("stm32l4xx_hal.h")
    HAL_Init();
#endif

    SystemClock_Config();

#if __has_include("stm32l4xx_hal.h")
    /* If we get here, HAL_Init + SystemClock_Config both succeeded.
     * Set up the LED early so the boot trace works. */
    boot_blink_led_minimal();
    boot_pulse(1);   /* 1 = past clock setup */
#endif

    MX_GPIO_Init();
#if __has_include("stm32l4xx_hal.h")
    boot_pulse(2);   /* 2 = past GPIO mux  */
#endif

    MX_TIM1_Init();                /* PA8 generates the 3.5712 MHz CLK   */
#if __has_include("stm32l4xx_hal.h")
    boot_pulse(3);   /* 3 = past TIM1 PWM  */
#endif

    MX_USART2_UART_Init();         /* USART2 = host bridge (ST-Link VCP) */
#if __has_include("stm32l4xx_hal.h")
    boot_pulse(4);   /* 4 = past USART2 init */
#endif

    apdu_bridge_init();
    iso7816_link_init();
#if __has_include("stm32l4xx_hal.h")
    boot_pulse(5);   /* 5 = past bridge / link init — about to enter super-loop */
#endif

#if __has_include("stm32l4xx_hal.h")
    /* Boot banner — proves the USART2 TX path works end-to-end.
     * If you open `pio device monitor -p /dev/cu.usbmodem2103 -b 115200`
     * and reset the Nucleo, this string should appear. */
    static const char k_banner[] = "stm32-card boot ok (rx=poll)\r\n";
    HAL_UART_Transmit(&huart2, (uint8_t *)k_banner,
                      (uint16_t)(sizeof(k_banner) - 1), 1000);

    /* RX is handled by direct register polling in the super-loop, not by
     * HAL_UART_Receive_IT — the IT path was failing to arm on this build,
     * and polling avoids any HAL state-machine entanglement. */
#endif

    /* Test ATR replied to PING frames so the host tooling can confirm a
     * healthy bridge before the link layer is implemented. */
    static const uint8_t k_test_atr[] = {
        0x3B, 0x65, 0x00, 0x00, 0x20, 0x63, 0xCB, 0x68, 0x00
    };

    /* Cooperative super-loop. Both pollers must be non-blocking. */
    for (;;) {
#if __has_include("stm32l4xx_hal.h")
        /* Direct USART2 RX poll — read RDR whenever the ISR_RXNE_RXFNE bit
         * is set in the ISR. Bypasses the HAL's interrupt-driven RX entirely.
         * On STM32L4 the bit names differ slightly across HAL versions; we
         * use the literal register bit (USART_ISR_RXNE on L432) which is the
         * common L4 spelling. Reading RDR clears RXNE automatically. */
        if (USART2->ISR & USART_ISR_RXNE) {
            uint8_t b = (uint8_t)(USART2->RDR & 0xFFu);
            apdu_bridge_rx_push(&b, 1);
            /* Diagnostic echo — proves the round-trip in `pio device monitor`. */
            HAL_UART_Transmit(&huart2, &b, 1, 100);
        }
#endif
        apdu_bridge_poll();
        iso7816_link_poll();

        uint8_t  type;
        uint8_t  payload[APDU_BRIDGE_MAX_PAYLOAD];
        uint16_t plen;

        if (apdu_bridge_try_pop_frame(&type, payload, &plen)) {
            switch (type) {
            case APDU_BRIDGE_TYPE_C_APDU:
                iso7816_link_send_apdu_to_card(payload, plen);
                break;

            case APDU_BRIDGE_TYPE_PING:
                apdu_bridge_send_response(APDU_BRIDGE_TYPE_ATR,
                                          k_test_atr,
                                          (uint16_t)sizeof(k_test_atr));
                break;

            default: {
                uint8_t err[2] = { 0x01u, type };
                apdu_bridge_send_response(APDU_BRIDGE_TYPE_ERROR,
                                          err, (uint16_t)sizeof(err));
                break;
            }
            }
        }

#if __has_include("stm32l4xx_hal.h")
        /* Non-blocking 1 Hz heartbeat. HAL_Delay would starve the RX poll
         * at 500ms per iteration; track elapsed ticks instead. */
        static uint32_t s_last_blink = 0;
        uint32_t now = HAL_GetTick();
        if ((now - s_last_blink) >= 500U) {
            s_last_blink = now;
            HAL_GPIO_TogglePin(LED_GPIO_PORT, LED_PIN);
        }
#endif
    }
}

/* ------------------------------------------------------------------------- */
/* SystemClock_Config — HSI16 -> PLL -> 80 MHz                               */
/* ------------------------------------------------------------------------- */

static void SystemClock_Config(void)
{
#if __has_include("stm32l4xx_hal.h")
    RCC_OscInitTypeDef       osc  = {0};
    RCC_ClkInitTypeDef       clk  = {0};
    RCC_PeriphCLKInitTypeDef pclk = {0};

    if (HAL_PWREx_ControlVoltageScaling(PWR_REGULATOR_VOLTAGE_SCALE1) != HAL_OK) {
        Error_Handler();
    }

    osc.OscillatorType      = RCC_OSCILLATORTYPE_HSI;
    osc.HSIState            = RCC_HSI_ON;
    osc.HSICalibrationValue = RCC_HSICALIBRATION_DEFAULT;
    osc.PLL.PLLState        = RCC_PLL_ON;
    osc.PLL.PLLSource       = RCC_PLLSOURCE_HSI;
    osc.PLL.PLLM            = 1;
    osc.PLL.PLLN            = 10;
    osc.PLL.PLLP            = RCC_PLLP_DIV7;
    osc.PLL.PLLQ            = RCC_PLLQ_DIV2;
    osc.PLL.PLLR            = RCC_PLLR_DIV2; /* 80 MHz SYSCLK */
    if (HAL_RCC_OscConfig(&osc) != HAL_OK) {
        Error_Handler();
    }

    clk.ClockType      = RCC_CLOCKTYPE_HCLK   | RCC_CLOCKTYPE_SYSCLK |
                         RCC_CLOCKTYPE_PCLK1  | RCC_CLOCKTYPE_PCLK2;
    clk.SYSCLKSource   = RCC_SYSCLKSOURCE_PLLCLK;
    clk.AHBCLKDivider  = RCC_SYSCLK_DIV1;
    clk.APB1CLKDivider = RCC_HCLK_DIV1;
    clk.APB2CLKDivider = RCC_HCLK_DIV1;
    if (HAL_RCC_ClockConfig(&clk, FLASH_LATENCY_4) != HAL_OK) {
        Error_Handler();
    }

    /* USART2 kernel clock = PCLK1 (80 MHz). USART1 will be configured in
     * Step 1 when SmartCard mode is brought up. */
    pclk.PeriphClockSelection = RCC_PERIPHCLK_USART2;
    pclk.Usart2ClockSelection = RCC_USART2CLKSOURCE_PCLK1;
    if (HAL_RCCEx_PeriphCLKConfig(&pclk) != HAL_OK) {
        Error_Handler();
    }
#endif
}

/* ------------------------------------------------------------------------- */
/* MX_GPIO_Init — clocks + pin muxing                                        */
/* ------------------------------------------------------------------------- */

static void MX_GPIO_Init(void)
{
#if __has_include("stm32l4xx_hal.h")
    GPIO_InitTypeDef gp = {0};

    __HAL_RCC_GPIOA_CLK_ENABLE();
    __HAL_RCC_GPIOB_CLK_ENABLE();
    __HAL_RCC_GPIOC_CLK_ENABLE();

    /* PA0 — C2 RST input with EXTI on both edges. */
    gp.Pin   = ISO_RST_PIN;
    gp.Mode  = GPIO_MODE_IT_RISING_FALLING;
    gp.Pull  = GPIO_PULLUP;
    gp.Speed = GPIO_SPEED_FREQ_LOW;
    HAL_GPIO_Init(ISO_RST_GPIO_PORT, &gp);

    /* PA2 — USART2_TX, AF7. */
    gp.Pin       = HOST_UART_TX_PIN;
    gp.Mode      = GPIO_MODE_AF_PP;
    gp.Pull      = GPIO_PULLUP;
    gp.Speed     = GPIO_SPEED_FREQ_HIGH;
    gp.Alternate = GPIO_AF7_USART2;
    HAL_GPIO_Init(HOST_UART_TX_PORT, &gp);

    /* PA15 — USART2_RX, AF3. Different alternate-function number from TX,
     * which is why this is a separate HAL_GPIO_Init call. PA15 is also
     * the JTDI/SWDIO line — multiplexing it onto USART2_RX disables JTAG
     * (we only use SWD via PA13/PA14, which is unaffected). */
    gp.Pin       = HOST_UART_RX_PIN;
    gp.Mode      = GPIO_MODE_AF_PP;
    gp.Pull      = GPIO_PULLUP;
    gp.Speed     = GPIO_SPEED_FREQ_HIGH;
    gp.Alternate = GPIO_AF3_USART2;
    HAL_GPIO_Init(HOST_UART_RX_PORT, &gp);

    /* PA9 / PA10 — USART1 SmartCard mode for the ISO 7816 I/O line.
     * Open-drain with internal pull-up — ISO 7816 wire is open-drain. */
    gp.Pin       = ISO_IO_TX_PIN | ISO_IO_RX_PIN;
    gp.Mode      = GPIO_MODE_AF_OD;
    gp.Pull      = GPIO_PULLUP;
    gp.Speed     = GPIO_SPEED_FREQ_HIGH;
    gp.Alternate = GPIO_AF7_USART1;
    HAL_GPIO_Init(GPIOA, &gp);

    /* PA8 — TIM1_CH1 PWM at ~3.478 MHz (ISO 7816 CLK). */
    gp.Pin       = ISO_CLK_PIN;
    gp.Mode      = GPIO_MODE_AF_PP;
    gp.Pull      = GPIO_NOPULL;
    gp.Speed     = GPIO_SPEED_FREQ_VERY_HIGH;
    gp.Alternate = GPIO_AF1_TIM1;
    HAL_GPIO_Init(ISO_CLK_GPIO_PORT, &gp);

    /* PB3 — on-board green LED. */
    gp.Pin   = LED_PIN;
    gp.Mode  = GPIO_MODE_OUTPUT_PP;
    gp.Pull  = GPIO_NOPULL;
    gp.Speed = GPIO_SPEED_FREQ_LOW;
    gp.Alternate = 0;
    HAL_GPIO_Init(LED_GPIO_PORT, &gp);
    HAL_GPIO_WritePin(LED_GPIO_PORT, LED_PIN, GPIO_PIN_RESET);

    /* IRQs: EXTI0 for RST, USART2 IRQ for the host RX-byte interrupt.
     * USART1 IRQ is left unenabled until Step 1 lights up the SmartCard line. */
    HAL_NVIC_SetPriority(EXTI0_IRQn, 5, 0);
    HAL_NVIC_EnableIRQ(EXTI0_IRQn);
    HAL_NVIC_SetPriority(USART2_IRQn, 6, 0);
    HAL_NVIC_EnableIRQ(USART2_IRQn);
#endif
}

/* ------------------------------------------------------------------------- */
/* MX_USART2_UART_Init — plain 115200 8N1 to the ST-Link VCP                  */
/* ------------------------------------------------------------------------- */

static void MX_USART2_UART_Init(void)
{
#if __has_include("stm32l4xx_hal.h")
    /* Enable the bus clock for USART2 BEFORE any register access, otherwise
     * HAL_UART_Init faults the bus when it tries to write USART2->CR1. */
    __HAL_RCC_USART2_CLK_ENABLE();

    /* Defensive: if the peripheral is in an unexpected state from a prior
     * boot or a partial init, clear it. DeInit ignores the gState check. */
    huart2.Instance = USART2;
    HAL_UART_DeInit(&huart2);

    /* Re-zero the Init struct so no stale field from earlier code paths
     * (e.g. SmartCard mode bits) lingers. */
    memset(&huart2.Init, 0, sizeof(huart2.Init));
    memset(&huart2.AdvancedInit, 0, sizeof(huart2.AdvancedInit));

    huart2.Instance              = USART2;
    huart2.Init.BaudRate         = 115200;
    huart2.Init.WordLength       = UART_WORDLENGTH_8B;
    huart2.Init.StopBits         = UART_STOPBITS_1;
    huart2.Init.Parity           = UART_PARITY_NONE;
    huart2.Init.Mode             = UART_MODE_TX_RX;
    huart2.Init.HwFlowCtl        = UART_HWCONTROL_NONE;
    huart2.Init.OverSampling     = UART_OVERSAMPLING_16;
    huart2.Init.OneBitSampling   = UART_ONE_BIT_SAMPLE_DISABLE;
    huart2.AdvancedInit.AdvFeatureInit = UART_ADVFEATURE_NO_INIT;

    if (HAL_UART_Init(&huart2) != HAL_OK) {
        Error_Handler();
    }
#endif
}

/* ------------------------------------------------------------------------- */
/* MX_USART1_SmartCard_Init — ISO 7816 T=0 on PA9 (TX) / PA10 (RX)            */
/*                                                                            */
/* Card clock f = 3.478 MHz on PA8, etu = 372 / f, default Fi/Di = 372/1.    */
/*   etu  ≈ 107 µs                                                            */
/*   baud ≈ 9352 bit/s (close enough to nominal 9600 for self-test).         */
/* ------------------------------------------------------------------------- */

static void MX_USART1_SmartCard_Init(void)
{
#if __has_include("stm32l4xx_hal.h")
    __HAL_RCC_USART1_CLK_ENABLE();

    hsmartcard1.Instance                  = USART1;
    hsmartcard1.Init.BaudRate             = 9600;
    hsmartcard1.Init.WordLength           = SMARTCARD_WORDLENGTH_9B;
    hsmartcard1.Init.StopBits             = SMARTCARD_STOPBITS_1_5;
    hsmartcard1.Init.Parity               = SMARTCARD_PARITY_EVEN;
    hsmartcard1.Init.Mode                 = SMARTCARD_MODE_TX_RX;
    hsmartcard1.Init.CLKPolarity          = SMARTCARD_POLARITY_LOW;
    hsmartcard1.Init.CLKPhase             = SMARTCARD_PHASE_1EDGE;
    hsmartcard1.Init.CLKLastBit           = SMARTCARD_LASTBIT_ENABLE;
    hsmartcard1.Init.Prescaler            = 11;
    hsmartcard1.Init.GuardTime            = 16;
    hsmartcard1.Init.NACKEnable           = SMARTCARD_NACK_ENABLE;
    hsmartcard1.Init.TimeOutEnable        = SMARTCARD_TIMEOUT_DISABLE;
    hsmartcard1.Init.BlockLength          = 0;
    hsmartcard1.Init.AutoRetryCount       = 3;
    hsmartcard1.Init.OneBitSampling       = SMARTCARD_ONE_BIT_SAMPLE_DISABLE;
    hsmartcard1.AdvancedInit.AdvFeatureInit = SMARTCARD_ADVFEATURE_NO_INIT;

    if (HAL_SMARTCARD_Init(&hsmartcard1) != HAL_OK) {
        Error_Handler();
    }
#endif
}

/* ------------------------------------------------------------------------- */
/* MX_TIM1_Init — PWM @ ~3.478 MHz on PA8 (TIM1_CH1)                          */
/* ------------------------------------------------------------------------- */

static void MX_TIM1_Init(void)
{
#if __has_include("stm32l4xx_hal.h")
    TIM_OC_InitTypeDef     oc   = {0};
    TIM_MasterConfigTypeDef mst = {0};

    __HAL_RCC_TIM1_CLK_ENABLE();

    htim1.Instance               = TIM1;
    htim1.Init.Prescaler         = 0;
    htim1.Init.CounterMode       = TIM_COUNTERMODE_UP;
    htim1.Init.Period            = 22;
    htim1.Init.ClockDivision     = TIM_CLOCKDIVISION_DIV1;
    htim1.Init.RepetitionCounter = 0;
    htim1.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
    if (HAL_TIM_PWM_Init(&htim1) != HAL_OK) {
        Error_Handler();
    }

    mst.MasterOutputTrigger = TIM_TRGO_RESET;
    mst.MasterSlaveMode     = TIM_MASTERSLAVEMODE_DISABLE;
    HAL_TIMEx_MasterConfigSynchronization(&htim1, &mst);

    oc.OCMode      = TIM_OCMODE_PWM1;
    oc.Pulse       = 11;
    oc.OCPolarity  = TIM_OCPOLARITY_HIGH;
    oc.OCFastMode  = TIM_OCFAST_DISABLE;
    oc.OCIdleState = TIM_OCIDLESTATE_RESET;
    if (HAL_TIM_PWM_ConfigChannel(&htim1, &oc, TIM_CHANNEL_1) != HAL_OK) {
        Error_Handler();
    }
    HAL_TIM_PWM_Start(&htim1, TIM_CHANNEL_1);
#endif
}

/* ------------------------------------------------------------------------- */
/* USART2 host-bridge wrappers                                               */
/* ------------------------------------------------------------------------- */

int bridge_uart_write(const uint8_t *buf, uint16_t len)
{
#if __has_include("stm32l4xx_hal.h")
    /* Blocking transmit — at 115200 baud, 1 byte ≈ 87 µs. A 30-byte ATR
     * frame costs ~2.6 ms, fine for the super-loop given heartbeat is
     * already taking longer than that. If we ever push large bursts (full
     * APDU at 256 bytes ≈ 22 ms), switch to HAL_UART_Transmit_IT. */
    HAL_StatusTypeDef rc = HAL_UART_Transmit(&huart2, (uint8_t *)buf, len, 1000);
    return (rc == HAL_OK) ? 0 : 1;
#else
    (void)buf; (void)len;
    return 0;
#endif
}

#if __has_include("stm32l4xx_hal.h")

/* USART2 RX interrupt: each completed 1-byte receive lands here, gets
 * pushed to the apdu_bridge ring buffer, and the receive is re-armed.
 *
 * DIAGNOSTIC: also queue an echo of the received byte back to the host so
 * we can prove the RX path is working with a plain serial-monitor session.
 * Once Step 0 is verified, this echo can be removed (it doesn't interfere
 * with the framed protocol — Studio's frame parser ignores stray bytes
 * outside the SOF/length/CRC envelope). */
volatile uint8_t s_echo_pending;
volatile uint8_t s_echo_byte;

void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
    if (huart->Instance == USART2) {
        uint8_t b = (uint8_t)s_uart_rx_byte;
        apdu_bridge_rx_push(&b, 1);
        s_echo_byte = b;
        s_echo_pending = 1;
        HAL_UART_Receive_IT(&huart2, (uint8_t *)&s_uart_rx_byte, 1);
    }
}

/* USART2 IRQ vector — also handles error flags so a glitch doesn't lock
 * up the receive chain. On any error we just re-arm the receive. */
void USART2_IRQHandler(void)
{
    HAL_UART_IRQHandler(&huart2);
}

void HAL_UART_ErrorCallback(UART_HandleTypeDef *huart)
{
    if (huart->Instance == USART2) {
        /* Clear pending errors implicitly via HAL_UART_Receive_IT re-arm. */
        HAL_UART_Receive_IT(&huart2, (uint8_t *)&s_uart_rx_byte, 1);
    }
}

/* USART1 IRQ vector is intentionally not defined here. It will be added
 * back in Step 1 along with MX_USART1_SmartCard_Init() and the link-layer
 * implementation. Leaving it absent means the weak default handler in the
 * Cube startup file traps any spurious USART1 interrupt — but the IRQ is
 * not enabled in NVIC so this code path never runs. */

#endif

/* ------------------------------------------------------------------------- */
/* Error_Handler — flash the LED in a tight loop.                            */
/* ------------------------------------------------------------------------- */

void Error_Handler(void)
{
#if __has_include("stm32l4xx_hal.h")
    __disable_irq();
    for (;;) {
        HAL_GPIO_TogglePin(LED_GPIO_PORT, LED_PIN);
        for (volatile uint32_t i = 0; i < 200000U; ++i) { __asm__ volatile("nop"); }
    }
#else
    for (;;) { }
#endif
}

#if __has_include("stm32l4xx_hal.h")
__attribute__((weak)) void SysTick_Handler(void)
{
    HAL_IncTick();
}
#endif

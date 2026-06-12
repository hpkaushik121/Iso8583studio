/*
 * iso7816_link.h — ISO/IEC 7816-3 contact link layer (card side).
 *
 * The firmware acts as the card. The terminal supplies VCC, CLK, and drives
 * RST. We answer with an ATR, optionally negotiate PPS, then run T=0 or T=1
 * APDU exchanges. All entry points here are non-blocking; the heavy lifting
 * happens inside iso7816_link_poll() and the USART2 SmartCard ISR.
 */

#ifndef ISO7816_LINK_H
#define ISO7816_LINK_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Link-layer protocol identifier as carried in TD1 nibble. */
typedef enum {
    ISO7816_PROTO_T0 = 0,
    ISO7816_PROTO_T1 = 1
} iso7816_proto_t;

/* Bring USART2 SmartCard mode + RST EXTI into a known state. Must be called
 * after MX_USART2_SMARTCARD_Init(). */
void iso7816_link_init(void);

/* Push the configured ATR onto the I/O line. Called from the RST-deassert
 * handler. The ATR layout is built once at boot and cached. */
void iso7816_link_send_atr(void);

/* Send an R-APDU (response data || SW1 SW2) from the card to the terminal,
 * framed appropriately for the currently active protocol (T=0 or T=1). */
void iso7816_link_send_response(uint8_t *data, uint16_t len);

/* Cooperative poll: drains the RX FIFO, advances the protocol state machine,
 * services WTX timers, and when a complete C-APDU has been assembled hands
 * it up to the bridge for forwarding to the PC. Must be called frequently. */
void iso7816_link_poll(void);

/* Stash a C-APDU received from the PC. The link layer holds onto it until
 * the terminal next polls for data, then uses iso7816_link_send_response to
 * answer. Stub for now — the body is filled in alongside the T=0 / T=1 work. */
void iso7816_link_send_apdu_to_card(const uint8_t *capdu, uint16_t len);

#ifdef __cplusplus
}
#endif

#endif /* ISO7816_LINK_H */

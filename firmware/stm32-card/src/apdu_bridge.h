/*
 * apdu_bridge.h — USB-CDC ↔ ISO 7816 link bridge.
 *
 * The bridge owns the framing layer described in README.md: it parses
 * incoming USB-CDC bytes into typed payloads, hands C-APDUs to the link
 * layer (so the firmware-as-card answers a terminal), and packages
 * outgoing R-APDUs / ATR notifications / events back to the PC.
 */

#ifndef APDU_BRIDGE_H
#define APDU_BRIDGE_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Payload type tags carried in payload[0] of every framed message. */
#define APDU_BRIDGE_TYPE_C_APDU      0x01u  /* PC -> firmware */
#define APDU_BRIDGE_TYPE_R_APDU      0x02u  /* firmware -> PC */
#define APDU_BRIDGE_TYPE_ATR         0x10u  /* firmware -> PC */
#define APDU_BRIDGE_TYPE_CARD_EVENT  0x20u  /* firmware -> PC */
#define APDU_BRIDGE_TYPE_ERROR       0xFEu  /* firmware -> PC */
#define APDU_BRIDGE_TYPE_PING        0xFFu  /* either direction */

/* Frame sentinel (first byte of every on-wire frame). */
#define APDU_BRIDGE_SOF              0xA5u

/* Maximum payload length we accept from the host. ISO 7816-4 short APDUs cap
 * at ~261 bytes; extended APDUs go up to 65535. Pick a generous-but-bounded
 * upper limit for the static buffers. */
#define APDU_BRIDGE_MAX_PAYLOAD      1024u

/* CRC-16/CCITT-FALSE: poly 0x1021, init 0xFFFF, no reflection, no final XOR.
 * Computed over the payload bytes only (not the SOF/length fields). */
uint16_t apdu_bridge_crc16(const uint8_t *data, uint16_t len);

/* Reset parser state and prime USB-CDC RX. */
void apdu_bridge_init(void);

/* Pull bytes off the USB-CDC RX ring and feed the frame parser. Must be
 * called in the super-loop. Non-blocking. */
void apdu_bridge_poll(void);

/* Encode (type, payload) as a framed message and queue it on the USB-CDC TX
 * endpoint. Used by iso7816_link.c to surface R-APDUs / ATR / events. */
void apdu_bridge_send_response(uint8_t type,
                               const uint8_t *payload,
                               uint16_t len);

/* Push raw bytes received from the USB-CDC OUT endpoint into the RX ring.
 * Safe to call from the CDC receive callback context. */
void apdu_bridge_rx_push(const uint8_t *data, uint32_t len);

/* Try to dequeue a fully-validated frame the parser produced.
 *
 *   out_type    — receives payload[0] (the type tag)
 *   out_payload — receives payload[1..] (length = *out_len)
 *   out_len     — set to the number of body bytes copied (excludes type)
 *
 * Returns 1 if a frame was popped, 0 if the inbox is empty. */
uint8_t apdu_bridge_try_pop_frame(uint8_t *out_type,
                                  uint8_t *out_payload,
                                  uint16_t *out_len);

#ifdef __cplusplus
}
#endif

#endif /* APDU_BRIDGE_H */

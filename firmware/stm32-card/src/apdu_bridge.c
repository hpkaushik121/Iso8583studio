/*
 * apdu_bridge.c — USART2 ↔ ISO 7816 framing bridge.
 *
 * Originally written for USB-CDC; rewired to USART2 (the on-board ST-Link
 * VCP route) since the Nucleo-L432KC PCB doesn't expose the L432KC's native
 * USB. The frame format is unchanged — only the underlying transport differs.
 *
 * Frame layout (little-endian length, CRC over payload only):
 *
 *   +------+--------+--------+----------------+----------+----------+
 *   | 0xA5 | len_lo | len_hi | payload[0..n-1]| crc_lo   | crc_hi   |
 *   +------+--------+--------+----------------+----------+----------+
 *
 * payload[0] is a type tag (see apdu_bridge.h). The CRC is CRC-16/CCITT-FALSE.
 *
 * This translation unit owns:
 *   - the byte-level frame parser (state machine in parser_feed)
 *   - the host RX ring buffer (single-producer USART2 ISR / single-consumer
 *     super-loop)
 *   - the inbox of fully-assembled frames waiting for the super-loop
 *   - the outbound serializer used by main.c and the link layer
 *
 * The ISO 7816 link layer is reached only via opaque calls — keeping the
 * dependency one-way means the bridge can be unit-tested on a host stub.
 */

#include "apdu_bridge.h"
#include "iso7816_link.h"
#include "main.h"

#include <string.h>

/* ------------------------------------------------------------------------- */
/* Frame parser state                                                        */
/* ------------------------------------------------------------------------- */

typedef enum {
    PARSE_WAIT_SOF = 0,
    PARSE_LEN_LO,
    PARSE_LEN_HI,
    PARSE_PAYLOAD,
    PARSE_CRC_LO,
    PARSE_CRC_HI
} parser_state_t;

typedef struct {
    parser_state_t state;
    uint16_t       expected_len;
    uint16_t       received;
    uint16_t       crc_rx;
    uint8_t        payload[APDU_BRIDGE_MAX_PAYLOAD];
} frame_parser_t;

static frame_parser_t s_parser;

/* ------------------------------------------------------------------------- */
/* USB-CDC RX ring buffer                                                    */
/*                                                                           */
/* Sized to roughly 4 maximum-size frames so a noisy host can burst without  */
/* stalling. Single producer (CDC ISR) / single consumer (super-loop) means  */
/* a plain head/tail with volatile indices is race-free on Cortex-M with     */
/* aligned 32-bit loads/stores.                                              */
/* ------------------------------------------------------------------------- */

#define RX_RING_SIZE   4096u  /* must be power-of-two */

static volatile uint16_t s_rx_head;   /* writer (CDC ISR)         */
static volatile uint16_t s_rx_tail;   /* reader (super-loop poll) */
static uint8_t           s_rx_ring[RX_RING_SIZE];

/* ------------------------------------------------------------------------- */
/* Inbox: one fully-validated frame waiting to be dispatched                 */
/*                                                                           */
/* A single slot is enough — the super-loop drains it every iteration. If a  */
/* second frame finishes before the first is consumed, the new one is        */
/* dropped and a TYPE_ERROR is queued (overflow).                            */
/* ------------------------------------------------------------------------- */

typedef struct {
    uint8_t  in_use;
    uint8_t  type;
    uint16_t len;        /* body length excluding the type byte */
    uint8_t  body[APDU_BRIDGE_MAX_PAYLOAD];
} inbox_slot_t;

static inbox_slot_t s_inbox;

/* ------------------------------------------------------------------------- */
/* CRC-16/CCITT-FALSE                                                        */
/*   poly = 0x1021, init = 0xFFFF, refin = false, refout = false, xorout = 0 */
/* ------------------------------------------------------------------------- */

uint16_t apdu_bridge_crc16(const uint8_t *data, uint16_t len)
{
    uint16_t crc = 0xFFFFu;
    for (uint16_t i = 0; i < len; ++i) {
        crc ^= (uint16_t)data[i] << 8;
        for (uint8_t b = 0; b < 8; ++b) {
            if (crc & 0x8000u) {
                crc = (uint16_t)((crc << 1) ^ 0x1021u);
            } else {
                crc = (uint16_t)(crc << 1);
            }
        }
    }
    return crc;
}

/* ------------------------------------------------------------------------- */
/* Internal helpers                                                          */
/* ------------------------------------------------------------------------- */

static void parser_reset(void)
{
    s_parser.state        = PARSE_WAIT_SOF;
    s_parser.expected_len = 0;
    s_parser.received     = 0;
    s_parser.crc_rx       = 0;
}

/* Place a fully-validated frame into the inbox (single slot, drop on full). */
static void inbox_post(uint8_t type, const uint8_t *body, uint16_t blen)
{
    if (s_inbox.in_use) {
        /* Drop: caller hasn't drained the previous frame yet. The super-loop
         * runs once per loop pass so this only triggers under sustained
         * back-pressure or extremely fast host bursts. */
        return;
    }
    s_inbox.type = type;
    s_inbox.len  = blen;
    if (blen > 0u) {
        memcpy(s_inbox.body, body, blen);
    }
    s_inbox.in_use = 1u;
}

/* Push a single received byte into the parser. On a valid frame, dispatch
 * the payload to the appropriate handler. Caller must already hold the
 * USB-CDC RX context (single-threaded super-loop is fine). */
static void parser_feed(uint8_t b)
{
    switch (s_parser.state) {
    case PARSE_WAIT_SOF:
        if (b == APDU_BRIDGE_SOF) {
            s_parser.state = PARSE_LEN_LO;
        }
        break;

    case PARSE_LEN_LO:
        s_parser.expected_len = b;
        s_parser.state        = PARSE_LEN_HI;
        break;

    case PARSE_LEN_HI:
        s_parser.expected_len |= (uint16_t)b << 8;
        if (s_parser.expected_len == 0u ||
            s_parser.expected_len > APDU_BRIDGE_MAX_PAYLOAD) {
            parser_reset();
        } else {
            s_parser.received = 0;
            s_parser.state    = PARSE_PAYLOAD;
        }
        break;

    case PARSE_PAYLOAD:
        s_parser.payload[s_parser.received++] = b;
        if (s_parser.received >= s_parser.expected_len) {
            s_parser.state = PARSE_CRC_LO;
        }
        break;

    case PARSE_CRC_LO:
        s_parser.crc_rx = b;
        s_parser.state  = PARSE_CRC_HI;
        break;

    case PARSE_CRC_HI: {
        s_parser.crc_rx |= (uint16_t)b << 8;
        uint16_t calc = apdu_bridge_crc16(s_parser.payload,
                                          s_parser.expected_len);
        if (calc == s_parser.crc_rx) {
            /* payload[0] = type, payload[1..] = body */
            uint8_t  type = s_parser.payload[0];
            uint16_t blen = (uint16_t)(s_parser.expected_len - 1u);
            inbox_post(type, &s_parser.payload[1], blen);
        } else {
            /* CRC mismatch → emit a TYPE_ERROR with status 0x02. */
            uint8_t err[1] = { 0x02u };
            apdu_bridge_send_response(APDU_BRIDGE_TYPE_ERROR,
                                      err, (uint16_t)sizeof(err));
        }
        parser_reset();
        break;
    }
    }
}

/* ------------------------------------------------------------------------- */
/* Public API                                                                */
/* ------------------------------------------------------------------------- */

void apdu_bridge_init(void)
{
    memset(&s_parser, 0, sizeof(s_parser));
    memset(&s_inbox,  0, sizeof(s_inbox));
    s_rx_head = 0;
    s_rx_tail = 0;
    parser_reset();
}

void apdu_bridge_rx_push(const uint8_t *data, uint32_t len)
{
    /* Single-producer enqueue. If the consumer is asleep and the ring fills,
     * we drop the oldest bytes by advancing head past tail — bytes lost here
     * will simply cause the parser to resync on the next 0xA5 SOF. */
    for (uint32_t i = 0; i < len; ++i) {
        uint16_t next = (uint16_t)((s_rx_head + 1u) & (RX_RING_SIZE - 1u));
        if (next == s_rx_tail) {
            /* Overrun: advance tail to make room. */
            s_rx_tail = (uint16_t)((s_rx_tail + 1u) & (RX_RING_SIZE - 1u));
        }
        s_rx_ring[s_rx_head] = data[i];
        s_rx_head = next;
    }
}

void apdu_bridge_poll(void)
{
    /* Single-consumer dequeue: pull whatever the CDC ISR has buffered and
     * feed the parser one byte at a time. */
    while (s_rx_tail != s_rx_head) {
        uint8_t b = s_rx_ring[s_rx_tail];
        s_rx_tail = (uint16_t)((s_rx_tail + 1u) & (RX_RING_SIZE - 1u));
        parser_feed(b);
    }
}

uint8_t apdu_bridge_try_pop_frame(uint8_t *out_type,
                                  uint8_t *out_payload,
                                  uint16_t *out_len)
{
    if (!s_inbox.in_use) {
        return 0;
    }
    if (out_type)    { *out_type = s_inbox.type; }
    if (out_len)     { *out_len  = s_inbox.len;  }
    if (out_payload && s_inbox.len > 0u) {
        memcpy(out_payload, s_inbox.body, s_inbox.len);
    }
    s_inbox.in_use = 0u;
    return 1u;
}

void apdu_bridge_send_response(uint8_t type,
                               const uint8_t *payload,
                               uint16_t len)
{
    /* Frame layout we're about to emit:
     *
     *   header[0]   = SOF
     *   header[1..2]= len + 1 (payload length includes the type byte)
     *   body[0]     = type
     *   body[1..]   = payload
     *   trailer[0..1] = crc16(body)
     */
    if (len > APDU_BRIDGE_MAX_PAYLOAD - 1u) {
        return; /* would exceed buffer; drop silently */
    }

    static uint8_t s_tx_buf[3 + APDU_BRIDGE_MAX_PAYLOAD + 2];
    uint16_t total_payload = (uint16_t)(len + 1u);

    s_tx_buf[0] = APDU_BRIDGE_SOF;
    s_tx_buf[1] = (uint8_t)(total_payload & 0xFFu);
    s_tx_buf[2] = (uint8_t)((total_payload >> 8) & 0xFFu);
    s_tx_buf[3] = type;
    if (len > 0u && payload != 0) {
        memcpy(&s_tx_buf[4], payload, len);
    }
    uint16_t crc = apdu_bridge_crc16(&s_tx_buf[3], total_payload);
    s_tx_buf[3 + total_payload]     = (uint8_t)(crc & 0xFFu);
    s_tx_buf[3 + total_payload + 1] = (uint8_t)((crc >> 8) & 0xFFu);

    uint16_t frame_len = (uint16_t)(3u + total_payload + 2u);
    (void)bridge_uart_write(s_tx_buf, frame_len);
}

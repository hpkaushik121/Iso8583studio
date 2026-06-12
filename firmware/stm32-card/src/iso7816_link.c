/*
 * iso7816_link.c — card-side ISO 7816-3 link layer (skeleton).
 *
 * This is intentionally a scaffolding file. Each function documents the
 * behaviour that must eventually live here so the bring-up engineer can
 * fill it in piece-by-piece against an oscilloscope and a real terminal.
 *
 * References:
 *   ISO/IEC 7816-3:2006 §8   — electrical / answer-to-reset
 *   ISO/IEC 7816-3:2006 §9   — PPS (protocol & parameters selection)
 *   ISO/IEC 7816-3:2006 §10  — T=0 character protocol
 *   ISO/IEC 7816-3:2006 §11  — T=1 block protocol
 */

#include "iso7816_link.h"
#include <string.h>

/* ------------------------------------------------------------------------- */
/* Module state                                                              */
/* ------------------------------------------------------------------------- */

/* Default ATR: 3B 90 11 00  — direct convention, T=0 only, no historical.
 * Replace with whatever the simulated card profile requires. The ATR is the
 * very first thing a terminal sees and is parsed strictly; build it carefully
 * (TS, T0, optional TA1/TB1/TC1/TD1..., historical bytes, TCK if needed). */
static const uint8_t k_default_atr[] = { 0x3B, 0x90, 0x11, 0x00 };

static iso7816_proto_t s_proto = ISO7816_PROTO_T0;

/* ------------------------------------------------------------------------- */
/* Public API                                                                */
/* ------------------------------------------------------------------------- */

void iso7816_link_init(void)
{
    /* TODO: configure RST EXTI callback, prime USART2 RX in interrupt mode,
     *       reset link state machine, default to T=0. */
    s_proto = ISO7816_PROTO_T0;
}

void iso7816_link_send_atr(void)
{
    /* TODO: blast k_default_atr[] out of USART2 with correct guard time;
     *       on cold reset, etu = 372 / f_clk, direct convention.
     *       Per §8 the first byte (TS) must be on the line within
     *       400..40000 clock cycles after RST deasserts.
     *       Remember to compute TCK (XOR of T1..Tk) when any T!=0 protocol
     *       is advertised. */
    (void)k_default_atr;
}

void iso7816_link_send_response(uint8_t *data, uint16_t len)
{
    /* TODO: dispatch on s_proto.
     *
     * T=0 (§10): the card emits SW1 SW2 only after the terminal has clocked
     *   in P3 bytes (or after a 0x61/0x6C procedure-byte handshake). For
     *   outgoing data (case 2/4) the card may insert NULL procedure bytes
     *   (0x60) to extend WWT, or send 0x6X / 0x9X to acknowledge.
     *
     * T=1 (§11): wrap data into one or more I-blocks (NAD | PCB | LEN |
     *   INF | EDC), respect IFSC, and honour BWT. Use S(WTX request) to
     *   ask for more time when processing exceeds BWT. EDC is LRC by
     *   default but TC1 of TD1 may select CRC.
     */
    (void)data;
    (void)len;
}

void iso7816_link_send_apdu_to_card(const uint8_t *capdu, uint16_t len)
{
    /* TODO: stash this C-APDU as the next response payload to feed to the
     * terminal during the procedure-byte / I-block handshake. For now, the
     * function is a no-op so the bridge has somewhere to forward bytes
     * without compile errors. */
    (void)capdu;
    (void)len;
}

void iso7816_link_poll(void)
{
    /* TODO:
     *  1. If RST just went low->high, send ATR and reset state.
     *  2. Drain USART2 RX bytes.
     *  3. Feed bytes into the active protocol's state machine (T=0/T=1).
     *  4. On a fully assembled C-APDU, forward to apdu_bridge for the PC.
     *  5. Service WTX/BWT/WWT timers; emit 0x60 NULL bytes (T=0) or
     *     S(WTX request) blocks (T=1) when processing is slow.
     *  6. If a PPS request (TS == 0xFF) is in-flight, validate and answer
     *     with PPSS/PPS0/PPS1/PPS2/PPS3/PCK before resuming normal traffic.
     */
}

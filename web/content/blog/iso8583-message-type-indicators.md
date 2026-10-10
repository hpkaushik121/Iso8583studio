---
title: "ISO 8583 Message Type Indicators (MTI): Structure, Classes, and Common Values"
description: "Understand ISO 8583 MTI digits: version, class, function, and origin—how to read MTIs and map them to request/response flows in real systems."
date: "2025-03-01"
updated: "2026-10-08"
tags: [MTI, message type, ISO 8583, authorization, network management]
category: "ISO8583 Fundamentals"
author: "Sourabh Kaushik"
read_time: "4 min read"
---

The **Message Type Indicator (MTI)** is the four-digit “header” that tells you what an **ISO 8583** message is trying to do—before you interpret amounts, merchant IDs, or EMV blobs. If the MTI is wrong for your state machine, everything downstream is nonsense: you might parse fields as if they were a request when they are actually a response, or treat a financial message like network management traffic.

This guide explains how MTIs are structured in common ISO 8583 teaching models, what the digits typically represent, and how to connect MTI literacy to practical debugging—especially when paired with tools like **ISO8583Studio**, a **free desktop payment testing application** for **Windows**, **macOS**, and **Linux** ([https://iso8583.studio](https://iso8583.studio)).

## What is an ISO 8583 0200 message?

An ISO 8583 **0200** is a **financial transaction request**: the message an acquirer sends to an issuer to ask for a transaction to be approved and posted to the account in one step (a **0100** is the authorization-only request). Its four digits decompose as version `0` (ISO 8583:1987), message class `2` (financial transaction), message function `0` (request) and originator `0` (acquirer). The issuer answers with a **0210**, the financial transaction response, which carries the decision in Field 39. The request/response pair is therefore `0200 → 0210`.

Related MTIs in the same family: `0220` financial advice, `0221` advice repeat, `0230` advice response, `0400` reversal request, `0410` reversal response, `0800` network management request (sign-on, sign-off, echo test) and `0810` its response. Networks restrict which MTIs they accept and add their own, but the `0200/0210` pairing comes from the ISO 8583:1987 base standard. In ISO8583Studio you can send a 0200 to a local host simulator and script the 0210 that comes back.

## The MTI’s job in one sentence

The MTI classifies the message so both endpoints agree on **which fields are expected**, **which state transitions are legal**, and **how responses correlate** to requests.

## The four-digit model: version, class, function, origin

Many references describe the MTI as four parts (one digit each):

1. **Version** — `0` = ISO 8583:1987, `1` = ISO 8583:1993, `2` = ISO 8583:2003. Almost all card networks still use `0`.
2. **Message class** — `1` authorization, `2` financial, `3` file action, `4` reversal / chargeback, `5` reconciliation, `6` administrative, `7` fee collection, `8` network management.
3. **Message function** — `0` request, `1` request response, `2` advice, `3` advice response, `4` notification, `5` notification acknowledgement.
4. **Originator** — `0` acquirer, `1` acquirer repeat, `2` issuer, `3` issuer repeat, `4` other, `5` other repeat.

### Why this matters in debugging

Teams often stare at Field 39 while the real inconsistency is earlier: **the MTI does not match the handler**. For example, treating an advice message like a normal authorization request can cause you to look for the wrong correlation identifiers or evaluate response rules incorrectly.

## Requests vs responses: pairing logic

ISO 8583 integrations usually require strict pairing between requests and responses. Practically, that means your software must track:

- The **MTI family** you sent
- The expected **response MTI** pattern for your implementation
- Correlation fields (such as **STAN**, date/time fields, retrieval reference, and network-specific tokens—exact requirements vary)

If your pairing is wrong, “duplicate” or “late response” issues become indistinguishable from host bugs.

## The MTIs you will actually see

| MTI | Meaning | Reply |
|-----|---------|-------|
| `0100` | Authorization request (approval only, no funds movement) | `0110` |
| `0120` | Authorization advice | `0130` |
| `0200` | Financial transaction request (authorize and post) | `0210` |
| `0220` | Financial advice (e.g. offline-approved, forced post) | `0230` |
| `0400` | Reversal request | `0410` |
| `0420` | Reversal advice | `0430` |
| `0500` | Reconciliation / settlement request | `0510` |
| `0800` | Network management (sign-on, echo test, key exchange) | `0810` |

Networks layer their own rules on top — Mastercard uses `0100/0110` for authorization and `1240` for clearing, Visa restricts which classes a given endpoint may send — but the digit meanings above are the ISO 8583:1987 base and are stable across them. Your implementation guide tells you which of these a given endpoint accepts; it does not redefine what they mean.

## MTI + bitmap together: the two-part headline

Experienced engineers read traces in this order:

1. **MTI** → what conversation are we in?
2. **Bitmap** → which fields are actually present?
3. **Fields** → parse with the correct lengths and encoding

Skipping straight to Field 54 (amounts) is how people misinterpret variable-length boundaries.

### A quick sanity pattern for newcomers

When you are handed a “random decline,” write down the **MTI**, the **bitmap hex**, and the **first three fields you believe are present** before you touch response codes. That single habit prevents hours of chasing issuer policy when the real issue is a parser mismatch or a duplicated header in your capture pipeline.

## Implementation reality: variants and custom mappings

ISO 8583 is a framework; processors extend it. You may encounter:

- Additional transport headers
- Proprietary fields inside DE62/DE63/DE127-style containers
- Gateway wrappers that log “ISO-like” payloads with extra metadata

Always ask: **Is this MTI from the raw ISO stream or from an internal API representation?**

## Practical examples

The flows below use the standard MTIs; your network's guide tells you which of them a given endpoint supports.

- A message whose function digit indicates **request** should have a response whose function digit indicates **response** under your ruleset.
- **Network management** messages may have very different field expectations than financial messages—your parser should route by MTI class before applying business validations.

If you paste examples into notes, include the **full MTI** and the **hex bitmap**—half-context screenshots waste time.

## How ISO8583Studio helps you practice MTI literacy

ISO8583Studio is designed for payment developers who need **local**, **repeatable** inspection of ISO 8583 artifacts alongside simulators and calculators. When you are learning a new network’s dialect, a desktop workbench accelerates the loop: capture bytes → parse structure → validate assumptions → compare to host documentation.

Download the latest release:

[iso8583.studio/download](https://iso8583.studio/download)

## Conclusion

The **MTI** is the entry point to ISO 8583 understanding: it sets **intent**, **message family**, and **request/response posture** before you touch individual fields. Learn the digit model at a high level, then go deep on **your** network’s tables—because payments is a standards-plus-contracts domain. Pair MTI discipline with bitmap discipline, and use **ISO8583Studio** to keep parsing and simulation workflows on your machine, under your controls, while you integrate faster with fewer wrong turns.

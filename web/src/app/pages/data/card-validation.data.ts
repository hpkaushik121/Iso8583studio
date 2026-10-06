/**
 * Card Validation Tools — page content. Imported only by
 * pages/site/docs-card-validation.ts. Strings are GuideRich markdown.
 */
import { RefGuide, shot } from './docs-reference.data';

export const CARD_VALIDATION_GUIDE: RefGuide = {
  slug: 'card-validation',
  crumb: 'Card Validation',
  meta: 'Tool reference · 2 calculators',
  title: 'Card Validation Tools',
  lede: 'Generate and validate the card security codes used by MasterCard and American Express — dynamic CVC3 for contactless taps, and the Amex CSC in its 3, 4 and 5-digit forms.',
  browse: 'cvc-mc',
  sections: [
    {
      id: 'overview', title: 'Introduction', rail: 'Introduction', icon: 'book-open',
      intro: 'Card security codes are short numeric values printed on a card or computed dynamically that prove the cardholder physically possesses the card or a valid token. They’re the first line of defence in card-not-present transactions and are checked on every authorisation alongside expiry date and address verification.',
      blocks: [
        { t: 'p', x: 'ISO8583Studio includes dedicated calculators per scheme so you can simulate issuer behaviour without an HSM and validate codes returned by your authorisation pipeline.' },
      ],
    },
    {
      id: 'concepts', title: 'Key Concepts', rail: 'Key concepts', icon: 'key',
      blocks: [
        {
          t: 'table', cols: '90px 180px auto auto',
          head: ['Code', 'Scheme', 'Where it lives', 'Length'],
          rows: [
            ['**CVV**', 'Visa', 'Magstripe (track 1 / 2)', '3 digits'],
            ['**CVV2**', 'Visa', 'Card back (signature panel)', '3 digits'],
            ['**iCVV**', 'Visa', 'EMV chip', '3 digits (computed with service code 999)'],
            ['**CVC**', 'MasterCard', 'Magstripe', '3 digits'],
            ['**CVC2**', 'MasterCard', 'Card back', '3 digits'],
            ['**CVC3**', 'MasterCard PayPass', 'Computed dynamically per tap', '3 digits'],
            ['**CSC**', 'American Express', 'Card front', '4 digits'],
          ],
        },
      ],
    },
    {
      id: 'cvc-mc', title: 'MasterCard CVC3 Calculator', rail: 'MasterCard CVC3', icon: 'credit-card',
      intro: 'CVC3 is the dynamic card verification code a contactless (PayPass) card computes for every tap, from a master key, the terminal’s unpredictable number and the Application Transaction Counter. The calculator has a **Generate** tab and a **Validate** tab, with the activity log beside them.',
      blocks: [
        shot('mastercard-cvc3-generate', 1600, 1039, 1512, 'Generate CVC3',
          'MasterCard CVC3 calculator on the Generate tab with IMK, PAN, PAN Seq No, Track 1/2 Data, Unpredictable Num and ATC fields and Dynamic CVC3 selected as the CVC3 type, beside the activity log'),
        { t: 'h3', x: 'Inputs' },
        {
          t: 'bullets', items: [
            ['IMK', 'The issuer master key the card’s CVC3 key is derived from.'],
            '**PAN** and **PAN Seq No** — Together they identify the individual card, which is what the derivation is bound to.',
            ['Track 1/2 Data', 'The track template the CVC3 digits are placed into.'],
            ['Unpredictable Num', 'The number supplied by the terminal for this tap.'],
            ['ATC', 'Application Transaction Counter, which advances every tap.'],
            ['CVC3 Type', 'Drop-down; `Dynamic CVC3` is the default.'],
          ],
        },
        { t: 'p', x: 'Button: **Generate**.' },
        { t: 'h3', x: 'Validate' },
        { t: 'p', x: 'The **Validate** tab takes the same card and transaction inputs plus the **Dynamic CVC3** value that arrived, recomputes it, and reports whether the two agree.' },
        shot('mastercard-cvc3-validate', 1600, 1039, 1512, 'Validate CVC3',
          'MasterCard CVC3 calculator on the Validate tab, with the same card and transaction fields plus a Dynamic CVC3 value field to check, beside the activity log'),
        { t: 'note', tone: 'blue', icon: 'info', title: 'CVC3 placement', x: 'The CVC3 digits replace discretionary data positions in the magstripe-equivalent track the contactless card emits. Combined with the ATC, every tap produces a different track 2.' },
      ],
    },
    {
      id: 'amex', title: 'AMEX CSC Calculator', rail: 'AMEX CSC', icon: 'identification-card',
      intro: 'American Express uses a Card Security Code (CSC) printed on the card front above the embossed PAN. The calculator generates one from the card data, or checks a presented value.',
      blocks: [
        shot('amex-csc-generate', 1516, 1632, 758, 'Generate CSC',
          'Amex CSC Calculator on the Generate tab with a CSC Version drop-down set to Version 1, a hex CSC Key field, PAN, expiration date, service code and a CSC verification value type above the Generate button',
          [
            { t: 'h3', x: 'Inputs' },
            {
              t: 'bullets', items: [
                ['CSC Version', 'Drop-down; `Version 1` is the default.'],
                ['CSC Key (Hex)', 'The key the code is computed under.'],
                ['PAN', '15 digits for Amex.'],
                '**Expiration Date (YYMM)**',
                '**Service Code**',
                ['Verification Value Type', 'Drop-down; `CSC` by default.'],
              ],
            },
            { t: 'p', x: 'Button: **Generate**. Every field is validated before the button becomes active, so an empty form shows its own reasons.' },
          ]),
        { t: 'h3', x: 'Validate' },
        { t: 'p', x: 'The **Validate** tab drops the key and asks instead for the values that arrived. Under *Values to Validate* it takes **CSC-5**, **CSC-4** and **CSC-3** — the three code lengths Amex uses across magstripe, card-front and contactless flows — and reports each against the recomputed value in the log.' },
        shot('amex-csc-validate', 1600, 978, 1512, 'Validate CSC',
          'Amex CSC Calculator on the Validate tab with PAN, expiration date, service code and verification value type, then CSC-5, CSC-4 and CSC-3 fields under a Values to Validate heading, beside the activity log'),
      ],
    },
    {
      id: 'service-codes', title: 'Service Codes', rail: 'Service codes', icon: 'list-numbers',
      intro: 'The 3-digit service code is fed into CVV/CVC algorithms. Each digit has independent meaning:',
      blocks: [
        {
          t: 'table', cols: '110px 160px auto',
          head: ['Position', 'Common Values', 'Meaning'],
          rows: [
            ['1st digit', '1, 2, 5, 6, 7, 9', 'Interchange and technology (international, EMV, etc.).'],
            ['2nd digit', '0, 2, 4', 'Authorisation processing (online, offline, by issuer).'],
            ['3rd digit', '0–7', 'Range of services and PIN requirement.'],
          ],
        },
        {
          t: 'bullets', items: [
            ['101', 'International, normal authorisation, no restrictions.'],
            ['201', 'Same but EMV-capable.'],
            ['120', 'Online authorisation only, PIN required.'],
            ['999', 'Special value used by Visa for iCVV computation.'],
            ['000', 'Special value for CVV2 / CVC2.'],
          ],
        },
      ],
    },
    {
      id: 'tips', title: 'Tips', rail: 'Tips', icon: 'lightbulb',
      tips: [
        'Use the same CVK / CVC key for CVV, CVV2, and iCVV — only the service code changes.',
        'If your CVV2 doesn’t match across systems, check whether the integrator strips the trailing PAN check digit before computation. Different specs handle that differently.',
        'For CVC3 testing, capture the UN and ATC from your terminal log alongside the track data — off-by-one ATC is a common error.',
      ],
    },
  ],
  cta: {
    heading: 'Try it on your own transactions',
    text: 'Free and open source. Download the studio and run these calculators on your desk in minutes.',
  },
};

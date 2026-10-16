import { PAYMENTS } from '../../content/payments-config';
import { formatMinor } from '../../core/payments';

/**
 * Pro's prices, charged in rupees at the rupee equivalent of the design's
 * dollar figures.
 *
 * The payment service sells in the tenant's currency (INR) and a page cannot
 * name another, so the dollar prices are converted here once, at a fixed rate,
 * rather than at checkout. Change the rate (or the dollar prices) here and
 * every place that states a Pro price follows.
 */

/** Rupees per US dollar used for the conversion — the rate on 2026-10-06. */
export const USD_INR = 96.38;

/** The design's prices, in dollars. */
export const PRO_MONTHLY_USD = 13.99;
export const PRO_RESERVE_USD = 9;

const TAX = PAYMENTS.taxBps / 10_000;

/** The monthly price at launch, in whole rupees, tax included. */
export const PRO_MONTHLY_INR = Math.round(PRO_MONTHLY_USD * USD_INR);

/**
 * The reservation is sold as units of the rupee price point, and the service
 * adds GST on top. So the units are chosen to make the total *with* tax the
 * dollar equivalent: $9 at ₹96.38 is ₹867.42, which is 735 units of ₹1 plus
 * 18% GST — ₹867.30.
 */
export const RESERVE_UNITS = Math.round((PRO_RESERVE_USD * USD_INR) / (PAYMENTS.unitRupees * (1 + TAX)));

/** What the reservation should come to, in paise, as the service will compute it. */
export const RESERVE_TOTAL_PAISE = Math.round(RESERVE_UNITS * PAYMENTS.unitRupees * 100 * (1 + TAX));

export const PRO_MONTHLY_LABEL = formatMinor(PRO_MONTHLY_INR * 100, 'INR', false);
export const RESERVE_LABEL = formatMinor(RESERVE_TOTAL_PAISE, 'INR');
export const GST_PERCENT = PAYMENTS.taxBps / 100;

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { UiIcon } from '../../ui';
import { LONGFORM, LfBadge, LfTocEntry } from '../shared/longform';

/**
 * The wording of this page is a legal document: the long-form shell supplies
 * the layout only, and every sentence is carried over from the policy as
 * published. Change the text only on instruction.
 */
@Component({
  selector: 'page-privacy-policy',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [...LONGFORM, RouterLink, UiIcon],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-privacy-policy' },
  template: `
    <lf-header [crumbs]="crumbs" [badge]="badge" title="Privacy Policy" [meta]="meta"
               lede="Your privacy and data security are fundamental to everything we do. This policy explains how ISO8583Studio handles your information with transparency and respect." />

    <lf-body [toc]="toc">
      <section lfSection id="overview" n="01" heading="Overview &amp; our commitment">
        <p class="lf-p">At ISO8583Studio, we are committed to protecting your privacy and ensuring the security of your personal information. As a professional desktop application for financial transaction processing, we understand the critical importance of data protection in the financial technology sector.</p>
        <lf-note tone="blue" icon="shield-check" title="Our commitment">We process only the minimum data necessary to provide our services, implement industry-leading security measures, and never sell your personal information to third parties.</lf-note>
        <p class="lf-p">This Privacy Policy applies to:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>ISO8583Studio Desktop Application</strong> — our primary software product</li>
          <li><strong>Official Website</strong> — <a href="https://iso8583.studio">https://iso8583.studio</a></li>
          <li><strong>Support Services</strong> — customer support and technical assistance</li>
          <li><strong>Documentation &amp; Resources</strong> — online guides and documentation</li>
        </ul>
      </section>

      <section lfSection id="data-collection" n="02" heading="Information we collect">
        <h3 class="lf-h3">Information you provide directly</h3>
        <p class="lf-p">We collect information you voluntarily provide when using our services:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Account information:</strong> name, email address, company name, and contact details when you create an account or request support</li>
          <li><strong>License information:</strong> license key details, activation information, and subscription data</li>
          <li><strong>Feedback &amp; surveys:</strong> your responses to surveys, feedback forms, and product improvement requests</li>
        </ul>
        <h3 class="lf-h3">Information collected automatically</h3>
        <p class="lf-p">Our application and website may automatically collect certain technical information:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Application usage data:</strong> feature usage statistics, performance metrics, and crash reports (anonymized)</li>
          <li><strong>System information:</strong> operating system version, hardware specifications, and software environment details</li>
          <li><strong>Website analytics:</strong> IP address, browser type, pages visited, and interaction patterns</li>
          <li><strong>Log data:</strong> application logs, error reports, and diagnostic information</li>
        </ul>
        <lf-note tone="blue" icon="desktop" title="Desktop application data">ISO8583Studio is primarily a desktop application. Most of your transaction data, configurations, and financial information remain on your local system and are not transmitted to our servers unless you explicitly use cloud features or request support.</lf-note>
        <h3 class="lf-h3">Desktop application usage analytics (opt-in)</h3>
        <p class="lf-p">The ISO8583Studio desktop application can report anonymous usage analytics to Google Analytics 4. This is <strong>off by default</strong>. On first launch the application asks whether you wish to enable it, and nothing is transmitted unless you accept. You can change your choice at any time under <em>Settings → Usage Analytics</em>.</p>
        <p class="lf-p">When you have opted in, the application sends:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Feature usage:</strong> which tools and simulators you open, how long sessions last, whether a calculation succeeded or failed, and the names of screens you visit</li>
          <li><strong>Environment:</strong> application version, operating system and version, CPU architecture, Java runtime version, language, timezone and screen resolution</li>
          <li><strong>Approximate location:</strong> city, region/state and country. To determine this, the application makes a request to a third-party IP geolocation service. Your IP address is used to resolve this location and to let Google resolve it; it is not stored by us as an analytics attribute, and precise GPS coordinates are never collected.</li>
          <li><strong>A random device identifier:</strong> generated on your machine, persisted between launches, and not derived from any hardware, account or network identifier. You can regenerate it at any time from Settings.</li>
          <li><strong>Error types:</strong> the class name of an unexpected error, without its message or stack trace</li>
        </ul>
        <lf-note tone="teal" icon="prohibit" title="What is never collected">Card numbers (PANs), PINs or PIN blocks, cryptographic keys or key components, cryptograms, MACs, the contents of any ISO 8583 or HSM message, file paths, hostnames, IP addresses of systems you connect to, or the names you give your simulator profiles. Analytics carries only the identity of the tool or screen in use, timings, and the environment details listed above.</lf-note>
        <h3 class="lf-h3">Financial &amp; transaction data</h3>
        <p class="lf-p">Important clarifications regarding sensitive financial data:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Local processing:</strong> transaction data processed through ISO8583Studio remains on your local system by default</li>
          <li><strong>No data collection:</strong> we do not collect, store, or transmit your actual financial transaction data</li>
          <li><strong>Configuration data:</strong> gateway configurations and message templates are stored locally unless you use cloud sync features</li>
          <li><strong>Support cases:</strong> if you share configuration or log files for support purposes, we handle them with strict confidentiality</li>
        </ul>
      </section>

      <section lfSection id="data-usage" n="03" heading="How we use your data">
        <p class="lf-p">We use collected information for the following purposes:</p>
        <h3 class="lf-h3">Service provision &amp; improvement</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>License management:</strong> verify licenses, manage subscriptions, and prevent unauthorized use</li>
          <li><strong>Technical support:</strong> provide customer support, troubleshoot issues, and resolve technical problems</li>
          <li><strong>Product development:</strong> improve our software, develop new features, and enhance user experience</li>
          <li><strong>Performance optimization:</strong> analyze usage patterns to optimize application performance and stability</li>
        </ul>
        <h3 class="lf-h3">Communication &amp; updates</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Product updates:</strong> notify you about software updates, security patches, and new releases</li>
          <li><strong>Support communications:</strong> respond to your support requests and provide technical assistance</li>
          <li><strong>Educational content:</strong> share documentation, tutorials, and best practices (with your consent)</li>
          <li><strong>Important notices:</strong> communicate critical security updates or service changes</li>
        </ul>
        <h3 class="lf-h3">Legal &amp; compliance</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Legal obligations:</strong> comply with applicable laws, regulations, and legal processes</li>
          <li><strong>Security protection:</strong> detect and prevent fraud, unauthorized access, and security threats</li>
          <li><strong>Terms enforcement:</strong> enforce our <a href="/terms-and-conditions">Terms and Conditions</a> and protect our rights and property</li>
        </ul>
      </section>

      <section lfSection id="data-sharing" n="04" heading="Data sharing &amp; disclosure">
        <p class="lf-p">We do not sell, rent, or trade your personal information. We may share information only in the following limited circumstances:</p>
        <h3 class="lf-h3">Service providers</h3>
        <p class="lf-p">We may share data with trusted third-party service providers who help us operate our business:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Cloud infrastructure:</strong> hosting services for our website and support systems</li>
          <li><strong>Analytics services:</strong> <a href="https://policies.google.com/privacy" target="_blank" rel="noopener">Google Analytics 4</a> for website and application usage measurement, and Google Ads for advertising measurement</li>
          <li><strong>Support tools:</strong> customer support and ticketing systems</li>
          <li><strong>Payment processing:</strong> payment processors for license purchases (they handle payment data independently)</li>
        </ul>
        <lf-note tone="blue" icon="file-text" title="Important">All service providers are contractually required to protect your data and use it only for the specific services they provide to us. They cannot use your information for their own purposes.</lf-note>
        <h3 class="lf-h3">Legal requirements</h3>
        <p class="lf-p">We may disclose information when required by law or to protect our rights:</p>
        <ul class="lf-ul">
          <li>In response to valid legal processes (subpoenas, court orders, etc.)</li>
          <li>To comply with applicable laws and regulations</li>
          <li>To protect our rights, property, or safety, or that of our users</li>
          <li>To investigate potential violations of our Terms and Conditions</li>
        </ul>
        <h3 class="lf-h3">Business transfers</h3>
        <p class="lf-p">In the event of a merger, acquisition, or sale of assets, your information may be transferred as part of the transaction. We will notify you of any such change and the choices you may have.</p>
      </section>

      <section lfSection id="security" n="05" heading="Security measures">
        <p class="lf-p">We implement comprehensive security measures to protect your information:</p>
        <h3 class="lf-h3">Technical safeguards</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Encryption:</strong> data in transit is protected using TLS/SSL encryption</li>
          <li><strong>Access controls:</strong> strict access controls and authentication for our systems</li>
          <li><strong>Secure infrastructure:</strong> industry-standard security practices for our servers and databases</li>
          <li><strong>Regular updates:</strong> timely security patches and software updates</li>
        </ul>
        <h3 class="lf-h3">Operational safeguards</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Employee training:</strong> regular security awareness training for all employees</li>
          <li><strong>Background checks:</strong> comprehensive background checks for personnel with data access</li>
          <li><strong>Incident response:</strong> established procedures for security incident detection and response</li>
          <li><strong>Regular audits:</strong> periodic security assessments and vulnerability testing</li>
        </ul>
        <h3 class="lf-h3">Application security</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Local data protection:</strong> your local data remains on your system with standard OS protections</li>
          <li><strong>Secure communications:</strong> all network communications use encrypted channels</li>
          <li><strong>Code signing:</strong> our application is digitally signed to ensure authenticity and integrity</li>
          <li><strong>Regular security reviews:</strong> continuous security assessment of our codebase and infrastructure</li>
        </ul>
        <lf-note tone="teal" icon="lock-key" title="Financial data security">Since ISO8583Studio processes financial transaction data, we follow industry best practices including PCI DSS guidelines, even though your transaction data typically remains on your local system.</lf-note>
      </section>

      <section lfSection id="retention" n="06" heading="Data retention">
        <p class="lf-p">We retain your information only as long as necessary to provide our services and comply with legal obligations:</p>
        <h3 class="lf-h3">Account information</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Active accounts:</strong> retained while your account is active and for service provision</li>
          <li><strong>Inactive accounts:</strong> deleted after 2 years of inactivity, unless legal obligations require longer retention</li>
          <li><strong>Support records:</strong> support tickets and communications retained for 3 years for quality and legal purposes</li>
        </ul>
        <h3 class="lf-h3">Technical data</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Usage analytics:</strong> aggregated and anonymized usage data retained for 24 months</li>
          <li><strong>Log files:</strong> server logs retained for 90 days for security and operational purposes</li>
          <li><strong>Crash reports:</strong> anonymous crash reports retained for 12 months for product improvement</li>
        </ul>
        <h3 class="lf-h3">Legal &amp; compliance data</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Financial records:</strong> license and payment records retained for 7 years for tax and accounting purposes</li>
          <li><strong>Legal holds:</strong> data subject to legal proceedings retained until resolution</li>
          <li><strong>Regulatory requirements:</strong> data retained as required by applicable financial services regulations</li>
        </ul>
      </section>

      <section lfSection id="rights" n="07" heading="Your rights">
        <p class="lf-p">You have several rights regarding your personal information. The specific rights available to you may depend on your location and applicable laws:</p>
        <h3 class="lf-h3">Access &amp; portability rights</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Access:</strong> request a copy of the personal information we hold about you</li>
          <li><strong>Portability:</strong> receive your data in a structured, machine-readable format</li>
          <li><strong>Information:</strong> learn about how we process your data and with whom we share it</li>
        </ul>
        <h3 class="lf-h3">Control &amp; correction rights</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Correction:</strong> update or correct inaccurate personal information</li>
          <li><strong>Deletion:</strong> request deletion of your personal information (subject to legal obligations)</li>
          <li><strong>Restriction:</strong> limit how we process your information in certain circumstances</li>
          <li><strong>Objection:</strong> object to processing based on legitimate interests</li>
        </ul>
        <h3 class="lf-h3">Communication preferences</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Marketing opt-out:</strong> unsubscribe from marketing communications at any time</li>
          <li><strong>Communication settings:</strong> choose how and when we contact you</li>
          <li><strong>Notification preferences:</strong> control which product updates and announcements you receive</li>
        </ul>
        <lf-note tone="blue" icon="envelope-simple" title="How to exercise your rights">Contact us at <a href="mailto:admin@iso8583.studio">admin@iso8583.studio</a> with your request. We will respond within 30 days and may require identity verification for security purposes.</lf-note>
      </section>

      <section lfSection id="cookies" n="08" heading="Cookies &amp; tracking technologies">
        <p class="lf-p">Our website uses cookies and similar technologies to improve your experience and understand how our services are used:</p>
        <h3 class="lf-h3">Types of cookies we use</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Essential cookies:</strong> required for website functionality and security</li>
          <li><strong>Analytics cookies:</strong> help us understand website usage and improve performance</li>
          <li><strong>Functional cookies:</strong> remember your preferences and settings</li>
          <li><strong>Marketing cookies:</strong> used for targeted advertising (with your consent)</li>
        </ul>
        <h3 class="lf-h3">Third-party cookies</h3>
        <p class="lf-p">We may use third-party services that set their own cookies:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Google Analytics 4:</strong> website traffic analysis and usage insights. We also enable Google Signals, which allows Google to associate visits with signed-in Google accounts for cross-device measurement and aggregated demographic reporting.</li>
          <li><strong>Google Ads:</strong> advertising measurement and remarketing audiences, where a campaign is running</li>
          <li><strong>Support chat:</strong> customer support chat functionality</li>
          <li><strong>CDN services:</strong> content delivery and website performance optimization</li>
        </ul>
        <h3 class="lf-h3">Managing cookies</h3>
        <p class="lf-p">You can control cookies through your browser settings:</p>
        <ul class="lf-ul">
          <li>Block all cookies (may affect website functionality)</li>
          <li>Block third-party cookies only</li>
          <li>Delete existing cookies</li>
          <li>Set preferences for future cookies</li>
        </ul>
        <lf-note tone="neutral" icon="info" title="Note">The desktop application does not use web cookies, but may store local configuration files and preferences on your system for application functionality.</lf-note>
      </section>

      <section lfSection id="international" n="09" heading="International data transfers">
        <p class="lf-p">ISO8583Studio operates globally, and your information may be transferred to and processed in countries other than your own:</p>
        <h3 class="lf-h3">Legal basis for transfers</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Adequacy decisions:</strong> transfers to countries with adequate data protection laws</li>
          <li><strong>Standard contractual clauses:</strong> EU-approved contracts ensuring data protection</li>
          <li><strong>Your consent:</strong> explicit consent for specific transfers when required</li>
          <li><strong>Necessity:</strong> transfers necessary for service provision or legal compliance</li>
        </ul>
        <h3 class="lf-h3">Safeguards for international transfers</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Encryption:</strong> all data transfers use strong encryption</li>
          <li><strong>Access controls:</strong> strict limits on who can access transferred data</li>
          <li><strong>Contractual protections:</strong> legal agreements requiring equivalent protection</li>
          <li><strong>Regular reviews:</strong> ongoing assessment of transfer arrangements and protections</li>
        </ul>
      </section>

      <section lfSection id="minors" n="10" heading="Children's privacy">
        <p class="lf-p">ISO8583Studio is designed for professional use in financial services and is not intended for children under 16. We do not knowingly collect personal information from children.</p>
        <p class="lf-p">If we become aware that we have collected information from a child under 16 without parental consent, we will take steps to delete that information promptly.</p>
      </section>

      <section lfSection id="updates" n="11" heading="Changes to this Privacy Policy">
        <p class="lf-p">We may update this Privacy Policy from time to time to reflect changes in our practices, technology, legal requirements, or other factors.</p>
        <h3 class="lf-h3">How we notify you of changes</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Website notice:</strong> prominent notice on our website for 30 days</li>
          <li><strong>Email notification:</strong> email to registered users for material changes</li>
          <li><strong>In-app notification:</strong> notification within the desktop application</li>
          <li><strong>Version dating:</strong> clear dating of policy versions for transparency</li>
        </ul>
        <h3 class="lf-h3">Types of changes</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Minor changes:</strong> clarifications, formatting, or contact information updates</li>
          <li><strong>Material changes:</strong> changes to data collection, use, or sharing practices</li>
          <li><strong>Legal changes:</strong> updates required by new laws or regulations</li>
        </ul>
        <lf-note tone="blue" icon="clock-counter-clockwise" title="Your continued use">Continued use of our services after policy changes constitutes acceptance of the updated terms. If you disagree with changes, please discontinue use and contact us about data deletion.</lf-note>
      </section>

      <section lfSection id="compliance" n="12" heading="Legal compliance &amp; frameworks">
        <p class="lf-p">ISO8583Studio complies with major data protection frameworks and regulations:</p>
        <h3 class="lf-h3">Regulatory compliance</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>GDPR:</strong> European General Data Protection Regulation compliance</li>
          <li><strong>CCPA:</strong> California Consumer Privacy Act protections</li>
          <li><strong>PIPEDA:</strong> Canadian Personal Information Protection Act compliance</li>
          <li><strong>Financial regulations:</strong> relevant financial services data protection requirements</li>
        </ul>
        <h3 class="lf-h3">Industry standards</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>ISO 27001:</strong> information security management best practices</li>
          <li><strong>PCI DSS guidelines:</strong> payment card industry security standards</li>
          <li><strong>SOC 2:</strong> security, availability, and confidentiality controls</li>
          <li><strong>Financial industry standards:</strong> sector-specific security and privacy requirements</li>
        </ul>
      </section>

      <section lfSection id="contact" n="13" heading="Contact information" [hold]="false">
        <p class="lf-p ds-hold" [style.--d]="260">If you have any questions about this Privacy Policy, wish to exercise your rights, or need to report a privacy concern, please get in touch.</p>
        <div class="lf-cards">
          <lf-link-card class="ds-item" [style.--d]="320" icon="envelope-simple" eyebrow="Email" title="Privacy requests"
                        body="Data access, correction and deletion requests, or any question about this policy."
                        cta="admin@iso8583.studio" href="mailto:admin@iso8583.studio" />
          <lf-link-card class="ds-item" [style.--d]="460" icon="chats-circle" eyebrow="All contact channels" title="Support"
                        body="Issues, discussions and engagement enquiries — pick the channel that fits."
                        cta="Contact us" href="/contact" />
        </div>
        <div class="lf-foot ds-hold" [style.--d]="380">
          <span class="lf-foot-line">Document version 2.1 · Effective date 9 June 2025 · Last reviewed 9 June 2025</span>
          <a class="lf-foot-link" routerLink="/terms-and-conditions">Terms and Conditions <ui-icon name="arrow-right" [size]="13" /></a>
        </div>
      </section>
    </lf-body>
  `,
})
export class PrivacyPolicyPage {
  protected readonly crumbs = [{ label: 'Home', link: '/' }, { label: 'Privacy Policy' }];
  protected readonly badge: LfBadge = { tone: 'teal', label: 'Legal · in effect' };
  protected readonly meta = [
    ['Version', '2.1'], ['Effective', '9 June 2025'], ['Last updated', '9 June 2025'],
  ] as const;
  protected readonly toc: LfTocEntry[] = [
    { id: 'overview', n: '01', label: 'Overview & commitment' },
    { id: 'data-collection', n: '02', label: 'Information we collect' },
    { id: 'data-usage', n: '03', label: 'How we use your data' },
    { id: 'data-sharing', n: '04', label: 'Data sharing & disclosure' },
    { id: 'security', n: '05', label: 'Security measures' },
    { id: 'retention', n: '06', label: 'Data retention' },
    { id: 'rights', n: '07', label: 'Your rights' },
    { id: 'cookies', n: '08', label: 'Cookies & tracking' },
    { id: 'international', n: '09', label: 'International transfers' },
    { id: 'minors', n: '10', label: "Children's privacy" },
    { id: 'updates', n: '11', label: 'Policy updates' },
    { id: 'compliance', n: '12', label: 'Legal compliance' },
    { id: 'contact', n: '13', label: 'Contact information' },
  ];
}

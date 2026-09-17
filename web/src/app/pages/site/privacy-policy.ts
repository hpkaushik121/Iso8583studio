import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-privacy-policy',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-privacy-policy' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <span>Privacy Policy</span>
        </div>

        <h1 class="page-title">Privacy Policy</h1>
        <p class="page-description">Your privacy and data security are fundamental to everything we do. This policy explains how ISO8583Studio handles your information with transparency and respect.</p>

        <div class="legal-meta">
            <span><b>Version</b> 2.1</span>
            <span><b>Effective</b> 9 June 2025</span>
            <span><b>Last updated</b> 9 June 2025</span>
        </div>

        <nav class="legal-toc" aria-label="Table of contents">
            <div class="info-card-title">Table of contents</div>
            <ol>
                <li><a href="/privacy-policy#overview">Overview &amp; commitment</a></li>
                <li><a href="/privacy-policy#data-collection">Information we collect</a></li>
                <li><a href="/privacy-policy#data-usage">How we use your data</a></li>
                <li><a href="/privacy-policy#data-sharing">Data sharing &amp; disclosure</a></li>
                <li><a href="/privacy-policy#security">Security measures</a></li>
                <li><a href="/privacy-policy#retention">Data retention</a></li>
                <li><a href="/privacy-policy#rights">Your rights</a></li>
                <li><a href="/privacy-policy#cookies">Cookies &amp; tracking</a></li>
                <li><a href="/privacy-policy#international">International transfers</a></li>
                <li><a href="/privacy-policy#minors">Children's privacy</a></li>
                <li><a href="/privacy-policy#updates">Policy updates</a></li>
                <li><a href="/privacy-policy#compliance">Legal compliance</a></li>
                <li><a href="/privacy-policy#contact">Contact information</a></li>
            </ol>
        </nav>

        <ui-section anchor="overview" heading="Overview &amp; our commitment">
            <p>At ISO8583Studio, we are committed to protecting your privacy and ensuring the security of your personal information. As a professional desktop application for financial transaction processing, we understand the critical importance of data protection in the financial technology sector.</p>

            <div class="info-card tip">
                <div class="info-card-title">Our commitment</div>
                <p>We process only the minimum data necessary to provide our services, implement industry-leading security measures, and never sell your personal information to third parties.</p>
            </div>

            <p>This Privacy Policy applies to:</p>
            <ul>
                <li><strong>ISO8583Studio Desktop Application</strong> — our primary software product</li>
                <li><strong>Official Website</strong> — <a href="https://iso8583.studio">https://iso8583.studio</a></li>
                <li><strong>Support Services</strong> — customer support and technical assistance</li>
                <li><strong>Documentation &amp; Resources</strong> — online guides and documentation</li>
            </ul>
        </ui-section>

        <ui-section anchor="data-collection" heading="Information we collect">

            <h3>Information you provide directly</h3>
            <p>We collect information you voluntarily provide when using our services:</p>
            <ul>
                <li><strong>Account information:</strong> name, email address, company name, and contact details when you create an account or request support</li>
                <li><strong>License information:</strong> license key details, activation information, and subscription data</li>
                <li><strong>Feedback &amp; surveys:</strong> your responses to surveys, feedback forms, and product improvement requests</li>
            </ul>

            <h3>Information collected automatically</h3>
            <p>Our application and website may automatically collect certain technical information:</p>
            <ul>
                <li><strong>Application usage data:</strong> feature usage statistics, performance metrics, and crash reports (anonymized)</li>
                <li><strong>System information:</strong> operating system version, hardware specifications, and software environment details</li>
                <li><strong>Website analytics:</strong> IP address, browser type, pages visited, and interaction patterns</li>
                <li><strong>Log data:</strong> application logs, error reports, and diagnostic information</li>
            </ul>

            <div class="info-card note">
                <div class="info-card-title">Desktop application data</div>
                <p>ISO8583Studio is primarily a desktop application. Most of your transaction data, configurations, and financial information remain on your local system and are not transmitted to our servers unless you explicitly use cloud features or request support.</p>
            </div>

            <h3>Desktop application usage analytics (opt-in)</h3>
            <p>The ISO8583Studio desktop application can report anonymous usage analytics to Google Analytics 4. This is <strong>off by default</strong>. On first launch the application asks whether you wish to enable it, and nothing is transmitted unless you accept. You can change your choice at any time under <em>Settings → Usage Analytics</em>.</p>

            <p>When you have opted in, the application sends:</p>
            <ul>
                <li><strong>Feature usage:</strong> which tools and simulators you open, how long sessions last, whether a calculation succeeded or failed, and the names of screens you visit</li>
                <li><strong>Environment:</strong> application version, operating system and version, CPU architecture, Java runtime version, language, timezone and screen resolution</li>
                <li><strong>Approximate location:</strong> city, region/state and country. To determine this, the application makes a request to a third-party IP geolocation service. Your IP address is used to resolve this location and to let Google resolve it; it is not stored by us as an analytics attribute, and precise GPS coordinates are never collected.</li>
                <li><strong>A random device identifier:</strong> generated on your machine, persisted between launches, and not derived from any hardware, account or network identifier. You can regenerate it at any time from Settings.</li>
                <li><strong>Error types:</strong> the class name of an unexpected error, without its message or stack trace</li>
            </ul>

            <div class="info-card warning">
                <div class="info-card-title">What is never collected</div>
                <p>Card numbers (PANs), PINs or PIN blocks, cryptographic keys or key components, cryptograms, MACs, the contents of any ISO 8583 or HSM message, file paths, hostnames, IP addresses of systems you connect to, or the names you give your simulator profiles. Analytics carries only the identity of the tool or screen in use, timings, and the environment details listed above.</p>
            </div>

            <h3>Financial &amp; transaction data</h3>
            <p>Important clarifications regarding sensitive financial data:</p>
            <ul>
                <li><strong>Local processing:</strong> transaction data processed through ISO8583Studio remains on your local system by default</li>
                <li><strong>No data collection:</strong> we do not collect, store, or transmit your actual financial transaction data</li>
                <li><strong>Configuration data:</strong> gateway configurations and message templates are stored locally unless you use cloud sync features</li>
                <li><strong>Support cases:</strong> if you share configuration or log files for support purposes, we handle them with strict confidentiality</li>
            </ul>
        </ui-section>

        <ui-section anchor="data-usage" heading="How we use your data">
            <p>We use collected information for the following purposes:</p>

            <h3>Service provision &amp; improvement</h3>
            <ul>
                <li><strong>License management:</strong> verify licenses, manage subscriptions, and prevent unauthorized use</li>
                <li><strong>Technical support:</strong> provide customer support, troubleshoot issues, and resolve technical problems</li>
                <li><strong>Product development:</strong> improve our software, develop new features, and enhance user experience</li>
                <li><strong>Performance optimization:</strong> analyze usage patterns to optimize application performance and stability</li>
            </ul>

            <h3>Communication &amp; updates</h3>
            <ul>
                <li><strong>Product updates:</strong> notify you about software updates, security patches, and new releases</li>
                <li><strong>Support communications:</strong> respond to your support requests and provide technical assistance</li>
                <li><strong>Educational content:</strong> share documentation, tutorials, and best practices (with your consent)</li>
                <li><strong>Important notices:</strong> communicate critical security updates or service changes</li>
            </ul>

            <h3>Legal &amp; compliance</h3>
            <ul>
                <li><strong>Legal obligations:</strong> comply with applicable laws, regulations, and legal processes</li>
                <li><strong>Security protection:</strong> detect and prevent fraud, unauthorized access, and security threats</li>
                <li><strong>Terms enforcement:</strong> enforce our <a href="/terms-and-conditions">Terms and Conditions</a> and protect our rights and property</li>
            </ul>
        </ui-section>

        <ui-section anchor="data-sharing" heading="Data sharing &amp; disclosure">
            <p>We do not sell, rent, or trade your personal information. We may share information only in the following limited circumstances:</p>

            <h3>Service providers</h3>
            <p>We may share data with trusted third-party service providers who help us operate our business:</p>
            <ul>
                <li><strong>Cloud infrastructure:</strong> hosting services for our website and support systems</li>
                <li><strong>Analytics services:</strong> <a href="https://policies.google.com/privacy" target="_blank" rel="noopener">Google Analytics 4</a> for website and application usage measurement, and Google Ads for advertising measurement</li>
                <li><strong>Support tools:</strong> customer support and ticketing systems</li>
                <li><strong>Payment processing:</strong> payment processors for license purchases (they handle payment data independently)</li>
            </ul>

            <div class="info-card warning">
                <div class="info-card-title">Important</div>
                <p>All service providers are contractually required to protect your data and use it only for the specific services they provide to us. They cannot use your information for their own purposes.</p>
            </div>

            <h3>Legal requirements</h3>
            <p>We may disclose information when required by law or to protect our rights:</p>
            <ul>
                <li>In response to valid legal processes (subpoenas, court orders, etc.)</li>
                <li>To comply with applicable laws and regulations</li>
                <li>To protect our rights, property, or safety, or that of our users</li>
                <li>To investigate potential violations of our Terms and Conditions</li>
            </ul>

            <h3>Business transfers</h3>
            <p>In the event of a merger, acquisition, or sale of assets, your information may be transferred as part of the transaction. We will notify you of any such change and the choices you may have.</p>
        </ui-section>

        <ui-section anchor="security" heading="Security measures">
            <p>We implement comprehensive security measures to protect your information:</p>

            <h3>Technical safeguards</h3>
            <ul>
                <li><strong>Encryption:</strong> data in transit is protected using TLS/SSL encryption</li>
                <li><strong>Access controls:</strong> strict access controls and authentication for our systems</li>
                <li><strong>Secure infrastructure:</strong> industry-standard security practices for our servers and databases</li>
                <li><strong>Regular updates:</strong> timely security patches and software updates</li>
            </ul>

            <h3>Operational safeguards</h3>
            <ul>
                <li><strong>Employee training:</strong> regular security awareness training for all employees</li>
                <li><strong>Background checks:</strong> comprehensive background checks for personnel with data access</li>
                <li><strong>Incident response:</strong> established procedures for security incident detection and response</li>
                <li><strong>Regular audits:</strong> periodic security assessments and vulnerability testing</li>
            </ul>

            <h3>Application security</h3>
            <ul>
                <li><strong>Local data protection:</strong> your local data remains on your system with standard OS protections</li>
                <li><strong>Secure communications:</strong> all network communications use encrypted channels</li>
                <li><strong>Code signing:</strong> our application is digitally signed to ensure authenticity and integrity</li>
                <li><strong>Regular security reviews:</strong> continuous security assessment of our codebase and infrastructure</li>
            </ul>

            <div class="info-card note">
                <div class="info-card-title">Financial data security</div>
                <p>Since ISO8583Studio processes financial transaction data, we follow industry best practices including PCI DSS guidelines, even though your transaction data typically remains on your local system.</p>
            </div>
        </ui-section>

        <ui-section anchor="retention" heading="Data retention">
            <p>We retain your information only as long as necessary to provide our services and comply with legal obligations:</p>

            <h3>Account information</h3>
            <ul>
                <li><strong>Active accounts:</strong> retained while your account is active and for service provision</li>
                <li><strong>Inactive accounts:</strong> deleted after 2 years of inactivity, unless legal obligations require longer retention</li>
                <li><strong>Support records:</strong> support tickets and communications retained for 3 years for quality and legal purposes</li>
            </ul>

            <h3>Technical data</h3>
            <ul>
                <li><strong>Usage analytics:</strong> aggregated and anonymized usage data retained for 24 months</li>
                <li><strong>Log files:</strong> server logs retained for 90 days for security and operational purposes</li>
                <li><strong>Crash reports:</strong> anonymous crash reports retained for 12 months for product improvement</li>
            </ul>

            <h3>Legal &amp; compliance data</h3>
            <ul>
                <li><strong>Financial records:</strong> license and payment records retained for 7 years for tax and accounting purposes</li>
                <li><strong>Legal holds:</strong> data subject to legal proceedings retained until resolution</li>
                <li><strong>Regulatory requirements:</strong> data retained as required by applicable financial services regulations</li>
            </ul>
        </ui-section>

        <ui-section anchor="rights" heading="Your rights">
            <p>You have several rights regarding your personal information. The specific rights available to you may depend on your location and applicable laws:</p>

            <h3>Access &amp; portability rights</h3>
            <ul>
                <li><strong>Access:</strong> request a copy of the personal information we hold about you</li>
                <li><strong>Portability:</strong> receive your data in a structured, machine-readable format</li>
                <li><strong>Information:</strong> learn about how we process your data and with whom we share it</li>
            </ul>

            <h3>Control &amp; correction rights</h3>
            <ul>
                <li><strong>Correction:</strong> update or correct inaccurate personal information</li>
                <li><strong>Deletion:</strong> request deletion of your personal information (subject to legal obligations)</li>
                <li><strong>Restriction:</strong> limit how we process your information in certain circumstances</li>
                <li><strong>Objection:</strong> object to processing based on legitimate interests</li>
            </ul>

            <h3>Communication preferences</h3>
            <ul>
                <li><strong>Marketing opt-out:</strong> unsubscribe from marketing communications at any time</li>
                <li><strong>Communication settings:</strong> choose how and when we contact you</li>
                <li><strong>Notification preferences:</strong> control which product updates and announcements you receive</li>
            </ul>

            <div class="info-card tip">
                <div class="info-card-title">How to exercise your rights</div>
                <p>Contact us at <a href="mailto:admin@iso8583.studio">admin@iso8583.studio</a> with your request. We will respond within 30 days and may require identity verification for security purposes.</p>
            </div>
        </ui-section>

        <ui-section anchor="cookies" heading="Cookies &amp; tracking technologies">
            <p>Our website uses cookies and similar technologies to improve your experience and understand how our services are used:</p>

            <h3>Types of cookies we use</h3>
            <ul>
                <li><strong>Essential cookies:</strong> required for website functionality and security</li>
                <li><strong>Analytics cookies:</strong> help us understand website usage and improve performance</li>
                <li><strong>Functional cookies:</strong> remember your preferences and settings</li>
                <li><strong>Marketing cookies:</strong> used for targeted advertising (with your consent)</li>
            </ul>

            <h3>Third-party cookies</h3>
            <p>We may use third-party services that set their own cookies:</p>
            <ul>
                <li><strong>Google Analytics 4:</strong> website traffic analysis and usage insights. We also enable Google Signals, which allows Google to associate visits with signed-in Google accounts for cross-device measurement and aggregated demographic reporting.</li>
                <li><strong>Google Ads:</strong> advertising measurement and remarketing audiences, where a campaign is running</li>
                <li><strong>Support chat:</strong> customer support chat functionality</li>
                <li><strong>CDN services:</strong> content delivery and website performance optimization</li>
            </ul>

            <h3>Managing cookies</h3>
            <p>You can control cookies through your browser settings:</p>
            <ul>
                <li>Block all cookies (may affect website functionality)</li>
                <li>Block third-party cookies only</li>
                <li>Delete existing cookies</li>
                <li>Set preferences for future cookies</li>
            </ul>

            <div class="info-card warning">
                <div class="info-card-title">Note</div>
                <p>The desktop application does not use web cookies, but may store local configuration files and preferences on your system for application functionality.</p>
            </div>
        </ui-section>

        <ui-section anchor="international" heading="International data transfers">
            <p>ISO8583Studio operates globally, and your information may be transferred to and processed in countries other than your own:</p>

            <h3>Legal basis for transfers</h3>
            <ul>
                <li><strong>Adequacy decisions:</strong> transfers to countries with adequate data protection laws</li>
                <li><strong>Standard contractual clauses:</strong> EU-approved contracts ensuring data protection</li>
                <li><strong>Your consent:</strong> explicit consent for specific transfers when required</li>
                <li><strong>Necessity:</strong> transfers necessary for service provision or legal compliance</li>
            </ul>

            <h3>Safeguards for international transfers</h3>
            <ul>
                <li><strong>Encryption:</strong> all data transfers use strong encryption</li>
                <li><strong>Access controls:</strong> strict limits on who can access transferred data</li>
                <li><strong>Contractual protections:</strong> legal agreements requiring equivalent protection</li>
                <li><strong>Regular reviews:</strong> ongoing assessment of transfer arrangements and protections</li>
            </ul>
        </ui-section>

        <ui-section anchor="minors" heading="Children's privacy">
            <p>ISO8583Studio is designed for professional use in financial services and is not intended for children under 16. We do not knowingly collect personal information from children.</p>
            <p>If we become aware that we have collected information from a child under 16 without parental consent, we will take steps to delete that information promptly.</p>
        </ui-section>

        <ui-section anchor="updates" heading="Changes to this Privacy Policy">
            <p>We may update this Privacy Policy from time to time to reflect changes in our practices, technology, legal requirements, or other factors.</p>

            <h3>How we notify you of changes</h3>
            <ul>
                <li><strong>Website notice:</strong> prominent notice on our website for 30 days</li>
                <li><strong>Email notification:</strong> email to registered users for material changes</li>
                <li><strong>In-app notification:</strong> notification within the desktop application</li>
                <li><strong>Version dating:</strong> clear dating of policy versions for transparency</li>
            </ul>

            <h3>Types of changes</h3>
            <ul>
                <li><strong>Minor changes:</strong> clarifications, formatting, or contact information updates</li>
                <li><strong>Material changes:</strong> changes to data collection, use, or sharing practices</li>
                <li><strong>Legal changes:</strong> updates required by new laws or regulations</li>
            </ul>

            <div class="info-card note">
                <div class="info-card-title">Your continued use</div>
                <p>Continued use of our services after policy changes constitutes acceptance of the updated terms. If you disagree with changes, please discontinue use and contact us about data deletion.</p>
            </div>
        </ui-section>

        <ui-section anchor="compliance" heading="Legal compliance &amp; frameworks">
            <p>ISO8583Studio complies with major data protection frameworks and regulations:</p>

            <h3>Regulatory compliance</h3>
            <ul>
                <li><strong>GDPR:</strong> European General Data Protection Regulation compliance</li>
                <li><strong>CCPA:</strong> California Consumer Privacy Act protections</li>
                <li><strong>PIPEDA:</strong> Canadian Personal Information Protection Act compliance</li>
                <li><strong>Financial regulations:</strong> relevant financial services data protection requirements</li>
            </ul>

            <h3>Industry standards</h3>
            <ul>
                <li><strong>ISO 27001:</strong> information security management best practices</li>
                <li><strong>PCI DSS guidelines:</strong> payment card industry security standards</li>
                <li><strong>SOC 2:</strong> security, availability, and confidentiality controls</li>
                <li><strong>Financial industry standards:</strong> sector-specific security and privacy requirements</li>
            </ul>
        </ui-section>

        <ui-section anchor="contact" heading="Contact information">
            <p>If you have any questions about this Privacy Policy, wish to exercise your rights, or need to report a privacy concern, please get in touch.</p>
            <div class="hub-grid">
                <a class="hub-card" href="mailto:admin@iso8583.studio"><div class="hub-body"><div class="hub-title">Email <span class="badge badge-teal">Privacy requests</span></div><p class="hub-desc">Data access, correction and deletion requests, or any question about this policy.</p><span class="hub-link">admin@iso8583.studio →</span></div></a>
                <a class="hub-card" href="/contact"><div class="hub-body"><div class="hub-title">All contact channels <span class="badge badge-blue">Support</span></div><p class="hub-desc">Issues, discussions and engagement enquiries — pick the channel that fits.</p><span class="hub-link">Contact us →</span></div></a>
            </div>

            <div class="legal-foot">
                <p><strong>Document version</strong> 2.1 &nbsp;·&nbsp; <strong>Effective date</strong> 9 June 2025 &nbsp;·&nbsp; <strong>Last reviewed</strong> 9 June 2025</p>
                <p>See also the <a href="/terms-and-conditions">Terms and Conditions</a>.</p>
            </div>
        </ui-section>
</main>`,
})
export class PrivacyPolicyPage {}

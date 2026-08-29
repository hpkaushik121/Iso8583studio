import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-terms-and-conditions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-terms-and-conditions' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <span>Terms and Conditions</span>
        </div>

        <h1 class="page-title">Terms and Conditions</h1>
        <p class="page-description">Open source software agreement for the ISO8583Studio desktop application. These terms govern the use of our financial transaction processing software.</p>

        <div class="legal-meta">
            <span><b>Version</b> 1.0 (Open Source)</span>
            <span><b>Effective</b> 9 June 2025</span>
            <span><b>Last revised</b> 9 June 2025</span>
            <span><b>License</b> Apache 2.0</span>
        </div>

        <div class="feature-grid">
            <div class="feature-item">
                <div class="feature-item-icon">🆓</div>
                <h4>Completely free</h4>
                <p>No payment required, no license fees, no subscriptions — forever free.</p>
            </div>
            <div class="feature-item">
                <div class="feature-item-icon">⚠</div>
                <h4>No warranties</h4>
                <p>Open source software provided "as is" with no guarantees or support obligations.</p>
            </div>
            <div class="feature-item">
                <div class="feature-item-icon">🤝</div>
                <h4>Community driven</h4>
                <p>Support and development by the open source community.</p>
            </div>
        </div>

        <nav class="legal-toc" aria-label="Table of contents">
            <div class="info-card-title">Table of contents</div>
            <ol>
                <li><a href="/terms-and-conditions#agreement">Open source software agreement</a></li>
                <li><a href="/terms-and-conditions#license">Apache License 2.0 grant</a></li>
                <li><a href="/terms-and-conditions#usage">Permitted usage</a></li>
                <li><a href="/terms-and-conditions#limitations">No warranties or guarantees</a></li>
                <li><a href="/terms-and-conditions#support">Community support</a></li>
                <li><a href="/terms-and-conditions#contact">Community and contact</a></li>
            </ol>
        </nav>

        <section class="doc-section">
            <h2 id="agreement">Open source software agreement</h2>
            <p>This Open Source Software Agreement ("Agreement") governs your use of ISO8583Studio desktop application software ("Software"), which is free and open source software provided by AiCortext Solutions Pvt. Ltd. ("we," "us," or "our") under the Apache License 2.0.</p>

            <div class="info-card tip">
                <div class="info-card-title">Acceptance</div>
                <p><strong>By downloading, installing, or using ISO8583Studio, you acknowledge that this is open source software provided "AS IS" without any warranty or obligation from the developers, distributed under the Apache License 2.0.</strong></p>
            </div>

            <div class="info-card warning">
                <div class="info-card-title">Important</div>
                <p>This is free, open source software distributed under Apache 2.0. There are no purchase requirements, no refunds (as the software is free), and no warranties or obligations from the developers regarding its performance or suitability for any purpose.</p>
            </div>
        </section>

        <section class="doc-section">
            <h2 id="license">Apache License 2.0 grant</h2>

            <h3>Open source license</h3>
            <p>ISO8583Studio is distributed under the Apache License 2.0, which grants you the following rights:</p>
            <ul>
                <li><strong>Use:</strong> use the Software for any purpose, including commercial purposes</li>
                <li><strong>Reproduce:</strong> make unlimited copies of the Software</li>
                <li><strong>Modify:</strong> create derivative works based on the Software</li>
                <li><strong>Distribute:</strong> distribute original or modified versions of the Software</li>
                <li><strong>Private use:</strong> use the Software privately without sharing your modifications</li>
                <li><strong>Commercial use:</strong> use the Software in commercial environments and profit-generating activities</li>
                <li><strong>Patent grant:</strong> receive patent rights from contributors for their contributions</li>
                <li><strong>Sublicense:</strong> grant sublicenses to third parties</li>
            </ul>

            <h3>Apache License 2.0 key features</h3>
            <div class="info-card note">
                <div class="info-card-title">Apache License 2.0 benefits</div>
                <ul>
                    <li><strong>Patent protection:</strong> includes express patent grant from contributors</li>
                    <li><strong>Attribution requirements:</strong> clear requirements for notices and attributions</li>
                    <li><strong>Trademark protection:</strong> does not grant rights to use trademarks</li>
                    <li><strong>Contribution licensing:</strong> automatic licensing of contributions</li>
                    <li><strong>Enterprise friendly:</strong> widely accepted in enterprise environments</li>
                </ul>
            </div>

            <h3>License requirements</h3>
            <p>When redistributing the Software, you must:</p>
            <ul>
                <li><strong>Preserve notices:</strong> include the original Apache License 2.0 notice</li>
                <li><strong>Include copyright:</strong> retain all copyright, patent, trademark, and attribution notices</li>
                <li><strong>Include license:</strong> provide a copy of the Apache License 2.0</li>
                <li><strong>State changes:</strong> include a NOTICE file documenting significant changes you made</li>
                <li><strong>No trademark use:</strong> do not use the names, trademarks, or logos without permission</li>
            </ul>
        </section>

        <section class="doc-section">
            <h2 id="usage">Permitted usage</h2>
            <p>As open source software under the Apache 2.0 License, you may use ISO8583Studio for any purpose, including:</p>

            <h3>Financial transaction processing</h3>
            <ul>
                <li><strong>ISO 8583 message processing:</strong> create, modify, parse, and analyze ISO 8583 financial messages</li>
                <li><strong>Gateway configuration:</strong> set up and manage payment gateways, switches, and transaction processors</li>
                <li><strong>Protocol support:</strong> use TCP/IP, RS232, dial-up, and REST/SOAP protocols for financial communications</li>
                <li><strong>Host simulation:</strong> simulate host systems for testing and development purposes</li>
                <li><strong>Transaction testing:</strong> test transaction flows, message formats, and system integrations</li>
            </ul>

            <h3>Development and modification</h3>
            <ul>
                <li><strong>Source code access:</strong> access, read, and study the complete source code</li>
                <li><strong>Modification rights:</strong> modify the software to meet your specific requirements</li>
                <li><strong>Custom features:</strong> add new features and functionality as needed</li>
                <li><strong>Integration development:</strong> create custom integrations with other systems</li>
                <li><strong>Bug fixes:</strong> fix bugs and improve software stability</li>
                <li><strong>Derivative works:</strong> create derivative works based on the original Software</li>
            </ul>

            <h3>Distribution and commercial use</h3>
            <ul>
                <li><strong>Commercial operations:</strong> use in production environments for profit-generating activities</li>
                <li><strong>Redistribution:</strong> distribute original or modified versions (with proper attribution and notices)</li>
                <li><strong>Incorporation:</strong> incorporate into other software products or services</li>
                <li><strong>Selling:</strong> sell software that includes or is based on ISO8583Studio</li>
                <li><strong>Sublicensing:</strong> grant sublicenses to third parties under the same terms</li>
            </ul>

            <div class="info-card tip">
                <div class="info-card-title">Apache 2.0 advantages</div>
                <p>The Apache License 2.0 provides explicit patent protection and clear contribution guidelines, making it particularly suitable for enterprise and commercial use while maintaining open source principles.</p>
            </div>
        </section>

        <section class="doc-section">
            <h2 id="limitations">No warranties or guarantees</h2>

            <h3>Apache License 2.0 disclaimer</h3>
            <div class="info-card danger">
                <div class="info-card-title">Disclaimer of warranty</div>
                <p><strong>UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING, LICENSOR PROVIDES THE WORK (AND EACH CONTRIBUTOR PROVIDES ITS CONTRIBUTIONS) ON AN "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, ANY WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE.</strong></p>
            </div>

            <p>As open source software distributed under the Apache License 2.0, ISO8583Studio comes with absolutely no warranties:</p>
            <ul>
                <li><strong>No warranty of functionality:</strong> no guarantee that the software will work for your specific needs</li>
                <li><strong>No warranty of quality:</strong> no assurance of software quality, reliability, or performance</li>
                <li><strong>No warranty of accuracy:</strong> no guarantee that calculations or processes will be accurate</li>
                <li><strong>No warranty of security:</strong> no guarantee that the software is secure or free from vulnerabilities</li>
                <li><strong>No warranty of compatibility:</strong> no guarantee of compatibility with your systems or environment</li>
                <li><strong>No warranty of non-infringement:</strong> no guarantee that use won't infringe third-party rights</li>
                <li><strong>No warranty of title:</strong> no warranty regarding ownership or title to the software</li>
            </ul>

            <h3>Financial transaction processing</h3>
            <div class="info-card warning">
                <div class="info-card-title">Critical warning</div>
                <p>This software processes financial transactions. You are solely responsible for testing, validation, and ensuring the software meets your regulatory and business requirements. No warranties are provided regarding financial accuracy or compliance.</p>
            </div>
            <ul>
                <li><strong>No transaction guarantees:</strong> no warranty that financial transactions will process correctly</li>
                <li><strong>No regulatory compliance:</strong> no guarantee of compliance with financial regulations</li>
                <li><strong>No data accuracy:</strong> no warranty regarding accuracy of financial calculations or data</li>
                <li><strong>No security assurance:</strong> no guarantee of security for sensitive financial data</li>
                <li><strong>No audit compliance:</strong> no warranty regarding audit trail accuracy or completeness</li>
            </ul>

            <h3>Complete disclaimer of liability</h3>
            <div class="info-card danger">
                <div class="info-card-title">Limitation of liability</div>
                <p><strong>IN NO EVENT AND UNDER NO LEGAL THEORY, WHETHER IN TORT (INCLUDING NEGLIGENCE), CONTRACT, OR OTHERWISE, UNLESS REQUIRED BY APPLICABLE LAW (SUCH AS DELIBERATE AND GROSSLY NEGLIGENT ACTS) OR AGREED TO IN WRITING, SHALL ANY CONTRIBUTOR BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES OF ANY CHARACTER ARISING AS A RESULT OF THIS LICENSE OR OUT OF THE USE OR INABILITY TO USE THE WORK.</strong></p>
            </div>

            <p>The developers and contributors of ISO8583Studio have no liability whatsoever for:</p>
            <ul>
                <li><strong>Financial losses:</strong> any financial losses, lost profits, or business damages</li>
                <li><strong>Data loss:</strong> loss of data, corruption, or unauthorized access to information</li>
                <li><strong>System failures:</strong> system downtime, crashes, or integration failures</li>
                <li><strong>Transaction errors:</strong> errors in financial transaction processing or calculations</li>
                <li><strong>Security breaches:</strong> security vulnerabilities or data breaches</li>
                <li><strong>Regulatory issues:</strong> regulatory violations or compliance failures</li>
            </ul>

            <div class="info-card warning">
                <div class="info-card-title">At your own risk</div>
                <p><strong>You use this software entirely at your own risk. You are responsible for evaluating the software's suitability for your intended use and for all consequences of its use.</strong></p>
            </div>
        </section>

        <section class="doc-section">
            <h2 id="support">Community support</h2>

            <h3>Open source support model</h3>
            <p>As open source software, support is primarily community-driven:</p>
            <ul>
                <li><strong>Community forums:</strong> user community provides peer-to-peer support</li>
                <li><strong>Documentation:</strong> comprehensive documentation maintained by the community</li>
                <li><strong>Issue tracking:</strong> public issue tracking on the project repository</li>
                <li><strong>Wiki and knowledge base:</strong> community-maintained documentation and guides</li>
            </ul>

            <h3>No guaranteed support</h3>
            <div class="info-card warning">
                <div class="info-card-title">Important</div>
                <p>There is no guaranteed support, service level agreements, or response times. Support is provided by volunteers on a best-effort basis.</p>
            </div>
            <ul>
                <li><strong>Best effort:</strong> community members help when available and willing</li>
                <li><strong>No SLA:</strong> no guaranteed response times or resolution schedules</li>
                <li><strong>Volunteer basis:</strong> all support is provided by volunteers in their spare time</li>
                <li><strong>Self-service:</strong> users are encouraged to read documentation and search existing issues first</li>
            </ul>

            <h3>How to get help</h3>
            <ul>
                <li><strong>Read documentation:</strong> check the <a href="/docs">documentation</a> first</li>
                <li><strong>Search issues:</strong> look through existing GitHub issues for solutions</li>
                <li><strong>Community forums:</strong> ask questions in community discussion forums</li>
                <li><strong>Report bugs:</strong> submit bug reports through the project's issue tracker</li>
                <li><strong>Contribute:</strong> contribute fixes and improvements back to the project</li>
            </ul>
        </section>

        <section class="doc-section">
            <h2 id="contact">Community and contact</h2>
            <p>ISO8583Studio is open source software. Support comes from the community, and contributions are welcome from everyone.</p>

            <div class="hub-grid">
                <a class="hub-card" href="https://github.com/hpkaushik121/Iso8583studio/discussions"><div class="hub-body"><div class="hub-title">Discussions <span class="badge badge-blue">Q&amp;A</span></div><p class="hub-desc">Community questions and discussions.</p><span class="hub-link">GitHub Discussions →</span></div></a>
                <a class="hub-card" href="https://github.com/hpkaushik121/Iso8583studio/issues"><div class="hub-body"><div class="hub-title">Bug reports <span class="badge badge-yellow">Issues</span></div><p class="hub-desc">Report bugs and request features.</p><span class="hub-link">GitHub Issues →</span></div></a>
                <a class="hub-card" href="https://github.com/hpkaushik121/Iso8583studio"><div class="hub-body"><div class="hub-title">Source code <span class="badge badge-green">Apache 2.0</span></div><p class="hub-desc">Read the source, and contribute back to the project.</p><span class="hub-link">Open repository →</span></div></a>
                <a class="hub-card" href="mailto:sk@iso8583.studio"><div class="hub-body"><div class="hub-title">Professional services <span class="badge badge-purple">Optional</span></div><p class="hub-desc">Paid consulting, custom development, or enterprise support.</p><span class="hub-link">sk@iso8583.studio →</span></div></a>
            </div>

            <div class="info-card note">
                <div class="info-card-title">Remember</div>
                <p>This is open source software. Support is provided by the community on a volunteer basis with no guarantees or service level agreements.</p>
            </div>

            <div class="info-card tip">
                <div class="info-card-title">Apache License 2.0 summary</div>
                <p><strong>Permissions:</strong> commercial use, modification, distribution, patent use, private use<br>
                   <strong>Conditions:</strong> license and copyright notice, state changes<br>
                   <strong>Limitations:</strong> liability, trademark use, warranty</p>
                <p>This is not legal advice. See the full <a href="https://github.com/hpkaushik121/Iso8583studio/blob/main/LICENSE">Apache License 2.0 text</a> for complete terms.</p>
            </div>

            <div class="legal-foot">
                <p><strong>Terms version</strong> 1.0 (Open Source) &nbsp;·&nbsp; <strong>Effective date</strong> 9 June 2025 &nbsp;·&nbsp; <strong>Last revised</strong> 9 June 2025</p>
                <p>See also the <a href="/privacy-policy">Privacy Policy</a>.</p>
            </div>
        </section>
</main>`,
})
export class TermsAndConditionsPage {}

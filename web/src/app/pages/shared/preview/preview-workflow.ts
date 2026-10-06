import {
  ChangeDetectionStrategy, Component, DOCUMENT, DestroyRef, ElementRef, PLATFORM_ID,
  afterNextRender, computed, effect, inject, input, signal, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UiIcon } from '../../../ui';
import { PreviewScenario, SimulatorPreviewData } from '../../data/simulator-preview.data';

/** Milliseconds each step stays on screen while the walk-through plays itself. */
const STEP_MS = 3200;

/** One step of one scenario, with the scenario's overrides applied. */
interface StepView {
  title: string;
  body: string;
  tag: string;
  request: string;
  activeNode: number;
  connections: number[];
  reverse: boolean;
  /** Line beside the tag: the step title, or the scenario's result on the last step. */
  state: string;
  /** Heading of the right-hand pane. */
  outLabel: string;
  /** Body of the right-hand pane. */
  output: string;
}

interface ScenarioView {
  id: string;
  label: string;
  steps: StepView[];
}

function scenarioView(page: SimulatorPreviewData, scenario: PreviewScenario): ScenarioView {
  const last = page.steps.length - 1;
  return {
    id: scenario.id,
    label: scenario.label,
    steps: page.steps.map((base, i) => {
      const step = { ...base, ...scenario.steps[i] };
      return {
        title: step.title,
        body: step.body,
        tag: step.tag,
        request: step.request,
        activeNode: step.activeNode,
        connections: step.connections,
        reverse: step.reverse,
        state: i === last ? scenario.result : step.title,
        outLabel: i === last ? 'Scenario outcome' : 'Next state',
        output: i === last ? `${scenario.code}\n${scenario.description}` : step.response,
      };
    }),
  };
}

/**
 * PreviewWorkflow — the illustrative walk-through on a concept guide: a glass
 * panel showing three devices and a sample exchange, above four step cards.
 * Three scenario tabs swap the text and payloads of those four steps.
 *
 *   <app-preview-workflow [page]="page" />
 *
 * Every scenario's step cards and every step's payloads are in the markup;
 * the ones not selected carry `hidden`. The prerendered page shows the first
 * scenario at its first step, not playing.
 *
 * In the browser the steps advance every 3.2 s while the panel is on screen,
 * the tab is in the foreground and the reader has not paused. Choosing a step
 * pauses; choosing a scenario or pressing replay starts again from step one.
 * Under reduced motion nothing advances on its own and the play button is
 * disabled.
 *
 * Tabs, step cards and the transport controls are <button>s: they change what
 * this panel shows and navigate nowhere.
 */
@Component({
  selector: 'app-preview-workflow',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sp-workflow-host' },
  template: `
    <div #player class="sp-workflow" [attr.data-running]="running()">
      @for (sc of views(); track sc.id; let s = $index) {
        <div class="sp-steps" role="group" [attr.aria-label]="'Workflow steps — ' + sc.label" [hidden]="scenario() !== s">
          @for (st of sc.steps; track $index; let i = $index) {
            <div class="ds-item" [style.--d]="410 + i * 110">
              <button type="button" class="sp-step" [class.is-active]="step() === i" (click)="select(i)"
                      [attr.aria-pressed]="step() === i" aria-controls="workflow-view">
                <span class="sp-step-index">0{{ i + 1 }}</span>
                <span class="sp-step-copy"><strong>{{ st.title }}</strong><span class="sp-step-body">{{ st.body }}</span></span>
              </button>
            </div>
          }
        </div>
      }

      <div class="sp-stage sp-demo ds-item" [class.off]="!visible()" [style.--d]="300">
        <div class="sp-glass-wrap">
          <div class="sp-glass-bg" aria-hidden="true"><span></span><span></span><span></span></div>
          <div class="sp-glass" id="workflow-view" role="region" aria-label="Illustrative transaction flow">
            <div class="sp-glass-dots" aria-hidden="true"></div>

            <div class="sp-glass-head sp-demo-top">
              <span class="sp-demo-name"><span class="sp-window-dots" aria-hidden="true"><i></i><i></i><i></i></span><ui-icon [name]="page().icon" [size]="14" />{{ page().name }} — concept</span>
              <span class="sp-mono">0{{ step() + 1 }} / 0{{ page().steps.length }}</span>
            </div>

            <div class="sp-tabs sp-scenarios" role="group" aria-label="Preview scenario">
              @for (sc of views(); track sc.id; let s = $index) {
                <button type="button" class="sp-tab" [class.on]="scenario() === s"
                        [attr.aria-pressed]="scenario() === s" (click)="choose(s)">{{ sc.label }}</button>
              }
            </div>

            <div class="sp-network" [attr.data-reverse]="current().reverse">
              <div class="sp-wire sp-wire-one" [class.is-lit]="current().connections.includes(0)" aria-hidden="true"><span></span></div>
              <div class="sp-wire sp-wire-two" [class.is-lit]="current().connections.includes(1)" aria-hidden="true"><span></span></div>
              @for (node of page().nodes; track node.label; let i = $index) {
                <div class="sp-node" [class.is-active]="current().activeNode === i">
                  <div class="sp-node-device"><ui-icon [name]="node.icon" [size]="30" /><span class="sp-node-led"></span></div>
                  <strong>{{ node.label }}</strong>
                  <span class="sp-node-detail">{{ node.detail }}</span>
                </div>
              }
            </div>

            @for (sc of views(); track sc.id; let s = $index) {
              @for (st of sc.steps; track $index; let i = $index) {
                <div class="sp-exchange" [hidden]="scenario() !== s || step() !== i">
                  <div class="sp-exchange-heading"><span class="sp-mono">{{ st.tag }}</span><span class="sp-exchange-state">{{ st.state }}</span></div>
                  <div class="sp-payloads">
                    <div class="sp-pane"><span class="sp-pane-h sp-pane-h--req">Input</span><pre>{{ st.request }}</pre></div>
                    <div class="sp-pane"><span class="sp-pane-h sp-pane-h--rsp">{{ st.outLabel }}</span><pre>{{ st.output }}</pre></div>
                  </div>
                </div>
              }
            }

            <div class="sp-demo-controls">
              <div class="sp-playback-buttons">
                <button type="button" aria-label="Replay workflow" title="Replay workflow" (click)="restart(true)"><ui-icon name="arrow-counter-clockwise" [size]="16" /></button>
                <button type="button" [disabled]="reduced()" (click)="toggle()"
                        [attr.aria-label]="reduced() ? 'Automatic motion disabled' : playing() ? 'Pause animation' : 'Play animation'"
                        [attr.title]="reduced() ? 'Reduced motion is enabled; select a step or use Next step' : null">
                  @if (playing() && !reduced()) { <ui-icon name="pause" [size]="15" /> } @else { <ui-icon name="play" [size]="15" /> }
                  {{ reduced() ? 'Static preview' : playing() ? 'Pause' : 'Play' }}
                </button>
              </div>
              <span class="sp-motion-caption">{{ reduced() ? 'Select any step to explore' : 'Concept motion · 4 steps' }}</span>
              <button type="button" aria-label="Next step" (click)="select((step() + 1) % page().steps.length)"><ui-icon name="arrow-right" [size]="17" /></button>
            </div>

            <div class="sp-timeline" [class.is-alt]="revision() % 2 === 1" aria-hidden="true">
              @for (st of page().steps; track $index; let i = $index) {
                <span [class.is-complete]="i <= step()"><i [class.is-current]="i === step()"></i></span>
              }
            </div>

            <div class="sp-sheen" aria-hidden="true"></div>
          </div>
        </div>
      </div>
    </div>

    <p class="sp-caption"><ui-icon name="info" [size]="15" /><span>Concept demonstration with sample data. This preview explains the planned flow; it does not run a simulator or connect to hardware.</span></p>
  `,
})
export class PreviewWorkflow {
  readonly page = input.required<SimulatorPreviewData>();

  protected readonly scenario = signal(0);
  protected readonly step = signal(0);
  protected readonly playing = signal(true);
  protected readonly visible = signal(false);
  protected readonly reduced = signal(false);
  /** Bumped by every restart, so the clock and the progress bar start over even when the step does not change. */
  protected readonly revision = signal(0);
  private readonly foreground = signal(true);

  protected readonly views = computed(() => this.page().scenarios.map((sc) => scenarioView(this.page(), sc)));
  protected readonly current = computed(() => this.views()[this.scenario()].steps[this.step()]);
  /** False in the prerender: nothing is on screen there. */
  protected readonly running = computed(() =>
    this.playing() && this.visible() && this.foreground() && !this.reduced());

  private readonly player = viewChild.required<ElementRef<HTMLElement>>('player');
  private readonly doc = inject(DOCUMENT);

  constructor() {
    // The clock: one timeout per step, re-armed whenever the step, the
    // scenario (through revision) or the running state changes.
    effect((onCleanup) => {
      this.step();
      this.revision();
      const steps = this.page().steps.length;
      if (!this.running()) return;
      const timer = setTimeout(() => this.step.update((s) => (s + 1) % steps), STEP_MS);
      onCleanup(() => clearTimeout(timer));
    });

    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const destroy = inject(DestroyRef);
    afterNextRender(() => this.watch(destroy));
  }

  /** A step chosen by hand stays put. */
  protected select(i: number): void {
    this.step.set(i);
    this.playing.set(false);
  }

  protected choose(i: number): void {
    this.scenario.set(i);
    this.restart();
  }

  protected restart(play = this.playing()): void {
    this.step.set(0);
    this.playing.set(play);
    this.revision.update((v) => v + 1);
  }

  protected toggle(): void {
    this.playing.update((on) => !on);
  }

  private watch(destroy: DestroyRef): void {
    const view = this.doc.defaultView;
    if (!view) return;

    const motion = view.matchMedia('(prefers-reduced-motion: reduce)');
    const onMotion = () => this.reduced.set(motion.matches);
    onMotion();
    motion.addEventListener('change', onMotion);
    destroy.onDestroy(() => motion.removeEventListener('change', onMotion));

    const onVisibility = () => this.foreground.set(!this.doc.hidden);
    onVisibility();
    this.doc.addEventListener('visibilitychange', onVisibility);
    destroy.onDestroy(() => this.doc.removeEventListener('visibilitychange', onVisibility));

    if (!('IntersectionObserver' in view)) { this.visible.set(true); return; }
    const io = new IntersectionObserver(([entry]) => this.visible.set(entry.isIntersecting), { threshold: 0.18 });
    io.observe(this.player().nativeElement);
    destroy.onDestroy(() => io.disconnect());
  }
}

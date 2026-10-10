// @ts-nocheck
/* Ported from the design system (templates/_journey/story-engine.js). Changes
   from the source, all marked "site:" below:
     - three.js and the model builders are ES imports (three through
       ./three-subset so the chunk carries only what is drawn);
     - the flat plates (.model-fallback) are visible in the prerendered page
       and are switched by the `journey-live` class on the section rather than
       by the hidden attribute;
     - the frame loop stops once the section has scrolled away above the
       viewport, and destroy() puts back everything the engine changed. */
import * as T from './three-subset';
import * as premium from './premium-models';
import * as batch from './scene-batch';
import * as terminal from './terminal-model';

// The payment-journey stages share one WebGL context. The renderer draws each
// stage's scene into an offscreen buffer and blits it into that stage's own 2D
// canvas, so scrolling stays cheap and every canvas keeps its last frame.
// STAGES grows one entry per journey stage; rows are matched to it by index.
const clamp = (x, a = 0, b = 1) => Math.max(a, Math.min(b, x));
const smooth = x => { x = clamp(x); return x * x * (3 - 2 * x); };

const STAGES = [{
  key: 'card',
  build(T, models) { const model = models.card; model.scale.setScalar(5); model.rotation.set(.12, .35, -.16); return { model, span: 4.2, fit: 4.6, position: [0, .2, 8], look: [0, 0, 0] }; },
  // Slow drift so the card breathes, plus a scroll-linked turn as the stage passes.
  pose(shot, t, local, still) {
    const drift = still ? 0 : Math.sin(t * .5);
    shot.model.rotation.set(.10 + drift * .02, -.34 + drift * .04 + local * .66, .15);
    shot.wrap.position.y = drift * .028;
    return 'card' + (still ? '0' : t.toFixed(3)) + local.toFixed(3);
  }
}, {
  key: 'terminal',
  build(T, models, terminalModule) {
    const model = terminalModule.buildTerminal(T);
    model.userData.setState('ready');
    const card = models.card.clone(true);
    card.scale.setScalar(2.8); card.rotation.set(-Math.PI / 2, 0, -Math.PI / 2);
    card.position.copy(model.userData.cardSlot.insertedPosition);
    model.add(card);
    return { model, card, exclude: [card], span: 4.65, fit: 4.7, position: [-5, 10, 8], look: [0, 0, .47] };
  },
  // The terminal's own card is only shown once the travelling card has seated;
  // cardTransfer owns the insertion, exactly as in the source animation.
  pose(shot, t, local, still) {
    const drift = still ? 0 : Math.sin(t * .4);
    shot.wrap.rotation.y = drift * .022;
    return 'terminal' + (shot.connection || 'ready') + (still ? '0' : drift.toFixed(3));
  }
}, {
  key: 'processing',
  build(T, models, terminalModule) {
    const model = terminalModule.buildTerminal(T);
    model.userData.setState('processing');
    const card = models.card.clone(true);
    card.scale.setScalar(2.8); card.rotation.set(-Math.PI / 2, 0, -Math.PI / 2);
    card.position.copy(model.userData.cardSlot.insertedPosition);
    model.add(card);
    return { model, card, exclude: [card], span: 4.65, fit: 4.7, position: [-5, 10, 8], look: [0, 0, .47] };
  },
  // The message parts fade up as the stage arrives, as in the source animation.
  pose(shot, t, local, still) {
    const stack = shot.row.querySelector('.message-stack');
    if (stack) stack.style.opacity = String(still ? 1 : .35 + .65 * smooth(local * 1.7));
    const drift = still ? 0 : Math.sin(t * .4);
    shot.wrap.rotation.y = drift * .018;
    return 'processing' + (still ? '0' : drift.toFixed(3));
  }
}, {
  key: 'host',
  build(T, models) {
    return { model: models.host, span: 2.5, fit: 3.4, position: [4, 2.1, 7], look: [0, 0, 0], shadowY: -.26 };
  },
  pose(shot, t, local, still) {
    const drift = still ? 0 : Math.sin(t * .45);
    shot.wrap.rotation.y = drift * .03;
    shot.wrap.position.y = drift * .012;
    return 'host' + (still ? '0' : drift.toFixed(3));
  }
}, {
  key: 'hsm',
  build(T, models, terminalModule, helpers) {
    const model = models.hsm;
    model.userData.lid.position.y = .94; model.userData.shield.position.y = .41;
    helpers.glow(model, 0, .18, 0, 1.6, 0x1e88e5, .8);
    return { model, span: 2.85, fit: 3.4, position: [3, 4.5, 7], look: [0, .42, 0], shadowY: -.31, exclude: [model.userData.lid, model.userData.shield, model.userData.core].filter(Boolean) };
  },
  // The cutaway opens as the stage arrives, and the verified stamp fades up behind it.
  pose(shot, t, local, still) {
    const open = still ? 1 : smooth((local - .12) / .65);
    shot.model.userData.lid.position.y = .27 + .75 * open;
    shot.model.userData.shield.position.y = .24 + .19 * open;
    const verified = shot.row.querySelector('.verified');
    if (verified) verified.style.opacity = String(still ? 1 : .22 + .78 * smooth((local - .55) / .25));
    return 'hsm' + open.toFixed(4);
  }
}, {
  key: 'network',
  build(T, models, terminalModule, helpers) {
    const model = models.network;
    helpers.glow(model, 0, 0, -.1, 3, 0x1e88e5, .35);
    model.traverse(o => { if (o.isMesh && o.geometry.type === 'SphereGeometry' && o.geometry.parameters.radius < .1) helpers.glow(o, 0, 0, 0, .24, 0x9fd3ff, .9); });
    for (let j = 0; j < 2; j++) {
      const ring = new T.Mesh(new T.TorusGeometry(1.42 + j * .11, .004, 4, 100), new T.MeshBasicMaterial({ color: 0x6ab7ff, transparent: true, opacity: .34 }));
      ring.rotation.set(1.10 + j * .75, .3, -.36); model.add(ring);
    }
    return { model, span: 2.95, fit: 3.15, position: [0, .3, 7], look: [0, 0, 0] };
  },
  // The globe turns continuously: the network never stops moving.
  pose(shot, t, local, still) {
    shot.model.rotation.y = still ? .15 : t * .12;
    return 'network' + (still ? '0' : t.toFixed(3));
  }
}, {
  key: 'issuer',
  build(T, models) {
    return { model: models.issuer, span: 2.9, fit: 3.3, position: [4.5, 2.6, 7], look: [0, 0, 0], shadowY: -.9 };
  },
  // The response panel resolves as the decision is made.
  pose(shot, t, local, still) {
    const panel = shot.row.querySelector('.response-panel');
    if (panel) panel.style.opacity = String(still ? 1 : .2 + .8 * smooth((local - .1) / .6));
    const drift = still ? 0 : Math.sin(t * .42);
    shot.wrap.rotation.y = drift * .02;
    return 'issuer' + (still ? '0' : drift.toFixed(3));
  }
}, {
  key: 'approved',
  build(T, models, terminalModule) {
    const model = terminalModule.buildTerminal(T);
    model.userData.setState('approved');
    const card = models.card.clone(true);
    card.scale.setScalar(2.8); card.rotation.set(-Math.PI / 2, 0, -Math.PI / 2);
    card.position.copy(model.userData.cardSlot.insertedPosition);
    model.add(card);
    return { model, card, exclude: [card], span: 4.65, fit: 4.7, position: [-5, 10, 8], look: [0, 0, .47] };
  },
  // The response lands on the same terminal the card went into.
  pose(shot, t, local, still) {
    const state = still || local > .38 ? 'approved' : 'processing';
    if (shot.state !== state) { shot.model.userData.setState(state); shot.state = state; }
    const drift = still ? 0 : Math.sin(t * .4);
    shot.wrap.rotation.y = drift * .018;
    return 'approved' + state + (still ? '0' : drift.toFixed(3));
  }
}];

export async function mountStory(root, opts = {}) {
  const rows = [...root.querySelectorAll('.story-stage')];
  const main = root.querySelector('.story-stages');
  const trail = root.querySelector('.journey-trail');
  const heroEl = document.querySelector(opts.heroSelector || '.hero-card-view');
  const media = matchMedia('(prefers-reduced-motion: reduce)');
  const cleanups = [], shots = [], textures = new Set(), geometries = new Set(), materials = new Set();
  const offscreen = document.createElement('canvas');
  let renderer, envTarget, raf = 0, disposed = false, visible = true, ready = false, contextLost = false;
  let paused = media.matches, still = media.matches, clock = 0, lastTime = 0, drawCount = 0;
  let renderScale = 1, slowFrames = 0, lastWidth = 0, bridge = null, heroPhase = '';
  let pathLength = 1, trailMask = null, layoutDirty = true;
  const trailPaths = trail ? [...trail.querySelectorAll('path')] : [];
  const trailLine = trail?.querySelector('.trail-line'), trailProgress = trail?.querySelector('.trail-progress');
  const packets = trail ? [...trail.querySelectorAll('circle')] : [];
  if (trail) {
    trailMask = document.createElementNS('http://www.w3.org/2000/svg', 'mask');
    trailMask.id = 'journey-copy-clearance'; trailMask.setAttribute('maskUnits', 'userSpaceOnUse');
    trail.querySelector('defs').append(trailMask);
    trailPaths.forEach(p => p.setAttribute('mask', 'url(#journey-copy-clearance)'));
  }
  // The trail is the wire: one curve threading the stage art, drawn behind the copy
  // (masked out around each text block) with a progress stroke and a running packet.
  function makeTrail() {
    layoutDirty = false;
    if (!trail || !rows.length) return;
    const mainRect = main.getBoundingClientRect();
    const narrow = root.clientWidth <= 700;
    const lanes = rows.map(row => {
      const align = row.dataset.align;
      if (align === 'center') return narrow ? .5 : .42; // behind the terminal, not the message stack
      return align === 'right' ? (narrow ? .5 : .32) : (narrow ? .5 : .7);
    });
    const pts = rows.map((row, i) => {
      const art = row.querySelector('.scene-art').getBoundingClientRect();
      return { x: mainRect.width * lanes[i], y: art.top - mainRect.top + art.height * .55 };
    });
    pts.unshift({ x: mainRect.width * (narrow ? .5 : .78), y: -140 });
    // The wire lands inside the section: the last point sits above the bottom edge so
    // the packet can arrive and settle on screen instead of being clipped away.
    pts.push({ x: mainRect.width * (lanes[lanes.length - 1] + (narrow ? 0 : .10)), y: mainRect.height - (narrow ? 26 : 34) });
    let d = 'M ' + pts[0].x + ' ' + pts[0].y;
    for (let i = 1; i < pts.length; i++) {
      const a = pts[i - 1], b = pts[i], dy = b.y - a.y, bend = (i % 2 ? 1 : -1) * mainRect.width * (narrow ? .13 : .08);
      d += ' C ' + (a.x + bend) + ' ' + (a.y + dy * .54) + ', ' + (b.x - bend) + ' ' + (b.y - dy * .47) + ', ' + b.x + ' ' + b.y;
    }
    trailMask.innerHTML = '<rect x="0" y="0" width="' + mainRect.width + '" height="' + mainRect.height + '" fill="white"/>' + rows.map(row => {
      const r = row.querySelector('.stage-copy').getBoundingClientRect();
      return '<rect x="' + (r.left - mainRect.left - 16) + '" y="' + (r.top - mainRect.top - 16) + '" width="' + (r.width + 32) + '" height="' + (r.height + 32) + '" rx="20" fill="black"/>';
    }).join('');
    trailPaths.forEach(p => p.setAttribute('d', d));
    pathLength = trailLine.getTotalLength();
    trailProgress.style.strokeDasharray = pathLength;
  }
  function readScroll() {
    if (!trail || !pathLength) return;
    const r = root.getBoundingClientRect();
    const span = Math.max(1, root.offsetHeight - innerHeight * .35);
    const progress = clamp((innerHeight * .65 - r.top) / span);
    trailProgress.style.strokeDashoffset = pathLength * (1 - progress);
    const p = trailLine.getPointAtLength(pathLength * progress);
    for (const packet of packets) { packet.setAttribute('cx', p.x); packet.setAttribute('cy', p.y); }
    // Arrival: over the last stretch the packet settles and blooms, so the journey
    // reads as completed rather than running off the edge. Reverses on scroll up.
    const land = smooth((progress - .93) / .07);
    const halo = packets[0], dot = packets[1];
    if (halo) { halo.setAttribute('r', (12 + 13 * land).toFixed(2)); halo.setAttribute('opacity', (.45 + .35 * land).toFixed(3)); }
    if (dot) { dot.setAttribute('r', (3 + 2.2 * land).toFixed(2)); }
  }
  const on = (target, event, fn, options) => { target.addEventListener(event, fn, options); cleanups.push(() => target.removeEventListener(event, fn, options)); };

  function lightScene(scene) {
    scene.environment = envTarget.texture; scene.environmentIntensity = .72;
    scene.add(new T.HemisphereLight(0xaed8ff, 0x111827, .75));
    const key = new T.DirectionalLight(0xf0f6ff, 3.6); key.position.set(-3, 6, 5); key.castShadow = true; key.shadow.autoUpdate = false; key.shadow.needsUpdate = true; key.shadow.mapSize.set(512, 512);
    key.shadow.camera.left = -5; key.shadow.camera.right = 5; key.shadow.camera.top = 5; key.shadow.camera.bottom = -5; key.shadow.normalBias = .025; key.shadow.bias = -.0004; scene.add(key);
    const rim = new T.DirectionalLight(0x6ab7ff, 2.4); rim.position.set(4, 2, -4); scene.add(rim);
    const fill = new T.DirectionalLight(0xb5dfff, .55); fill.position.set(3, 1, 5); scene.add(fill);
  }
  let glowMap = null;
  function textureGlow() {
    const c = document.createElement('canvas'); c.width = c.height = 128;
    const x = c.getContext('2d'), g = x.createRadialGradient(64, 64, 0, 64, 64, 64);
    g.addColorStop(0, 'rgba(180,223,255,1)'); g.addColorStop(.12, 'rgba(100,180,255,.6)');
    g.addColorStop(.45, 'rgba(50,130,255,.12)'); g.addColorStop(1, 'rgba(30,100,255,0)');
    x.fillStyle = g; x.fillRect(0, 0, 128, 128);
    const t = new T.CanvasTexture(c); textures.add(t); return t;
  }
  function glow(parent, x, y, z, size, color = 0x6ab7ff, opacity = .6) {
    const o = new T.Sprite(new T.SpriteMaterial({ map: glowMap, color, transparent: true, opacity, depthWrite: false, blending: T.AdditiveBlending }));
    o.position.set(x, y, z); o.scale.setScalar(size); parent.add(o); return o;
  }
  function shadow(scene, y, size = 7) {
    const o = new T.Mesh(new T.PlaneGeometry(size, size), new T.ShadowMaterial({ opacity: .19 }));
    o.rotation.x = -Math.PI / 2; o.position.y = y; o.receiveShadow = true; scene.add(o);
  }
  function setupEnvironment() {
    const scene = new T.Scene(); scene.background = new T.Color(0x101928);
    for (const [p, s, intensity] of [[[0, 6, 0], [9, 1, 8], 3.1], [[-5, 2, 3], [1, 6, 5], 2.0], [[5, 1, -2], [1, 3, 5], 3.5]]) {
      const m = new T.Mesh(new T.BoxGeometry(...s), new T.MeshBasicMaterial({ color: 0xffffff })); m.material.color.multiplyScalar(intensity); m.position.set(...p); scene.add(m);
    }
    const pmrem = new T.PMREMGenerator(renderer); envTarget = pmrem.fromScene(scene, .035); pmrem.dispose();
    scene.traverse(o => { o.geometry?.dispose(); o.material?.dispose(); });
  }
  // Yields to the event loop between the heavy steps, so a tap that lands during the build
  // waits for one step (tens of ms) instead of the whole scene construction.
  const breathe = () => new Promise(resolve => { const s = globalThis.scheduler; if (s?.yield) s.yield().then(resolve, resolve); else setTimeout(resolve, 0); });
  async function setup() {
    try { renderer = new T.WebGLRenderer({ canvas: offscreen, alpha: true, antialias: true, preserveDrawingBuffer: true, powerPreference: 'high-performance' }); }
    catch (error) { fallback(); ready = true; return; }
    root.classList.add('journey-live'); // site: plates give way to the scenes
    renderer.setClearColor(0x000000, 0); renderer.outputColorSpace = T.SRGBColorSpace; renderer.toneMapping = T.ACESFilmicToneMapping; renderer.toneMappingExposure = 1.05;
    renderer.shadowMap.enabled = true; renderer.shadowMap.type = T.PCFSoftShadowMap;
    setupEnvironment();
    await breathe(); if (disposed) return;
    glowMap = textureGlow();
    const models = premium.buildPremium(T);
    for (const [index, row] of rows.entries()) {
      await breathe(); if (disposed) return;
      const def = STAGES[index]; if (!def) continue;
      const scene = new T.Scene(); lightScene(scene);
      const wrap = new T.Group(); scene.add(wrap);
      const camera = new T.OrthographicCamera(-3, 3, 3, -3, .1, 100);
      const built = def.build(T, models, terminal, { glow, shadow });
      batch.batchStaticChildren(T, built.model, { exclude: built.exclude || [] });
      wrap.add(built.model);
      if (built.shadowY !== undefined) shadow(scene, built.shadowY);
      camera.position.set(...built.position); camera.lookAt(...built.look);
      built.model.traverse(o => { if (o.isMesh && !o.material.transparent) { o.castShadow = true; o.receiveShadow = true; } });
      const canvas = row.querySelector('canvas');
      shots.push({ index, def, row, scene, wrap, model: built.model, card: built.card, camera, span: built.span, fit: built.fit, canvas, ctx: canvas.getContext('2d', { alpha: true }), view: row.querySelector('.scene-view'), width: 1, height: 1, dirty: true, lastPose: '' });
    }
    await breathe(); if (disposed) return;
    setupBridge();
    on(offscreen, 'webglcontextlost', e => { e.preventDefault(); contextLost = true; cancelAnimationFrame(raf); raf = 0; fallback(); });
    on(offscreen, 'webglcontextrestored', () => { contextLost = false; envTarget?.dispose(); setupEnvironment(); shots.forEach(s => { s.scene.environment = envTarget.texture; s.dirty = true; }); root.classList.remove('no-webgl'); root.classList.add('journey-live'); resize(); wake(); }); // site: class switch
    ready = true;
  }
  // The card lifts out of the hero dashboard's tile, flies into stage 01 and then on
  // into the terminal's slot in stage 02. It travels in its own fixed, full-viewport
  // canvas so it can cross the sections between them. A depth-only copy of the
  // terminal occludes it as it enters the slot.
  function setupBridge() {
    if (!shots.length) return;
    const scene = new T.Scene(); lightScene(scene);
    scene.traverse(o => { if (o.isLight) o.castShadow = false; });
    const lightGroup = new T.Group();
    [...scene.children].filter(o => o.isLight).forEach(light => lightGroup.add(light));
    scene.add(lightGroup);
    const card = shots[0].model.clone(true); card.name = 'travelling_card'; scene.add(card);
    let occluder = null;
    const second = shots[1];
    if (second) {
      const depthMaterial = new T.MeshBasicMaterial({ colorWrite: false, depthWrite: true });
      occluder = second.model.clone(true);
      occluder.getObjectByName('persistent_test_card')?.removeFromParent();
      occluder.traverse(o => { if (o.isMesh) { o.material = depthMaterial; o.castShadow = false; o.receiveShadow = false; o.renderOrder = -1; } });
      occluder.visible = false; scene.add(occluder);
    }
    const canvas = document.createElement('canvas'); canvas.style.cssText = 'display:block;width:100%;height:100%';
    const element = document.createElement('div');
    element.className = 'journey-card-bridge'; element.hidden = true; element.setAttribute('aria-hidden', 'true');
    element.style.cssText = 'position:fixed;left:0;top:0;width:100%;height:100%;z-index:60;pointer-events:none;contain:layout style';
    element.appendChild(canvas); document.body.appendChild(element);
    const lights = []; scene.traverse(o => { if (o.isLight) lights.push([o, o.intensity]); });
    bridge = { element, canvas, ctx: canvas.getContext('2d', { alpha: true }), scene, card, occluder, lights, lightGroup, camera: (second || shots[0]).camera.clone(), width: 1, height: 1, active: false, dirty: true };
    cleanups.push(() => element.remove());
  }
  function setHeroPhase(phase) {
    if (!heroEl || heroPhase === phase) return; heroPhase = phase;
    heroEl.dataset.cardPhase = phase;
    const face = heroEl.querySelector('.hero-card-face'), note = heroEl.querySelector('.hero-departed-note');
    if (face) { face.style.opacity = phase === 'hero' ? '1' : '0'; face.style.transition = 'opacity .25s'; }
    if (note) note.style.display = phase === 'hero' ? 'none' : 'block';
  }
  function setHandshake(shot, phase, seated) {
    const label = shot?.row.querySelector('.handshake'); if (!label) return;
    const text = seated ? 'EMV handshake' : phase === 'reading' ? 'Reading chip…' : 'Insert test card';
    const span = label.querySelector('span');
    if (span && span.textContent !== text) span.textContent = text;
    const mark = label.querySelector('b');
    if (mark) mark.style.visibility = seated ? 'visible' : 'hidden';
  }
  function setTerminal(shot, state) {
    if (!shot) return;
    setHandshake(shot, state, state === 'connected');
    if (shot.connection === state) return;
    shot.model.userData.setState(state); shot.connection = state; shot.dirty = true;
  }
  function setVisible(object, visible, shot) { if (object && object.visible !== visible) { object.visible = visible; shot.dirty = true; } }
  // Keep the lighting camera-relative as the card changes scene cameras; without
  // this the metallic card flashes white the moment it detaches.
  function bridgeLighting(blend, fromCamera) {
    const rotation = bridge.camera.quaternion.clone().multiply(fromCamera.quaternion.clone().invert()).slerp(new T.Quaternion(), blend);
    bridge.lightGroup.quaternion.copy(rotation);
    if (bridge.scene.environmentRotation) bridge.scene.environmentRotation.setFromQuaternion(rotation);
  }
  function bridgeLit(level) {
    for (const [light, base] of bridge.lights) light.intensity = base * level;
    bridge.scene.environmentIntensity = .72 * level;
  }
  function layoutBridge(camera, b, unit) {
    const cx = b.left + b.width / 2, cy = b.top + b.height / 2;
    bridge.camera.copy(camera);
    bridge.camera.left = -cx * unit; bridge.camera.right = (innerWidth - cx) * unit;
    bridge.camera.top = cy * unit; bridge.camera.bottom = -(innerHeight - cy) * unit;
    bridge.camera.updateProjectionMatrix();
    bridge.scene.updateMatrixWorld(true);
  }
  function cardTransfer() {
    if (!bridge) return;
    // Mobile browsers change the viewport without a resize event (address bar,
    // rotation), which would leave the bridge canvas the wrong size and the card
    // landing off its target. Keep it in step with the live viewport.
    const bw = Math.max(2, Math.round(innerWidth)), bh = Math.max(2, Math.round(innerHeight));
    if (bridge.width !== bw || bridge.height !== bh) {
      bridge.width = bw; bridge.height = bh; bridge.canvas.width = bw; bridge.canvas.height = bh;
      if (renderer) renderer.setSize(Math.max(...shots.map(sh => sh.width), bw), Math.max(...shots.map(sh => sh.height), bh), false);
      layoutDirty = true;
    }
    const first = shots[0], second = shots[1];
    const a = first.view.getBoundingClientRect();
    const dest = second || first, b = dest.view.getBoundingClientRect();
    if (still) {
      setHeroPhase('hero'); setVisible(first.model, true, first);
      if (second) { setVisible(second.card, true, second); setTerminal(second, 'connected'); }
      bridge.active = false; bridge.element.hidden = true; return;
    }
    const camera = dest.camera, unit = (camera.top - camera.bottom) / b.height;
    dest.model.updateWorldMatrix(true, true);
    // Where the card ends up: seated in the terminal's slot, or stage 01's own pose.
    const seatObject = second ? second.card : first.model;
    const target = seatObject.getWorldPosition(new T.Vector3());
    const targetRotation = seatObject.getWorldQuaternion(new T.Quaternion());
    const targetScale = seatObject.getWorldScale(new T.Vector3());
    const approach = second ? second.model.localToWorld(new T.Vector3(0, .065, 3.06)) : target.clone();
    const projected = target.clone().project(camera);
    const targetPixel = { x: b.left + (projected.x + 1) * b.width / 2, y: b.top + (1 - projected.y) * b.height / 2 };
    // Stage 01's card pose, expressed in the destination camera's space.
    const introProjected = first.model.getWorldPosition(new T.Vector3()).project(first.camera);
    const sourcePixel = { x: a.left + (introProjected.x + 1) * a.width / 2, y: a.top + (1 - introProjected.y) * a.height / 2 };
    const source = new T.Vector3((sourcePixel.x - b.left) / b.width * 2 - 1, 1 - (sourcePixel.y - b.top) / b.height * 2, projected.z).unproject(camera);
    const sourceRotation = camera.quaternion.clone().multiply(first.camera.quaternion.clone().invert()).multiply(first.model.getWorldQuaternion(new T.Quaternion()));
    const sourceUnit = (first.camera.top - first.camera.bottom) / a.height;
    const sourceScale = first.model.getWorldScale(new T.Vector3()).multiplyScalar(unit / sourceUnit);
    let heroTo = 0;
    if (heroEl) {
      const h = heroEl.getBoundingClientRect();
      const heroFrom = Math.max(0, h.top + h.height / 2 + scrollY - innerHeight * .62);
      heroTo = Math.max(heroFrom + 220, a.top + a.height / 2 + scrollY - innerHeight * .54);
      if (scrollY < heroTo) {
        const release = smooth((scrollY - heroFrom) / (heroTo - heroFrom));
        const phase = scrollY <= heroFrom ? 'hero' : 'release';
        setHeroPhase(phase);
        setVisible(first.model, false, first);
        if (second) { setVisible(second.card, false, second); setTerminal(second, 'ready'); }
        bridge.active = phase === 'release'; bridge.element.hidden = !bridge.active;
        if (bridge.occluder) bridge.occluder.visible = false;
        if (!bridge.active) return;
        const start = new T.Vector3((h.left + h.width / 2 - b.left) / b.width * 2 - 1, 1 - (h.top + h.height / 2 - b.top) / b.height * 2, projected.z).unproject(camera);
        const startScale = Math.max(.2, h.width * .9 * unit / .856);
        const startQ = camera.quaternion.clone();
        const ease = smooth(release), turn = smooth((release - .06) / .94);
        const lift = new T.Vector3().subVectors(source, start).length() * .09;
        const curve = new T.CubicBezierCurve3(start, start.clone().lerp(source, .26).add(new T.Vector3(lift * .5, lift, 0)), start.clone().lerp(source, .74).add(new T.Vector3(lift * .25, lift * .15, 0)), source);
        bridge.card.position.copy(curve.getPoint(ease));
        bridge.card.quaternion.copy(startQ).slerp(sourceRotation, turn);
        const s = startScale + (sourceScale.x - startScale) * ease;
        bridge.card.scale.setScalar(s);
        // In flight the card reads as a deeper layer: dim, soft, low contrast. It comes
        // forward and lights up over the last stretch, as it settles into stage 01.
        const arrive = smooth((release - .52) / .48);
        bridgeLighting(1, camera); bridgeLit(.26 + .74 * arrive);
        bridge.element.style.opacity = (.34 + .66 * arrive).toFixed(3);
        bridge.element.style.filter = arrive > .98 ? 'none' : 'blur(' + (2.2 * (1 - arrive)).toFixed(2) + 'px)';
        layoutBridge(camera, b, unit);
        return;
      }
      setHeroPhase('arrived');
    }
    if (!second) { setVisible(first.model, true, first); bridge.active = false; bridge.element.hidden = true; return; }
    const from = Math.max(a.top + a.height / 2 + scrollY - innerHeight * .52, heroTo ? heroTo + 100 : 0);
    // The card must be seated and connected by the time stage 02 is properly in
    // view, so the travel and insert legs are anchored to the slot entering the
    // viewport rather than reaching its centre — a faster hop between the stages.
    const alignAt = Math.max(from + 90, targetPixel.y + scrollY - innerHeight * 1.02);
    const seatAt = alignAt + Math.min(110, innerHeight * .14);
    const travel = smooth((scrollY - from) / (alignAt - from));
    const insertion = smooth((scrollY - alignAt) / (seatAt - alignAt));
    const phase = scrollY <= from ? 'intro' : scrollY < alignAt ? 'travel' : scrollY < seatAt ? 'insert' : 'connected';
    setVisible(first.model, phase === 'intro', first);
    setVisible(second.card, phase === 'connected', second);
    setTerminal(second, phase === 'connected' ? 'connected' : phase === 'insert' && insertion > .12 ? 'reading' : 'ready');
    bridge.active = phase === 'travel' || phase === 'insert';
    bridge.element.hidden = !bridge.active;
    if (!bridge.active) return;
    bridge.element.style.opacity = '1'; bridge.element.style.filter = 'none';
    bridgeLighting(smooth(travel / .88), first.camera); bridgeLit(1);
    const curve = new T.CubicBezierCurve3(source, source.clone().lerp(approach, .30), approach.clone().add(new T.Vector3(.45, .28, .40)), approach);
    bridge.card.position.copy(phase === 'insert' ? approach.clone().lerp(target, insertion) : curve.getPoint(travel));
    bridge.card.quaternion.copy(sourceRotation).slerp(targetRotation, smooth(travel / .88));
    bridge.card.scale.copy(sourceScale).lerp(targetScale, smooth(travel / .85));
    if (bridge.occluder) {
      bridge.occluder.position.copy(second.model.getWorldPosition(new T.Vector3()));
      bridge.occluder.quaternion.copy(second.model.getWorldQuaternion(new T.Quaternion()));
      bridge.occluder.scale.copy(second.model.getWorldScale(new T.Vector3()));
      bridge.occluder.visible = travel > .70;
    }
    layoutBridge(camera, b, unit);
  }
  function fallback() { root.classList.add('no-webgl'); root.classList.remove('journey-live'); } // site: class switch
  function resize() {
    if (disposed || !shots.length) return;
    const ratio = Math.min(devicePixelRatio || 1, root.clientWidth <= 700 ? 1.25 : 1.5) * renderScale;
    for (const shot of shots) {
      const r = shot.view.getBoundingClientRect();
      shot.width = Math.max(2, Math.round(r.width * ratio)); shot.height = Math.max(2, Math.round(r.height * ratio));
      shot.canvas.width = shot.width; shot.canvas.height = shot.height; shot.dirty = true;
      const aspect = r.width / Math.max(1, r.height), span = Math.max(shot.span, shot.fit / aspect);
      shot.camera.left = -span * aspect / 2; shot.camera.right = span * aspect / 2; shot.camera.top = span / 2; shot.camera.bottom = -span / 2; shot.camera.updateProjectionMatrix();
    }
    if (bridge) { bridge.width = Math.max(2, Math.round(innerWidth)); bridge.height = Math.max(2, Math.round(innerHeight)); bridge.canvas.width = bridge.width; bridge.canvas.height = bridge.height; }
    if (renderer) { renderer.setSize(Math.max(...shots.map(s => s.width), bridge?.width || 1), Math.max(...shots.map(s => s.height), bridge?.height || 1), false); renderer.setScissorTest(true); }
    lastWidth = root.clientWidth; makeTrail(); render(true);
  }
  function stageProgress(shot) { const r = shot.row.getBoundingClientRect(); return smooth((innerHeight * .76 - r.top) / (innerHeight * .42 + r.height * .38)); }
  function pose(shot, t) {
    shot.wrap.position.set(0, 0, 0); shot.wrap.rotation.set(0, 0, 0);
    const key = shot.def.pose(shot, t, stageProgress(shot), still) || 'static';
    if (shot.lastPose !== key) { shot.dirty = true; shot.lastPose = key; }
    shot.scene.updateMatrixWorld(true);
  }
  function drawShot(shot) {
    renderer.setViewport(0, 0, shot.width, shot.height); renderer.setScissor(0, 0, shot.width, shot.height); renderer.render(shot.scene, shot.camera);
    shot.ctx.clearRect(0, 0, shot.width, shot.height);
    shot.ctx.drawImage(offscreen, 0, offscreen.height - shot.height, shot.width, shot.height, 0, 0, shot.width, shot.height);
    drawCount++; shot.dirty = false;
  }
  function render(all = false, forcedTime) {
    if (layoutDirty) makeTrail();
    readScroll();
    if (disposed || !ready || contextLost || !renderer) return;
    const t = forcedTime === undefined ? clock : forcedTime;
    for (const shot of shots) pose(shot, t);
    cardTransfer();
    for (const shot of shots) {
      const r = shot.view.getBoundingClientRect();
      if (!all && (r.bottom < 0 || r.top > innerHeight || !shot.dirty)) continue;
      if (shot.index === 0 && !shot.model.visible) { shot.ctx.clearRect(0, 0, shot.width, shot.height); shot.dirty = false; continue; }
      drawShot(shot);
    }
    if (bridge?.active) drawShot(bridge);
  }
  function tick(now) {
    raf = 0; if (disposed || !visible || contextLost) return;
    if (now - lastTime >= 1000 / 24) {
      const dt = Math.min(.1, (now - lastTime) / 1000); if (!paused) clock += dt;
      const before = performance.now(), count = drawCount; render(false); const cost = performance.now() - before;
      if (drawCount > count && (cost > 90 || now - lastTime > 120)) slowFrames++; else slowFrames = Math.max(0, slowFrames - 1);
      if (slowFrames >= 10 && renderScale > .52) { renderScale = Math.max(.5, renderScale * .8); slowFrames = 0; resize(); }
      lastTime = now;
    }
    if (!paused && !scrolledPast()) raf = requestAnimationFrame(tick); // site: idle once the section is behind the reader
  }
  function scrolledPast() { return root.getBoundingClientRect().bottom < -240; }
  function wake() { if (!raf && !disposed && visible && !paused) raf = requestAnimationFrame(tick); }
  function syncPaused() { root.classList.toggle('motion-paused', paused); if (raf) cancelAnimationFrame(raf); raf = 0; render(true); wake(); }

  on(media, 'change', () => { paused = media.matches; still = media.matches; syncPaused(); });
  on(window, 'scroll', () => { if (paused) render(false); else wake(); }, { passive: true });
  on(window, 'resize', () => { layoutDirty = true; resize(); }, { passive: true });
  on(window, 'orientationchange', () => { layoutDirty = true; resize(); }, { passive: true });
  if (window.visualViewport) on(window.visualViewport, 'resize', () => { layoutDirty = true; resize(); }, { passive: true });
  on(document, 'visibilitychange', () => { visible = !document.hidden; if (!visible && raf) { cancelAnimationFrame(raf); raf = 0; } else wake(); });
  const observer = new ResizeObserver(() => { if (root.clientWidth !== lastWidth) resize(); });
  observer.observe(root); cleanups.push(() => observer.disconnect());

  setup().then(() => { if (!disposed) { resize(); syncPaused(); } });
  document.fonts?.ready.then(() => { if (!disposed) resize(); });

  return {
    get ready() { return ready; }, get paused() { return paused; }, get drawCount() { return drawCount; },
    renderNow(time) { render(true, time); },
    destroy() {
      if (disposed) return; disposed = true; cancelAnimationFrame(raf); cleanups.forEach(f => f());
      // site: hand the section back as the prerendered page had it.
      root.classList.remove('journey-live', 'motion-paused');
      root.querySelectorAll('.message-stack, .verified, .response-panel').forEach(e => { e.style.opacity = ''; });
      if (heroEl) { const face = heroEl.querySelector('.hero-card-face'), note = heroEl.querySelector('.hero-departed-note'); if (face) face.style.opacity = ''; if (note) note.style.display = ''; delete heroEl.dataset.cardPhase; }
      [...shots, ...(bridge ? [bridge] : [])].forEach(s => s.scene.traverse(o => {
        if (o.geometry) geometries.add(o.geometry);
        (Array.isArray(o.material) ? o.material : [o.material]).filter(Boolean).forEach(m => { materials.add(m); Object.values(m).forEach(v => { if (v?.isTexture) textures.add(v); }); });
        if (o.isLight) o.shadow?.map?.dispose();
      }));
      geometries.forEach(g => g.dispose()); materials.forEach(m => m.dispose()); textures.forEach(t => t.dispose());
      envTarget?.dispose(); renderer?.dispose(); renderer?.forceContextLoss(); offscreen.remove();
    }
  };
}

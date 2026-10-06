/**
 * The WebGL half of the hero's prism beam: a light beam striking the top edge
 * of the framed dashboard. Ported from the design system's PrismBeam.jsx; the
 * shaders are unchanged.
 *
 * Imported only through the dynamic import in prism-beam.ts, so none of this
 * is in the initial bundle and none of it runs during prerender.
 */

const PB_VERT = `attribute vec2 aPos;void main(){gl_Position=vec4(aPos,0.0,1.0);}`;

const PB_FRAG = `
precision highp float;
uniform vec2  uRes;
uniform float uTime;
uniform float uBeamX;
uniform float uEdgeY;
uniform float uCardL;
uniform float uCardR;
uniform float uCardB;
uniform float uCorner;
uniform float uIntensity;
uniform float uSpread;
uniform float uEdgeLight;
uniform float uBeamLen;
uniform float uIntro;
uniform float uAsym;
uniform float uDispersion;
uniform float uSmoke;
uniform float uSpeed;
uniform float uFlow;
uniform float uFlowSpeed;
uniform float uPulse;
uniform float uPulseRate;
uniform float uBalance;
uniform float uSpill;
uniform float uReach;
uniform vec3  uCoreCol;
uniform vec3  uGlowCol;
uniform vec3  uDeepCol;
uniform vec3  uWarmCol;
uniform vec3  uMidCol;
uniform vec3  uBgCol;

vec2 hash2(vec2 p){
  p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
  return fract(sin(p) * 43758.5453) * 2.0 - 1.0;
}
float gnoise(vec2 p){
  vec2 i = floor(p), f = fract(p);
  vec2 u = f * f * (3.0 - 2.0 * f);
  return mix(mix(dot(hash2(i + vec2(0.0,0.0)), f - vec2(0.0,0.0)),
                 dot(hash2(i + vec2(1.0,0.0)), f - vec2(1.0,0.0)), u.x),
             mix(dot(hash2(i + vec2(0.0,1.0)), f - vec2(0.0,1.0)),
                 dot(hash2(i + vec2(1.0,1.0)), f - vec2(1.0,1.0)), u.x), u.y);
}
float fbm(vec2 p){
  float v = 0.0, a = 0.55;
  for (int i = 0; i < 4; i++){
    v += a * gnoise(p);
    p  = mat2(1.7, 1.1, -1.1, 1.7) * p;
    a *= 0.52;
  }
  return v;
}

/* signed distance to the card's rounded rectangle, in width-fraction units.
   Negative inside. Lets the edge glow wrap the real corner. */
float sdRoundRect(vec2 p, vec2 b, float r){
  r = min(r, min(b.x, b.y));
  vec2 q = abs(p) - b + r;
  return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

/* how much light passes through the beam at height y at time t */
float flowProfile(float y, float t){
  float f1 = gnoise(vec2(3.1, y * 13.0 - t * 1.10));
  float f2 = gnoise(vec2(9.7, y * 31.0 - t * 2.05));
  float v  = 1.0 + uFlow * (0.60 * f1 + 0.30 * f2);
  float head = fract(t * uPulseRate);
  float py   = mix(-0.46, 0.0, head);
  float dy   = (y - py) / 0.040;
  float ty   = (y - py) / 0.150;
  float trav = exp(-dy * dy) + 0.45 * exp(-ty * ty) * step(y, py);
  v += uPulse * 2.4 * trav * (1.0 - smoothstep(-0.02, 0.012, y));
  return max(0.18, v);
}

/* the vertical beam: razor thin, saturated, only above the edge */
float easeOut(float x){ return 1.0 - pow(1.0 - clamp(x, 0.0, 1.0), 3.0); }

float beamCore(vec2 q, float t){
  float shimmer = 1.0 + uFlow * 0.30 * gnoise(vec2(1.7, q.y * 7.0 - t * 0.75));
  float bw   = 0.0018 * shimmer
             * (1.0 + 7.5 * pow(smoothstep(-0.155, 0.006, q.y), 2.2));
  float bxn  = q.x / bw;
  float beam = exp(-bxn * bxn);
  /* the beam has a finite length above the edge, fading out at its top */
  float hb = max(-q.y, 0.0);
  beam *= 1.0 - smoothstep(uBeamLen * 0.40, uBeamLen, hb);
  beam *= mix(0.45, 1.0, smoothstep(uBeamLen, uBeamLen * 0.25, hb));
  beam *= 1.0 - smoothstep(-0.004, 0.030, q.y);
  /* start animation: the leading edge travels down and strikes the card */
  float front = mix(-uBeamLen * 1.20, 0.008,
                    easeOut(clamp(uIntro / 0.62, 0.0, 1.0)));
  beam *= 1.0 - smoothstep(front, front + 0.018, q.y);
  return beam * flowProfile(q.y, t);
}

void lightField(vec2 q, float sh, float ar, vec2 uv,
                out float flare, out float halo,
                out float spillR, out float spillL,
                out float flareT, out float spillTR, out float spillTL,
                out float sheath, out float spillP)
{
  float X = q.x - sh;
  float Y = q.y;

  /* anamorphic flare sheet riding the top edge — wider to the left */
  float fw  = 0.135 * uSpread * (X < 0.0 ? uAsym : 1.0);
  float fxn = abs(X) / fw;
  float fy1 = Y / 0.0115;
  float fy2 = Y / (0.0115 + 0.022 * fxn);
  float sheet = exp(-pow(fxn, 2.3)) * exp(-fy1 * fy1);
  sheet += 0.20 * exp(-fxn * 1.35) * exp(-fy2 * fy2);
  sheet *= 1.0 - smoothstep(0.003, min(0.030 + 0.10 * fxn, 0.055), Y);
  flareT = clamp(fxn, 0.0, 1.8);

  /* compact bloom + faint atmospheric wash around the contact point */
  float r = length(vec2(X, Y));
  halo = 0.40 / (1.0 + pow(r / 0.026, 2.15)) + 0.030 * exp(-r / 0.060);

  /* bluish sheath hugging the beam on its way up, bounded by the beam's
     own length so it never washes over what sits above the hero image */
  sheath = exp(-pow(abs(X) / 0.021, 1.30))
         * (1.0 - smoothstep(-0.005, 0.030, Y))
         * (1.0 - smoothstep(uBeamLen * 0.45, uBeamLen * 1.30, max(-Y, 0.0)));

  flare = sheet;

  /* light escaping past the card and wrapping its top corners */
  float Yc = (uv.y - uEdgeY) * ar;
  vec2  cmin  = vec2(uCardL, uEdgeY * ar);
  vec2  cmax  = vec2(uCardR, uCardB  * ar);
  vec2  ccen  = (cmin + cmax) * 0.5;
  vec2  chalf = max((cmax - cmin) * 0.5, vec2(0.001));
  vec2  pp    = vec2(uv.x - sh, uv.y * ar) - ccen;
  float d = sdRoundRect(pp, chalf, uCorner);

  float visB  = min(uCardB, 1.0);
  float cardH = max((visB - uEdgeY) * ar, 0.001);
  float wIn   = 0.008 + 0.010 * max(Yc, 0.0);
  float wOut  = 0.011 + 0.070 * max(Yc, 0.0);
  float env   = 1.0 - smoothstep(0.15 * uReach, 0.95 * uReach,
                                 max(Yc, 0.0) / cardH);
  /* the outward offset belongs to the sides, where light pools against a
     vertical edge; along the straight top run it goes to zero, or the band
     and the edge layer read as a pair of parallel lines */
  float inX      = min(uv.x - uCardL, uCardR - uv.x);
  float sideness = 1.0 - smoothstep(uCorner * 0.8, uCorner * 2.6, max(inX, 0.0));
  float dp   = max(d, 0.0) - 0.0089 * sideness;
  float dd   = dp / (dp < 0.0 ? wIn : wOut);
  float wrap = exp(-dd * dd) * env;

  float tw = clamp(max(d, 0.0) / wOut, 0.0, 1.4);
  spillP = clamp(max(d, 0.0) / (0.010 + 0.120 * max(Yc, 0.0)), 0.0, 1.4);

  float side = step(ccen.x, uv.x);
  spillR  = wrap * side;
  spillTR = tw;
  spillL  = wrap * (1.0 - side) * uBalance;
  spillTL = tw;
}

/* white -> periwinkle -> deep blue ramp */
vec3 ramp(float t){
  vec3 c = mix(uCoreCol, uGlowCol, smoothstep(0.10, 0.72, t));
  return mix(c, uDeepCol, smoothstep(0.68, 1.45, t));
}

void main(){
  vec2 uv = gl_FragCoord.xy / uRes;
  uv.y = 1.0 - uv.y;
  float ar = uRes.y / uRes.x;
  vec2 q = vec2(uv.x - uBeamX, (uv.y - uEdgeY) * ar);
  float o = 0.0085 * uDispersion;

  float tf   = uTime * uFlowSpeed;
  float core = beamCore(q, tf);

  /* the flare, spill and surface light only exist once the beam lands,
     with a brief overshoot at the moment of impact */
  float dImp  = (uIntro - 0.62) / 0.075;
  float land  = smoothstep(0.58, 1.00, uIntro);
  float flash = exp(-dImp * dImp) * step(0.02, uIntro);

  float headG  = fract(tf * uPulseRate);
  float dHead  = headG - 1.0;
  float arrive = exp(-dHead * dHead / 0.004) + exp(-headG * headG / 0.004);
  float flareGain = 1.0
      + uFlow  * 0.16 * gnoise(vec2(3.1, -tf * 1.10))
      + uPulse * 0.50 * arrive;

  float fR,hR,srR,slR,ftR,trR,tlR,shR;
  float fG,hG,srG,slG,ftG,trG,tlG,shG;
  float fB,hB,srB,slB,ftB,trB,tlB,shB;
  float pR_, pG_, pB_;
  lightField(q,  o,  ar, uv, fR,hR,srR,slR,ftR,trR,tlR,shR,pR_);
  lightField(q,  0.0,ar, uv, fG,hG,srG,slG,ftG,trG,tlG,shG,pG_);
  lightField(q, -o,  ar, uv, fB,hB,srB,slB,ftB,trB,tlB,shB,pB_);

  /* drifting smoke, only visible where the light reaches */
  float t  = uTime * uSpeed;
  vec2  np = vec2(q.x * 2.6, q.y * 2.0);
  float n1 = fbm(np + vec2(t * 0.045, -t * 0.085));
  float n2 = fbm(np * 2.4 + vec2(-t * 0.07, t * 0.035) + 17.0);
  float cloud = 0.5 + 0.9 * n1 + 0.4 * n2;

  float h    = max(-q.y, 0.0);
  float hs   = max(uBeamLen, 0.10) * 1.35;
  float up   = smoothstep(0.0, 0.035, h) * exp(-pow(h / hs, 1.55));
  float lat  = exp(-pow(abs(q.x) / 0.42, 2.0));
  float dark = fbm(np * 0.62 + vec2(t * 0.028, -t * 0.018) + 31.0);
  float shade = 1.0 - 0.72 * smoothstep(-0.02, 0.36, dark);
  float neb  = uSmoke * up * lat * max(cloud, 0.0) * shade;
  float wisp = uSmoke * up * lat * smoothstep(0.62, 1.25, cloud) * shade;
  float mott = 1.0 + uSmoke * 0.85 * (n1 * 0.9 + n2 * 0.5);

  vec3 lin = vec3(0.0);
  lin += uCoreCol * core * 3.4;

  vec3 fl = vec3(fR, fG, fB) * mott * flareGain;
  lin += ramp(ftG) * fl * 2.7 * (land + flash * 0.85);

  vec3 ha = vec3(hR, hG, hB) * mott * mix(1.0, flareGain, 0.55);
  lin += ramp(clamp(length(q) / 0.16, 0.0, 1.3)) * ha * 2.2 * (land + flash * 0.7);

  /* a thin light layer lying along the whole top edge */
  float ey   = q.y / 0.0090;
  float edge = exp(-ey * ey) * (1.0 - smoothstep(0.003, 0.024, q.y));
  float hx   = (uv.x - uCardL) / max(uCardR - uCardL, 0.001);
  edge *= smoothstep(-0.010, 0.055, hx) * smoothstep(1.010, 0.945, hx);
  edge *= uEdgeLight * mott;
  float warmish = exp(-abs(q.x) / 0.18);
  lin += mix(uGlowCol, uCoreCol, warmish) * edge * 0.55 * land;

  float fl2 = 1.0 + 1.9 * (flowProfile(q.y, tf) - 1.0);
  vec3 shv = vec3(shR, shG, shB) * mott * max(fl2, 0.0);
  lin += ramp(clamp(0.82 + abs(q.x) / 0.13, 0.0, 1.3))
       * vec3(0.72, 0.84, 1.40) * shv * 0.30 * land;

  vec3 spR = vec3(srR, srG, srB);
  vec3 spL = vec3(slR, slG, slB);
  float dmix = smoothstep(0.30, 1.05, uDispersion);

  vec3 pR = mix(uWarmCol, uMidCol, smoothstep(0.04, 0.20, pG_));
  pR = mix(pR, ramp(pG_ * 1.35), smoothstep(0.18, 0.50, pG_));
  vec3 coolR = ramp(clamp(0.80 + trG * 0.25, 0.0, 1.3)) * vec3(0.70, 0.66, 1.26);
  vec3 cR2 = mix(coolR, pR, dmix);

  vec3 pL = mix(uWarmCol, uMidCol, smoothstep(0.04, 0.20, pG_));
  pL = mix(pL, ramp(pG_ * 1.35), smoothstep(0.18, 0.50, pG_));
  vec3 coolL = ramp(clamp(0.80 + tlG * 0.25, 0.0, 1.3)) * vec3(0.70, 0.66, 1.26);
  vec3 cL2 = mix(coolL, pL, dmix);
  lin += cR2 * spR * 4.4 * uSpill * land;
  lin += cL2 * spL * 4.4 * uSpill * land;

  vec3 hazeCol = vec3(0.58, 0.66, 0.86);
  vec3 nebCol  = uDeepCol * vec3(0.42, 0.50, 1.35);
  float smokeIn = smoothstep(0.04, 0.55, uIntro);
  lin += hazeCol * neb  * 0.48 * smokeIn;
  lin += nebCol  * wisp * 3.6 * smokeIn;

  lin *= uIntensity;

  vec3 col = vec3(1.0) - exp(-lin);
  col = max(col, uBgCol);

  float dz = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
  col += (dz - 0.5) * 2.2 / 255.0;
  gl_FragColor = vec4(col, 1.0);
}
`;

/** The look the home hero uses (PrismBeam defaults, with the hero's overrides). */
const LOOK = {
  radius: 12, beamX: 0.5, intensity: 1, spread: 0.85, edgeLight: 0, asym: 1,
  dispersion: 0.35, smoke: 0.55, speed: 1, flow: 0.7, flowSpeed: 1,
  pulse: 0.35, pulseRate: 0.45, balance: 1, spill: 0.1, reach: 0.45,
  beamLength: 10, introDuration: 1.6,
};

/** Tokens the beam is coloured from, resolved against the host element. */
const COLOURS = {
  core: '--art-beam-core', glow: '--blue-hi', deep: '--blue-deep', warm: '--amber-hi', mid: '--teal-hi',
} as const;

/** Medium quality: the canvas is never denser than this many device pixels per CSS pixel. */
const DPR_CAP = 1.5;

type Rgb = [number, number, number];

function toRgb(input: string): Rgb {
  let s = input.trim();
  const fn = s.match(/rgba?\(([^)]+)\)/i);
  if (fn) {
    const p = fn[1].split(',').map((v) => parseFloat(v));
    return [(p[0] || 0) / 255, (p[1] || 0) / 255, (p[2] || 0) / 255];
  }
  s = s.replace('#', '');
  if (s.length === 3) s = s[0] + s[0] + s[1] + s[1] + s[2] + s[2];
  if (s.length === 8) s = s.slice(0, 6);
  const n = parseInt(s, 16);
  if (Number.isNaN(n)) return [1, 1, 1];
  return [((n >> 16) & 255) / 255, ((n >> 8) & 255) / 255, (n & 255) / 255];
}

function compile(gl: WebGLRenderingContext, type: number, source: string): WebGLShader {
  const shader = gl.createShader(type)!;
  gl.shaderSource(shader, source);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) console.warn('[PrismBeam]', gl.getShaderInfoLog(shader));
  return shader;
}

export interface BeamHandle { destroy(): void; }

/**
 * Starts the beam on `canvas`, which fills `host`. `frame` is the product
 * frame the beam lands on: the shader is told where its edges are so the
 * light wraps its corners. `delay` (ms) holds the strike back so it lands as
 * the frame finishes rising.
 *
 * Returns null when no WebGL context is available. The loop runs only while
 * the host is on screen and the tab is visible.
 */
export function startBeam(
  host: HTMLElement, canvas: HTMLCanvasElement, frame: HTMLElement, delay: number,
): BeamHandle | null {
  const gl = canvas.getContext('webgl', {
    antialias: false, alpha: false, premultipliedAlpha: false, powerPreference: 'high-performance',
  });
  if (!gl) return null;

  const program = gl.createProgram()!;
  const vert = compile(gl, gl.VERTEX_SHADER, PB_VERT);
  const frag = compile(gl, gl.FRAGMENT_SHADER, PB_FRAG);
  gl.attachShader(program, vert);
  gl.attachShader(program, frag);
  gl.linkProgram(program);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    console.warn('[PrismBeam]', gl.getProgramInfoLog(program));
    return null;
  }
  gl.useProgram(program);

  const buffer = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
  const aPos = gl.getAttribLocation(program, 'aPos');
  gl.enableVertexAttribArray(aPos);
  gl.vertexAttribPointer(aPos, 2, gl.FLOAT, false, 0, 0);

  const U = (name: string) => gl.getUniformLocation(program, name);
  const loc = {
    res: U('uRes'), time: U('uTime'), beamX: U('uBeamX'), edgeY: U('uEdgeY'),
    cardL: U('uCardL'), cardR: U('uCardR'), cardB: U('uCardB'), corner: U('uCorner'),
    intensity: U('uIntensity'), spread: U('uSpread'), edgeLight: U('uEdgeLight'), asym: U('uAsym'),
    beamLen: U('uBeamLen'), intro: U('uIntro'),
    dispersion: U('uDispersion'), smoke: U('uSmoke'), speed: U('uSpeed'), flow: U('uFlow'),
    flowSpeed: U('uFlowSpeed'), pulse: U('uPulse'), pulseRate: U('uPulseRate'), balance: U('uBalance'),
    spill: U('uSpill'), reach: U('uReach'), core: U('uCoreCol'), glow: U('uGlowCol'),
    deep: U('uDeepCol'), warm: U('uWarmCol'), mid: U('uMidCol'), bg: U('uBgCol'),
  };

  // Everything that does not change from frame to frame is set once.
  const style = getComputedStyle(host);
  const colour = (token: string) => toRgb(style.getPropertyValue(token) || '');
  gl.uniform1f(loc.beamX, LOOK.beamX);
  gl.uniform1f(loc.intensity, LOOK.intensity);
  gl.uniform1f(loc.spread, LOOK.spread);
  gl.uniform1f(loc.edgeLight, LOOK.edgeLight);
  gl.uniform1f(loc.asym, LOOK.asym);
  gl.uniform1f(loc.beamLen, LOOK.beamLength / 100);
  gl.uniform1f(loc.dispersion, LOOK.dispersion);
  gl.uniform1f(loc.smoke, LOOK.smoke);
  gl.uniform1f(loc.speed, LOOK.speed);
  gl.uniform1f(loc.flow, LOOK.flow);
  gl.uniform1f(loc.flowSpeed, LOOK.flowSpeed);
  gl.uniform1f(loc.pulse, LOOK.pulse);
  gl.uniform1f(loc.pulseRate, LOOK.pulseRate);
  gl.uniform1f(loc.balance, LOOK.balance);
  gl.uniform1f(loc.spill, LOOK.spill);
  gl.uniform1f(loc.reach, LOOK.reach);
  gl.uniform3fv(loc.core, colour(COLOURS.core));
  gl.uniform3fv(loc.glow, colour(COLOURS.glow));
  gl.uniform3fv(loc.deep, colour(COLOURS.deep));
  gl.uniform3fv(loc.warm, colour(COLOURS.warm));
  gl.uniform3fv(loc.mid, colour(COLOURS.mid));
  // Black background: the canvas screen-blends, so black is where nothing is added.
  gl.uniform3f(loc.bg, 0, 0, 0);

  let raf = 0;
  let disposed = false;
  let started = false;
  let onScreen = true;
  let lost = false;
  let t0 = 0;
  let layoutStale = true;

  /* Where the frame sits inside the canvas. Read from layout offsets rather
     than getBoundingClientRect so the frame's own rise animation (a transform)
     does not drag the light around with it; both elements share an offset
     parent. */
  const layout = () => {
    layoutStale = false;
    const W = Math.max(host.clientWidth, 1);
    const H = Math.max(host.clientHeight, 1);
    const dpr = Math.min(window.devicePixelRatio || 1, DPR_CAP);
    const pw = Math.max(1, Math.round(W * dpr));
    const ph = Math.max(1, Math.round(H * dpr));
    if (canvas.width !== pw || canvas.height !== ph) { canvas.width = pw; canvas.height = ph; }

    const cardW = Math.min(frame.offsetWidth, W);
    const cardL = (W - cardW) / 2;
    const cardT = frame.offsetTop - host.offsetTop;

    gl.viewport(0, 0, pw, ph);
    gl.uniform2f(loc.res, pw, ph);
    gl.uniform1f(loc.edgeY, cardT / H);
    gl.uniform1f(loc.cardL, cardL / W);
    gl.uniform1f(loc.cardR, (cardL + cardW) / W);
    gl.uniform1f(loc.cardB, (cardT + frame.offsetHeight) / H);
    gl.uniform1f(loc.corner, LOOK.radius / W);
  };

  // The intro clock starts on the first rendered frame: shader compilation can
  // land well after mount, and anchoring to mount time would eat the strike.
  const loop = (now: number) => {
    raf = 0;
    if (disposed || lost || !onScreen || document.hidden) return;
    if (!started) { started = true; t0 = now; }
    if (layoutStale) layout();
    const ms = LOOK.introDuration * 1000;
    const intro = Math.min(Math.max(now - t0 - delay, 0) / ms, 1);
    gl.uniform1f(loc.time, (now - t0) / 1000);
    gl.uniform1f(loc.intro, intro);
    gl.drawArrays(gl.TRIANGLES, 0, 3);
    raf = requestAnimationFrame(loop);
  };
  const wake = () => { if (!raf && !disposed && !lost && onScreen && !document.hidden) raf = requestAnimationFrame(loop); };

  const resized = new ResizeObserver(() => { layoutStale = true; });
  resized.observe(host);
  resized.observe(frame);

  const seen = new IntersectionObserver(([entry]) => { onScreen = entry.isIntersecting; wake(); });
  seen.observe(host);

  const onVisibility = () => wake();
  const onLost = (e: Event) => { e.preventDefault(); lost = true; cancelAnimationFrame(raf); raf = 0; };
  document.addEventListener('visibilitychange', onVisibility);
  canvas.addEventListener('webglcontextlost', onLost);

  wake();

  return {
    destroy() {
      if (disposed) return;
      disposed = true;
      cancelAnimationFrame(raf);
      resized.disconnect();
      seen.disconnect();
      document.removeEventListener('visibilitychange', onVisibility);
      canvas.removeEventListener('webglcontextlost', onLost);
      gl.deleteBuffer(buffer);
      gl.deleteProgram(program);
      gl.deleteShader(vert);
      gl.deleteShader(frag);
      gl.getExtension('WEBGL_lose_context')?.loseContext();
    },
  };
}

// SkyLook compass / radar visualization + shared utilities

// ── CSS animations ────────────────────────────────────────────
if (typeof document !== 'undefined' && !document.getElementById('sky-anims')) {
  const s = document.createElement('style');
  s.id = 'sky-anims';
  s.textContent = `
    @keyframes skyPlanePulse {
      0%, 100% { opacity: 0.7; transform: translate(-50%,-50%) scale(1); }
      50%       { opacity: 1;   transform: translate(-50%,-50%) scale(1.18); }
    }
    @keyframes skyPlaneGlow {
      0%, 100% { filter: drop-shadow(0 0 4px rgba(91,179,255,0.6)); }
      50%       { filter: drop-shadow(0 0 10px rgba(91,179,255,1)); }
    }
    @keyframes skyTrackedRing {
      0%   { transform: translate(-50%,-50%) scale(1);   opacity: 0.7; }
      100% { transform: translate(-50%,-50%) scale(2.6); opacity: 0; }
    }
    @keyframes skyBeamPulse {
      0%, 100% { opacity: 0.8; }
      50%       { opacity: 1; }
    }
    @keyframes skyInterestingBadge {
      0%, 100% { box-shadow: 0 0 8px rgba(155,125,255,0.4); }
      50%       { box-shadow: 0 0 18px rgba(155,125,255,0.9); }
    }
    @keyframes skyMilitaryBadge {
      0%, 100% { box-shadow: 0 0 8px rgba(245,168,58,0.4); }
      50%       { box-shadow: 0 0 18px rgba(245,168,58,0.9); }
    }
  `;
  document.head.appendChild(s);
}

const TAU = Math.PI * 2;

// ── Shared utilities ─────────────────────────────────────────

// Human-friendly "where to look" from absolute bearing + altitude.
// Used on both phone and TV surfaces.
function humanDirAbsolute(bearing, altitudeFt) {
  const b = ((bearing % 360) + 360) % 360;
  const compass8 = ['North','North-East','East','South-East','South','South-West','West','North-West'];
  const dir = compass8[Math.round(b / 45) % 8];
  if (altitudeFt > 38000) return `High in the ${dir}ern sky`;
  if (altitudeFt > 25000) return `Look ${dir}`;
  if (altitudeFt > 12000) return `${dir} — mid sky`;
  return `Low in the ${dir}ern sky`;
}

// Convert absolute bearing to position around the ring
function ringPos(absoluteBearingDeg, heading, radius, cx, cy) {
  const relDeg = ((absoluteBearingDeg - heading + 540) % 360) - 180; // -180..180
  const a = (relDeg - 90) * Math.PI / 180;
  return { x: cx + Math.cos(a) * radius, y: cy + Math.sin(a) * radius, relDeg };
}

// Blue front / violet sides / red behind
function colorForRel(relDeg) {
  const a = Math.abs(relDeg);
  if (a < 60)  return '#5BB3FF';
  if (a < 130) return '#9B7DFF';
  return '#FF5B7A';
}

// ── Aircraft silhouettes ─────────────────────────────────────
const PLANE_PATHS = {
  narrowbody: 'M12 2 L13.2 9.5 L22 12.5 L22 14 L13.2 13 L12.6 19 L15 20.5 L15 22 L12 21 L9 22 L9 20.5 L11.4 19 L12 13 L2 14 L2 12.5 L11 9.5 Z',
  widebody:   'M12 2 L14.5 9 L23 13 L23 15 L14.5 14 L14 20 L16.5 21.5 L16.5 23 L12 21.5 L7.5 23 L7.5 21.5 L10 20 L9.5 14 L1 15 L1 13 L9.5 9 Z',
  regional:   'M12 3 L13 9.5 L20 12.5 L20 14 L13 12.5 L12.5 19 L14.5 20.5 L14.5 22 L12 21 L9.5 22 L9.5 20.5 L11.5 19 L11 12.5 L4 14 L4 12.5 L11 9.5 Z',
  private:    'M12 2 L13 10 L22 16 L21.5 17.5 L13 14 L12.5 20 L15 21.5 L15 23 L12 22 L9 23 L9 21.5 L11.5 20 L11 14 L2.5 17.5 L2 16 L11 10 Z',
  helicopter: 'M10.5 4 L13.5 4 L13.5 10.5 L20 10.5 L20 13.5 L13.5 13.5 L13.5 21 L10.5 21 L10.5 13.5 L4 13.5 L4 10.5 L10.5 10.5 Z',
  military:   'M12 1 L14.5 10 L22 17 L21 18.5 L14.5 15 L14 20.5 L16.5 22 L16 23.5 L12 22 L8 23.5 L7.5 22 L10 20.5 L9.5 15 L3 18.5 L2 17 L9.5 10 Z',
};

function silhouetteType(model, isMilitary) {
  if (isMilitary) return 'military';
  const m = (model || '').toLowerCase();
  if (m.includes('a380') || m.includes('747') || m.includes('777') ||
      m.includes('a350') || m.includes('a330') || m.includes('787') ||
      m.includes('a340') || m.includes('767') || m.includes('748')) return 'widebody';
  if (m.includes('helicopter') || m.includes('h130') || m.includes('h135') ||
      m.includes('ec1') || m.includes('ec3') || m.includes('bell ') ||
      m.includes(' r44') || m.includes(' r66')) return 'helicopter';
  if (m.includes('crj') || m.includes('e170') || m.includes('e175') ||
      m.includes('e190') || m.includes('e195') || m.includes('atr') ||
      m.includes('q400') || m.includes('embraer 1')) return 'regional';
  if (m.includes('gulfstream') || m.includes('learjet') || m.includes('citation') ||
      m.includes('global') || m.includes('falcon') || m.includes('cessna') ||
      m.includes('pilatus') || m.includes('tbm') || m.includes('challenger')) return 'private';
  return 'narrowbody';
}

// ── PlaneGlyph ───────────────────────────────────────────────
function PlaneGlyph({ size = 22, color = '#fff', rotation = 0, opacity = 1, type = 'narrowbody' }) {
  const d = PLANE_PATHS[type] || PLANE_PATHS.narrowbody;
  return (
    <svg width={size} height={size} viewBox="0 0 24 24"
      style={{ transform: `rotate(${rotation}deg)`, opacity, display: 'block', flexShrink: 0 }}>
      <path d={d} fill={color} />
    </svg>
  );
}

// ── CompassRing ──────────────────────────────────────────────
function CompassRing({ size = 320, heading = 0, dimmed = false, locked = false }) {
  const cx = size / 2;
  const cy = size / 2;
  const outerR = size / 2 - 4;
  const innerR = outerR - 14;

  return (
    <div style={{
      position: 'absolute', width: size, height: size,
      left: '50%', top: '50%', transform: `translate(-50%, -50%) rotate(${-heading}deg)`,
      transition: 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    }}>
      {/* gradient ring */}
      <div style={{
        position: 'absolute', inset: 0, borderRadius: '50%',
        background: 'conic-gradient(from 0deg, #5BB3FF 0deg, #7A9BFF 45deg, #9B7DFF 90deg, #C064C8 135deg, #FF5B7A 180deg, #C064C8 225deg, #9B7DFF 270deg, #7A9BFF 315deg, #5BB3FF 360deg)',
        WebkitMask: `radial-gradient(circle, transparent ${innerR}px, #000 ${innerR + 1}px, #000 ${outerR}px, transparent ${outerR + 1}px)`,
        mask: `radial-gradient(circle, transparent ${innerR}px, #000 ${innerR + 1}px, #000 ${outerR}px, transparent ${outerR + 1}px)`,
        opacity: dimmed ? 0.4 : (locked ? 1 : 0.85),
        filter: locked ? 'saturate(1.2) brightness(1.1)' : 'none',
        transition: 'opacity 0.3s, filter 0.3s',
      }} />
      {/* outer glow */}
      <div style={{
        position: 'absolute', inset: -8, borderRadius: '50%',
        background: 'conic-gradient(from 0deg, #5BB3FF, #9B7DFF, #FF5B7A, #9B7DFF, #5BB3FF)',
        WebkitMask: `radial-gradient(circle, transparent ${outerR}px, #000 ${outerR + 2}px, #000 ${outerR + 8}px, transparent ${outerR + 9}px)`,
        mask: `radial-gradient(circle, transparent ${outerR}px, #000 ${outerR + 2}px, #000 ${outerR + 8}px, transparent ${outerR + 9}px)`,
        opacity: 0.25, filter: 'blur(4px)',
      }} />
      {/* tick marks */}
      <svg width={size} height={size} style={{ position: 'absolute', inset: 0 }}>
        {Array.from({ length: 36 }).map((_, i) => {
          const deg = i * 10;
          const a = (deg - 90) * Math.PI / 180;
          const major = deg % 90 === 0;
          const r1 = innerR - (major ? 10 : 5);
          const r2 = innerR - 1;
          return (
            <line key={i}
              x1={cx + Math.cos(a) * r1} y1={cy + Math.sin(a) * r1}
              x2={cx + Math.cos(a) * r2} y2={cy + Math.sin(a) * r2}
              stroke="rgba(255,255,255,0.35)"
              strokeWidth={major ? 1.5 : 0.8}
              opacity={dimmed ? 0.4 : 1}
            />
          );
        })}
      </svg>
      {/* cardinal labels — counter-rotate to stay upright */}
      {[
        { l: 'N', deg: 0,   c: '#5BB3FF' },
        { l: 'E', deg: 90,  c: '#9B7DFF' },
        { l: 'S', deg: 180, c: '#FF5B7A' },
        { l: 'W', deg: 270, c: '#9B7DFF' },
      ].map(({ l, deg, c }) => {
        const a = (deg - 90) * Math.PI / 180;
        const r = innerR - 22;
        return (
          <div key={l} style={{
            position: 'absolute',
            left: cx + Math.cos(a) * r,
            top: cy + Math.sin(a) * r,
            transform: `translate(-50%, -50%) rotate(${heading}deg)`,
            color: c, fontSize: 14, fontWeight: 700, letterSpacing: 0.5,
            opacity: dimmed ? 0.5 : 1,
          }}>{l}</div>
        );
      })}
      {/* north triangle indicator */}
      <div style={{
        position: 'absolute', left: '50%', top: 1,
        transform: 'translate(-50%, -100%)',
        width: 0, height: 0,
        borderLeft: '5px solid transparent',
        borderRight: '5px solid transparent',
        borderBottom: '7px solid rgba(255,255,255,0.6)',
        opacity: dimmed ? 0.4 : 1,
      }} />
    </div>
  );
}

// ── Compass ──────────────────────────────────────────────────
function Compass({
  size = 320, heading = 0, aircraft = [], radiusKm = 30,
  focused = null, locked = false, showBeam = true,
  onAircraftTap = () => {}, dimmed = false,
  trackedFlights = [],
}) {
  const cx = size / 2;
  const cy = size / 2;
  const outerR = size / 2 - 4;
  const innerR = outerR - 14;
  const planeR = innerR - 28;

  const within = aircraft.filter(p => p.distance <= radiusKm);
  const focusedPlane = within.find(p => p.id === focused);

  return (
    <div style={{ position: 'relative', width: size, height: size }}>
      <CompassRing size={size} heading={heading} dimmed={dimmed} locked={locked} />

      {/* focus beam cone */}
      {showBeam && focusedPlane && (() => {
        const rel = ((focusedPlane.bearing - heading + 540) % 360) - 180;
        return (
          <div style={{
            position: 'absolute', left: cx, top: cy,
            width: 0, height: 0,
            transform: `translate(-50%, -50%) rotate(${rel}deg)`,
            transition: 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            pointerEvents: 'none',
          }}>
            <div style={{
              position: 'absolute', left: '50%', bottom: 0,
              width: 110, height: planeR + 10,
              transform: 'translateX(-50%)',
              background: locked
                ? 'radial-gradient(ellipse at bottom, rgba(91,179,255,0.6) 0%, rgba(91,179,255,0.15) 40%, transparent 75%)'
                : 'radial-gradient(ellipse at bottom, rgba(91,179,255,0.35) 0%, rgba(91,179,255,0.08) 50%, transparent 80%)',
              filter: 'blur(2px)',
              animation: 'skyBeamPulse 2.5s ease-in-out infinite',
            }} />
          </div>
        );
      })()}

      {/* center home icon */}
      <div style={{
        position: 'absolute', left: '50%', top: '50%',
        transform: 'translate(-50%, -50%)',
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        gap: 2, zIndex: 3,
      }}>
        <PlaneGlyph size={26} color="#fff" />
        <div style={{
          width: 0, height: 0,
          borderLeft: '5px solid transparent',
          borderRight: '5px solid transparent',
          borderTop: '6px solid rgba(255,255,255,0.7)',
        }} />
      </div>

      {/* aircraft markers */}
      {within.map(p => {
        const pos = ringPos(p.bearing, heading, planeR, cx, cy);
        const c = colorForRel(pos.relDeg);
        const isFocused = p.id === focused;
        const isTracked = trackedFlights.includes(p.id);
        const isInteresting = p.interesting;
        const isMilitary = p.isMilitary;
        const glyphColor = isFocused ? '#fff' : isMilitary ? '#F5A83A' : isTracked ? c : 'rgba(255,255,255,0.9)';

        return (
          <React.Fragment key={p.id}>
            {/* tracked flight ping ring */}
            {isTracked && (
              <div style={{
                position: 'absolute',
                left: pos.x, top: pos.y,
                width: 32, height: 32, borderRadius: '50%',
                border: `2px solid ${c}`,
                transform: 'translate(-50%, -50%) scale(1)',
                animation: 'skyTrackedRing 2s ease-out infinite',
                pointerEvents: 'none',
                zIndex: 1,
              }} />
            )}
            {/* military badge ring */}
            {isMilitary && !isTracked && (
              <div style={{
                position: 'absolute',
                left: pos.x, top: pos.y,
                width: 36, height: 36, borderRadius: '50%',
                border: '1.5px solid #F5A83A',
                transform: 'translate(-50%, -50%)',
                animation: 'skyMilitaryBadge 2s ease-in-out infinite',
                pointerEvents: 'none',
                zIndex: 1,
              }} />
            )}
            {/* interesting badge ring */}
            {isInteresting && !isMilitary && !isTracked && (
              <div style={{
                position: 'absolute',
                left: pos.x, top: pos.y,
                width: 36, height: 36, borderRadius: '50%',
                border: '1.5px solid #9B7DFF',
                transform: 'translate(-50%, -50%)',
                animation: 'skyInterestingBadge 2s ease-in-out infinite',
                pointerEvents: 'none',
                zIndex: 1,
              }} />
            )}
            {/* aircraft icon */}
            <div
              onClick={() => onAircraftTap(p.id)}
              style={{
                position: 'absolute',
                left: pos.x, top: pos.y,
                transform: isFocused
                  ? 'translate(-50%, -50%) scale(1)'
                  : 'translate(-50%, -50%)',
                display: 'flex', flexDirection: 'column', alignItems: 'center',
                gap: 4, cursor: 'pointer',
                transition: 'left 0.5s cubic-bezier(0.4, 0, 0.2, 1), top 0.5s cubic-bezier(0.4, 0, 0.2, 1)',
                zIndex: isFocused ? 4 : 2,
                animation: isFocused ? 'skyPlanePulse 2s ease-in-out infinite' : 'none',
                filter: isFocused ? `drop-shadow(0 0 8px ${c})` : 'none',
              }}>
              <PlaneGlyph
                size={isFocused ? 28 : (isInteresting || isMilitary) ? 24 : 22}
                color={glyphColor}
                rotation={p.track || 0}
                type={silhouetteType(p.model, isMilitary)}
              />
              <div style={{
                fontSize: 10.5, color: isFocused ? '#fff' : 'rgba(255,255,255,0.65)',
                fontVariantNumeric: 'tabular-nums', fontWeight: isFocused ? 600 : 500,
                whiteSpace: 'nowrap',
                textShadow: isFocused ? `0 0 8px ${c}` : 'none',
              }}>
                {isFocused ? p.callsign : `${p.distance.toFixed(1)} km`}
              </div>
            </div>
          </React.Fragment>
        );
      })}
    </div>
  );
}

Object.assign(window, { Compass, CompassRing, PlaneGlyph, colorForRel, humanDirAbsolute, silhouetteType });

// SkyLook off-phone surfaces: home-screen widgets, smart display, TV ambient

// Airline logo from kiwi.com CDN; falls back to a letter avatar on 404 or missing code.
function AirlineLogoImg({ iataCode, airline, size = 44 }) {
  const [failed, setFailed] = React.useState(!iataCode);
  const letter = (airline || '?')[0].toUpperCase();
  const style = { width: size, height: size, borderRadius: size * 0.22,
    flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' };
  if (failed) {
    return (
      <div style={{ ...style, background: 'rgba(91,179,255,0.14)' }}>
        <span style={{ color: '#5BB3FF', fontWeight: 700, fontSize: size * 0.44 }}>{letter}</span>
      </div>
    );
  }
  return (
    <img
      src={`https://images.kiwi.com/airlines/64/${iataCode}.png`}
      alt={airline}
      style={{ ...style, background: 'rgba(255,255,255,0.07)', objectFit: 'contain' }}
      onError={() => setFailed(true)}
    />
  );
}

// Small compass card optimized for widget sizes.
// onAircraftTap(id) is optional — enables click-selection on TV/display surfaces.
// React.memo prevents re-renders from parent state changes unrelated to compass data.
const MiniCompass = React.memo(function MiniCompass({ size, aircraft = [], heading = 0, showLabels = true, planeSize = 16,
  tickColor = 'rgba(255,255,255,0.35)', selectedId = null, trackedIds = [],
  onAircraftTap = null }) {
  const cx = size / 2;
  const cy = size / 2;
  const outerR = size / 2 - 2;
  const ringW = Math.max(4, size * 0.04);
  const innerR = outerR - ringW;
  const planeR = innerR - planeSize * 1.2;

  return (
    <div style={{ position: 'relative', width: size, height: size }}>
      {/* gradient ring */}
      <div style={{
        position: 'absolute', inset: 0, borderRadius: '50%',
        background: 'conic-gradient(from 0deg, #5BB3FF 0deg, #7A9BFF 45deg, #9B7DFF 90deg, #C064C8 135deg, #FF5B7A 180deg, #C064C8 225deg, #9B7DFF 270deg, #7A9BFF 315deg, #5BB3FF 360deg)',
        WebkitMask: `radial-gradient(circle, transparent ${innerR}px, #000 ${innerR + 0.5}px, #000 ${outerR}px, transparent ${outerR + 0.5}px)`,
        mask: `radial-gradient(circle, transparent ${innerR}px, #000 ${innerR + 0.5}px, #000 ${outerR}px, transparent ${outerR + 0.5}px)`,
        opacity: 0.9,
      }} />
      {/* ticks */}
      <svg width={size} height={size} style={{ position: 'absolute', inset: 0 }}>
        {Array.from({ length: 24 }).map((_, i) => {
          const deg = i * 15;
          const a = (deg - 90) * Math.PI / 180;
          const major = deg % 90 === 0;
          const r1 = innerR - (major ? size * 0.045 : size * 0.022);
          const r2 = innerR - 0.5;
          return (
            <line key={i}
              x1={cx + Math.cos(a) * r1} y1={cy + Math.sin(a) * r1}
              x2={cx + Math.cos(a) * r2} y2={cy + Math.sin(a) * r2}
              stroke={tickColor}
              strokeWidth={major ? 1.4 : 0.7}
            />
          );
        })}
      </svg>
      {/* cardinals */}
      {showLabels && [
        { l: 'N', deg: 0,   c: '#5BB3FF' },
        { l: 'E', deg: 90,  c: '#9B7DFF' },
        { l: 'S', deg: 180, c: '#FF5B7A' },
        { l: 'W', deg: 270, c: '#9B7DFF' },
      ].map(({ l, deg, c }) => {
        const a = (deg - 90) * Math.PI / 180;
        const r = innerR - size * 0.075;
        return (
          <div key={l} style={{
            position: 'absolute',
            left: cx + Math.cos(a) * r, top: cy + Math.sin(a) * r,
            transform: 'translate(-50%, -50%)',
            color: c, fontWeight: 700,
            fontSize: Math.max(8, size * 0.038), letterSpacing: 0.3,
          }}>{l}</div>
        );
      })}
      {/* home glyph */}
      <div style={{
        position: 'absolute', left: '50%', top: '50%', transform: 'translate(-50%, -50%)',
        opacity: 0.35,
      }}>
        <svg width={size * 0.12} height={size * 0.12} viewBox="0 0 24 24" fill="none">
          <path d="M3 11l9-8 9 8v10a1 1 0 01-1 1h-5v-6h-6v6H4a1 1 0 01-1-1V11z" stroke="#fff" strokeWidth="1.5" strokeLinejoin="round"/>
        </svg>
      </div>
      {/* planes — positions memoized so recalc only when aircraft or heading changes */}
      {React.useMemo(() => aircraft.map((p, i) => {
        const rel = ((p.bearing - heading + 540) % 360) - 180;
        const a = (rel - 90) * Math.PI / 180;
        const x = cx + Math.cos(a) * planeR;
        const y = cy + Math.sin(a) * planeR;
        const isSelected = p.id === selectedId;
        const isTracked = trackedIds.includes(p.id);
        const planeColor = isSelected ? '#fff' : isTracked ? '#5BB3FF' : '#fff';
        const planeOpacity = isSelected ? 1 : 0.9;
        return (
          <React.Fragment key={p.id || i}>
            {isTracked && (
              <div style={{
                position: 'absolute', left: x, top: y,
                width: planeSize * 2.2, height: planeSize * 2.2, borderRadius: '50%',
                border: '1.5px solid #5BB3FF',
                transform: 'translate(-50%, -50%)',
                animation: 'skyTrackedRing 2.5s ease-out infinite',
                pointerEvents: 'none',
              }} />
            )}
            {isSelected && (
              <div style={{
                position: 'absolute', left: x, top: y,
                width: planeSize * 2.6, height: planeSize * 2.6, borderRadius: '50%',
                border: '2px solid rgba(255,255,255,0.8)',
                transform: 'translate(-50%, -50%)',
                pointerEvents: 'none',
              }} />
            )}
            <div
              onClick={onAircraftTap ? () => onAircraftTap(p.id) : undefined}
              style={{
                position: 'absolute', left: x, top: y,
                transform: 'translate(-50%, -50%)',
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1,
                cursor: onAircraftTap ? 'pointer' : 'default',
                filter: isSelected ? 'drop-shadow(0 0 6px rgba(255,255,255,0.8))' : 'none',
              }}>
              <PlaneGlyph size={isSelected ? planeSize * 1.2 : planeSize} color={planeColor} opacity={planeOpacity} />
              {p.timeLabel && (
                <div style={{
                  fontSize: Math.max(8, size * 0.028), color: isSelected ? '#fff' : 'rgba(255,255,255,0.7)',
                  fontVariantNumeric: 'tabular-nums', fontWeight: isSelected ? 700 : 500,
                  whiteSpace: 'nowrap',
                }}>{p.timeLabel}</div>
              )}
            </div>
          </React.Fragment>
        );
      }), [aircraft, heading, cx, cy, planeR, planeSize, selectedId, trackedIds, onAircraftTap])}
    </div>
  );
});

// ── iOS home-screen widget — Small ───────────────────────────
function WidgetSmall({ aircraft = [], nextIn = '2:15' }) {
  const size = 170;
  return (
    <div style={{
      width: size, height: size, borderRadius: 22,
      background: 'linear-gradient(135deg, #0E1421 0%, #0A0F1A 100%)',
      padding: 14, boxSizing: 'border-box',
      color: '#fff', position: 'relative', overflow: 'hidden',
      fontFamily: '-apple-system, system-ui',
    }}>
      <div style={{
        position: 'absolute', top: -30, right: -30, width: 100, height: 100,
        borderRadius: '50%', background: 'radial-gradient(circle, rgba(91,179,255,0.15) 0%, transparent 70%)',
      }}/>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)', fontWeight: 600, letterSpacing: 0.6 }}>SKYLOOK</div>
          <div style={{ marginTop: 2, fontSize: 28, fontWeight: 700, letterSpacing: -0.6, lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>
            {aircraft.length}
          </div>
          <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.6)', marginTop: 2, fontWeight: 500 }}>
            overhead
          </div>
        </div>
        <PlaneGlyph size={14} color="#5BB3FF" />
      </div>
      <div style={{ position: 'absolute', left: 14, bottom: 12, right: 14 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ flexShrink: 0 }}>
            <MiniCompass size={70} aircraft={aircraft.slice(0, 3).map(p => ({ ...p, timeLabel: null }))} planeSize={9} showLabels={false} tickColor="rgba(255,255,255,0.2)"/>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 9, color: 'rgba(255,255,255,0.5)', fontWeight: 600, letterSpacing: 0.5 }}>NEXT</div>
            <div style={{ fontSize: 18, fontWeight: 700, color: '#5BB3FF', letterSpacing: -0.4, fontVariantNumeric: 'tabular-nums', marginTop: -2 }}>
              {nextIn}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── iOS home-screen widget — Medium ─────────────────────────
function WidgetMedium({ aircraft = [], nextIn = '2:15' }) {
  return (
    <div style={{
      width: 364, height: 170, borderRadius: 22,
      background: 'linear-gradient(135deg, #0E1421 0%, #0A0F1A 100%)',
      padding: 16, boxSizing: 'border-box',
      color: '#fff', position: 'relative', overflow: 'hidden',
      fontFamily: '-apple-system, system-ui',
      display: 'flex', gap: 14,
    }}>
      <div style={{
        position: 'absolute', top: -40, right: -40, width: 140, height: 140,
        borderRadius: '50%', background: 'radial-gradient(circle, rgba(91,179,255,0.12) 0%, transparent 70%)',
      }}/>
      <MiniCompass size={138} aircraft={aircraft.slice(0, 4)} planeSize={13} tickColor="rgba(255,255,255,0.25)"/>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)', fontWeight: 600, letterSpacing: 0.6 }}>SKYLOOK</div>
          <PlaneGlyph size={12} color="#5BB3FF" />
        </div>
        <div style={{ marginTop: 6, fontSize: 32, fontWeight: 700, letterSpacing: -0.8, lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>
          {aircraft.length}
        </div>
        <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.6)', marginTop: 2 }}>aircraft overhead</div>
        <div style={{ marginTop: 'auto', paddingTop: 8 }}>
          <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)', fontWeight: 600, letterSpacing: 0.6 }}>NEXT IN</div>
          <div style={{ fontSize: 22, fontWeight: 700, color: '#5BB3FF', letterSpacing: -0.6, fontVariantNumeric: 'tabular-nums' }}>
            {nextIn}
          </div>
          <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.55)', marginTop: 1 }}>
            {aircraft[0]?.callsign} · {aircraft[0]?.fromCode}→{aircraft[0]?.toCode}
          </div>
        </div>
      </div>
    </div>
  );
}

// ── iOS home-screen widget — Large ───────────────────────────
function WidgetLarge({ aircraft = [], nextIn = '2:15' }) {
  return (
    <div style={{
      width: 364, height: 382, borderRadius: 22,
      background: 'linear-gradient(135deg, #0E1421 0%, #0A0F1A 100%)',
      padding: 18, boxSizing: 'border-box',
      color: '#fff', position: 'relative', overflow: 'hidden',
      fontFamily: '-apple-system, system-ui',
      display: 'flex', flexDirection: 'column',
    }}>
      <div style={{
        position: 'absolute', top: -60, right: -60, width: 220, height: 220,
        borderRadius: '50%', background: 'radial-gradient(circle, rgba(91,179,255,0.1) 0%, transparent 70%)',
      }}/>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)', fontWeight: 600, letterSpacing: 0.6 }}>SKYLOOK</div>
          <div style={{ marginTop: 4, fontSize: 13, color: 'rgba(255,255,255,0.6)' }}>
            <span style={{ color: '#fff', fontWeight: 600, fontSize: 24, letterSpacing: -0.4 }}>{aircraft.length}</span> overhead now
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)', fontWeight: 600, letterSpacing: 0.6 }}>NEXT IN</div>
          <div style={{ fontSize: 22, fontWeight: 700, color: '#5BB3FF', letterSpacing: -0.6, fontVariantNumeric: 'tabular-nums' }}>{nextIn}</div>
        </div>
      </div>
      <div style={{ display: 'flex', justifyContent: 'center', margin: '8px 0' }}>
        <MiniCompass size={210} aircraft={aircraft} planeSize={16} tickColor="rgba(255,255,255,0.3)"/>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 'auto' }}>
        {aircraft.slice(0, 3).map(p => (
          <div key={p.id} style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: '6px 10px', borderRadius: 10,
            background: 'rgba(255,255,255,0.04)',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <PlaneGlyph size={12} color="rgba(255,255,255,0.85)" />
              <div>
                <div style={{ fontSize: 12, fontWeight: 600 }}>{p.callsign}</div>
                <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)' }}>{p.fromCode} → {p.toCode}</div>
              </div>
            </div>
            <div style={{ fontSize: 12, color: '#5BB3FF', fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
              {p.timeLabel || (p.overheadMin + ' min')}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Smart display (landscape kitchen-counter, 1024×600) ──────
function SmartDisplay({ aircraft = [], nextIn = '2:15', time = '7:42', temp = '21°', trackedIds = ['BA123'] }) {
  const tracked = aircraft.filter(p => trackedIds.includes(p.id));

  return (
    <div style={{
      width: 1024, height: 600, borderRadius: 28,
      background: 'linear-gradient(135deg, #0E1421 0%, #06090F 100%)',
      color: '#fff', position: 'relative', overflow: 'hidden',
      fontFamily: '-apple-system, system-ui',
      padding: 36, boxSizing: 'border-box',
      display: 'flex', alignItems: 'stretch', gap: 30,
    }}>
      {/* ambient glows */}
      <div style={{
        position: 'absolute', top: -200, left: '30%', width: 600, height: 600,
        borderRadius: '50%', background: 'radial-gradient(circle, rgba(91,179,255,0.08) 0%, transparent 65%)',
        pointerEvents: 'none',
      }}/>
      <div style={{
        position: 'absolute', bottom: -200, right: '20%', width: 500, height: 500,
        borderRadius: '50%', background: 'radial-gradient(circle, rgba(255,91,122,0.06) 0%, transparent 65%)',
        pointerEvents: 'none',
      }}/>

      {/* header strip */}
      <div style={{
        position: 'absolute', top: 24, left: 36, right: 36,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        fontSize: 16, color: 'rgba(255,255,255,0.6)', fontWeight: 500,
      }}>
        <div style={{ fontVariantNumeric: 'tabular-nums' }}>{time}</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 18, fontWeight: 700, color: '#fff', letterSpacing: -0.3 }}>
          SkyLook <PlaneGlyph size={14} color="#5BB3FF"/>
        </div>
        <div>☀ {temp}</div>
      </div>

      {/* left panel */}
      <div style={{
        width: 230, marginTop: 64,
        display: 'flex', flexDirection: 'column', gap: 12, justifyContent: 'flex-start',
      }}>
        <div style={{
          padding: '20px 22px', borderRadius: 18,
          background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)',
        }}>
          <div style={{ fontSize: 56, fontWeight: 700, letterSpacing: -1.8, lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>
            {aircraft.length}
          </div>
          <div style={{ marginTop: 2, fontSize: 11, color: 'rgba(255,255,255,0.6)', fontWeight: 600, letterSpacing: 0.6 }}>AIRCRAFT NEARBY</div>
          <div style={{ marginTop: 16, fontSize: 12, color: 'rgba(255,255,255,0.55)' }}>Next overhead in</div>
          <div style={{ marginTop: 2, fontSize: 30, fontWeight: 700, color: '#5BB3FF', letterSpacing: -0.6, fontVariantNumeric: 'tabular-nums' }}>{nextIn}</div>
        </div>

        {/* tracked flights */}
        {tracked.length > 0 && (
          <div style={{
            padding: '14px 16px', borderRadius: 18,
            background: 'rgba(91,179,255,0.06)', border: '1px solid rgba(91,179,255,0.14)',
          }}>
            <div style={{ fontSize: 10, color: '#5BB3FF', fontWeight: 700, letterSpacing: 1, marginBottom: 10, display: 'flex', alignItems: 'center', gap: 5 }}>
              ♥ TRACKED
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {tracked.map(p => (
                <div key={p.id} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <PlaneGlyph size={12} color="#5BB3FF" />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 13, fontWeight: 700 }}>{p.callsign}</div>
                    <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)' }}>
                      {p.toCode} · {p.timeLabel || (p.overheadMin + ' min')}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* compass center */}
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: 24 }}>
        <MiniCompass size={410} aircraft={aircraft.slice(0, 6)} planeSize={22}
          tickColor="rgba(255,255,255,0.3)" trackedIds={trackedIds} />
      </div>

      {/* right list */}
      <div style={{
        width: 230, marginTop: 64,
        display: 'flex', flexDirection: 'column', gap: 8,
      }}>
        <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)', fontWeight: 700, letterSpacing: 1, marginBottom: 4 }}>NEARBY</div>
        {aircraft.slice(0, 5).map((p, i) => (
          <div key={p.id} style={{
            padding: '11px 14px', borderRadius: 14,
            background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.05)',
            display: 'flex', alignItems: 'center', gap: 10,
          }}>
            <PlaneGlyph size={15} color={trackedIds.includes(p.id) ? '#5BB3FF' : 'rgba(255,255,255,0.85)'}/>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 700, letterSpacing: -0.2 }}>{p.callsign}</div>
              <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)', fontVariantNumeric: 'tabular-nums' }}>
                {p.timeLabel || (p.overheadMin + ' min')}{i === 0 ? ' overhead' : ''}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── TV ambient view — 1920×1080 ──────────────────────────────
function TVView({ aircraft = [], nextIn = '2:15', time = '7:42 PM', temp = '21°C', date = 'May 26',
  trackedIds: initialTrackedIds = ['BA123'] }) {
  const [selectedId, setSelectedId] = React.useState(null);
  const [trackedIds, setTrackedIds] = React.useState(initialTrackedIds);

  const selected = aircraft.find(p => p.id === selectedId);
  const tracked = aircraft.filter(p => trackedIds.includes(p.id));

  function toggleTrack(id) {
    setTrackedIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  }

  // Keyboard navigation when container is focused
  const containerRef = React.useRef(null);
  React.useEffect(() => {
    const onKey = (e) => {
      if (!containerRef.current) return;
      const ids = aircraft.map(p => p.id);
      if (e.key === 'Escape') { setSelectedId(null); return; }
      if (e.key === 'ArrowDown' || e.key === 'ArrowRight') {
        const idx = ids.indexOf(selectedId);
        setSelectedId(ids[(idx + 1) % ids.length]);
      }
      if (e.key === 'ArrowUp' || e.key === 'ArrowLeft') {
        const idx = ids.indexOf(selectedId);
        setSelectedId(ids[(idx - 1 + ids.length) % ids.length]);
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [selectedId, aircraft]);

  return (
    <div ref={containerRef} tabIndex={0} style={{
      width: 1920, height: 1080,
      background: 'radial-gradient(ellipse at center, #0E1421 0%, #050810 100%)',
      color: '#fff', position: 'relative', overflow: 'hidden',
      fontFamily: '-apple-system, system-ui',
      display: 'flex', alignItems: 'center', gap: 60,
      padding: '80px 100px', boxSizing: 'border-box',
      outline: 'none',
    }}>
      {/* ambient glows */}
      <div style={{
        position: 'absolute', top: -300, left: '20%', width: 900, height: 900,
        borderRadius: '50%', background: 'radial-gradient(circle, rgba(91,179,255,0.08) 0%, transparent 60%)',
        pointerEvents: 'none', filter: 'blur(20px)',
      }}/>
      <div style={{
        position: 'absolute', bottom: -300, right: '20%', width: 900, height: 900,
        borderRadius: '50%', background: 'radial-gradient(circle, rgba(255,91,122,0.07) 0%, transparent 60%)',
        pointerEvents: 'none', filter: 'blur(20px)',
      }}/>

      {/* LEFT — clock + count + tracked + nearby list */}
      <div style={{ width: 480, display: 'flex', flexDirection: 'column', gap: 24 }}>
        {/* clock */}
        <div>
          <div style={{ fontSize: 56, fontWeight: 700, letterSpacing: -1.2, lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>{time}</div>
          <div style={{ marginTop: 10, fontSize: 22, color: 'rgba(255,255,255,0.6)', display: 'flex', alignItems: 'center', gap: 12 }}>
            <span>{date} · Clear sky</span>
            <span>☀</span>
            <span>{temp}</span>
          </div>
        </div>

        {/* count card */}
        <div style={{
          padding: '28px 32px', borderRadius: 24,
          background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)',
          display: 'flex', alignItems: 'center', gap: 24,
        }}>
          <div style={{ fontSize: 100, fontWeight: 700, letterSpacing: -3, lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>{aircraft.length}</div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: 1, color: 'rgba(255,255,255,0.8)' }}>AIRCRAFT</div>
            <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: 1, color: 'rgba(255,255,255,0.8)' }}>NEARBY</div>
            <div style={{ marginTop: 12, fontSize: 14, color: 'rgba(255,255,255,0.5)' }}>Next overhead in</div>
            <div style={{ fontSize: 32, fontWeight: 700, color: '#5BB3FF', letterSpacing: -0.6, fontVariantNumeric: 'tabular-nums', lineHeight: 1 }}>{nextIn}</div>
          </div>
          <PlaneGlyph size={28} color="rgba(255,255,255,0.45)"/>
        </div>

        {/* tracked flights (shown if any) */}
        {tracked.length > 0 && (
          <div style={{
            padding: '20px 24px', borderRadius: 24,
            background: 'rgba(91,179,255,0.06)', border: '1px solid rgba(91,179,255,0.18)',
          }}>
            <div style={{ fontSize: 13, color: '#5BB3FF', fontWeight: 700, letterSpacing: 1.4, marginBottom: 14, display: 'flex', alignItems: 'center', gap: 8 }}>
              ♥ TRACKED FLIGHTS
            </div>
            {tracked.map((p, i) => (
              <div key={p.id}
                onClick={() => setSelectedId(p.id === selectedId ? null : p.id)}
                style={{
                  display: 'flex', alignItems: 'center', gap: 14,
                  padding: '12px 0',
                  borderTop: i > 0 ? '1px solid rgba(255,255,255,0.05)' : 'none',
                  cursor: 'pointer',
                  borderRadius: 8,
                }}>
                <PlaneGlyph size={18} color="#5BB3FF"/>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: -0.3 }}>{p.callsign}</div>
                  <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.55)', marginTop: 2 }}>
                    {p.fromCode} → {p.toCode} · {p.overheadMin}m
                  </div>
                </div>
                <div style={{
                  fontSize: 13, color: '#5BB3FF', fontWeight: 600, letterSpacing: 1,
                  padding: '4px 10px', borderRadius: 6, background: 'rgba(91,179,255,0.12)',
                }}>
                  {p.timeLabel || (p.overheadMin + ' min')}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* nearby aircraft list */}
        <div style={{
          padding: '20px 24px', borderRadius: 24,
          background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)',
          flex: tracked.length > 0 ? undefined : 1,
        }}>
          <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.55)', fontWeight: 600, letterSpacing: 1.4, marginBottom: 12 }}>
            NEARBY AIRCRAFT
            <span style={{ marginLeft: 8, fontSize: 11, color: 'rgba(255,255,255,0.35)', fontWeight: 400, letterSpacing: 0 }}>↑↓ arrow keys to navigate</span>
          </div>
          {aircraft.slice(0, 4).map((p, i) => (
            <div key={p.id}
              onClick={() => setSelectedId(p.id === selectedId ? null : p.id)}
              style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
                padding: '12px 10px',
                marginLeft: -10, marginRight: -10,
                borderTop: i > 0 ? '1px solid rgba(255,255,255,0.05)' : 'none',
                cursor: 'pointer', borderRadius: 10,
                background: p.id === selectedId ? 'rgba(91,179,255,0.1)' : 'transparent',
                transition: 'background 0.15s',
              }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14, flex: 1 }}>
                <PlaneGlyph size={18} color={trackedIds.includes(p.id) ? '#5BB3FF' : 'rgba(255,255,255,0.85)'}/>
                <div>
                  <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: -0.3, color: p.id === selectedId ? '#fff' : 'rgba(255,255,255,0.95)' }}>
                    {p.callsign}
                    {p.interesting && <span style={{ marginLeft: 8, fontSize: 12, color: '#9B7DFF', fontWeight: 600 }}>A380</span>}
                  </div>
                  <div style={{ marginTop: 2, fontSize: 13, color: 'rgba(255,255,255,0.55)' }}>
                    {p.fromCity || p.fromCode} → {p.toCity || p.toCode}
                  </div>
                  <div style={{ marginTop: 2, fontSize: 12, color: 'rgba(255,255,255,0.4)', fontVariantNumeric: 'tabular-nums' }}>
                    {(p.altitudeFt || 0).toLocaleString()} ft · {p.speedMph} mph
                  </div>
                </div>
              </div>
              <div style={{ textAlign: 'right', flexShrink: 0 }}>
                <div style={{ fontSize: 18, fontWeight: 700, color: '#5BB3FF', fontVariantNumeric: 'tabular-nums' }}>
                  {p.timeLabel || (p.overheadMin + ' min')}
                </div>
                {i === 0 && <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', textTransform: 'uppercase', letterSpacing: 0.8, marginTop: 2 }}>overhead</div>}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* CENTER — big compass */}
      <div style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <MiniCompass size={720} aircraft={aircraft.slice(0, 6)} planeSize={40}
          tickColor="rgba(255,255,255,0.35)"
          selectedId={selectedId}
          trackedIds={trackedIds}
          onAircraftTap={(id) => setSelectedId(id === selectedId ? null : id)} />
      </div>

      {/* RIGHT — detail panel OR direction guide */}
      <div style={{ width: 340, display: 'flex', flexDirection: 'column', gap: 18 }}>
        {selected ? (
          // ── Selected aircraft detail ──
          <>
            <div style={{
              padding: '22px 24px', borderRadius: 20,
              background: 'rgba(91,179,255,0.08)', border: '1px solid rgba(91,179,255,0.22)',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
                  {/* Airline logo — falls back to first-letter avatar on 404 */}
                  <AirlineLogoImg iataCode={selected.iataCode} airline={selected.airline} size={52} />
                  <div>
                    <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.55)', fontWeight: 600, letterSpacing: 1.4 }}>SELECTED AIRCRAFT</div>
                    <div style={{ marginTop: 6, fontSize: 40, fontWeight: 700, letterSpacing: -1, lineHeight: 1 }}>{selected.callsign}</div>
                    <div style={{ marginTop: 4, fontSize: 16, color: 'rgba(255,255,255,0.65)' }}>{selected.airline}</div>
                    {selected.model && (
                      <div style={{ marginTop: 2, fontSize: 13, color: 'rgba(255,255,255,0.4)' }}>{selected.model}</div>
                    )}
                  </div>
                </div>
                <button onClick={() => setSelectedId(null)} style={{
                  width: 36, height: 36, borderRadius: '50%',
                  border: '1px solid rgba(255,255,255,0.15)',
                  background: 'rgba(255,255,255,0.06)',
                  cursor: 'pointer', color: 'rgba(255,255,255,0.7)',
                  fontSize: 18, display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>×</button>
              </div>

              {/* route */}
              <div style={{
                padding: '14px 16px', borderRadius: 12,
                background: 'rgba(255,255,255,0.04)',
                marginBottom: 16,
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div style={{ fontSize: 22, fontWeight: 700, letterSpacing: -0.4 }}>{selected.fromCode || '—'}</div>
                    <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.5)', marginTop: 2 }}>{selected.fromCity || ''}</div>
                  </div>
                  <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <PlaneGlyph size={20} color="rgba(255,255,255,0.4)" rotation={90} />
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: 22, fontWeight: 700, letterSpacing: -0.4 }}>{selected.toCode || '—'}</div>
                    <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.5)', marginTop: 2 }}>{selected.toCity || ''}</div>
                  </div>
                </div>
                {selected.progress != null && (
                  <div style={{ marginTop: 10, height: 3, borderRadius: 2, background: 'rgba(255,255,255,0.1)' }}>
                    <div style={{ width: `${selected.progress * 100}%`, height: '100%', borderRadius: 2, background: 'linear-gradient(90deg, #5BB3FF, #9B7DFF)' }} />
                  </div>
                )}
              </div>

              {/* stats */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 14 }}>
                {[
                  ['Altitude', `${(selected.altitudeFt || 0).toLocaleString()} ft`],
                  ['Speed', `${selected.speedMph} mph`],
                  ['Distance', `${(selected.distance || '—')} km`],
                  ['Overhead in', `${selected.overheadMin} min`],
                ].map(([k, v]) => (
                  <div key={k} style={{
                    padding: '10px 12px', borderRadius: 10,
                    background: 'rgba(255,255,255,0.04)',
                  }}>
                    <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.45)', fontWeight: 600, letterSpacing: 0.5 }}>{k.toUpperCase()}</div>
                    <div style={{ marginTop: 3, fontSize: 16, fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>{v}</div>
                  </div>
                ))}
              </div>

              {/* where to look */}
              <div style={{
                padding: '12px 14px', borderRadius: 12,
                background: 'rgba(91,179,255,0.08)', border: '1px solid rgba(91,179,255,0.18)',
                marginBottom: 14,
              }}>
                <div style={{ fontSize: 11, color: '#5BB3FF', fontWeight: 700, letterSpacing: 1 }}>WHERE TO LOOK</div>
                <div style={{ marginTop: 4, fontSize: 16, fontWeight: 600 }}>
                  {humanDirAbsolute(selected.bearing, selected.altitudeFt || 35000)}
                </div>
              </div>

              {/* track button */}
              <button onClick={() => toggleTrack(selected.id)} style={{
                width: '100%', height: 44, borderRadius: 12,
                border: trackedIds.includes(selected.id) ? '1px solid rgba(91,179,255,0.4)' : '1px solid rgba(255,255,255,0.12)',
                background: trackedIds.includes(selected.id) ? 'rgba(91,179,255,0.15)' : 'rgba(255,255,255,0.05)',
                color: trackedIds.includes(selected.id) ? '#5BB3FF' : 'rgba(255,255,255,0.8)',
                fontSize: 14, fontWeight: 600, cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
                fontFamily: '-apple-system, system-ui',
                transition: 'all 0.2s',
              }}>
                {trackedIds.includes(selected.id) ? '♥ Tracking' : '♡ Track this flight'}
              </button>
            </div>
          </>
        ) : (
          // ── Default right panel ──
          <>
            <div style={{
              padding: '22px 24px', borderRadius: 20,
              background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)',
            }}>
              <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.55)', fontWeight: 600, letterSpacing: 1.4, marginBottom: 14 }}>DIRECTION GUIDE</div>
              {[
                { c: '#5BB3FF', l: 'Blue · In front / North' },
                { c: '#9B7DFF', l: 'Purple · Sides / East–West' },
                { c: '#FF5B7A', l: 'Red · Behind / South' },
              ].map(d => (
                <div key={d.l} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '7px 0' }}>
                  <div style={{ width: 10, height: 10, borderRadius: '50%', background: d.c, boxShadow: `0 0 12px ${d.c}`, flexShrink: 0 }}/>
                  <div style={{ fontSize: 16, color: 'rgba(255,255,255,0.85)' }}>{d.l}</div>
                </div>
              ))}
            </div>

            <div style={{
              padding: '22px 24px', borderRadius: 20,
              background: 'linear-gradient(135deg, rgba(91,179,255,0.18), rgba(91,179,255,0.06))',
              border: '1px solid rgba(91,179,255,0.25)',
            }}>
              <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.7)', fontWeight: 600, letterSpacing: 1.4, marginBottom: 10 }}>BEST TIME TO LOOK</div>
              <div style={{ fontSize: 28, fontWeight: 700, color: '#5BB3FF', letterSpacing: -0.4, marginBottom: 8 }}>Right now</div>
              <div style={{ fontSize: 14, color: 'rgba(255,255,255,0.7)', lineHeight: 1.5 }}>
                {aircraft[0] ? humanDirAbsolute(aircraft[0].bearing, aircraft[0].altitudeFt || 35000) : 'Look up.'}
              </div>
              <div style={{ marginTop: 8, fontSize: 13, color: 'rgba(255,255,255,0.5)' }}>
                {aircraft[0] ? `${aircraft[0].callsign} overhead in ${aircraft[0].overheadMin || aircraft[0].timeLabel} min` : ''}
              </div>
            </div>

            <div style={{
              padding: '22px 24px', borderRadius: 20,
              background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)',
            }}>
              <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.55)', fontWeight: 600, letterSpacing: 1.4, marginBottom: 12 }}>QUICK SETTINGS</div>
              {[
                ['Radius', '50 km'],
                ['Min altitude', '10,000 ft'],
                ['Units', 'Metric'],
              ].map(([k, v]) => (
                <div key={k} style={{
                  display: 'flex', justifyContent: 'space-between',
                  padding: '7px 0',
                  fontSize: 14, color: 'rgba(255,255,255,0.65)',
                }}>
                  <span>{k}</span>
                  <span style={{ color: '#fff', fontWeight: 500 }}>{v}</span>
                </div>
              ))}
            </div>

            <div style={{
              padding: '16px 20px', borderRadius: 16,
              background: 'rgba(155,125,255,0.06)', border: '1px solid rgba(155,125,255,0.15)',
              fontSize: 14, color: 'rgba(255,255,255,0.65)', lineHeight: 1.5,
            }}>
              <span style={{ color: '#9B7DFF', fontWeight: 600 }}>Tip:</span> Click any aircraft to see full details and track it.
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// ── Frame chrome ─────────────────────────────────────────────
function TVChrome({ children, width = 1920, height = 1080 }) {
  const bezel = 36;
  const standW = 280;
  return (
    <div style={{
      width: width + bezel * 2, position: 'relative',
      display: 'flex', flexDirection: 'column', alignItems: 'center',
    }}>
      <div style={{
        width: width + bezel * 2, height: height + bezel * 2,
        background: 'linear-gradient(180deg, #1a1a1a, #0a0a0a)',
        borderRadius: 22,
        padding: bezel, boxSizing: 'border-box',
        boxShadow: '0 40px 100px rgba(0,0,0,0.6), inset 0 0 0 1px rgba(255,255,255,0.04)',
      }}>
        <div style={{ width: '100%', height: '100%', borderRadius: 4, overflow: 'hidden' }}>
          {children}
        </div>
      </div>
      <div style={{
        width: standW, height: 50,
        background: 'linear-gradient(180deg, #2a2a2a, #161616)',
        borderRadius: '0 0 24px 24px',
        marginTop: -2,
      }}/>
      <div style={{
        width: standW + 100, height: 12,
        background: 'linear-gradient(180deg, #1a1a1a, #0a0a0a)',
        borderRadius: 6,
      }}/>
    </div>
  );
}

function SmartDisplayChrome({ children, width = 1024, height = 600 }) {
  const bezel = 22;
  return (
    <div style={{
      width: width + bezel * 2,
      display: 'flex', flexDirection: 'column', alignItems: 'center',
    }}>
      <div style={{
        width: width + bezel * 2, height: height + bezel * 2,
        background: 'linear-gradient(160deg, #e8e8e8, #c4c4c4)',
        borderRadius: 24,
        padding: bezel, boxSizing: 'border-box',
        boxShadow: '0 30px 80px rgba(0,0,0,0.4), inset 0 0 0 1px rgba(0,0,0,0.06)',
      }}>
        <div style={{ width: '100%', height: '100%', borderRadius: 8, overflow: 'hidden' }}>
          {children}
        </div>
      </div>
      <div style={{
        width: width + bezel * 2 - 80, height: 70,
        marginTop: -10, zIndex: -1,
        background: 'linear-gradient(160deg, #d0d0d0, #a8a8a8)',
        borderRadius: '0 0 32px 32px',
      }}/>
    </div>
  );
}

// iOS home-screen background container
function HomeScreen({ children, label }) {
  return (
    <div style={{
      width: 402, minHeight: 580,
      background: 'linear-gradient(180deg, #1a2845 0%, #3a1f4a 50%, #5a1a2e 100%)',
      borderRadius: 0,
      padding: '60px 22px 30px', boxSizing: 'border-box',
      position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 24,
    }}>
      <div style={{
        position: 'absolute', inset: 0,
        background: 'radial-gradient(ellipse at 30% 20%, rgba(255,255,255,0.06), transparent 50%), radial-gradient(ellipse at 70% 80%, rgba(0,0,0,0.2), transparent 50%)',
        pointerEvents: 'none',
      }}/>
      <div style={{
        position: 'absolute', top: 16, left: 0, right: 0,
        display: 'flex', justifyContent: 'space-between',
        padding: '0 30px', color: '#fff', fontSize: 17, fontWeight: 600,
        fontVariantNumeric: 'tabular-nums',
      }}>
        <span>9:41</span>
        <span style={{ fontSize: 14, opacity: 0.85 }}>●●● ⌧</span>
      </div>
      {label && (
        <div style={{
          position: 'absolute', top: 50, left: 0, right: 0,
          textAlign: 'center', color: 'rgba(255,255,255,0.7)',
          fontSize: 12, fontWeight: 600, letterSpacing: 1,
        }}>{label}</div>
      )}
      <div style={{ position: 'relative', marginTop: 24 }}>{children}</div>
    </div>
  );
}

Object.assign(window, {
  MiniCompass,
  WidgetSmall, WidgetMedium, WidgetLarge,
  SmartDisplay, SmartDisplayChrome,
  TVView, TVChrome,
  HomeScreen,
});

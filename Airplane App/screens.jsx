// SkyLook screens — onboarding + main + filter + detail + settings + track

const C = {
  bg: '#0A0F1A',
  bg2: '#0E1421',
  ink: '#FFFFFF',
  ink2: 'rgba(255,255,255,0.65)',
  ink3: 'rgba(255,255,255,0.42)',
  card: 'rgba(255,255,255,0.06)',
  cardBorder: 'rgba(255,255,255,0.08)',
  accent: '#5BB3FF',
  blue: '#5BB3FF',
  violet: '#9B7DFF',
  red: '#FF5B7A',
  green: '#5BFF9B',
};

// ── shared bits ──────────────────────────────────────────────
function ScreenBG({ children, gradient = true }) {
  return (
    <div style={{
      position: 'absolute', inset: 0,
      background: C.bg,
      overflow: 'hidden',
    }}>
      {gradient && (
        <>
          <div style={{
            position: 'absolute', top: -120, left: -80, width: 360, height: 360,
            borderRadius: '50%', background: 'radial-gradient(circle, rgba(91,179,255,0.18) 0%, transparent 65%)',
            filter: 'blur(8px)',
          }} />
          <div style={{
            position: 'absolute', bottom: -120, right: -80, width: 360, height: 360,
            borderRadius: '50%', background: 'radial-gradient(circle, rgba(255,91,122,0.12) 0%, transparent 65%)',
            filter: 'blur(8px)',
          }} />
        </>
      )}
      {children}
    </div>
  );
}

function Btn({ children, primary, onClick, style = {}, disabled }) {
  return (
    <button onClick={disabled ? undefined : onClick}
      style={{
        height: 52, width: '100%', borderRadius: 16,
        border: 'none', cursor: disabled ? 'default' : 'pointer',
        fontSize: 17, fontWeight: 600, letterSpacing: -0.2,
        fontFamily: '-apple-system, system-ui',
        background: primary
          ? (disabled ? 'rgba(91,179,255,0.3)' : C.accent)
          : 'rgba(255,255,255,0.08)',
        color: primary ? '#02121F' : C.ink,
        opacity: disabled ? 0.5 : 1,
        ...style,
      }}>
      {children}
    </button>
  );
}

function StatusPill({ children, dark = true }) {
  return (
    <div style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      padding: '6px 14px', borderRadius: 9999,
      background: 'rgba(255,255,255,0.08)',
      border: '1px solid rgba(255,255,255,0.05)',
      backdropFilter: 'blur(16px)',
      fontSize: 13, color: C.ink2, fontWeight: 500,
    }}>{children}</div>
  );
}

function HeartIcon({ filled, color = C.blue, size = 16 }) {
  if (filled) return (
    <svg width={size} height={size} viewBox="0 0 16 16" fill={color}>
      <path d="M8 14l-1-1C3 9.5 1 7.5 1 5.5 1 3.6 2.6 2 4.5 2c1 0 2 .5 3.5 2C9.5 2.5 10.5 2 11.5 2 13.4 2 15 3.6 15 5.5c0 2-2 4-6 7.5l-1 1z"/>
    </svg>
  );
  return (
    <svg width={size} height={size} viewBox="0 0 16 16" fill="none">
      <path d="M8 14l-1-1C3 9.5 1 7.5 1 5.5 1 3.6 2.6 2 4.5 2c1 0 2 .5 3.5 2C9.5 2.5 10.5 2 11.5 2 13.4 2 15 3.6 15 5.5c0 2-2 4-6 7.5l-1 1z"
        stroke="rgba(255,255,255,0.5)" strokeWidth="1.5" strokeLinejoin="round"/>
    </svg>
  );
}

// ── Welcome ──────────────────────────────────────────────────
function ScreenWelcome({ onNext }) {
  return (
    <ScreenBG>
      <div style={{
        position: 'absolute', inset: 0,
        display: 'flex', flexDirection: 'column',
        padding: '120px 32px 48px', boxSizing: 'border-box',
      }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flex: 1, justifyContent: 'center' }}>
          <div style={{ position: 'relative', width: 200, height: 200, marginBottom: 32 }}>
            {[1, 0.78, 0.56].map((s, i) => (
              <div key={i} style={{
                position: 'absolute', inset: `${(1 - s) * 50}%`,
                borderRadius: '50%',
                background: 'conic-gradient(from 0deg, #5BB3FF, #9B7DFF, #FF5B7A, #9B7DFF, #5BB3FF)',
                WebkitMask: 'radial-gradient(circle, transparent 60%, #000 61%, #000 67%, transparent 68%)',
                mask: 'radial-gradient(circle, transparent 60%, #000 61%, #000 67%, transparent 68%)',
                opacity: 0.4 + i * 0.15,
              }} />
            ))}
            <div style={{
              position: 'absolute', left: '50%', top: '50%',
              transform: 'translate(-50%, -50%)',
            }}>
              <PlaneGlyph size={42} color="#fff" />
            </div>
          </div>
          <div style={{
            fontSize: 44, fontWeight: 700, color: C.ink,
            letterSpacing: -1.2, lineHeight: 1.02,
          }}>SkyLook</div>
          <div style={{
            marginTop: 16, fontSize: 17, color: C.ink2,
            textAlign: 'center', lineHeight: 1.4, maxWidth: 280,
          }}>
            Know what's above you.<br/>In real time.
          </div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Btn primary onClick={onNext}>Get started</Btn>
          <div style={{ fontSize: 12, color: C.ink3, textAlign: 'center' }}>
            Uses ADS-B. Your location stays on device.
          </div>
        </div>
      </div>
    </ScreenBG>
  );
}

// ── Permission: Location ─────────────────────────────────────
function ScreenLocation({ onAllow, onSkip }) {
  return (
    <ScreenBG>
      <div style={{
        position: 'absolute', inset: 0,
        display: 'flex', flexDirection: 'column',
        padding: '110px 28px 36px', boxSizing: 'border-box',
      }}>
        <StepDots step={0} total={4} />
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <div style={{
            width: 80, height: 80, borderRadius: 22,
            background: 'linear-gradient(135deg, #5BB3FF, #7A9BFF)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            marginBottom: 28,
            boxShadow: '0 8px 24px rgba(91,179,255,0.3)',
          }}>
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
              <path d="M12 22s8-9 8-14a8 8 0 10-16 0c0 5 8 14 8 14z" stroke="#fff" strokeWidth="2" strokeLinejoin="round"/>
              <circle cx="12" cy="8" r="2.5" stroke="#fff" strokeWidth="2"/>
            </svg>
          </div>
          <div style={{ fontSize: 32, fontWeight: 700, color: C.ink, letterSpacing: -0.7, lineHeight: 1.1 }}>
            Allow your location
          </div>
          <div style={{ marginTop: 14, fontSize: 16, color: C.ink2, lineHeight: 1.45 }}>
            We need your position to know which aircraft are overhead. Your location is processed on your device and never sent to a server.
          </div>
          <div style={{ marginTop: 28, display: 'flex', flexDirection: 'column', gap: 10 }}>
            <Feature icon={<DotI c={C.green}/>} label="Used only while the app is open"/>
            <Feature icon={<DotI c={C.green}/>} label="No tracking, no analytics on location"/>
            <Feature icon={<DotI c={C.green}/>} label="You can switch to manual entry anytime"/>
          </div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <Btn primary onClick={onAllow}>Allow precise location</Btn>
          <Btn onClick={onSkip}>Enter manually</Btn>
        </div>
      </div>
    </ScreenBG>
  );
}

function Feature({ icon, label }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      {icon}
      <div style={{ fontSize: 15, color: C.ink2 }}>{label}</div>
    </div>
  );
}
function DotI({ c }) {
  return (
    <div style={{ width: 22, height: 22, borderRadius: '50%',
      background: 'rgba(91,255,155,0.12)', display: 'flex',
      alignItems: 'center', justifyContent: 'center' }}>
      <svg width="12" height="12" viewBox="0 0 12 12">
        <path d="M2.5 6.5l2.5 2.5 5-5" stroke={c} strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    </div>
  );
}

function StepDots({ step, total }) {
  return (
    <div style={{ display: 'flex', gap: 6, marginBottom: 0 }}>
      {Array.from({ length: total }).map((_, i) => (
        <div key={i} style={{
          height: 4, flex: 1, borderRadius: 2,
          background: i <= step ? C.accent : 'rgba(255,255,255,0.12)',
          transition: 'background 0.3s',
        }} />
      ))}
    </div>
  );
}

// ── Sensor detection ─────────────────────────────────────────
function ScreenSensorCheck({ deviceMode, onContinue, onUseManual, onRetry }) {
  const hasGyro = deviceMode === 'gyro';
  return (
    <ScreenBG>
      <div style={{
        position: 'absolute', inset: 0,
        display: 'flex', flexDirection: 'column',
        padding: '110px 28px 36px', boxSizing: 'border-box',
      }}>
        <StepDots step={1} total={4} />
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <div style={{
            position: 'relative', width: 200, height: 200,
            margin: '0 auto 32px',
          }}>
            <div style={{
              position: 'absolute', inset: 0, borderRadius: '50%',
              background: hasGyro
                ? 'conic-gradient(from 0deg, #5BB3FF, #9B7DFF, #FF5B7A, #9B7DFF, #5BB3FF)'
                : 'conic-gradient(from 0deg, rgba(255,255,255,0.15), rgba(255,255,255,0.1), rgba(255,255,255,0.15))',
              WebkitMask: 'radial-gradient(circle, transparent 64%, #000 65%, #000 71%, transparent 72%)',
              mask: 'radial-gradient(circle, transparent 64%, #000 65%, #000 71%, transparent 72%)',
              opacity: 0.85,
            }} />
            <div style={{
              position: 'absolute', left: '50%', top: '50%',
              transform: 'translate(-50%, -50%)',
              width: 100, height: 100, borderRadius: '50%',
              background: hasGyro ? 'rgba(91,255,155,0.1)' : 'rgba(255,255,255,0.04)',
              border: hasGyro ? '1px solid rgba(91,255,155,0.3)' : '1px solid rgba(255,255,255,0.08)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              {hasGyro ? (
                <svg width="44" height="44" viewBox="0 0 24 24" fill="none">
                  <path d="M5 13l4 4 10-10" stroke={C.green} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              ) : (
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="9" stroke={C.ink3} strokeWidth="1.5"/>
                  <path d="M12 8v4M12 16h.01" stroke={C.ink3} strokeWidth="2" strokeLinecap="round"/>
                </svg>
              )}
            </div>
          </div>

          <div style={{ fontSize: 28, fontWeight: 700, color: C.ink, letterSpacing: -0.6, lineHeight: 1.15 }}>
            {hasGyro ? 'Compass detected' : 'No compass found'}
          </div>
          <div style={{ marginTop: 12, fontSize: 16, color: C.ink2, lineHeight: 1.45 }}>
            {hasGyro
              ? 'Your device has a gyroscope and magnetometer. SkyLook will track your heading automatically.'
              : "We couldn't access your motion sensors. You can still use SkyLook by setting your facing direction manually."}
          </div>

          <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 8 }}>
            <SensorRow label="Gyroscope" ok={hasGyro}/>
            <SensorRow label="Magnetometer" ok={hasGyro}/>
            <SensorRow label="Accelerometer" ok={true}/>
          </div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {hasGyro ? (
            <Btn primary onClick={onContinue}>Continue</Btn>
          ) : (
            <>
              <Btn primary onClick={onUseManual}>Set heading manually</Btn>
              <Btn onClick={onRetry}>Retry sensor check</Btn>
            </>
          )}
        </div>
      </div>
    </ScreenBG>
  );
}

function SensorRow({ label, ok }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '12px 16px', borderRadius: 12,
      background: C.card, border: `1px solid ${C.cardBorder}`,
    }}>
      <div style={{ fontSize: 15, color: C.ink, fontWeight: 500 }}>{label}</div>
      <div style={{
        display: 'inline-flex', alignItems: 'center', gap: 6,
        fontSize: 13, fontWeight: 600,
        color: ok ? C.green : C.red,
      }}>
        <div style={{
          width: 7, height: 7, borderRadius: '50%',
          background: ok ? C.green : C.red,
          boxShadow: `0 0 8px ${ok ? C.green : C.red}`,
        }} />
        {ok ? 'Available' : 'Unavailable'}
      </div>
    </div>
  );
}

// ── Manual calibration ───────────────────────────────────────
function ScreenManualCalibrate({ manualHeading, onChange, onDone, onBack }) {
  const [drag, setDrag] = React.useState(false);
  const ref = React.useRef(null);

  function handle(e) {
    if (!ref.current) return;
    const r = ref.current.getBoundingClientRect();
    const cx = r.left + r.width / 2;
    const cy = r.top + r.height / 2;
    const pt = e.touches ? e.touches[0] : e;
    const dx = pt.clientX - cx;
    const dy = pt.clientY - cy;
    const a = Math.atan2(dy, dx) * 180 / Math.PI + 90;
    onChange(((a % 360) + 360) % 360);
  }

  return (
    <ScreenBG>
      <div style={{
        position: 'absolute', inset: 0,
        display: 'flex', flexDirection: 'column',
        padding: '110px 28px 36px', boxSizing: 'border-box',
      }}>
        <StepDots step={2} total={4} />
        <div style={{ fontSize: 28, fontWeight: 700, color: C.ink, letterSpacing: -0.6, marginTop: 28, lineHeight: 1.15 }}>
          Point yourself north
        </div>
        <div style={{ marginTop: 10, fontSize: 15, color: C.ink2, lineHeight: 1.45 }}>
          Drag the ring so the <span style={{ color: C.blue, fontWeight: 600 }}>N</span> marker matches the direction you're facing.
        </div>

        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div ref={ref}
            onMouseDown={(e) => { setDrag(true); handle(e); }}
            onMouseMove={(e) => drag && handle(e)}
            onMouseUp={() => setDrag(false)}
            onMouseLeave={() => setDrag(false)}
            onTouchStart={(e) => { setDrag(true); handle(e); }}
            onTouchMove={(e) => drag && handle(e)}
            onTouchEnd={() => setDrag(false)}
            style={{ position: 'relative', width: 260, height: 260, cursor: drag ? 'grabbing' : 'grab', touchAction: 'none' }}>
            <CompassRing size={260} heading={manualHeading} />
            <div style={{
              position: 'absolute', left: '50%', top: -22,
              transform: 'translateX(-50%)',
              display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
            }}>
              <div style={{ fontSize: 10, color: C.ink3, fontWeight: 600, letterSpacing: 1 }}>FACING</div>
              <svg width="14" height="10" viewBox="0 0 14 10">
                <path d="M7 10 L0 0 L14 0 Z" fill="#fff" opacity="0.85"/>
              </svg>
            </div>
            <div style={{
              position: 'absolute', left: '50%', top: '50%',
              transform: 'translate(-50%, 30px)',
              fontVariantNumeric: 'tabular-nums',
              fontSize: 13, color: C.ink2, fontWeight: 500, letterSpacing: 0.5,
            }}>
              {Math.round(manualHeading)}° {compassPoint(manualHeading)}
            </div>
          </div>
        </div>

        <div style={{ marginBottom: 12, fontSize: 12, color: C.ink3, textAlign: 'center' }}>
          You can recalibrate any time from Settings.
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <Btn primary onClick={onDone}>Save heading</Btn>
          <Btn onClick={onBack}>Back</Btn>
        </div>
      </div>
    </ScreenBG>
  );
}

function compassPoint(deg) {
  const pts = ['N','NE','E','SE','S','SW','W','NW'];
  return pts[Math.round(((deg % 360) / 45)) % 8];
}

// ── Tutorial ─────────────────────────────────────────────────
function ScreenTutorial({ onDone }) {
  const steps = [
    { n: 1, t: 'Scan', d: 'See every aircraft within your radius at a glance on the radar.' },
    { n: 2, t: 'Tap', d: 'Tap any aircraft dot to instantly focus on it and bring up flight info.' },
    { n: 3, t: 'Lock on', d: 'Tap again or select to lock on — see full flight details.' },
    { n: 4, t: 'Track', d: 'Pin a flight to follow it. Perfect for tracking family on a trip.' },
  ];
  return (
    <ScreenBG>
      <div style={{
        position: 'absolute', inset: 0,
        display: 'flex', flexDirection: 'column',
        padding: '110px 28px 36px', boxSizing: 'border-box',
      }}>
        <StepDots step={3} total={4} />
        <div style={{ fontSize: 28, fontWeight: 700, color: C.ink, letterSpacing: -0.6, marginTop: 28, lineHeight: 1.15 }}>
          How it works
        </div>
        <div style={{ marginTop: 10, fontSize: 15, color: C.ink2, lineHeight: 1.45 }}>
          Tap, select, track. From overview to full flight detail in seconds.
        </div>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 14 }}>
          {steps.map(s => (
            <div key={s.n} style={{
              display: 'flex', gap: 14, padding: 14, borderRadius: 16,
              background: C.card, border: `1px solid ${C.cardBorder}`,
            }}>
              <div style={{
                width: 36, height: 36, borderRadius: 10,
                background: 'linear-gradient(135deg, #5BB3FF, #9B7DFF)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                flexShrink: 0,
                fontSize: 16, fontWeight: 700, color: '#02121F',
              }}>{s.n}</div>
              <div>
                <div style={{ fontSize: 16, fontWeight: 600, color: C.ink }}>{s.t}</div>
                <div style={{ marginTop: 2, fontSize: 13, color: C.ink2, lineHeight: 1.4 }}>{s.d}</div>
              </div>
            </div>
          ))}
        </div>
        <Btn primary onClick={onDone}>Start scanning</Btn>
      </div>
    </ScreenBG>
  );
}

// ── Tracked flights strip ────────────────────────────────────
function TrackedStrip({ aircraft, trackedFlights, awayTracked = [], onSelect }) {
  const nearby = aircraft.filter(p => trackedFlights.includes(p.id));
  if (!nearby.length && !awayTracked.length) return null;
  return (
    <div style={{
      position: 'absolute', bottom: 120, left: 16, right: 16, zIndex: 5,
    }}>
      <div style={{
        padding: '10px 12px 10px 14px', borderRadius: 16,
        background: 'rgba(6,9,15,0.85)', backdropFilter: 'blur(24px)',
        border: '1px solid rgba(91,179,255,0.15)',
        boxShadow: '0 4px 24px rgba(0,0,0,0.3)',
      }}>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8,
        }}>
          <HeartIcon filled size={10} color={C.blue} />
          <div style={{ fontSize: 9, color: C.blue, fontWeight: 700, letterSpacing: 1 }}>TRACKING</div>
        </div>
        <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 2 }}>
          {nearby.map(p => (
            <div key={p.id} onClick={() => onSelect && onSelect(p.id)} style={{
              display: 'flex', alignItems: 'center', gap: 7, cursor: 'pointer',
              padding: '6px 10px', borderRadius: 10, flexShrink: 0,
              background: 'rgba(91,179,255,0.08)',
              border: '1px solid rgba(91,179,255,0.18)',
              transition: 'background 0.15s',
            }}>
              <PlaneGlyph size={11} color={C.blue} type={silhouetteType(p.model, p.isMilitary)} />
              <div>
                <div style={{ fontSize: 12, fontWeight: 700, color: C.ink, letterSpacing: -0.1 }}>{p.callsign}</div>
                <div style={{ fontSize: 10, color: C.ink3, fontVariantNumeric: 'tabular-nums', marginTop: 1 }}>
                  {p.overheadMin}m · {p.toCode}
                </div>
              </div>
            </div>
          ))}
          {awayTracked.map(f => (
            <div key={f.id} style={{
              display: 'flex', alignItems: 'center', gap: 7,
              padding: '6px 10px', borderRadius: 10, flexShrink: 0,
              background: 'rgba(255,255,255,0.03)',
              border: '1px solid rgba(255,255,255,0.07)',
            }}>
              <PlaneGlyph size={11} color='rgba(255,255,255,0.35)' />
              <div>
                <div style={{ fontSize: 12, fontWeight: 700, color: 'rgba(255,255,255,0.5)', letterSpacing: -0.1 }}>{f.callsign}</div>
                <div style={{ fontSize: 10, color: C.ink3, marginTop: 1 }}>
                  Over {f.currentCity} · {f.toCode}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── Main radar ───────────────────────────────────────────────
function ScreenMain({
  state, // 'idle' | 'focused' | 'locked'
  heading, aircraft, focused, radiusKm,
  onAircraftTap, onOpenFilter, onOpenDetail, onRecenter, onOpenTrack, deviceMode,
  trackedFlights = [], isTracked, onTrack, onUntrack, awayTracked = [],
}) {
  const focusedPlane = aircraft.find(p => p.id === focused);
  const visible = aircraft.filter(p => p.distance <= radiusKm).length;
  const currentlyTracked = isTracked ? isTracked(focused) : false;

  return (
    <ScreenBG gradient={false}>
      {/* count pill */}
      <div style={{
        position: 'absolute', top: 64, left: '50%',
        transform: 'translateX(-50%)', zIndex: 5,
      }}>
        <StatusPill>
          <span style={{
            width: 6, height: 6, borderRadius: '50%',
            background: C.green, boxShadow: `0 0 8px ${C.green}`,
          }} />
          {visible} aircraft nearby
        </StatusPill>
      </div>

      {/* device-mode chip */}
      {deviceMode === 'manual' && (
        <div style={{ position: 'absolute', top: 110, left: '50%', transform: 'translateX(-50%)', zIndex: 5 }}>
          <StatusPill>
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M2 6h8M6 2l4 4-4 4" stroke={C.ink2} strokeWidth="1.5" strokeLinecap="round"/>
            </svg>
            Manual heading · {Math.round(heading)}°
          </StatusPill>
        </div>
      )}

      {/* compass */}
      <div style={{
        position: 'absolute', left: '50%', top: '50%',
        transform: 'translate(-50%, -50%)',
      }}>
        <Compass
          size={340}
          heading={heading}
          aircraft={aircraft}
          radiusKm={radiusKm}
          focused={state === 'idle' ? null : focused}
          locked={state === 'locked'}
          showBeam={state === 'focused' || state === 'locked'}
          onAircraftTap={onAircraftTap}
          trackedFlights={trackedFlights}
        />
      </div>

      {/* center info card — focused or locked */}
      {(state === 'focused' || state === 'locked') && focusedPlane && (
        <div style={{
          position: 'absolute', left: '50%', top: '54%',
          transform: 'translate(-50%, 60px)',
          textAlign: 'center', pointerEvents: state === 'locked' ? 'auto' : 'none',
          minWidth: 220,
        }}>
          {/* callsign + track toggle */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
            <div style={{ fontSize: 26, fontWeight: 700, color: C.ink, letterSpacing: -0.4 }}>
              {focusedPlane.callsign}
            </div>
            {state === 'locked' && (
              <button
                onClick={() => currentlyTracked ? onUntrack(focused) : onTrack(focused)}
                style={{
                  width: 32, height: 32, borderRadius: '50%', border: 'none', cursor: 'pointer',
                  background: currentlyTracked ? 'rgba(91,179,255,0.2)' : 'rgba(255,255,255,0.08)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  transition: 'background 0.2s',
                }}>
                <HeartIcon filled={currentlyTracked} size={14} />
              </button>
            )}
          </div>
          <div style={{ marginTop: 2, fontSize: 14, color: C.ink2 }}>
            {focusedPlane.model}
          </div>
          <div style={{
            marginTop: 6, fontSize: 13, color: C.ink2,
            fontVariantNumeric: 'tabular-nums',
            display: 'flex', justifyContent: 'center', gap: 8,
          }}>
            <span>{focusedPlane.altitudeFt.toLocaleString()} ft</span>
            <span style={{ color: C.ink3 }}>•</span>
            <span>{focusedPlane.speedMph} mph</span>
          </div>
          {state === 'locked' && (
            <>
              <div style={{ marginTop: 5, fontSize: 12, color: C.blue, fontWeight: 500, fontStyle: 'italic' }}>
                {humanDirAbsolute(focusedPlane.bearing, focusedPlane.altitudeFt)}
              </div>
              <div style={{ marginTop: 3, fontSize: 13, color: C.blue, fontWeight: 600 }}>
                {focusedPlane.overheadMin} min overhead
              </div>
            </>
          )}
        </div>
      )}

      {/* idle hint */}
      {state === 'idle' && (
        <div style={{
          position: 'absolute', left: '50%', top: '54%',
          transform: 'translate(-50%, 60px)',
          textAlign: 'center',
        }}>
          <div style={{ fontSize: 14, color: C.ink2, fontWeight: 500 }}>
            Tap any aircraft to focus
          </div>
          <div style={{ marginTop: 4, fontSize: 12, color: C.ink3 }}>
            {radiusKm} km radius
          </div>
        </div>
      )}

      {/* tracked flights strip */}
      {trackedFlights.length > 0 && (
        <TrackedStrip
          aircraft={aircraft}
          trackedFlights={trackedFlights}
          awayTracked={awayTracked}
          onSelect={(id) => onAircraftTap && onAircraftTap(id)}
        />
      )}

      {/* bottom action row */}
      <div style={{
        position: 'absolute', bottom: 56, left: 20, right: 20,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      }}>
        <BottomBtn onClick={onRecenter} aria-label="Settings">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M3 11l18-8-8 18-2-8-8-2z" stroke="#fff" strokeWidth="1.8" strokeLinejoin="round"/>
          </svg>
        </BottomBtn>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          {state === 'locked' && (
            <button onClick={onOpenDetail} style={{
              height: 52, padding: '0 28px', borderRadius: 26,
              border: 'none', cursor: 'pointer',
              background: C.accent, color: '#02121F',
              fontSize: 15, fontWeight: 600,
              display: 'flex', alignItems: 'center', gap: 8,
              boxShadow: '0 6px 20px rgba(91,179,255,0.4)',
              fontFamily: '-apple-system, system-ui',
            }}>
              Full details
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M3 7h8M7 3l4 4-4 4" stroke="#02121F" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </button>
          )}
          <BottomBtn onClick={onOpenTrack} aria-label="Track flight">
            <HeartIcon filled={false} size={18} />
          </BottomBtn>
        </div>
        <BottomBtn onClick={onOpenFilter} aria-label="Filter">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M4 6h16M7 12h10M10 18h4" stroke="#fff" strokeWidth="1.8" strokeLinecap="round"/>
            <circle cx="9" cy="6" r="2" stroke="#fff" strokeWidth="1.6" fill={C.bg}/>
            <circle cx="15" cy="12" r="2" stroke="#fff" strokeWidth="1.6" fill={C.bg}/>
            <circle cx="11" cy="18" r="2" stroke="#fff" strokeWidth="1.6" fill={C.bg}/>
          </svg>
        </BottomBtn>
      </div>
    </ScreenBG>
  );
}

function BottomBtn({ children, onClick }) {
  return (
    <button onClick={onClick} style={{
      width: 52, height: 52, borderRadius: 16,
      border: '1px solid rgba(255,255,255,0.08)',
      background: 'rgba(255,255,255,0.06)',
      backdropFilter: 'blur(16px)',
      cursor: 'pointer',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>{children}</button>
  );
}

// ── Filter sheet ─────────────────────────────────────────────
function ScreenFilter({ radiusKm, onChange, onClose, minAlt, onMinAltChange, types, onTypesToggle }) {
  return (
    <ScreenBG gradient={false}>
      <div style={{
        position: 'absolute', inset: 0,
        background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)',
      }} onClick={onClose}/>

      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        background: '#11161F', borderTopLeftRadius: 28, borderTopRightRadius: 28,
        padding: '14px 24px 36px',
        border: '1px solid rgba(255,255,255,0.06)',
        borderBottom: 'none',
      }}>
        <div style={{
          width: 38, height: 5, borderRadius: 3,
          background: 'rgba(255,255,255,0.18)',
          margin: '0 auto 18px',
        }}/>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          marginBottom: 6,
        }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: C.ink, letterSpacing: -0.4 }}>Filter</div>
          <button onClick={onClose} style={{
            border: 'none', background: 'rgba(255,255,255,0.08)',
            width: 32, height: 32, borderRadius: 16, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <svg width="14" height="14" viewBox="0 0 14 14"><path d="M1 1l12 12M13 1L1 13" stroke="#fff" strokeWidth="2" strokeLinecap="round"/></svg>
          </button>
        </div>

        <div style={{ marginTop: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
            <div style={{ fontSize: 14, color: C.ink2, fontWeight: 500, textTransform: 'uppercase', letterSpacing: 0.6 }}>Search radius</div>
            <div style={{ fontSize: 28, fontWeight: 700, color: C.ink, letterSpacing: -0.4, fontVariantNumeric: 'tabular-nums' }}>
              {radiusKm} <span style={{ fontSize: 14, color: C.ink2, fontWeight: 500 }}>km</span>
            </div>
          </div>
          <RadiusSlider value={radiusKm} onChange={onChange} />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: C.ink3, marginTop: 4 }}>
            <span>5 km</span>
            <span>50</span>
            <span>100</span>
            <span>200 km</span>
          </div>
          <div style={{ marginTop: 14, padding: 12, borderRadius: 12, background: 'rgba(91,179,255,0.08)', border: '1px solid rgba(91,179,255,0.18)', display: 'flex', gap: 10 }}>
            <svg width="16" height="16" viewBox="0 0 16 16" style={{ flexShrink: 0, marginTop: 1 }}>
              <circle cx="8" cy="8" r="7" stroke={C.blue} strokeWidth="1.4" fill="none"/>
              <path d="M8 4v4M8 11h.01" stroke={C.blue} strokeWidth="1.6" strokeLinecap="round"/>
            </svg>
            <div style={{ fontSize: 12, color: C.ink2, lineHeight: 1.4 }}>
              {radiusKm <= 30
                ? 'Tight radius — only see aircraft directly overhead. Good for plane spotting.'
                : radiusKm >= 100
                ? 'Wide radius — see distant aircraft. Useful at altitude or watching for approaching flights.'
                : 'Balanced — covers a typical metro flight corridor.'}
            </div>
          </div>
        </div>

        <div style={{ marginTop: 26 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
            <div style={{ fontSize: 14, color: C.ink2, fontWeight: 500, textTransform: 'uppercase', letterSpacing: 0.6 }}>Min altitude</div>
            <div style={{ fontSize: 17, fontWeight: 600, color: C.ink, fontVariantNumeric: 'tabular-nums' }}>
              {minAlt.toLocaleString()} <span style={{ fontSize: 12, color: C.ink2, fontWeight: 500 }}>ft</span>
            </div>
          </div>
          <SimpleSlider min={0} max={40000} step={500} value={minAlt} onChange={onMinAltChange} />
        </div>

        <div style={{ marginTop: 24 }}>
          <div style={{ fontSize: 14, color: C.ink2, fontWeight: 500, textTransform: 'uppercase', letterSpacing: 0.6, marginBottom: 10 }}>Aircraft types</div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {[
              { id: 'commercial', l: 'Commercial' },
              { id: 'private', l: 'Private' },
              { id: 'cargo', l: 'Cargo' },
              { id: 'mil', l: 'Military' },
              { id: 'heli', l: 'Helicopter' },
            ].map(t => (
              <Chip key={t.id} on={types.includes(t.id)} onClick={() => onTypesToggle(t.id)}>{t.l}</Chip>
            ))}
          </div>
        </div>

        <div style={{ marginTop: 28 }}>
          <Btn primary onClick={onClose}>Apply</Btn>
        </div>
      </div>
    </ScreenBG>
  );
}

function Chip({ children, on, onClick }) {
  return (
    <button onClick={onClick} style={{
      padding: '8px 14px', borderRadius: 9999,
      border: on ? `1px solid ${C.accent}` : '1px solid rgba(255,255,255,0.1)',
      background: on ? 'rgba(91,179,255,0.18)' : 'rgba(255,255,255,0.04)',
      color: on ? C.blue : C.ink2,
      fontSize: 13, fontWeight: 600, cursor: 'pointer',
      fontFamily: '-apple-system, system-ui',
    }}>{children}</button>
  );
}

function RadiusSlider({ value, onChange }) {
  const min = 5, max = 200;
  const pct = ((value - min) / (max - min)) * 100;
  return (
    <div style={{ position: 'relative', height: 56, marginTop: 14 }}>
      <input type="range" min={min} max={max} step={1} value={value}
        onChange={(e) => onChange(parseInt(e.target.value))}
        style={{
          position: 'absolute', inset: 0, width: '100%', height: '100%',
          opacity: 0, cursor: 'pointer', margin: 0,
        }} />
      <div style={{
        position: 'absolute', left: 0, right: 0, top: 26, height: 4, borderRadius: 2,
        background: 'rgba(255,255,255,0.1)',
      }}/>
      <div style={{
        position: 'absolute', left: 0, top: 26, height: 4, borderRadius: 2,
        width: `${pct}%`,
        background: `linear-gradient(90deg, ${C.blue}, ${C.violet})`,
      }}/>
      <div style={{
        position: 'absolute', left: `${pct}%`, top: 28,
        transform: 'translate(-50%, -50%)',
        width: 28, height: 28, borderRadius: '50%',
        background: '#fff',
        boxShadow: '0 2px 10px rgba(0,0,0,0.5), 0 0 0 3px rgba(91,179,255,0.2)',
      }}/>
      {[5, 30, 50, 100, 150, 200].map(v => {
        const p = ((v - min) / (max - min)) * 100;
        return (
          <div key={v} style={{
            position: 'absolute', left: `${p}%`, top: 36,
            width: 1, height: 6,
            background: 'rgba(255,255,255,0.2)',
            transform: 'translateX(-50%)',
          }}/>
        );
      })}
    </div>
  );
}

function SimpleSlider({ min, max, step, value, onChange }) {
  const pct = ((value - min) / (max - min)) * 100;
  return (
    <div style={{ position: 'relative', height: 32, marginTop: 10 }}>
      <input type="range" min={min} max={max} step={step} value={value}
        onChange={(e) => onChange(parseInt(e.target.value))}
        style={{
          position: 'absolute', inset: 0, width: '100%', height: '100%',
          opacity: 0, cursor: 'pointer', margin: 0,
        }} />
      <div style={{ position: 'absolute', left: 0, right: 0, top: 14, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.1)' }}/>
      <div style={{ position: 'absolute', left: 0, top: 14, height: 4, borderRadius: 2, width: `${pct}%`, background: C.blue }}/>
      <div style={{ position: 'absolute', left: `${pct}%`, top: 16, transform: 'translate(-50%, -50%)', width: 22, height: 22, borderRadius: '50%', background: '#fff', boxShadow: '0 2px 8px rgba(0,0,0,0.4)' }}/>
    </div>
  );
}

// ── Lock-on full detail ──────────────────────────────────────
function ScreenDetail({ aircraft, onClose, trackedFlights = [], isTracked, onTrack, onUntrack }) {
  if (!aircraft) return null;
  const tracked = isTracked ? isTracked(aircraft.id) : false;

  return (
    <ScreenBG gradient={false}>
      {/* header */}
      <div style={{
        position: 'absolute', top: 60, left: 20, right: 20, zIndex: 5,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      }}>
        <button onClick={onClose} style={{
          width: 40, height: 40, borderRadius: 20,
          border: 'none', cursor: 'pointer',
          background: 'rgba(255,255,255,0.08)', backdropFilter: 'blur(16px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="14" height="14" viewBox="0 0 14 14"><path d="M9 1L3 7l6 6" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none"/></svg>
        </button>
        <StatusPill>
          <span style={{ width: 6, height: 6, borderRadius: '50%', background: C.green, boxShadow: `0 0 8px ${C.green}` }} />
          Live · ADS-B
        </StatusPill>
        {/* Track / untrack button */}
        <button onClick={() => tracked ? onUntrack(aircraft.id) : onTrack(aircraft.id)} style={{
          width: 40, height: 40, borderRadius: 20,
          border: 'none', cursor: 'pointer',
          background: tracked ? 'rgba(91,179,255,0.2)' : 'rgba(255,255,255,0.08)',
          backdropFilter: 'blur(16px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          transition: 'background 0.2s',
          boxShadow: tracked ? '0 0 16px rgba(91,179,255,0.3)' : 'none',
        }}>
          <HeartIcon filled={tracked} size={16} color={tracked ? C.blue : undefined} />
        </button>
      </div>

      <div style={{
        position: 'absolute', top: 120, left: 0, right: 0, bottom: 36,
        padding: '0 24px', overflowY: 'auto',
      }}>
        {/* hero */}
        <div style={{ textAlign: 'center', marginBottom: 20 }}>
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: 6,
            padding: '4px 10px', borderRadius: 9999,
            background: aircraft.isMilitary ? 'rgba(245,168,58,0.15)' : aircraft.interesting ? 'rgba(155,125,255,0.15)' : 'rgba(91,179,255,0.15)',
            fontSize: 11, fontWeight: 700,
            color: aircraft.isMilitary ? '#F5A83A' : aircraft.interesting ? C.violet : C.blue,
            letterSpacing: 1,
          }}>
            <PlaneGlyph size={12}
              color={aircraft.isMilitary ? '#F5A83A' : aircraft.interesting ? C.violet : C.blue}
              type={silhouetteType(aircraft.model, aircraft.isMilitary)} />
            {aircraft.isMilitary ? 'MILITARY' : aircraft.interesting ? aircraft.model : 'LOCKED ON'}
          </div>
          <div style={{ marginTop: 12, fontSize: 40, fontWeight: 700, color: C.ink, letterSpacing: -1 }}>
            {aircraft.callsign}
          </div>
          <div style={{ marginTop: 2, fontSize: 15, color: C.ink2 }}>
            {aircraft.airline}
          </div>
        </div>

        {/* where to look */}
        <div style={{
          padding: '14px 16px', borderRadius: 14,
          background: 'rgba(91,179,255,0.07)', border: '1px solid rgba(91,179,255,0.2)',
          marginBottom: 12, display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <div style={{
            width: 36, height: 36, borderRadius: '50%',
            background: 'rgba(91,179,255,0.12)', border: '1px solid rgba(91,179,255,0.25)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="9" stroke={C.blue} strokeWidth="2"/>
              <circle cx="12" cy="12" r="3" fill={C.blue}/>
              <path d="M12 3v2M12 19v2M3 12h2M19 12h2" stroke={C.blue} strokeWidth="2" strokeLinecap="round"/>
            </svg>
          </div>
          <div>
            <div style={{ fontSize: 10, color: C.blue, fontWeight: 700, letterSpacing: 1 }}>WHERE TO LOOK</div>
            <div style={{ marginTop: 3, fontSize: 15, fontWeight: 600, color: C.ink, letterSpacing: -0.2 }}>
              {humanDirAbsolute(aircraft.bearing, aircraft.altitudeFt)}
            </div>
          </div>
        </div>

        {/* route */}
        <div style={{
          padding: '20px 18px', borderRadius: 18,
          background: C.card, border: `1px solid ${C.cardBorder}`,
          marginBottom: 12,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ fontSize: 11, color: C.ink3, fontWeight: 600, letterSpacing: 0.8 }}>FROM</div>
              <div style={{ marginTop: 4, fontSize: 22, fontWeight: 700, color: C.ink, letterSpacing: -0.4 }}>{aircraft.fromCode}</div>
              <div style={{ marginTop: 2, fontSize: 12, color: C.ink2 }}>{aircraft.fromCity}</div>
            </div>
            <div style={{ flex: 1, padding: '0 14px', display: 'flex', alignItems: 'center', flexDirection: 'column', gap: 6 }}>
              <div style={{ position: 'relative', width: '100%', height: 1, background: 'rgba(255,255,255,0.15)' }}>
                <div style={{
                  position: 'absolute', left: `${aircraft.progress * 100}%`, top: '50%',
                  transform: 'translate(-50%, -50%)',
                }}>
                  <PlaneGlyph size={18} color={C.blue} rotation={90} />
                </div>
              </div>
              <div style={{
                width: `${aircraft.progress * 100}%`, height: 2, borderRadius: 1,
                background: `linear-gradient(90deg, ${C.blue}, ${C.violet})`,
                alignSelf: 'flex-start',
                marginTop: -6,
              }} />
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: 11, color: C.ink3, fontWeight: 600, letterSpacing: 0.8 }}>TO</div>
              <div style={{ marginTop: 4, fontSize: 22, fontWeight: 700, color: C.ink, letterSpacing: -0.4 }}>{aircraft.toCode}</div>
              <div style={{ marginTop: 2, fontSize: 12, color: C.ink2 }}>{aircraft.toCity}</div>
            </div>
          </div>
          <div style={{ marginTop: 10, textAlign: 'center', fontSize: 12, color: C.ink3 }}>
            {Math.round(aircraft.progress * 100)}% complete · {aircraft.status}
          </div>
        </div>

        {/* stats grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 14 }}>
          <Stat label="Aircraft" value={aircraft.model} small/>
          <Stat label="Altitude" value={`${aircraft.altitudeFt.toLocaleString()} ft`} />
          <Stat label="Speed" value={`${aircraft.speedMph} mph`} />
          <Stat label="Distance" value={`${aircraft.distance.toFixed(1)} km`} />
          <Stat label="Where to look" value={humanDirAbsolute(aircraft.bearing, aircraft.altitudeFt)} />
          <Stat label="Overhead in" value={`${aircraft.overheadMin} min`} highlight />
        </div>

        {/* aircraft backstory */}
        {(aircraft.registration || aircraft.yearBuilt) && (
          <div style={{
            padding: '16px 18px', borderRadius: 18,
            background: C.card, border: `1px solid ${C.cardBorder}`,
            marginBottom: 14,
          }}>
            <div style={{ fontSize: 10, color: C.ink3, fontWeight: 700, letterSpacing: 1, marginBottom: 10 }}>AIRCRAFT</div>
            {aircraft.registration && (
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                <span style={{ fontSize: 13, color: C.ink2 }}>Registration</span>
                <span style={{ fontSize: 13, fontWeight: 700, color: C.ink, fontVariantNumeric: 'tabular-nums' }}>{aircraft.registration}</span>
              </div>
            )}
            {aircraft.yearBuilt && (
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ fontSize: 13, color: C.ink2 }}>Built</span>
                <span style={{ fontSize: 13, fontWeight: 700, color: C.ink }}>{aircraft.yearBuilt} · {2026 - aircraft.yearBuilt} years old</span>
              </div>
            )}
          </div>
        )}

        {/* actions */}
        <div style={{ display: 'flex', gap: 10 }}>
          <SecAction
            icon={<HeartIcon filled={tracked} size={16} color={tracked ? C.blue : undefined} />}
            label={tracked ? 'Tracking' : 'Track'}
            onClick={() => tracked ? onUntrack(aircraft.id) : onTrack(aircraft.id)}
            active={tracked}
          />
          <SecAction icon={<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 2a4 4 0 014 4c0 5-4 8-4 8s-4-3-4-8a4 4 0 014-4z" stroke="#fff" strokeWidth="1.5"/><circle cx="8" cy="6" r="1.5" fill="#fff"/></svg>} label="On map"/>
          <SecAction icon={<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" stroke="#fff" strokeWidth="1.5"/><path d="M3 8c2-3 8-3 10 0" stroke="#fff" strokeWidth="1.5"/></svg>} label="Notify"/>
        </div>
      </div>
    </ScreenBG>
  );
}

function Stat({ label, value, highlight, small }) {
  return (
    <div style={{
      padding: '14px 14px',
      borderRadius: 14,
      background: highlight ? 'rgba(91,179,255,0.1)' : C.card,
      border: highlight ? '1px solid rgba(91,179,255,0.25)' : `1px solid ${C.cardBorder}`,
    }}>
      <div style={{ fontSize: 11, color: highlight ? C.blue : C.ink3, fontWeight: 600, letterSpacing: 0.5, textTransform: 'uppercase' }}>{label}</div>
      <div style={{ marginTop: 4, fontSize: small ? 14 : 18, fontWeight: 700, color: C.ink, letterSpacing: -0.3, fontVariantNumeric: 'tabular-nums' }}>{value}</div>
    </div>
  );
}

function SecAction({ icon, label, onClick, active }) {
  return (
    <button onClick={onClick} style={{
      flex: 1, padding: '14px 0', borderRadius: 14,
      border: active ? '1px solid rgba(91,179,255,0.3)' : '1px solid rgba(255,255,255,0.08)',
      background: active ? 'rgba(91,179,255,0.1)' : 'rgba(255,255,255,0.05)',
      display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
      cursor: 'pointer', color: C.ink,
      fontFamily: '-apple-system, system-ui',
      fontSize: 12, fontWeight: 500,
      transition: 'background 0.2s, border-color 0.2s',
    }}>
      {icon}
      <span style={{ color: active ? C.blue : C.ink2 }}>{label}</span>
    </button>
  );
}

// ── Settings ─────────────────────────────────────────────────
function ScreenSettings({ deviceMode, onToggleMode, onClose, onRecalibrate, alerts, onAlertToggle }) {
  return (
    <ScreenBG gradient={false}>
      <div style={{
        position: 'absolute', top: 60, left: 20, right: 20, zIndex: 5,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      }}>
        <button onClick={onClose} style={{
          width: 40, height: 40, borderRadius: 20, border: 'none', cursor: 'pointer',
          background: 'rgba(255,255,255,0.08)', backdropFilter: 'blur(16px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="14" height="14" viewBox="0 0 14 14"><path d="M9 1L3 7l6 6" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none"/></svg>
        </button>
        <div style={{ fontSize: 17, fontWeight: 600, color: C.ink }}>Settings</div>
        <div style={{ width: 40 }}/>
      </div>

      <div style={{
        position: 'absolute', top: 120, left: 24, right: 24, bottom: 36, overflowY: 'auto',
      }}>
        <SettingsGroup title="Orientation">
          <SettingsRow
            title="Heading source"
            value={deviceMode === 'gyro' ? 'Compass + Gyro' : 'Manual'}
            onClick={onToggleMode}
          />
          <SettingsRow
            title="Recalibrate"
            value={deviceMode === 'gyro' ? 'Figure-8 motion' : 'Set heading'}
            onClick={onRecalibrate}
          />
        </SettingsGroup>

        <SettingsGroup title="Alerts">
          <SettingsToggle title="Interesting aircraft nearby" on={alerts.interesting} onChange={onAlertToggle}/>
        </SettingsGroup>

        <SettingsGroup title="Display">
          <SettingsRow title="Units" value="Metric (km, m)"/>
          <SettingsRow title="Aircraft labels" value="Callsign"/>
          <SettingsRow title="Theme" value="Dark"/>
        </SettingsGroup>

        <SettingsGroup title="Data">
          <SettingsRow title="ADS-B source" value="OpenSky"/>
          <SettingsRow title="Privacy" value="On-device"/>
        </SettingsGroup>

        <div style={{ marginTop: 18, fontSize: 11, color: C.ink3, textAlign: 'center' }}>
          SkyLook 1.0 · Know what's above you
        </div>
      </div>
    </ScreenBG>
  );
}

function SettingsGroup({ title, children }) {
  return (
    <div style={{ marginBottom: 22 }}>
      <div style={{
        fontSize: 11, color: C.ink3, fontWeight: 600,
        letterSpacing: 0.8, textTransform: 'uppercase',
        padding: '0 4px 8px',
      }}>{title}</div>
      <div style={{
        background: C.card,
        border: `1px solid ${C.cardBorder}`,
        borderRadius: 16,
        overflow: 'hidden',
      }}>{children}</div>
    </div>
  );
}

function SettingsRow({ title, value, onClick }) {
  return (
    <div onClick={onClick} style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '14px 16px',
      borderBottom: '1px solid rgba(255,255,255,0.04)',
      cursor: onClick ? 'pointer' : 'default',
    }}>
      <div style={{ fontSize: 15, color: C.ink }}>{title}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ fontSize: 14, color: C.ink2 }}>{value}</span>
        <svg width="8" height="14" viewBox="0 0 8 14"><path d="M1 1l6 6-6 6" stroke={C.ink3} strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>
      </div>
    </div>
  );
}

function SettingsToggle({ title, on, onChange }) {
  return (
    <div onClick={onChange} style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '14px 16px',
      borderBottom: '1px solid rgba(255,255,255,0.04)',
      cursor: 'pointer',
    }}>
      <div style={{ fontSize: 15, color: C.ink }}>{title}</div>
      <div style={{
        width: 44, height: 26, borderRadius: 13,
        background: on ? C.accent : 'rgba(255,255,255,0.15)',
        position: 'relative', transition: 'background 0.2s',
      }}>
        <div style={{
          position: 'absolute', top: 2, left: on ? 20 : 2,
          width: 22, height: 22, borderRadius: '50%',
          background: '#fff',
          transition: 'left 0.2s',
          boxShadow: '0 1px 4px rgba(0,0,0,0.3)',
        }}/>
      </div>
    </div>
  );
}

// ── Track a Flight screen ─────────────────────────────────────
function FlightRow({ aircraft: p, tracked, onTrack, onUntrack, onSelect }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      padding: '12px 14px', borderRadius: 16,
      background: tracked ? 'rgba(91,179,255,0.07)' : C.card,
      border: tracked ? '1px solid rgba(91,179,255,0.2)' : `1px solid ${C.cardBorder}`,
      cursor: 'pointer',
    }} onClick={onSelect}>
      <div style={{
        width: 42, height: 42, borderRadius: 12, flexShrink: 0,
        background: tracked
          ? 'rgba(91,179,255,0.14)'
          : p.isMilitary
          ? 'rgba(245,168,58,0.1)'
          : p.interesting
          ? 'rgba(155,125,255,0.1)'
          : 'rgba(255,255,255,0.04)',
        border: tracked
          ? '1px solid rgba(91,179,255,0.3)'
          : p.isMilitary
          ? '1px solid rgba(245,168,58,0.3)'
          : p.interesting
          ? '1px solid rgba(155,125,255,0.25)'
          : '1px solid rgba(255,255,255,0.06)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        animation: p.isMilitary && !tracked ? 'skyMilitaryBadge 2.5s ease-in-out infinite'
          : p.interesting && !tracked ? 'skyInterestingBadge 2.5s ease-in-out infinite' : 'none',
      }}>
        <PlaneGlyph size={16}
          color={tracked ? C.blue : p.isMilitary ? '#F5A83A' : p.interesting ? C.violet : 'rgba(255,255,255,0.75)'}
          type={silhouetteType(p.model, p.isMilitary)} />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
          <div style={{ fontSize: 15, fontWeight: 700, color: C.ink }}>{p.callsign}</div>
          {p.isMilitary && (
            <div style={{
              fontSize: 9, fontWeight: 700, letterSpacing: 0.5,
              padding: '2px 6px', borderRadius: 4,
              background: 'rgba(245,168,58,0.15)', color: '#F5A83A',
            }}>MIL</div>
          )}
          {p.interesting && !p.isMilitary && (
            <div style={{
              fontSize: 9, fontWeight: 700, letterSpacing: 0.5,
              padding: '2px 6px', borderRadius: 4,
              background: 'rgba(155,125,255,0.15)', color: C.violet,
            }}>{p.model.split(' ').slice(-1)[0]}</div>
          )}
          {tracked && (
            <div style={{
              fontSize: 9, fontWeight: 700, letterSpacing: 0.5,
              padding: '2px 6px', borderRadius: 4,
              background: 'rgba(91,179,255,0.15)', color: C.blue,
            }}>TRACKED</div>
          )}
        </div>
        <div style={{ marginTop: 1, fontSize: 12, color: C.ink2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {p.fromCode} → {p.toCode} · {p.airline}
        </div>
        <div style={{ marginTop: 2, fontSize: 11, color: C.ink3, fontVariantNumeric: 'tabular-nums' }}>
          {p.overheadMin} min · {(p.altitudeFt / 1000).toFixed(0)}k ft · {p.distance.toFixed(0)} km
        </div>
      </div>
      <button
        onClick={e => { e.stopPropagation(); tracked ? onUntrack() : onTrack(); }}
        style={{
          width: 38, height: 38, borderRadius: '50%', border: 'none', cursor: 'pointer', flexShrink: 0,
          background: tracked ? 'rgba(91,179,255,0.18)' : 'rgba(255,255,255,0.06)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          transition: 'background 0.2s',
          boxShadow: tracked ? '0 0 12px rgba(91,179,255,0.25)' : 'none',
        }}>
        <HeartIcon filled={tracked} size={16} color={tracked ? C.blue : undefined} />
      </button>
    </div>
  );
}

function ScreenTrackFlight({ aircraft, trackedFlights = [], isTracked, onTrack, onUntrack, onSelect, onClose }) {
  const [query, setQuery] = React.useState('');
  const [inputFocused, setInputFocused] = React.useState(false);

  const filteredAircraft = query.length >= 2
    ? aircraft.filter(p =>
        p.callsign.toLowerCase().includes(query.toLowerCase()) ||
        p.airline.toLowerCase().includes(query.toLowerCase()) ||
        p.fromCode.toLowerCase().includes(query.toLowerCase()) ||
        p.toCode.toLowerCase().includes(query.toLowerCase()) ||
        p.fromCity.toLowerCase().includes(query.toLowerCase()) ||
        p.toCity.toLowerCase().includes(query.toLowerCase())
      )
    : [];

  const tracked = aircraft.filter(p => trackedFlights.includes(p.id));
  const interesting = aircraft.filter(p => p.interesting && !trackedFlights.includes(p.id));
  const other = aircraft.filter(p => !p.interesting && !trackedFlights.includes(p.id));

  return (
    <ScreenBG>
      {/* header */}
      <div style={{
        position: 'absolute', top: 60, left: 20, right: 20,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center', zIndex: 5,
      }}>
        <button onClick={onClose} style={{
          width: 40, height: 40, borderRadius: 20, border: 'none', cursor: 'pointer',
          background: 'rgba(255,255,255,0.08)', backdropFilter: 'blur(16px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="14" height="14" viewBox="0 0 14 14"><path d="M9 1L3 7l6 6" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none"/></svg>
        </button>
        <div style={{ fontSize: 17, fontWeight: 600, color: C.ink }}>Track a Flight</div>
        <div style={{ width: 40 }} />
      </div>

      <div style={{
        position: 'absolute', top: 120, left: 20, right: 20, bottom: 36, overflowY: 'auto',
      }}>
        {/* search bar */}
        <div style={{ marginBottom: 24 }}>
          <div style={{
            height: 52, borderRadius: 16,
            background: 'rgba(255,255,255,0.07)',
            border: `1px solid ${inputFocused ? C.accent : 'rgba(255,255,255,0.1)'}`,
            display: 'flex', alignItems: 'center', gap: 12,
            padding: '0 16px',
            transition: 'border-color 0.2s',
          }}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <circle cx="7" cy="7" r="5" stroke={C.ink3} strokeWidth="1.6"/>
              <path d="M11 11l3 3" stroke={C.ink3} strokeWidth="1.6" strokeLinecap="round"/>
            </svg>
            <input
              type="text" placeholder="BA123 · Emirates · JFK–LHR…"
              value={query}
              onChange={e => setQuery(e.target.value)}
              onFocus={() => setInputFocused(true)}
              onBlur={() => setInputFocused(false)}
              style={{
                flex: 1, background: 'none', border: 'none', outline: 'none',
                fontSize: 16, color: C.ink,
                fontFamily: '-apple-system, system-ui',
              }}
            />
            {query && (
              <button onClick={() => setQuery('')} style={{
                width: 20, height: 20, borderRadius: '50%',
                background: 'rgba(255,255,255,0.18)', border: 'none', cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <svg width="8" height="8" viewBox="0 0 10 10"><path d="M1 1l8 8M9 1L1 9" stroke="#fff" strokeWidth="1.6" strokeLinecap="round"/></svg>
              </button>
            )}
          </div>
        </div>

        {/* search results */}
        {filteredAircraft.length > 0 && (
          <div style={{ marginBottom: 28 }}>
            <div style={{ fontSize: 10, color: C.ink3, fontWeight: 700, letterSpacing: 1, marginBottom: 10 }}>RESULTS</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {filteredAircraft.map(p => (
                <FlightRow key={p.id} aircraft={p} tracked={trackedFlights.includes(p.id)}
                  onTrack={() => onTrack(p.id)} onUntrack={() => onUntrack(p.id)}
                  onSelect={() => onSelect(p.id)} />
              ))}
            </div>
          </div>
        )}

        {/* tracked flights */}
        {!query && tracked.length > 0 && (
          <div style={{ marginBottom: 28 }}>
            <div style={{ fontSize: 10, color: C.blue, fontWeight: 700, letterSpacing: 1, marginBottom: 10, display: 'flex', alignItems: 'center', gap: 6 }}>
              <HeartIcon filled size={10} color={C.blue} />
              YOUR TRACKED FLIGHTS
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {tracked.map(p => (
                <FlightRow key={p.id} aircraft={p} tracked
                  onTrack={() => onTrack(p.id)} onUntrack={() => onUntrack(p.id)}
                  onSelect={() => onSelect(p.id)} />
              ))}
            </div>
          </div>
        )}

        {/* interesting nearby */}
        {!query && interesting.length > 0 && (
          <div style={{ marginBottom: 28 }}>
            <div style={{ fontSize: 10, color: C.violet, fontWeight: 700, letterSpacing: 1, marginBottom: 10 }}>✦ INTERESTING NEARBY</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {interesting.map(p => (
                <FlightRow key={p.id} aircraft={p} tracked={false}
                  onTrack={() => onTrack(p.id)} onUntrack={() => onUntrack(p.id)}
                  onSelect={() => onSelect(p.id)} />
              ))}
            </div>
          </div>
        )}

        {/* all nearby */}
        {!query && other.length > 0 && (
          <div style={{ marginBottom: 28 }}>
            <div style={{ fontSize: 10, color: C.ink3, fontWeight: 700, letterSpacing: 1, marginBottom: 10 }}>NEARBY FLIGHTS</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {other.map(p => (
                <FlightRow key={p.id} aircraft={p} tracked={false}
                  onTrack={() => onTrack(p.id)} onUntrack={() => onUntrack(p.id)}
                  onSelect={() => onSelect(p.id)} />
              ))}
            </div>
          </div>
        )}

        {/* empty state */}
        {query.length >= 2 && filteredAircraft.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <PlaneGlyph size={32} color={C.ink3} />
            <div style={{ marginTop: 12, fontSize: 15, color: C.ink2 }}>No flights found for "{query}"</div>
            <div style={{ marginTop: 4, fontSize: 13, color: C.ink3 }}>Try a flight number like BA123 or EK7</div>
          </div>
        )}

        <div style={{ height: 20 }} />
      </div>
    </ScreenBG>
  );
}

Object.assign(window, {
  ScreenWelcome, ScreenLocation, ScreenSensorCheck, ScreenManualCalibrate,
  ScreenTutorial, ScreenMain, ScreenFilter, ScreenDetail, ScreenSettings,
  ScreenTrackFlight, TrackedStrip, FlightRow, HeartIcon,
  C, // share palette
});

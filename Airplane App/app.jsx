// SkyLook prototype app — state + router

const FLIGHTS = [
  { id: 'BA123', callsign: 'BA123', airline: 'British Airways', iataCode: 'BA', model: 'Boeing 777-300ER',
    bearing: 5,  distance: 3.2, altitudeFt: 32500, speedMph: 515, track: 90,
    overheadMin: 2, fromCode: 'JFK', fromCity: 'New York', toCode: 'LHR', toCity: 'London',
    progress: 0.58, interesting: false, isMilitary: false, status: 'En route',
    registration: 'G-STBH', yearBuilt: 2012 },
  { id: 'UA88',  callsign: 'UA88',  airline: 'United Airlines', iataCode: 'UA', model: 'Airbus A350-900',
    bearing: 78, distance: 5.6, altitudeFt: 38000, speedMph: 540, track: 110,
    overheadMin: 5, fromCode: 'SFO', fromCity: 'San Francisco', toCode: 'FRA', toCity: 'Frankfurt',
    progress: 0.42, interesting: false, isMilitary: false, status: 'En route',
    registration: 'N41140', yearBuilt: 2019 },
  { id: 'AF22',  callsign: 'AF22',  airline: 'Air France', iataCode: 'AF', model: 'Boeing 787-9',
    bearing: 188, distance: 8.1, altitudeFt: 28000, speedMph: 480, track: 270,
    overheadMin: 12, fromCode: 'CDG', fromCity: 'Paris', toCode: 'BOS', toCity: 'Boston',
    progress: 0.7, interesting: false, isMilitary: false, status: 'En route',
    registration: 'F-HRBA', yearBuilt: 2016 },
  { id: 'DL451', callsign: 'DL451', airline: 'Delta Air Lines', iataCode: 'DL', model: 'Airbus A321neo',
    bearing: 240, distance: 22, altitudeFt: 18000, speedMph: 410, track: 60,
    overheadMin: 18, fromCode: 'ATL', fromCity: 'Atlanta', toCode: 'SEA', toCity: 'Seattle',
    progress: 0.3, interesting: false, isMilitary: false, status: 'Descending',
    registration: 'N503DN', yearBuilt: 2022 },
  { id: 'LH401', callsign: 'LH401', airline: 'Lufthansa', iataCode: 'LH', model: 'Airbus A340-600',
    bearing: 305, distance: 42, altitudeFt: 36000, speedMph: 525, track: 200,
    overheadMin: 30, fromCode: 'MUC', fromCity: 'Munich', toCode: 'IAD', toCity: 'Washington',
    progress: 0.85, interesting: false, isMilitary: false, status: 'En route',
    registration: 'D-AIHR', yearBuilt: 2003 },
  { id: 'EK7',   callsign: 'EK7',   airline: 'Emirates', iataCode: 'EK', model: 'Airbus A380-800',
    bearing: 140, distance: 15, altitudeFt: 41000, speedMph: 575, track: 320,
    overheadMin: 8, fromCode: 'DXB', fromCity: 'Dubai', toCode: 'JFK', toCity: 'New York',
    progress: 0.52, interesting: true, isMilitary: false, status: 'En route',
    registration: 'A6-EOA', yearBuilt: 2014 },
  { id: 'RCH209', callsign: 'RCH209', airline: 'US Air Force', iataCode: '', model: 'Boeing C-17 Globemaster III',
    bearing: 55, distance: 35, altitudeFt: 29000, speedMph: 490, track: 145,
    overheadMin: 25, fromCode: 'RMS', fromCity: 'Ramstein', toCode: 'BWI', toCity: 'Washington',
    progress: 0.45, interesting: true, isMilitary: true, status: 'En route',
    registration: '05-5140', yearBuilt: 2006 },
];

// Tracked flights that are currently outside radar radius
const AWAY_FLIGHTS = [
  { id: 'QF1', callsign: 'QF1', airline: 'Qantas',
    fromCode: 'SYD', toCode: 'LHR', currentCity: 'Dubai', progress: 0.35 },
  { id: 'SQ317', callsign: 'SQ317', airline: 'Singapore Airlines',
    fromCode: 'SIN', toCode: 'JFK', currentCity: 'Mumbai', progress: 0.22 },
];

function SkyLookApp({ initialScreen, deviceMode: dmProp }) {
  const [screen, setScreen] = React.useState(initialScreen || 'welcome');
  const [deviceMode, setDeviceMode] = React.useState(dmProp || 'gyro'); // 'gyro' | 'manual'
  const [manualHeading, setManualHeading] = React.useState(20);
  const [heading, setHeading] = React.useState(0);
  const [focused, setFocused] = React.useState('BA123');
  const [radiusKm, setRadiusKm] = React.useState(50);
  const [minAlt, setMinAlt] = React.useState(0);
  const [types, setTypes] = React.useState(['commercial','private','cargo']);
  const [alerts, setAlerts] = React.useState({ interesting: true });
  const [trackedFlights, setTrackedFlights] = React.useState(['BA123', 'QF1']);

  const lockTo = initialScreen;

  function trackFlight(id) {
    setTrackedFlights(prev => prev.includes(id) ? prev : [...prev, id]);
  }
  function untrackFlight(id) {
    setTrackedFlights(prev => prev.filter(x => x !== id));
  }
  function isTracked(id) { return trackedFlights.includes(id); }

  const heading2 = deviceMode === 'manual' ? manualHeading : heading;
  const focusedPlane = FLIGHTS.find(p => p.id === focused);

  // Auto-rotate heading slightly when "live" demo screen is shown
  React.useEffect(() => {
    if (lockTo && !lockTo.startsWith('live')) return;
    if (deviceMode === 'manual') return;
    const t = setInterval(() => setHeading(h => (h + 0.3) % 360), 50);
    return () => clearInterval(t);
  }, [deviceMode, lockTo]);

  const trackProps = { trackedFlights, isTracked, onTrack: trackFlight, onUntrack: untrackFlight };
  const awayTracked = AWAY_FLIGHTS.filter(f => trackedFlights.includes(f.id));

  const screens = {
    'welcome': <ScreenWelcome onNext={() => setScreen('location')} />,
    'location': <ScreenLocation
      onAllow={() => { setDeviceMode('gyro'); setScreen('sensor-gyro'); }}
      onSkip={() => { setDeviceMode('manual'); setScreen('sensor-manual'); }} />,
    'sensor-gyro': <ScreenSensorCheck deviceMode="gyro"
      onContinue={() => setScreen('tutorial')} />,
    'sensor-manual': <ScreenSensorCheck deviceMode="manual"
      onUseManual={() => setScreen('manual-cal')}
      onRetry={() => { setDeviceMode('gyro'); setScreen('sensor-gyro'); }} />,
    'manual-cal': <ScreenManualCalibrate
      manualHeading={manualHeading}
      onChange={setManualHeading}
      onDone={() => setScreen('tutorial')}
      onBack={() => setScreen('sensor-manual')} />,
    'tutorial': <ScreenTutorial onDone={() => setScreen('main-idle')} />,
    'main-idle': <ScreenMain state="idle"
      heading={heading2} aircraft={FLIGHTS} focused={focused} radiusKm={radiusKm}
      deviceMode={deviceMode} {...trackProps}
      onAircraftTap={(id) => { setFocused(id); setScreen('main-focused'); }}
      onOpenFilter={() => setScreen('filter')}
      onOpenTrack={() => setScreen('track')}
      onRecenter={() => setScreen('settings')} awayTracked={awayTracked} />,
    'main-focused': <ScreenMain state="focused"
      heading={heading2} aircraft={FLIGHTS} focused={focused} radiusKm={radiusKm}
      deviceMode={deviceMode} {...trackProps}
      onAircraftTap={(id) => { setFocused(id); }}
      onOpenFilter={() => setScreen('filter')}
      onOpenTrack={() => setScreen('track')}
      onRecenter={() => setScreen('main-idle')} awayTracked={awayTracked} />,
    'main-locked': <ScreenMain state="locked"
      heading={heading2} aircraft={FLIGHTS} focused={focused} radiusKm={radiusKm}
      deviceMode={deviceMode} {...trackProps}
      onOpenDetail={() => setScreen('detail')}
      onAircraftTap={(id) => { setFocused(id); }}
      onOpenFilter={() => setScreen('filter')}
      onOpenTrack={() => setScreen('track')}
      onRecenter={() => setScreen('main-idle')} awayTracked={awayTracked} />,
    'filter': <ScreenFilter radiusKm={radiusKm} onChange={setRadiusKm}
      minAlt={minAlt} onMinAltChange={setMinAlt}
      types={types} onTypesToggle={(t) => setTypes(types.includes(t) ? types.filter(x => x !== t) : [...types, t])}
      onClose={() => setScreen('main-idle')} />,
    'detail': <ScreenDetail aircraft={focusedPlane} {...trackProps}
      onClose={() => setScreen('main-locked')} />,
    'settings': <ScreenSettings deviceMode={deviceMode}
      onToggleMode={() => { setDeviceMode(m => m === 'gyro' ? 'manual' : 'gyro'); }}
      onRecalibrate={() => setScreen(deviceMode === 'gyro' ? 'sensor-gyro' : 'manual-cal')}
      alerts={alerts}
      onAlertToggle={() => setAlerts(a => ({ interesting: !a.interesting }))}
      onClose={() => setScreen('main-idle')} />,
    'track': <ScreenTrackFlight
      aircraft={FLIGHTS} {...trackProps}
      onSelect={(id) => { setFocused(id); setScreen('main-locked'); }}
      onClose={() => setScreen('main-idle')} />,
  };

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      {screens[screen] || <ScreenWelcome onNext={() => setScreen('location')}/>}
    </div>
  );
}

// Wrapper that puts SkyLookApp inside an iOS frame
function Phone({ initialScreen, deviceMode, locked }) {
  return (
    <div data-screen-label={labelFor(initialScreen)} style={{ display: 'inline-block' }}>
      <IOSDevice dark>
        <div style={{ position: 'relative', width: '100%', height: '100%' }}>
          <SkyLookApp initialScreen={initialScreen} deviceMode={deviceMode} />
        </div>
      </IOSDevice>
    </div>
  );
}

function labelFor(s) {
  const map = {
    'welcome': '01 Welcome',
    'location': '02 Location',
    'sensor-gyro': '03a Sensor — Gyro',
    'sensor-manual': '03b Sensor — Manual',
    'manual-cal': '04 Manual calibration',
    'tutorial': '05 Tutorial',
    'main-idle': '06 Main — Idle',
    'main-focused': '07 Main — Focused',
    'main-locked': '08 Main — Locked',
    'filter': '09 Radius filter',
    'detail': '10 Lock-on detail',
    'settings': '11 Settings',
    'track': '12 Track a flight',
  };
  return map[s] || s;
}

Object.assign(window, { SkyLookApp, Phone, FLIGHTS, labelFor });

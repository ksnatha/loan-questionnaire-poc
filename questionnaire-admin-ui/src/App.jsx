export default function App() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <header style={{
        background: 'linear-gradient(180deg, var(--bg-banner-2), var(--bg-banner))',
        color: 'var(--text-on-banner)',
        padding: '14px 28px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottom: '1px solid rgba(255,255,255,0.1)',
      }}>
        <div>
          <div style={{ fontWeight: 600, fontSize: 17 }}>Questionnaire Admin</div>
          <div style={{ fontSize: 13, color: 'var(--text-on-banner-muted)' }}>questionnaire-admin-ui · Phase 0</div>
        </div>
        <span style={{
          fontSize: 12, fontWeight: 500,
          background: 'rgba(255,255,255,0.14)',
          padding: '3px 10px', borderRadius: 999,
          color: 'var(--text-on-banner)',
        }}>
          Workspace setup
        </span>
      </header>

      <main style={{
        flex: 1, display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center', gap: 12,
        padding: 40,
      }}>
        <div style={{
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 8,
          padding: '40px 48px',
          textAlign: 'center',
          maxWidth: 480,
          width: '100%',
        }}>
          <div style={{ fontSize: 32, marginBottom: 12 }}>✓</div>
          <h1 style={{ fontSize: 20, fontWeight: 600, marginBottom: 8 }}>Phase 0 complete</h1>
          <p style={{ color: 'var(--text-secondary)', marginBottom: 24 }}>
            Workspace is set up. This admin UI will allow business users to
            edit and publish questionnaire templates (Phase 8).
          </p>
          <div style={{
            background: 'var(--bg-app)',
            border: '1px solid var(--border)',
            borderRadius: 6,
            padding: '12px 16px',
            fontSize: 13,
            color: 'var(--text-secondary)',
            textAlign: 'left',
          }}>
            <div>API → <code style={{ color: 'var(--accent)' }}>http://localhost:8082</code></div>
            <div style={{ marginTop: 4 }}>Next: Phase 1 — contract review</div>
          </div>
        </div>
      </main>
    </div>
  )
}

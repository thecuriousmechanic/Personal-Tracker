"use client";

import { BarChart3, ChevronRight, CircleUserRound, CloudOff, Database, Download, HeartPulse, RefreshCcw, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import { LoadingScreen } from "@/components/ui/LoadingScreen";

export function ProfileSettings() {
  const { snapshot, isReady, resetData } = usePersonalOS();
  const [confirmReset, setConfirmReset] = useState(false);
  if (!isReady) return <LoadingScreen />;
  const totalRecords = Object.values(snapshot).reduce((sum, value) => sum + value.length, 0);

  async function handleReset() {
    await resetData();
    setConfirmReset(false);
  }

  function handleExport() {
    const payload = JSON.stringify({ exportedAt: new Date().toISOString(), data: snapshot }, null, 2);
    const url = URL.createObjectURL(new Blob([payload], { type: "application/json" }));
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `personal-os-${new Date().toISOString().slice(0, 10)}.json`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="page-shell">
      <header className="page-header"><div><span className="eyebrow"><CircleUserRound size={14} /> Profile</span><h1>Your system, your data.</h1><p>Personalize the workspace and stay in control of everything it remembers.</p></div></header>
      <section className="profile-hero">
        <div className="avatar">K</div>
        <div><span className="status-pill"><span /> Local profile</span><h2>Krishna</h2><p>Building a calmer, more connected way to work and live.</p></div>
        <div className="profile-hero__stat"><strong>{snapshot.tasks.filter((task) => task.status === "completed").length}</strong><span>tasks completed</span></div>
      </section>
      <div className="settings-grid">
        <section>
          <div className="section-heading"><div><span className="eyebrow">Workspace</span><h2>Explore your system</h2></div></div>
          <div className="settings-list">
            <Link href="/health"><span className="settings-icon green"><HeartPulse size={18} /></span><div><strong>Health</strong><small>Workouts, recovery, and body metrics</small></div><ChevronRight size={17} /></Link>
            <Link href="/analytics"><span className="settings-icon violet"><BarChart3 size={18} /></span><div><strong>Analytics</strong><small>Patterns across every life area</small></div><ChevronRight size={17} /></Link>
            <button type="button" onClick={handleExport}><span className="settings-icon blue"><Download size={18} /></span><div><strong>Export data</strong><small>Download a portable JSON copy</small></div><ChevronRight size={17} /></button>
          </div>
        </section>
        <section>
          <div className="section-heading"><div><span className="eyebrow">Data</span><h2>Local-first storage</h2></div></div>
          <div className="data-card">
            <div className="data-card__title"><span><Database size={19} /></span><div><strong>IndexedDB</strong><small>{totalRecords} records on this device</small></div><ShieldCheck size={18} /></div>
            <div className="data-facts"><div><CloudOff size={15} /><span><strong>Works offline</strong><small>Core actions do not need a network</small></span></div><div><RefreshCcw size={15} /><span><strong>Sync-ready boundary</strong><small>Remote sync can be added behind the repository</small></span></div></div>
            {!confirmReset ? (
              <button type="button" className="secondary-button" onClick={() => setConfirmReset(true)}>Reset demo workspace</button>
            ) : (
              <div className="reset-confirm"><p>Replace local changes with the starter workspace?</p><div><button type="button" className="text-button" onClick={() => setConfirmReset(false)}>Cancel</button><button type="button" className="danger-button" onClick={handleReset}>Reset data</button></div></div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

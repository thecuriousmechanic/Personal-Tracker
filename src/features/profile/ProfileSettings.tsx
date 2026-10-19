"use client";

import {
  AlertCircle,
  BarChart3,
  CheckCircle2,
  ChevronRight,
  CircleUserRound,
  CloudOff,
  Database,
  Download,
  HeartPulse,
  RefreshCcw,
  ShieldCheck,
  Upload,
} from "lucide-react";
import Link from "next/link";
import { useRef, useState, type ChangeEvent } from "react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import { LoadingScreen } from "@/components/ui/LoadingScreen";
import type { AppSnapshot } from "@/types/entities";

interface PendingImport {
  snapshot: AppSnapshot;
  count: number;
  filename: string;
}

export function ProfileSettings() {
  const { snapshot, isReady, resetData, importSnapshot } = usePersonalOS();
  const [confirmReset, setConfirmReset] = useState(false);
  const [pendingImport, setPendingImport] = useState<PendingImport | null>(null);
  const [importStatus, setImportStatus] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [isImporting, setIsImporting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isReady) return <LoadingScreen />;
  const totalRecords = Object.values(snapshot).reduce((sum, value) => sum + (Array.isArray(value) ? value.length : 0), 0);

  async function handleReset() {
    await resetData();
    setConfirmReset(false);
    setImportStatus({ type: "success", message: "Demo workspace reset successfully." });
  }

  function handleExport() {
    const payload = JSON.stringify({ exportedAt: new Date().toISOString(), data: snapshot }, null, 2);
    const url = URL.createObjectURL(new Blob([payload], { type: "application/json" }));
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `personal-os-${new Date().toISOString().slice(0, 10)}.json`;
    anchor.click();
    URL.revokeObjectURL(url);
    setImportStatus({ type: "success", message: "Workspace exported to JSON successfully." });
  }

  function handleFileSelect(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const text = event.target?.result as string;
        const parsed = JSON.parse(text);

        // Allow wrapping in { data: AppSnapshot } or { snapshot: AppSnapshot } or direct AppSnapshot
        const raw = (parsed.data || parsed.snapshot || parsed) as Partial<AppSnapshot>;

        // Verify that at least some known collection exists
        const knownCollections = ["tasks", "habits", "notes", "projects", "goals", "learningItems", "workouts"];
        const hasKnown = knownCollections.some((k) => Array.isArray((raw as Record<string, unknown>)[k]));

        if (!hasKnown) {
          setImportStatus({
            type: "error",
            message: "Invalid file format: JSON does not contain recognized Personal OS workspace data.",
          });
          return;
        }

        // Normalize into a valid AppSnapshot
        const normalized: AppSnapshot = {
          lifeAreas: Array.isArray(raw.lifeAreas) ? raw.lifeAreas : [],
          goals: Array.isArray(raw.goals) ? raw.goals : [],
          projects: Array.isArray(raw.projects) ? raw.projects : [],
          tasks: Array.isArray(raw.tasks) ? raw.tasks : [],
          habits: Array.isArray(raw.habits) ? raw.habits : [],
          notes: Array.isArray(raw.notes) ? raw.notes : [],
          learningItems: Array.isArray(raw.learningItems) ? raw.learningItems : [],
          metrics: Array.isArray(raw.metrics) ? raw.metrics : [],
          workouts: Array.isArray(raw.workouts) ? raw.workouts : [],
          relations: Array.isArray(raw.relations) ? raw.relations : [],
          activities: Array.isArray(raw.activities) ? raw.activities : [],
        };

        const count = Object.values(normalized).reduce((acc, arr) => acc + arr.length, 0);

        setPendingImport({
          snapshot: normalized,
          count,
          filename: file.name,
        });
        setImportStatus(null);
      } catch (err) {
        console.error(err);
        setImportStatus({
          type: "error",
          message: "Failed to parse JSON file. Please ensure it is a valid Personal OS backup.",
        });
      } finally {
        if (fileInputRef.current) fileInputRef.current.value = "";
      }
    };
    reader.readAsText(file);
  }

  async function handleConfirmImport() {
    if (!pendingImport) return;
    setIsImporting(true);
    try {
      await importSnapshot(pendingImport.snapshot);
      setImportStatus({
        type: "success",
        message: `Successfully restored ${pendingImport.count} records from ${pendingImport.filename}!`,
      });
      setPendingImport(null);
    } catch (err) {
      console.error(err);
      setImportStatus({
        type: "error",
        message: "Failed to save imported workspace into local storage.",
      });
    } finally {
      setIsImporting(false);
    }
  }

  return (
    <div className="page-shell">
      <header className="page-header">
        <div>
          <span className="eyebrow"><CircleUserRound size={14} /> Profile</span>
          <h1>Your system, your data.</h1>
          <p>Personalize the workspace and stay in control of everything it remembers.</p>
        </div>
      </header>

      {/* Notification Toast/Banner */}
      {importStatus && (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "10px",
            padding: "12px 16px",
            borderRadius: "10px",
            marginBottom: "16px",
            background: importStatus.type === "success" ? "rgba(16, 185, 129, 0.12)" : "rgba(239, 68, 68, 0.12)",
            border: `1px solid ${importStatus.type === "success" ? "rgba(16, 185, 129, 0.3)" : "rgba(239, 68, 68, 0.3)"}`,
            color: importStatus.type === "success" ? "#34d399" : "#f87171",
          }}
        >
          {importStatus.type === "success" ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
          <span style={{ fontSize: "0.9rem", flex: 1 }}>{importStatus.message}</span>
          <button
            type="button"
            className="text-button"
            onClick={() => setImportStatus(null)}
            style={{ padding: "2px 6px", fontSize: "0.8rem" }}
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Pending Import Confirmation Dialog */}
      {pendingImport && (
        <section
          className="panel"
          style={{
            marginBottom: "20px",
            borderColor: "rgba(99, 102, 241, 0.4)",
            background: "linear-gradient(170deg, rgba(30, 39, 65, 0.9), rgba(17, 23, 40, 0.95))",
          }}
        >
          <div className="section-heading">
            <div>
              <span className="eyebrow" style={{ color: "#a5b4fc" }}>Restore Backup Confirmation</span>
              <h2>Ready to import &ldquo;{pendingImport.filename}&rdquo;?</h2>
            </div>
          </div>
          <p style={{ color: "#94a3b8", fontSize: "0.92rem", marginBottom: "14px" }}>
            This will replace the current local workspace on this device with{" "}
            <strong style={{ color: "#fff" }}>{pendingImport.count} total records</strong>:
          </p>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))",
              gap: "8px",
              marginBottom: "16px",
            }}
          >
            <div style={{ background: "rgba(255, 255, 255, 0.04)", padding: "8px 12px", borderRadius: "6px" }}>
              <span style={{ fontSize: "0.75rem", color: "#8b9bb4" }}>Tasks</span>
              <div style={{ fontSize: "1.1rem", fontWeight: 600 }}>{pendingImport.snapshot.tasks.length}</div>
            </div>
            <div style={{ background: "rgba(255, 255, 255, 0.04)", padding: "8px 12px", borderRadius: "6px" }}>
              <span style={{ fontSize: "0.75rem", color: "#8b9bb4" }}>Projects</span>
              <div style={{ fontSize: "1.1rem", fontWeight: 600 }}>{pendingImport.snapshot.projects.length}</div>
            </div>
            <div style={{ background: "rgba(255, 255, 255, 0.04)", padding: "8px 12px", borderRadius: "6px" }}>
              <span style={{ fontSize: "0.75rem", color: "#8b9bb4" }}>Notes</span>
              <div style={{ fontSize: "1.1rem", fontWeight: 600 }}>{pendingImport.snapshot.notes.length}</div>
            </div>
            <div style={{ background: "rgba(255, 255, 255, 0.04)", padding: "8px 12px", borderRadius: "6px" }}>
              <span style={{ fontSize: "0.75rem", color: "#8b9bb4" }}>Habits</span>
              <div style={{ fontSize: "1.1rem", fontWeight: 600 }}>{pendingImport.snapshot.habits.length}</div>
            </div>
            <div style={{ background: "rgba(255, 255, 255, 0.04)", padding: "8px 12px", borderRadius: "6px" }}>
              <span style={{ fontSize: "0.75rem", color: "#8b9bb4" }}>Goals</span>
              <div style={{ fontSize: "1.1rem", fontWeight: 600 }}>{pendingImport.snapshot.goals.length}</div>
            </div>
            <div style={{ background: "rgba(255, 255, 255, 0.04)", padding: "8px 12px", borderRadius: "6px" }}>
              <span style={{ fontSize: "0.75rem", color: "#8b9bb4" }}>Learning Items</span>
              <div style={{ fontSize: "1.1rem", fontWeight: 600 }}>{pendingImport.snapshot.learningItems.length}</div>
            </div>
          </div>
          <div style={{ display: "flex", gap: "10px", justifyContent: "flex-end" }}>
            <button
              type="button"
              className="text-button"
              onClick={() => setPendingImport(null)}
              disabled={isImporting}
            >
              Cancel
            </button>
            <button
              type="button"
              className="primary-button"
              onClick={handleConfirmImport}
              disabled={isImporting}
            >
              {isImporting ? "Restoring Workspace…" : "Confirm & Restore"}
            </button>
          </div>
        </section>
      )}

      <section className="profile-hero">
        <div className="avatar">K</div>
        <div>
          <span className="status-pill"><span /> Local profile</span>
          <h2>Krishna</h2>
          <p>Building a calmer, more connected way to work and live.</p>
        </div>
        <div className="profile-hero__stat">
          <strong>{snapshot.tasks.filter((task) => task.status === "completed").length}</strong>
          <span>tasks completed</span>
        </div>
      </section>

      <div className="settings-grid">
        <section>
          <div className="section-heading">
            <div>
              <span className="eyebrow">Workspace</span>
              <h2>Explore your system</h2>
            </div>
          </div>
          <div className="settings-list">
            <Link href="/health">
              <span className="settings-icon green"><HeartPulse size={18} /></span>
              <div>
                <strong>Health</strong>
                <small>Workouts, recovery, and body metrics</small>
              </div>
              <ChevronRight size={17} />
            </Link>
            <Link href="/analytics">
              <span className="settings-icon violet"><BarChart3 size={18} /></span>
              <div>
                <strong>Analytics</strong>
                <small>Patterns across every life area</small>
              </div>
              <ChevronRight size={17} />
            </Link>
            <button type="button" onClick={handleExport}>
              <span className="settings-icon blue"><Download size={18} /></span>
              <div>
                <strong>Export data</strong>
                <small>Download a portable JSON copy</small>
              </div>
              <ChevronRight size={17} />
            </button>
            <button type="button" onClick={() => fileInputRef.current?.click()}>
              <span className="settings-icon green"><Upload size={18} /></span>
              <div>
                <strong>Import backup</strong>
                <small>Restore workspace from a JSON file</small>
              </div>
              <ChevronRight size={17} />
            </button>
          </div>
          <input
            ref={fileInputRef}
            type="file"
            accept=".json,application/json"
            onChange={handleFileSelect}
            style={{ display: "none" }}
            aria-label="Upload backup JSON file"
          />
        </section>

        <section>
          <div className="section-heading">
            <div>
              <span className="eyebrow">Data</span>
              <h2>Local-first storage</h2>
            </div>
          </div>
          <div className="data-card">
            <div className="data-card__title">
              <span><Database size={19} /></span>
              <div>
                <strong>IndexedDB</strong>
                <small>{totalRecords} records on this device</small>
              </div>
              <ShieldCheck size={18} />
            </div>
            <div className="data-facts">
              <div>
                <CloudOff size={15} />
                <span>
                  <strong>Works offline</strong>
                  <small>Core actions do not need a network</small>
                </span>
              </div>
              <div>
                <RefreshCcw size={15} />
                <span>
                  <strong>Sync-ready boundary</strong>
                  <small>Remote sync can be added behind the repository</small>
                </span>
              </div>
            </div>
            {!confirmReset ? (
              <button type="button" className="secondary-button" onClick={() => setConfirmReset(true)}>
                Reset demo workspace
              </button>
            ) : (
              <div className="reset-confirm">
                <p>Replace local changes with the starter workspace?</p>
                <div>
                  <button type="button" className="text-button" onClick={() => setConfirmReset(false)}>
                    Cancel
                  </button>
                  <button type="button" className="danger-button" onClick={handleReset}>
                    Reset data
                  </button>
                </div>
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

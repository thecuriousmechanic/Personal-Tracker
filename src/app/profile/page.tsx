import React, { useState } from 'react';
import { syncEngine } from '@/database/sync/sync.engine';

export default function ProfilePage() {
  const [syncing, setSyncing] = useState(false);
  const [lastSync, setLastSync] = useState<Date | null>(null);

  async function handleSync() {
    setSyncing(true);
    try {
      await syncEngine.sync();
      setLastSync(new Date());
    } finally {
      setSyncing(false);
    }
  }

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-4xl font-bold mb-8 text-primary">Profile</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <section className="p-6 bg-surface rounded-lg border border-foreground/10 space-y-6">
          <h2 className="text-xl font-semibold text-foreground/80">Account</h2>
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-foreground/60">User ID</span>
              <span className="font-mono text-sm">user-123</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-foreground/60">Email</span>
              <span className="font-medium">krishna@example.com</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-foreground/60">Locale</span>
              <span className="font-medium">en-US</span>
            </div>
          </div>
        </section>

        <section className="p-6 bg-surface rounded-lg border border-foreground/10 space-y-6">
          <h2 className="text-xl font-semibold text-foreground/80">System</h2>
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-foreground/60">Sync Status</span>
              <span className="text-sm text-success">Connected</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-foreground/60">Last Sync</span>
              <span className="text-sm">{lastSync ? lastSync.toLocaleString() : 'Never'}</span>
            </div>
            <button
              onClick={handleSync}
              disabled={syncing}
              className="w-full py-2 bg-primary text-white rounded-lg font-medium disabled:opacity-50"
            >
              {syncing ? 'Syncing...' : 'Sync Now'}
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}

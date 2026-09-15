"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Activity,
  BarChart3,
  BookOpen,
  CircleUserRound,
  HeartPulse,
  Home,
  Layers3,
  Plus,
  Search,
} from "lucide-react";
import { useEffect, type ReactNode } from "react";
import { usePersonalOS } from "@/app/providers/AppProvider";
import { CaptureModal } from "@/components/capture/CaptureModal";

const primaryItems = [
  { href: "/", label: "Today", icon: Home },
  { href: "/plan", label: "Plan", icon: Layers3 },
  { href: "/knowledge", label: "Knowledge", icon: BookOpen },
  { href: "/profile", label: "Profile", icon: CircleUserRound },
];

const secondaryItems = [
  { href: "/health", label: "Health", icon: HeartPulse },
  { href: "/analytics", label: "Analytics", icon: BarChart3 },
];

function isActive(pathname: string, href: string): boolean {
  return href === "/" ? pathname === "/" : pathname.startsWith(href);
}

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const { openCapture, error, captureOpen, captureType } = usePersonalOS();

  useEffect(() => {
    const handleShortcut = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        openCapture();
      }
    };
    window.addEventListener("keydown", handleShortcut);
    return () => window.removeEventListener("keydown", handleShortcut);
  }, [openCapture]);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link href="/" className="brand" aria-label="Personal-Tracker home">
          <span className="brand__mark"><Activity size={20} /></span>
          <span><strong>Personal-Tracker</strong><small>Your personal workspace</small></span>
        </Link>

        <button type="button" className="capture-button" onClick={() => openCapture()}>
          <Plus size={18} strokeWidth={2.5} />
          Quick capture
          <kbd>⌘ K</kbd>
        </button>

        <nav className="sidebar-nav" aria-label="Primary navigation">
          <span className="nav-label">Workspace</span>
          {primaryItems.map((item) => {
            const Icon = item.icon;
            return (
              <Link key={item.href} href={item.href} data-active={isActive(pathname, item.href)}>
                <Icon size={18} />
                <span>{item.label}</span>
              </Link>
            );
          })}
          <span className="nav-label nav-label--spaced">Domains</span>
          {secondaryItems.map((item) => {
            const Icon = item.icon;
            return (
              <Link key={item.href} href={item.href} data-active={isActive(pathname, item.href)}>
                <Icon size={18} />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="sidebar__status">
          <span className="status-dot" />
          <div><strong>Offline-first</strong><small>Changes saved locally</small></div>
        </div>
      </aside>

      <header className="mobile-header">
        <Link href="/" className="brand" aria-label="Personal-Tracker home">
          <span className="brand__mark"><Activity size={18} /></span>
          <span><strong>Personal-Tracker</strong></span>
        </Link>
        <Link href="/knowledge" className="icon-button" aria-label="Search knowledge">
          <Search size={19} />
        </Link>
      </header>

      <main className="app-main">
        {error && <div className="error-banner" role="alert">{error}</div>}
        {children}
      </main>

      <nav className="mobile-nav" aria-label="Primary navigation">
        {primaryItems.slice(0, 2).map((item) => {
          const Icon = item.icon;
          return (
            <Link key={item.href} href={item.href} data-active={isActive(pathname, item.href)}>
              <Icon size={20} /><span>{item.label}</span>
            </Link>
          );
        })}
        <button type="button" className="mobile-capture" onClick={() => openCapture()} aria-label="Quick capture">
          <Plus size={24} />
        </button>
        {primaryItems.slice(2).map((item) => {
          const Icon = item.icon;
          return (
            <Link key={item.href} href={item.href} data-active={isActive(pathname, item.href)}>
              <Icon size={20} /><span>{item.label}</span>
            </Link>
          );
        })}
      </nav>

      {captureOpen && <CaptureModal key={captureType} />}
    </div>
  );
}

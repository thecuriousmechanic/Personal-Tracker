import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";
import { AppProvider } from "@/app/providers/AppProvider";
import { AppShell } from "@/components/shell/AppShell";

export const metadata: Metadata = {
  title: "Personal-Tracker — Make space for what matters",
  description: "A connected, offline-first system for execution, knowledge, health, and progress.",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <AppProvider>
          <AppShell>{children}</AppShell>
        </AppProvider>
      </body>
    </html>
  );
}

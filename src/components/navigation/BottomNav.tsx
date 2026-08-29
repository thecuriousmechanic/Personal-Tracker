import React from 'react';
import Link from 'next/link';

export default function Navigation() {
  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-surface border-t border-foreground/10 p-4 flex justify-around items-center z-50">
      <Link href="/" className="text-primary hover:text-primary/80 flex flex-col items-center gap-1">
        <span className="text-xs">Home</span>
      </Link>
      <Link href="/inbox" className="text-foreground/60 hover:text-primary flex flex-col items-center gap-1">
        <span className="text-xs">Inbox</span>
      </Link>
      <button className="w-12 h-12 bg-primary text-white rounded-full shadow-lg flex items-center justify-center text-2xl font-bold">
        +
      </button>
      <Link href="/knowledge" className="text-foreground/60 hover:text-primary flex flex-col items-center gap-1">
        <span className="text-xs">Knowledge</span>
      </Link>
      <Link href="/profile" className="text-foreground/60 hover:text-primary flex flex-col items-center gap-1">
        <span className="text-xs">Profile</span>
      </Link>
    </nav>
  );
}

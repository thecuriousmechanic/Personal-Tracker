import React from 'react';
import { knowledgeService } from '@/services/knowledge.service';

export default async function KnowledgePage() {
  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-4xl font-bold mb-8 text-primary">Knowledge</h1>

      <div className="flex gap-4 mb-8">
        <input
          type="text"
          placeholder="Search notes, resources..."
          className="flex-1 p-3 bg-surface border border-foreground/10 rounded-lg text-foreground"
        />
        <button className="px-6 py-2 bg-primary text-white rounded-lg font-medium">Search</button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="md:col-span-2 space-y-4">
          <h2 className="text-xl font-semibold mb-4 text-foreground/80">Recent Notes</h2>
          <div className="space-y-3">
            <div className="p-4 bg-surface rounded-lg border border-foreground/10 hover:border-primary/50 cursor-pointer transition-colors">
              <h3 className="font-medium">RAG Architecture</h3>
              <p className="text-sm text-foreground/60 line-clamp-2">Researching vector databases and retrieval pipelines...</p>
            </div>
            <div className="p-4 bg-surface rounded-lg border border-foreground/10 hover:border-primary/50 cursor-pointer transition-colors">
              <h3 className="font-medium">Cognitive OS Design</h3>
              <p className="text-sm text-foreground/60 line-clamp-2">Mapping out the core entity relationships and data flow...</p>
            </div>
          </div>
        </div>

        <div className="space-y-8">
          <section>
            <h2 className="text-xl font-semibold mb-4 text-foreground/80">Learning</h2>
            <div className="p-4 bg-surface rounded-lg border border-foreground/10">
              <h3 className="font-medium mb-2">Computer Science</h3>
              <div className="space-y-1 text-sm text-foreground/60">
                <div className="flex items-center gap-2">
                  <span className="text-success">✓</span> Operating Systems
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-success">✓</span> Virtual Memory
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-primary">○</span> Distributed Systems
                </div>
              </div>
            </div>
          </section>

          <section>
            <h2 className="text-xl font-semibold mb-4 text-foreground/80">Resources</h2>
            <div className="space-y-2">
              <div className="p-3 bg-surface rounded-lg border border-foreground/10 text-sm">
                <span className="text-primary font-medium">Book:</span> Clean Architecture
              </div>
              <div className="p-3 bg-surface rounded-lg border border-foreground/10 text-sm">
                <span className="text-primary font-medium">Paper:</span> Attention is All You Need
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}

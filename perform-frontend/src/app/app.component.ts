/**
 * @file app.component.ts
 * @description Root component. Owns app state and orchestrates communication
 *              between AthleteInputComponent and ResultsPanelComponent.
 * @author Md Ehteshamul Haque Tamvir <mtamvir@gmail.com>
 */
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AthleteInputComponent } from './athlete-input.component';
import { ResultsPanelComponent } from './results-panel.component';
import { ApiService, JobResult } from './api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, AthleteInputComponent, ResultsPanelComponent],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>Perform — Athlete Analysis</h1>
      </header>
      <main class="page-body">
        <app-athlete-input (jobSubmitted)="onJobSubmitted($event)" />
        <app-results-panel [result]="result" [status]="status" (refresh)="fetchResult()" />
      </main>
    </div>
  `,
  styles: [`
    .page { min-height: 100vh; background: #f4f7fb; }
    .page-header { background: #1a56db; color: #fff; padding: 16px 24px; }
    .page-header h1 { margin: 0; font-size: 1.25rem; font-weight: 600; }
    .page-body { max-width: 640px; margin: 32px auto; padding: 0 16px; display: flex; flex-direction: column; gap: 24px; }
  `]
})
export class AppComponent {
  result: JobResult | null = null;
  status: 'idle' | 'polling' | 'done' | 'error' = 'idle';

  locationUrl = '';

  constructor(private api: ApiService) {}

  onJobSubmitted(locationUrl: string) {
    this.result = null;
    this.status = 'polling';
    this.locationUrl = locationUrl;
    this.fetchResult();
  }

  async fetchResult() {
    this.status = 'polling';
    try {
      const res = await this.api.getAnalysis(this.locationUrl);
      this.result = res;
      this.status = 'done';
    } catch (err: any) {
      this.status = 'error';
    }
  }
}

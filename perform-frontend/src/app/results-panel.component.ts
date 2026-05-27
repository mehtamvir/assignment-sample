/**
 * @file results-panel.component.ts
 * @description Purely presentational component. Displays job state and result
 *              received via @Input. Emits a refresh event on user request.
 * @author Md Ehteshamul Haque Tamvir <mtamvir@gmail.com>
 */
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JobResult } from './api.service';

@Component({
  selector: 'app-results-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card" *ngIf="status !== 'idle'">
      <div class="panel-header">
        <span class="panel-title">Results</span>
        <span class="status-badge" [class]="status">{{ statusLabel }}</span>
        <button class="btn-refresh" (click)="refresh.emit()" [disabled]="status === 'polling'">Check Status</button>
      </div>
      <div class="spinner" *ngIf="status === 'polling'"></div>
      <pre class="result-pre" *ngIf="result">{{ result | json }}</pre>
    </div>
  `,
  styles: [`
    .card { background: #fff; border-radius: 10px; padding: 24px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
    .panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .panel-title { font-weight: 600; font-size: 1rem; }
    .status-badge { font-size: .8rem; padding: 3px 10px; border-radius: 12px; font-weight: 500; }
    .status-badge.polling { background: #e8f0fe; color: #1a56db; }
    .status-badge.done    { background: #e6f9f0; color: #1a7a4a; }
    .status-badge.error   { background: #fdecea; color: #c0392b; }
    .spinner { width: 24px; height: 24px; border: 3px solid #d1ddf5; border-top-color: #1a56db; border-radius: 50%; animation: spin .8s linear infinite; margin: 8px 0; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .btn-refresh { font-size: .8rem; padding: 4px 10px; border: 1px solid #1a56db; background: #fff; color: #1a56db; border-radius: 6px; cursor: pointer; }
    .btn-refresh:disabled { opacity: .5; cursor: not-allowed; }
    .result-pre { background: #f4f7fb; border-radius: 7px; padding: 14px; font-size: .85rem; overflow-x: auto; margin: 0; }
  `]
})
export class ResultsPanelComponent {
  @Input() result: JobResult | null = null;
  @Input() status: 'idle' | 'polling' | 'done' | 'error' = 'idle';
  @Output() refresh = new EventEmitter<void>();

  get statusLabel(): string {
    return { idle: '', polling: 'Polling...', done: 'Complete', error: 'Error' }[this.status];
  }
}

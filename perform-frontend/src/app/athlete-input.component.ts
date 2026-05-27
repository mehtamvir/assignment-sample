/**
 * @file athlete-input.component.ts
 * @description Presentational input component. Collects athlete name, validates,
 *              submits the analysis job and emits the job location URL to the parent.
 * @author Md Ehteshamul Haque Tamvir <mtamvir@gmail.com>
 */
import { Component, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ApiService } from './api.service';

@Component({
  selector: 'app-athlete-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <label class="field-label" for="athlete-input">Athlete</label>
      <input
        id="athlete-input"
        class="field-input"
        [(ngModel)]="athlete"
        name="athlete"
        placeholder="Name or ID (max 100 chars)"
        maxlength="100"
        [disabled]="loading"
      />
      <p class="field-error" *ngIf="error">{{ error }}</p>
      <button class="btn-primary" (click)="submit()" [disabled]="loading">
        {{ loading ? 'Submitting...' : 'Run Analysis' }}
      </button>
    </div>
  `,
  styles: [`
    .card { background: #fff; border-radius: 10px; padding: 24px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
    .field-label { display: block; font-weight: 600; margin-bottom: 6px; font-size: .9rem; }
    .field-input { width: 100%; padding: 10px 12px; border: 1px solid #d1ddf5; border-radius: 7px; font-size: 1rem; box-sizing: border-box; }
    .field-input:focus { outline: none; border-color: #1a56db; }
    .field-error { color: #c0392b; font-size: .875rem; margin: 6px 0 0; }
    .btn-primary { margin-top: 14px; width: 100%; padding: 10px; background: #1a56db; color: #fff; border: none; border-radius: 7px; font-size: 1rem; cursor: pointer; }
    .btn-primary:disabled { opacity: .6; cursor: not-allowed; }
  `]
})
export class AthleteInputComponent {
  @Output() jobSubmitted = new EventEmitter<string>();

  athlete = '';
  loading = false;
  error = '';

  constructor(private api: ApiService) {}

  async submit() {
    this.error = '';
    if (!this.athlete.trim()) { this.error = 'Please enter an athlete name.'; return; }
    this.loading = true;
    try {
      const location = await this.api.submitAnalysis(this.athlete.trim());
      this.jobSubmitted.emit(location);
    } catch (err: any) {
      if (err?.status === 400) this.error = 'Validation failed: blank or oversized input.';
      else if (err?.status === 429) this.error = 'Rate limit exceeded — try again shortly.';
      else this.error = 'Network or server error.';
    } finally {
      this.loading = false;
    }
  }
}

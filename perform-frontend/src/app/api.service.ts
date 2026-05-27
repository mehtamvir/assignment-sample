/**
 * @file api.service.ts
 * @description HTTP service layer. Wraps POST /analysis and GET /analysis/{id}
 *              endpoints. No UI logic — returns plain data or throws on error.
 * @author Md Ehteshamul Haque Tamvir <mtamvir@gmail.com>
 */
import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface JobResult { state?: string; metrics?: any; }

@Injectable({ providedIn: 'root' })
export class ApiService {
  private base = '/api/v1';
  constructor(private http: HttpClient) {}

  async submitAnalysis(athlete: string): Promise<string> {
    const res = await firstValueFrom(
      this.http.post(`${this.base}/analysis`, { athlete }, { observe: 'response' })
    );
    const location = (res as HttpResponse<any>).headers.get('Location') ?? (res as HttpResponse<any>).headers.get('location');
    if (res.status === 202 && location) return location;
    throw { status: res.status };
  }

  async getAnalysis(locationUrl: string): Promise<JobResult> {
    const id = locationUrl.split('/').pop();
    const res = await firstValueFrom(
      this.http.get<JobResult>(`${this.base}/analysis/${id}`, { observe: 'response' })
    );
    return res.body ?? {};
  }
}

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { AuthService } from './core/auth.service';

describe('App', () => {
  const user = signal<{ username: string; roles: string[] } | null>(null);

  beforeEach(async () => {
    user.set(null);
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { user, isAdmin: () => false, logout: () => {} } }
      ]
    }).compileComponents();
  });

  it('hides the navigation when nobody is logged in', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('nav')).toBeNull();
  });

  it('shows the navigation and the user once logged in', async () => {
    user.set({ username: 'lecteur', roles: ['USER'] });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(Array.from(el.querySelectorAll('nav a')).map(a => a.textContent?.trim())).toEqual(['Documents', 'Assistant']);
    expect(el.querySelector('.account .user')?.textContent).toContain('lecteur');
  });
});

import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'documents' },
  {
    path: 'login',
    title: 'Connexion · Assistant documentaire',
    loadComponent: () => import('./pages/login/login-page').then(m => m.LoginPage)
  },
  {
    path: 'documents',
    title: 'Documents · Assistant documentaire',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/documents/documents-page').then(m => m.DocumentsPage)
  },
  {
    path: 'chat',
    title: 'Assistant · Assistant documentaire',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/chat/chat-page').then(m => m.ChatPage)
  },
  { path: '**', redirectTo: 'documents' }
];

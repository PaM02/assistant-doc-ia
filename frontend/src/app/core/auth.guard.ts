import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/** Bloque l'accès aux pages tant que l'utilisateur n'est pas connecté. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.token ? true : inject(Router).createUrlTree(['/login'], { queryParams: { redirect: state.url } });
};

import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Error interceptor to handle HTTP errors globally
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);
  const translate = inject(TranslateService);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message = '';

      switch (error.status) {
        case 401:
          // Unauthorized - token expired or invalid
          message = translate.instant('errors.unauthorized');
          authService.logout();
          router.navigate(['/login']);
          break;

        case 403:
          // Forbidden - no permission
          message = error.error?.message || translate.instant('errors.forbidden');
          break;

        case 404:
          // Not found
          message = error.error?.message || translate.instant('errors.notFound');
          break;

        case 500:
          // Server error
          message = translate.instant('errors.serverError');
          break;

        case 0:
          // Network error
          message = translate.instant('errors.network');
          break;

        default:
          // Other errors
          message = error.error?.message || translate.instant('errors.generic');
          break;
      }

      // Show error notification
      if (message) {
        snackBar.open(message, translate.instant('common.close'), {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }

      return throwError(() => error);
    })
  );
};

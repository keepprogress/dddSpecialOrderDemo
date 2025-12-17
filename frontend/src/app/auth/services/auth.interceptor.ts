import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap, catchError, throwError } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';
import { Router } from '@angular/router';
import { ErrorHandlerService } from '../../core/services/error-handler.service';

/**
 * Auth Interceptor (Angular 21+ Functional Interceptor)
 * 自動附加 Bearer Token 到 HTTP 請求
 * 處理 401/403 錯誤並使用統一錯誤處理
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);
  const errorHandler = inject(ErrorHandlerService);

  // 跳過公開端點
  if (isPublicEndpoint(req.url)) {
    return next(req);
  }

  return from(keycloak.getToken()).pipe(
    switchMap((token) => {
      if (token) {
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        });
        return next(authReq);
      }
      return next(req);
    }),
    catchError((error: HttpErrorResponse) => {
      // 使用統一錯誤處理
      errorHandler.handleHttpError(error);

      if (error.status === 401) {
        // Token 過期，重新登入
        keycloak.login();
      } else if (error.status === 403) {
        // 權限不足，導向錯誤頁面
        router.navigate(['/error', 'forbidden']);
      } else if (error.status === 0) {
        // 無法連線，可能是 Keycloak 不可用
        errorHandler.handleKeycloakUnavailable();
      }
      return throwError(() => error);
    })
  );
};

/**
 * 判斷是否為公開端點
 */
function isPublicEndpoint(url: string): boolean {
  const publicPaths = ['/health', '/actuator/health', '/public'];
  return publicPaths.some((path) => url.includes(path));
}

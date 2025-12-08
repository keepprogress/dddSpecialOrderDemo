# Claude Codebase Guide - Angular 21+

This document outlines the coding standards and architectural patterns for this Angular 21+ project.

## Tech Stack
- **Angular**: v21 (Standalone, Signals, Zoneless-ready)
- **TypeScript**: Latest
- **Bundler**: Esbuild/Vite

## Key Patterns & Rules

### 1. Modern Angular Syntax
- **MUST** use Standalone Components (`standalone: true`).
- **MUST** use Angular Signals for local state (`signal()`, `computed()`, `input()`, `output()`).
- **MUST** use New Control Flow (`@if`, `@for`, `@switch`).
- **MUST** use `inject()` for dependency injection.
- **AVOID** `NgModule` unless absolutely necessary for legacy library integration.

### 2. Signal-based Inputs/Outputs
- Use `input()` and `input.required()` instead of `@Input()`.
- Use `output()` instead of `@Output()` with `EventEmitter`.
- Use `model()` for two-way binding.

### 3. Reactive Programming
- Prefer **Signals** for synchronous state and UI bindings.
- Use **RxJS** mainly for asynchronous events (HTTP) and complex orchestrations.
- Use `toSignal` to convert Observables to Signals for template consumption.

### 4. Code Style
- File names: `kebab-case` (e.g., `user-profile.component.ts`).
- Class names: `PascalCase` (e.g., `UserProfileComponent`).
- Service names: `UserProfileService`.

### 5. Template Example
```html
<!-- BAD -->
<div *ngIf="users$ | async as users">
  <div *ngFor="let user of users">{{ user.name }}</div>
</div>

<!-- GOOD -->
@if (users(); as usersList) {
  @for (user of usersList; track user.id) {
    <div>{{ user.name }}</div>
  } @empty {
    <p>No users found.</p>
  }
}
```

### 6. Logic Example
```typescript
export class UserComponent {
  private userService = inject(UserService);
  
  // Signal input
  userId = input.required<string>();
  
  // Resource (Experimental/New in recent versions) or toSignal
  user = toSignal(this.userService.getUser(this.userId));
}
```

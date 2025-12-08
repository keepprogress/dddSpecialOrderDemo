# Angular 21+ Project Context & Best Practices for Gemini

## Project Overview
This is a modern Angular 21+ frontend application using Standalone Components, Signals, and the latest control flow syntax.

## Core Technologies
- **Framework**: Angular 21+
- **Language**: TypeScript 5.9+
- **Build Tool**: Angular CLI (via Vite/Esbuild)
- **Styling**: CSS
- **State Management**: Angular Signals (Preferred over RxJS for synchronous state)

## Coding Guidelines

### 1. Components
- **Standalone**: All components must be `standalone: true`.
- **Imports**: Import dependencies directly in the `@Component` metadata.
- **Change Detection**: Use `ChangeDetectionStrategy.OnPush` by default.

### 2. State Management (Signals)
- Use `signal()` for mutable state.
- Use `computed()` for derived state.
- Use `effect()` sparingly for side effects.
- Avoid `Zone.js` reliance where possible (prepare for Zoneless).

### 3. Control Flow
- Use the new built-in control flow syntax:
  - `@if`, `@else` instead of `*ngIf`
  - `@for`, `@empty` instead of `*ngFor`
  - `@switch`, `@case`, `@default` instead of `[ngSwitch]`

### 4. Dependency Injection
- Use `inject()` function instead of constructor injection.

### 5. Performance
- Use `@defer` for lazy loading components (deferrable views).
- Optimize images with `NgOptimizedImage`.

## Project Structure
- `src/app`: Core application logic.
- `src/main.ts`: Application bootstrap (using `bootstrapApplication`).

## Example Component
```typescript
import { Component, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-example',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1>{{ title() }}</h1>
    <button (click)="increment()">Count: {{ count() }}</button>
    <p>Double: {{ doubleCount() }}</p>

    @if (count() > 5) {
      <p>High count!</p>
    }
  `,
  styles: [`:host { display: block; }`]
})
export class ExampleComponent {
  // Signals
  title = signal('Hello Gemini');
  count = signal(0);
  
  // Computed
  doubleCount = computed(() => this.count() * 2);

  // Actions
  increment() {
    this.count.update(c => c + 1);
  }
}
```

# Copilot / AI agent instructions for this repository

**Short summary:** This is a Vue 3 + Vite + TypeScript single-page app using Pinia for state, `vue-router` for navigation, and simple role-based UI controls. The app currently uses mocked authentication stored in `src/stores/userData.ts`.

## Quick start (commands)
- Start dev server: `npm run dev` (calls `vite`)
- Build for production: `npm run build` (calls `vite build`)
- Type-check: `npm run type-check` (runs `vue-tsc --build`)
- Format code: `npm run format` (runs `prettier --write src/`)

> Node engine constraint in `package.json` - target Node ^20.19.0 or >=22.12.0 when running dev/build tasks.

## Big-picture architecture
- Frontend SPA (no backend included in repo)
- Tech stack: Vue 3 (Composition API + `<script setup>`), TypeScript, Vite
- State management: Pinia stores in `src/stores/` (e.g., `userData.ts` uses setup-style `defineStore`)
- Routing: `src/router/index.ts` — routes are nested under Layout components and use route meta flags:
  - `meta: { public: true }` for public routes
  - `meta: { requiresAuth: true }` for protected routes
  - `router.beforeEach` hydrates the `userData` store each navigation
- Layouts: `src/layouts/DefaultLayout.vue` (sidebar + content) and `NoNavLayout.vue` (public pages)
- Views: `src/views/` holds page components. `Dashboard` pages are grouped under `src/views/Dashboard/`.

## Project-specific patterns & conventions (do not invent new patterns)
- Component naming: PascalCase `.vue` files (e.g., `HeaderLogo.vue`, `NavMenu.vue`) and `@/` alias import (see `tsconfig.app.json` paths)
- Use of Composition API & `<script setup>` everywhere; prefer `ref`, `computed`, and `defineStore` setup style
- Route protection pattern:
  - Add `meta.requiresAuth` to routes that need authentication
  - `router.beforeEach` calls `useUserDataStore().hydrate()` and checks `userStore.isLoggedIn`
- Role-based UI: navigation and visibility are filtered by role strings matching the `UserRole` enum in `src/stores/userData.ts`.
  - Example: `NavMenu.vue` filters `allNavItems` by `item.roles` (an array of `UserRole` values).
- Local development auth is mocked in `src/stores/userData.ts` with `TEMPORARY_ACCOUNTS` and a `login` method that checks the selected role + credentials. The store persists user info to `localStorage` under `user_data` encoded with `btoa`/`atob`.

## How to add common changes (examples)
- Add a new page:
  1. Create `src/views/MyPage.vue` using `<script setup>`.
  2. Add route in `src/router/index.ts` (choose which Layout and set `meta.public` or `meta.requiresAuth`).
  3. If it should appear in the sidebar, add an entry to `allNavItems` in `src/components/NavMenu.vue` and include `roles` when access is limited.

- Add a new store:
  - Create `src/stores/myStore.ts` using `defineStore('myStore', () => { ... })` to match existing stores.

- Replace mocked auth with a backend:
  - Replace `login` in `src/stores/userData.ts` to call your API and then `setUser` on success.
  - Remove `TEMPORARY_ACCOUNTS` and ensure `hydrate()` reads server-issued tokens or user objects (current `localStorage` key is `user_data`).

## Files to review first when debugging or adding features
- `src/stores/userData.ts` — authentication, roles, and persistent login behavior
- `src/router/index.ts` — access control and navigation structure
- `src/components/NavMenu.vue` — role-based navigation items
- `src/layouts/DefaultLayout.vue` — main app frame (sidebar + content)
- `src/main.ts` — app initialization (Pinia + Router)

## Mini-FAQ / gotchas (from repo observation)
- No tests / CI are provided in repo; expect to run manual checks in browser and `npm run type-check`.
- Passwords and mock accounts are in source code for local dev only — do not ship them to production. Search `TEMPORARY_ACCOUNTS` to find these.
- Route selection uses `router.path === path` in `NavMenu.vue` which can be brittle for nested routes; be careful when adding parameterized routes.
- The `UserRole` enum defines recognized roles. Some UI code contains values or role names without corresponding temporary accounts (e.g., `OC`), so verify role availability when adding role-restricted UI.

---
If anything here is unclear or you'd like additional examples (e.g., a suggested PR template or task checklist for adding backend auth), tell me which areas to expand and I will iterate. ✅

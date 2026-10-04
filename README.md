# Barber Agenda — App móvil

Aplicación para que las clientas de **Joss Estética y Barbería** consulten horarios disponibles y reserven. Forma parte del sistema Barber Agenda junto con [barber-agenda-web](../barber-agenda-web), que contiene el panel de la dueña y la API.

Proyecto Cuatrimestral Integral · IDGS · UTJ · 2026C · 9.° B · Equipo 1.

## Estado

Estructura inicial. La funcionalidad de reservas está **fuera del alcance de la entrega de este mes**; ver [docs/alcance.md](docs/alcance.md).

## Tecnología

- React Native con Expo
- Consumirá la API REST de `barber-agenda-web`; la app no guarda reglas de disponibilidad propias.

## Arranque

```bash
git clone <url-del-repo> barber-agenda-mobile
cd barber-agenda-mobile
npm install
npx expo start
```

Escanea el código QR con la app Expo Go en tu teléfono.

## Estructura

```
App.js                 punto de entrada
src/screens/           pantallas de la app
docs/                  alcance y decisiones
assets/                íconos y splash
```

## Forma de trabajo

Misma convención que el repositorio web: rama por historia de Jira (`feature/SCRUM-XX-descripcion`), commits con la clave al inicio y pull request revisado por otro integrante antes de fusionar a `main`.

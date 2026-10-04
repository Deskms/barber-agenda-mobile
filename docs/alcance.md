# Alcance de la app móvil

## Prevista

- Consultar servicios con su duración y precio.
- Consultar horarios libres por servicio y día.
- Reservar, ver y cancelar citas propias.
- Recibir confirmación de la reserva.

## En esta entrega

La entrega del mes se concentra en el panel web y el canal de WhatsApp. Para la app móvil solo se entrega:

- Proyecto Expo con estructura inicial.
- Este documento de alcance y el README.

## Regla que la app debe respetar

La app **no calcula disponibilidad por su cuenta**. Siempre consulta a la API de Laravel y la API revalida antes de crear la cita. Así, la app, el panel y WhatsApp trabajan sobre la misma agenda y no se confirman citas empalmadas.

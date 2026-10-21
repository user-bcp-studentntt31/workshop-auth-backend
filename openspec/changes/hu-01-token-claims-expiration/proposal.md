# Propuesta: HU-01 Token firmado con expiracion y claims minimos

## Referencia
- Issue: #13
- URL: https://github.com/user-bcp-studentntt31/workshop-auth-backend/issues/13

## Por que importa
El endpoint POST /auth/login emite un token usado por clientes de laboratorio. El Issue #13 exige trazabilidad minima del emisor y tiempo de vida verificable para reducir riesgos de suplantacion y de uso de tokens fuera de ventana valida.

## Alcance
- Definir requisitos observables para que el token de login incluya claims minimos: sub, client, iat y exp.
- Definir validacion del campo client de entrada para que el token represente el cliente solicitante.
- Definir coherencia verificable entre expiresIn y la expiracion real del token.
- Definir requisitos de no exposicion de datos sensibles en logs durante el flujo de autenticacion.
- Definir requisito de decision documentada sobre mecanismo de token (continuidad HMAC actual o adopcion de libreria JWT).

## Fuera de alcance
- Diseno tecnico de implementacion.
- Refactor estructural del modulo de autenticacion.
- Cambio de otros endpoints distintos de POST /auth/login.
- Migracion completa del modelo de usuarios o de almacenamiento.

## Comportamiento que debe preservarse
Baseline: docs/ENDPOINTS.md (Dia 1)
- Se preserva el endpoint POST /auth/login como endpoint publico sin autenticacion previa.
- Se preservan los flujos base de respuesta: 200 para login valido, 400 para validaciones de entrada, 401 para credenciales invalidas, 500 para error no controlado.
- Se preserva la respuesta con token, expiresIn y user.
- Se preserva GET /q/health sin cambios.

## Cambio explicito sobre el contrato baseline
En docs/ENDPOINTS.md el campo client figura como opcional y sin impacto en autenticacion. Este cambio actualiza el comportamiento comprometido por el Issue #13: client pasa a ser validado y utilizado para el token emitido.

## Verificacion esperada
- Escenarios observables para claims minimos del token.
- Escenarios observables de coherencia temporal de expiracion.
- Escenarios observables de validacion de client.
- Escenarios observables de ausencia de datos sensibles en logs.
- Evidencia de decision documentada sobre mecanismo de token.

# Architecture Decision - HU-01 (Token firmado con expiración y claims mínimos)

## Contexto y entrada
- Spec base: `HU-01` (aprobada con ajustes) y checklist generado en `HU-01.output.md`.
- Sistema actual observado:
  - Endpoint [AuthResource.login](C:/Users/Usuario/Documents/t53174-felix-chacaliaza-gutierrez/reqflow-workspace/workshop-auth-backend/src/main/java/com/bcp/workshop/auth/AuthResource.java:24)
  - Acceso a datos [LegacyDatabase.findByCredentials](C:/Users/Usuario/Documents/t53174-felix-chacaliaza-gutierrez/reqflow-workspace/workshop-auth-backend/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:32)
  - Emisión de token [LegacyTokenUtil.create](C:/Users/Usuario/Documents/t53174-felix-chacaliaza-gutierrez/reqflow-workspace/workshop-auth-backend/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:16)
  - Prueba actual [AuthResourceTest.logsInWithWorkshopUser](C:/Users/Usuario/Documents/t53174-felix-chacaliaza-gutierrez/reqflow-workspace/workshop-auth-backend/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:12)

## Impacto
- **Funcional**: se mantiene `POST /auth/login`; se refuerza validación de entrada y consistencia de expiración.
- **Seguridad**: se exige claim `client`, se endurece la política de secreto de firma y se protege el logging de datos sensibles.
- **Compatibilidad observable**: se conserva forma de respuesta (`token`, `expiresIn`, `user`) y códigos HTTP principales ya usados por cliente.
- **Operación**: se incrementa sensibilidad a configuración (`AUTH_TOKEN_SECRET`) fuera de laboratorio.

## Componentes afectados
- [AuthResource.java](C:/Users/Usuario/Documents/t53174-felix-chacaliaza-gutierrez/reqflow-workspace/workshop-auth-backend/src/main/java/com/bcp/workshop/auth/AuthResource.java)
  - Validación de `client`.
  - Ensamble de respuesta con `expiresIn` consistente con `exp`.
  - Manejo de errores sin exponer secretos/tokens.
- [LegacyTokenUtil.java](C:/Users/Usuario/Documents/t53174-felix-chacaliaza-gutierrez/reqflow-workspace/workshop-auth-backend/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java)
  - Construcción de claims mínimos obligatorios (`sub`, `client`, `iat`, `exp`).
  - Política de resolución de secreto (`AUTH_TOKEN_SECRET` vs default de laboratorio).
  - Cálculo de expiración para coherencia verificable.
- [AuthResourceTest.java](C:/Users/Usuario/Documents/t53174-felix-chacaliaza-gutierrez/reqflow-workspace/workshop-auth-backend/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java)
  - Cobertura de login válido, credenciales inválidas y expiración.
  - Verificación de claims y consistencia de expiración.

## Contratos protegidos
- **Contrato HTTP de login**:
  - Ruta y método: `POST /auth/login` (sin cambio).
  - Éxito: `200` con `token`, `expiresIn`, `user`.
  - Error de credenciales: `401` con mensaje controlado.
  - Error de validación: `400` para request inválido.
- **Contrato de token**:
  - Debe incluir al menos `sub`, `client`, `iat`, `exp`.
  - `expiresIn` debe representar `exp - now` dentro de tolerancia definida.
- **Contrato de seguridad operativa**:
  - No registrar token, secreto ni payload sensible en logs.
  - Secreto de firma configurable por entorno.

## Estrategia incremental
1. **Blindar pruebas antes del cambio**
   - Añadir/ajustar pruebas que fijen contrato actual esperado y nuevos criterios HU-01.
2. **Refactor mínimo de emisión de token**
   - Centralizar cálculo de tiempos (`iat`, `exp`, `expiresIn`) para eliminar drift.
3. **Endurecer validación y configuración**
   - Validar `client` de forma explícita.
   - Aplicar política de secreto por entorno (manteniendo excepción de laboratorio documentada).
4. **Higiene de logs y errores**
   - Revisar mensajes y trazas para evitar exposición de datos sensibles.
5. **Evidencia de cierre**
   - Ejecutar pruebas necesarias y adjuntar evidencia solicitada por HU-01.

Cada paso es pequeño, reversible y mantiene compatibilidad observable.

## Pruebas necesarias
- **Unitarias / integración de API**
  - Login válido retorna `200` y estructura esperada.
  - Credenciales inválidas retornan `401`.
  - Request inválido (incluyendo `client` ausente/vacío) retorna `400`.
- **Semántica de token**
  - Verificar presencia de `sub`, `client`, `iat`, `exp`.
  - Verificar relación `expiresIn ~= exp - now` con tolerancia explícita.
- **Seguridad operativa**
  - Verificar que logs de flujo auth no incluyan token ni secreto.
- **No regresión**
  - Mantener forma de `user` y comportamiento básico de login consumido por frontend.

## Riesgos / trade-offs
- **Riesgo**: endurecer validación de `client` puede rechazar clientes que hoy pasan.
  - **Trade-off**: más seguridad y trazabilidad, posible ajuste en consumidores.
- **Riesgo**: remover/limitar secreto por defecto rompe ejecuciones locales no configuradas.
  - **Trade-off**: mejora de postura de seguridad vs fricción de onboarding.
- **Riesgo**: cambios de tiempo (`expiresIn`) generan flakiness por latencia de ejecución.
  - **Trade-off**: exactitud del contrato temporal vs necesidad de tolerancia técnica.
- **Riesgo**: migrar a librería JWT reduce código propio pero introduce dependencia/curva.
  - **Trade-off**: robustez criptográfica y mantenimiento vs costo de adopción inmediata.

## Decisiones abiertas
1. **Firma de token**: mantener implementación HMAC actual o migrar a librería JWT.
2. **Política de secreto por entorno**: criterio exacto para permitir default solo en laboratorio.
3. **Regla de validación de `client`**: formato/whitelist y estrategia de error.
4. **Tolerancia temporal**: valor oficial para validar `expiresIn` frente a `exp - now`.

## Punto de control (obligatorio)
**Detenido para revisión humana antes de implementar.**  
No se han realizado cambios de código fuente en esta etapa.

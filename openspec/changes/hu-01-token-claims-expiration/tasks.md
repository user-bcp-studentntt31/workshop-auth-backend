# Tasks - HU-01 Token firmado con expiracion y claims minimos

## Alcance y trazabilidad
- Unidad de trabajo de producto: GitHub Issue #13.
- Fuente funcional aprobada: proposal.md, specs/auth-login/spec.md y design.md del cambio hu-01-token-claims-expiration.
- Restriccion: no incluir refactors amplios, rediseno total ni mejoras no aprobadas.

## Fase 1 - Caracterizacion (antes de cambio funcional)

- [x] 1.1 Caracterizar login exitoso actual
  - Accion: ampliar pruebas para fijar contrato 200 con token, expiresIn, user y debug.
  - Criterio de verificacion observable: la prueba pasa y confirma estructura de respuesta protegida.

- [x] 1.2 Caracterizar errores actuales de credenciales
  - Accion: agregar prueba para credenciales invalidas con respuesta 401.
  - Criterio de verificacion observable: la prueba pasa y valida estado 401.

- [x] 1.3 Caracterizar validaciones actuales de request
  - Accion: agregar pruebas para request nulo y username/password nulos, vacios o en blanco con 400.
  - Criterio de verificacion observable: todas las pruebas pasan con estado 400 esperado.

- [x] 1.4 Caracterizar comportamiento actual de client
  - Accion: agregar pruebas que documenten el estado actual cuando client falta o viene en blanco.
  - Criterio de verificacion observable: existe evidencia automatizada del comportamiento baseline previo al cambio HU-01.

### Evidencia de caracterizacion (2026-10-21)
- Comando: `mvn -q -f "C:\Users\Usuario\Documents\t53174-felix-chacaliaza-gutierrez\reqflow-workspace\workshop-auth-backend\pom.xml" -Dtest=AuthResourceTest test`
- Resultado final: 6 pruebas, 0 fallos, 0 errores y 0 omitidas. La suite usa una SQLite temporal mediante `AuthDatabaseTestResource`.
- Diagnostico del primer intento sin aislamiento: 1 prueba fallo porque el login devolvio 401 en vez de 200; al aislar la base de test, la caracterizacion paso sin cambiar la logica de autenticacion ni el contrato HTTP.

## Fase 2 - Implementacion incremental

- [ ] 2.1 Configurar secreto de firma desde entorno
  - Accion: mover el secreto de firma a configuracion externa mediante AUTH_TOKEN_SECRET y eliminar dependencia del secreto hardcodeado.
  - Criterio de verificacion observable: con secreto valido la aplicacion emite token; sin secreto valido falla de forma controlada sin exponer secretos.

- [x] 2.2 Incorporar biblioteca de token estandar
  - Accion: agregar dependencia mantenida para JWT firmado HS256 y preparar utilitario de emision.
  - Criterio de verificacion observable: build y pruebas compilan con la nueva dependencia sin romper rutas existentes.

- [x] 2.3 Validar client como campo obligatorio
  - Accion: en login rechazar client ausente, nulo, vacio o en blanco con 400 y mensaje claro.
  - Criterio de verificacion observable: pruebas de client ausente y en blanco pasan con 400 y mensaje correspondiente.

- [ ] 2.4 Emitir token con claims minimos
  - Accion: emitir token firmado con sub, client, iat y exp usando el mismo instante base.
  - Criterio de verificacion observable: pruebas validan presencia y coherencia de claims en token exitoso.

- [ ] 2.5 Alinear expiresIn con exp
  - Accion: calcular expiresIn coherente con la expiracion real del token segun tolerancia aprobada.
  - Criterio de verificacion observable: prueba de coherencia temporal pasa con tolerancia maxima de 5 segundos.

- [ ] 2.6 Proteger observabilidad sensible
  - Accion: asegurar que el flujo de login no registre token completo ni valor de AUTH_TOKEN_SECRET.
  - Criterio de verificacion observable: pruebas o validaciones de logs demuestran ausencia de token completo y secreto.

## Fase 3 - Validacion y evidencia de cierre

- [ ] 3.1 Ejecutar suite de pruebas del modulo auth
  - Accion: ejecutar pruebas unitarias/integracion del endpoint de autenticacion tras los cambios.
  - Criterio de verificacion observable: todas las pruebas del modulo auth pasan.

- [ ] 3.2 Verificar preservacion de contratos protegidos
  - Accion: comprobar que se mantiene endpoint, estados base (200, 400, 401, 500) y estructura user del contrato.
  - Criterio de verificacion observable: reporte de pruebas evidencia preservacion de contrato y cambio explicito de client.

- [ ] 3.3 Documentar decision tecnica HMAC vs JWT
  - Accion: registrar en documento tecnico la decision final, fecha, impacto y riesgos.
  - Criterio de verificacion observable: existe evidencia trazable de la decision referenciada por el cambio OpenSpec.

- [ ] 3.4 Actualizar contrato funcional del endpoint
  - Accion: actualizar documentacion de endpoint para reflejar client obligatorio y semantica de token firmada.
  - Criterio de verificacion observable: la documentacion publicada coincide con comportamientos verificados por pruebas.

- [ ] 3.5 Consolidar evidencia para el Issue #13
  - Accion: compilar resultados de pruebas, validaciones de seguridad de logs y evidencia documental en el cierre del trabajo.
  - Criterio de verificacion observable: checklist completo con enlaces o referencias a evidencia verificable para aprobacion.

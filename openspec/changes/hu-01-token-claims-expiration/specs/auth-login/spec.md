## ADDED Requirements

### Requirement: Login token con claims minimos obligatorios
El sistema SHALL emitir en POST /auth/login un token valido que represente al usuario autenticado e incluya los claims sub, client, iat y exp.

#### Scenario: Emision de token con claims minimos
- GIVEN un usuario valido con credenciales correctas
- AND un client valido
- WHEN el cliente invoca POST /auth/login
- THEN la respuesta es 200
- AND el token emitido contiene sub, client, iat y exp
- AND sub identifica al usuario autenticado
- AND client coincide con el client de la solicitud

### Requirement: Coherencia entre expiresIn y expiracion real
El sistema SHALL mantener coherencia entre expiresIn y la expiracion real del token mediante una tolerancia temporal definida por la especificacion.

#### Scenario: Expiracion coherente
- GIVEN un login exitoso
- WHEN se obtiene la respuesta de POST /auth/login
- THEN expiresIn es consistente con exp - now dentro de una tolerancia de 5 segundos

### Requirement: Validacion de client en login
El sistema SHALL validar client en la solicitud de login y rechazar solicitudes sin client valido.

#### Scenario: Client ausente
- GIVEN una solicitud de login con username y password validos
- AND sin campo client
- WHEN se invoca POST /auth/login
- THEN la respuesta es 400
- AND el mensaje de error describe que client es requerido

#### Scenario: Client en blanco
- GIVEN una solicitud de login con username y password validos
- AND client vacio o en blanco
- WHEN se invoca POST /auth/login
- THEN la respuesta es 400
- AND el mensaje de error describe que client es invalido

### Requirement: No exposicion de informacion sensible en logs
El sistema SHALL evitar registrar token completo, secreto de firma o cualquier valor sensible durante el flujo de autenticacion.

#### Scenario: Observabilidad segura en login exitoso
- GIVEN un login exitoso
- WHEN se revisan los eventos de log emitidos por el flujo
- THEN no aparece el token completo
- AND no aparece AUTH_TOKEN_SECRET ni su valor

### Requirement: Decision documentada sobre mecanismo de token
El sistema SHALL registrar una decision tecnica explicita sobre continuidad de HMAC actual o adopcion de libreria JWT, con justificacion y fecha.

#### Scenario: Evidencia de decision disponible
- GIVEN la propuesta aprobada para HU-01
- WHEN se revisa la documentacion de decisiones tecnicas
- THEN existe un registro trazable de la decision HMAC vs JWT
- AND el registro identifica impacto, riesgos y criterio de adopcion

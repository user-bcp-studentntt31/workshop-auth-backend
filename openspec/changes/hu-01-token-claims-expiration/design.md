# Diseno: HU-01 Token firmado con expiracion y claims minimos

## Contexto tecnico relevante

La fuente funcional aprobada es `specs/auth-login/spec.md`; el contrato
observable previo esta descrito en `docs/ENDPOINTS.md`.

Hoy `POST /auth/login` valida `username` y `password`, consulta al usuario y
devuelve `token`, `expiresIn`, `user` y `debug`. El campo `client` existe en el
request pero no se valida ni se usa. La prueba automatizada actual solo cubre
un login exitoso.

El token actual **no esta firmado**: `LegacyTokenUtil` concatena username, rol,
expiracion y un secreto fijo en el codigo, y luego codifica el texto con
Base64. Base64 no protege ni autentica el contenido; el secreto se puede leer
al decodificar el token. `expiresIn` se informa como 3600 segundos y la
expiracion se calcula por separado. La configuracion actual no define
`AUTH_TOKEN_SECRET` y el flujo no tiene registros de log propios.

## Goals

- Emitir un token firmado que contenga `sub`, `client`, `iat` y `exp`.
- Usar como `sub` la identidad del usuario autenticado y conservar en `client`
  el valor no vacio enviado en la solicitud.
- Rechazar `client` ausente, nulo, vacio o compuesto solo por espacios con
  HTTP 400 y un mensaje que explique el motivo.
- Mantener `expiresIn` coherente con la expiracion del token dentro de la
  tolerancia aprobada de 5 segundos.
- Evitar que el flujo registre el token completo o el secreto de firma.
- Registrar en este documento la decision tecnica sobre el mecanismo del
  token, con su fecha, impacto y alternativas.

## Non-goals

- Redisenar el modulo de autenticacion, la persistencia de usuarios o los
  endpoints ajenos a `POST /auth/login`.
- Cambiar el flujo de salud `GET /q/health`.
- Implementar autorizacion por roles, renovacion/revocacion de tokens o una
  migracion general de consumidores.
- Cambiar en este alcance el contrato de errores 500 ni eliminar la marca
  `debug`; son temas separados, aunque la exposicion actual de detalles de
  excepcion merece seguimiento conforme a `docs/GOOD_PRACTICES.md`.
- Definir una lista permitida de nombres de cliente: la spec solo requiere
  rechazar valores ausentes o en blanco. Cualquier restriccion adicional
  requiere una regla de negocio aprobada.

## Contratos protegidos

Se mantienen, salvo el cambio aprobado para `client` y el contenido interno
del token:

- `POST /auth/login` sigue siendo publico y en la misma URL.
- Un login correcto responde HTTP 200 e incluye `token`, `expiresIn` y `user`.
- `user` conserva `username`, `displayName` y `role`; tambien se conserva el
  campo actual `debug` en este cambio.
- Las solicitudes invalidas responden 400; las credenciales incorrectas,
  401; y los errores no controlados mantienen el estado 500.
- `client` pasa a ser obligatorio. Si falta, el mensaje indica que es
  requerido; si esta vacio o en blanco, indica que es invalido.
- `GET /q/health` no cambia.

El texto del token cambia intencionalmente y deja de ser el Base64 actual. No
se cambia el nombre del campo `token` en la respuesta. Como no se ha verificado
el comportamiento de consumidores fuera de este repositorio, la compatibilidad
con cualquier consumidor que interprete el formato antiguo debe comprobarse
antes del despliegue.

## Componentes afectados

- `AuthResource`: validar `client`, coordinar la emision del token y mantener
  los estados y la forma general de la respuesta.
- `LoginRequest`: contrato de entrada existente; no requiere redisenar el DTO,
  pero `client` pasa a ser obligatorio funcionalmente.
- `LegacyTokenUtil`: reemplazar la construccion Base64 por emision y firma del
  token estandar; el nombre de la clase puede ajustarse si mejora la claridad,
  sin ampliar el alcance.
- `application.properties` y configuracion de ejecucion: recibir el secreto
  de firma desde configuracion externa mediante `AUTH_TOKEN_SECRET`, sin
  valor secreto predeterminado dentro del codigo o del repositorio.
- `pom.xml`: incorporar una biblioteca JWT mantenida y compatible con la
  version de Java/Quarkus del proyecto; seleccionar y fijar su version durante
  la implementacion.
- `AuthResourceTest`: caracterizar los contratos existentes y agregar pruebas
  para las nuevas reglas, claims, expiracion y ausencia de datos sensibles en
  logs.

## Decisiones tecnicas y alternativas consideradas

### Decision registrada el 2026-10-21: usar JWT estandar firmado con HS256

Se adopta como direccion de implementacion una biblioteca JWT mantenida, con
firma HMAC-SHA-256 (HS256), y no una implementacion criptografica propia. La
clave se suministrara por `AUTH_TOKEN_SECRET`; no se reutilizara el secreto
hardcodeado actual ni se incluira el valor real en archivos, respuestas o logs.
La clave debe tener entropia y longitud adecuadas para HS256, y la aplicacion
no debe arrancar con una clave ausente o invalida.

El JWT incluira:

- `sub`: username del usuario autenticado.
- `client`: valor de cliente de la solicitud, sin normalizarlo, para que
  coincida con lo recibido.
- `iat`: instante de emision expresado en segundos Unix.
- `exp`: instante de expiracion expresado en segundos Unix, una hora despues
  de la emision.

`iat` y `exp` deben derivarse del mismo instante de emision. `expiresIn` se
calculara a partir de esos mismos instantes, sin mantener un segundo calculo
independiente. La prueba de contrato comprobara al recibir la respuesta que
`expiresIn` y `exp - now` difieren como maximo 5 segundos.

**Motivo e impacto:** la spec requiere claims identificables y la firma del
token. El mecanismo actual no cumple la firma y revela su secreto al
decodificarse. JWT proporciona un formato estandar para claims y una firma
verificable; usar una biblioteca evita inventar el formato o la criptografia.
La eleccion agrega una dependencia y exige operar una clave externa. Tambien
cambia el formato del valor `token`, aunque se preservan el endpoint y la
estructura general de respuesta.

**Alternativas consideradas:**

1. **Mantener el formato actual o endurecerlo.** Descartada: Base64 no firma
   datos y el secreto esta expuesto en el contenido; no satisface el requisito.
2. **Crear un formato propio firmado con HMAC.** Descartada: obligaria a
   mantener codificacion, validacion y compatibilidad propias, sin la
   interoperabilidad de un estandar.
3. **JWT con firma asimetrica.** No elegida para este alcance: agrega gestion
   de pares de claves y distribucion de clave publica sin un requisito actual
   que lo justifique. Podra reevaluarse si mas adelante varios servicios
   necesitan verificar tokens sin compartir el secreto.

La version concreta de la biblioteca y la politica de rotacion de claves se
cerraran antes de implementar; no cambian la decision de usar JWT/HS256.

### Decision sobre el valor de `client`

Para esta entrega, un `client` valido es un valor presente que no sea vacio ni
solo espacios. No se agrega una lista permitida ni una regla de formato no
definida por negocio. La comprobacion de blanco no debe alterar el valor que se
incluye en el claim, de modo que el claim conserve el valor recibido.

## Estrategia incremental

1. **Caracterizar el comportamiento actual.** Ejecutar y ampliar primero las
   pruebas del login, registrando que pasa hoy con `client` ausente o en
   blanco. Esos casos documentan el comportamiento anterior, pero su resultado
   esperado cambiara explicitamente a HTTP 400 al aplicar HU-01.
2. **Acordar la configuracion y la biblioteca.** Elegir una version mantenida,
   configurar una clave de prueba fuera del codigo y definir el fallo visible
   de configuracion ausente/invalida antes de emitir tokens.
3. **Aplicar la validacion de `client`.** Mantener los estados 400 existentes
   para `username` y `password` y agregar mensajes diferenciados para el
   cliente ausente o en blanco.
4. **Sustituir solo la emision del token.** Incorporar los cuatro claims y la
   firma; calcular `expiresIn` usando el mismo instante que `iat` y `exp`.
5. **Verificar logs y contratos.** Comprobar que no aparezcan el token
   completo, el nombre `AUTH_TOKEN_SECRET` ni su valor, y que se mantengan las
   respuestas protegidas.
6. **Revisar compatibilidad y documentacion.** Confirmar con los responsables
   del consumidor si alguien depende del formato antiguo, y actualizar la
   documentacion del endpoint tras acordar la transicion.

## Pruebas de caracterizacion antes del cambio critico

Antes de modificar validacion o emision, agregar o ejecutar pruebas que fijen
el comportamiento actual:

- Login valido: HTTP 200; existen `token`, `expiresIn`, `user` y `debug`, y
  `user` conserva sus campos actuales.
- Credenciales incorrectas: HTTP 401.
- Request nulo y `username`/`password` ausentes, nulos o en blanco: HTTP 400.
- `client` ausente y en blanco: capturar que hoy se aceptan en la ruta de login
  para evidenciar el cambio aprobado a HTTP 400.
- Respuesta exitosa: fijar los nombres y la estructura de los campos que no
  deben romperse.

Luego, las pruebas de HU-01 deben cubrir:

- Token validamente firmado y con `sub`, `client`, `iat` y `exp`; los valores
  de `sub` y `client` corresponden a la solicitud autenticada.
- `expiresIn` y expiracion dentro de la tolerancia de 5 segundos, sin depender
  de comparar segundos exactos ni de pausas arbitrarias.
- `client` ausente, nulo, vacio o en blanco con HTTP 400 y mensaje apropiado.
- Logs del login exitoso y fallido sin token completo ni secreto.
- Ausencia de una clave de firma valida: no se debe emitir un token con una
  clave insegura por defecto.

## Riesgos y trade-offs

- **Compatibilidad del token:** un consumidor podria interpretar el formato
  Base64 antiguo. Mitigacion: comprobar dependencias de consumidores antes del
  despliegue; el alcance actual no incluye inspeccionar ni modificar otros
  repositorios.
- **Configuracion de la clave:** una clave debil, compartida o faltante reduce
  la seguridad o impide emitir tokens. Mitigacion: configuracion externa,
  validacion de arranque y clave fuerte; no registrar su valor.
- **Dependencia JWT:** agrega mantenimiento y debe revisarse por compatibilidad
  y vulnerabilidades antes de adoptarla.
- **Tiempo de expiracion:** los segundos Unix y el tiempo de transporte pueden
  causar diferencias pequenas. Mitigacion: usar un unico instante de emision
  y aplicar la tolerancia especificada de 5 segundos en la verificacion.
- **Alcance limitado:** no se resuelven otros hallazgos del endpoint, como los
  detalles internos actualmente devueltos en errores 500; deben quedar como
  trabajo separado y no bloquearse silenciosamente dentro de HU-01.

## Rollback

Si el cambio rompe consumidores prioritarios o la autenticacion, se puede
revertir el despliegue de la aplicacion a la version anterior, que mantendra el
formato Base64 previo. Los JWT emitidos durante la version nueva tendran una
vida maxima de una hora; tras el rollback, los usuarios afectados deberan
autenticarse de nuevo cuando expire el token o limpiar su sesion.

La reversa no hace compatibles entre si ambos formatos. Si se confirma que un
consumidor necesita una transicion sin interrupcion, se debe acordar y probar
un plan de compatibilidad antes del despliegue, no introducirlo de forma
implicita en este cambio.
